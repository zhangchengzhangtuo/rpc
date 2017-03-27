package com.apin.rpc.server;

import com.apin.common.exception.RemotingSendRequestException;
import com.apin.common.exception.RemotingTimeoutException;
import com.apin.common.utils.EpollNativeSupport;
import com.apin.common.utils.NamedThreadFactory;
import com.apin.common.utils.Pair;
import com.apin.rpc.NettyChannelInactiveProcessor;
import com.apin.rpc.NettyRemotingBase;
import com.apin.rpc.NettyRequestProcessor;
import com.apin.rpc.RPCHook;
import com.apin.rpc.code.RemotingTransporterDecoder;
import com.apin.rpc.code.RemotingTransporterEncoder;
import com.apin.rpc.idle.AcceptorIdleStateTrigger;
import com.apin.rpc.idle.IdleStateChecker;
import com.apin.rpc.model.RemotingTransporter;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.internal.PlatformDependent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static com.apin.common.utils.Constants.*;
/**
 * NettyServer 和 NettyClient 由于身份不同所以导致启动方式不同，但是他们的业务处理其实是一样的，也就是都可以接受请求和接受应答
 */
public class NettyRemotingServer extends NettyRemotingBase implements RemotingServer{

    private static final Logger logger= LoggerFactory.getLogger(NettyRemotingServer.class);

    private ServerBootstrap serverBootstrap;

    private EventLoopGroup boss;
    private EventLoopGroup worker;

    private int workerNum;

    private int writeBufferLowWaterMark;
    private int writeBufferHighWatermark;

    protected final HashedWheelTimer timer=new HashedWheelTimer(new NamedThreadFactory("netty.acceptor.timer"));

    protected volatile ByteBufAllocator allocator;

    private final NettyServerConfig nettyServerConfig;

    private DefaultEventExecutorGroup defaultEventExecutorGroup;

    private final ExecutorService publicExecutor;

    private final AcceptorIdleStateTrigger acceptorIdleStateTrigger=new AcceptorIdleStateTrigger();

    private RPCHook rpcHook;

    public NettyRemotingServer(){
        this(new NettyServerConfig());
    }

    public NettyRemotingServer(NettyServerConfig nettyServerConfig){
        this.nettyServerConfig=nettyServerConfig;
        if(null!=nettyServerConfig){
            workerNum=nettyServerConfig.getServerWorkerThreads();
            writeBufferLowWaterMark=nettyServerConfig.getWriteBufferLowWaterMark();
            writeBufferHighWatermark=nettyServerConfig.getWriteBufferHighWaterMark();
        }
        this.publicExecutor= Executors.newFixedThreadPool(4, new ThreadFactory() {
            private AtomicInteger threadIndex=new AtomicInteger(0);
            public Thread newThread(Runnable r) {
                return new Thread(r,"NettyServerPublicExecutor_"+this.threadIndex.incrementAndGet());
            }
        });
        init();
    }

    private EventLoopGroup initEventLoopGroup(int workers,ThreadFactory bossFactory){
        return isNativeEt()?new EpollEventLoopGroup(workers,bossFactory):new NioEventLoopGroup(workers,bossFactory);
    }

    private boolean isNativeEt(){
        return EpollNativeSupport.isSupportNativeET();
    }

    public void init() {
        ThreadFactory bossFactory=new DefaultThreadFactory("netty.boss");
        ThreadFactory workerFactory=new DefaultThreadFactory("netty.worker");
        boss=initEventLoopGroup(1,bossFactory);
        if(workerNum<=0){
            workerNum=Runtime.getRuntime().availableProcessors()<<1;
        }
        worker=initEventLoopGroup(workerNum, workerFactory);
        serverBootstrap=new ServerBootstrap().group(boss, worker);
        allocator=new PooledByteBufAllocator(PlatformDependent.directBufferPreferred());
        serverBootstrap.childOption(ChannelOption.ALLOCATOR,allocator).childOption(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT);
        if(boss instanceof EpollEventLoopGroup){
            ((EpollEventLoopGroup)boss).setIoRatio(100);
        }else if(boss instanceof NioEventLoopGroup){
            ((NioEventLoopGroup)boss).setIoRatio(100);
        }

        if(worker instanceof EpollEventLoopGroup){
            ((EpollEventLoopGroup)worker).setIoRatio(100);
        }else if(worker instanceof NioEventLoopGroup){
            ((NioEventLoopGroup)worker).setIoRatio(100);
        }

        serverBootstrap.option(ChannelOption.SO_BACKLOG,32768);
        serverBootstrap.option(ChannelOption.SO_REUSEADDR,true);

        serverBootstrap.childOption(ChannelOption.SO_REUSEADDR,true)
                .childOption(ChannelOption.SO_KEEPALIVE,true)
                .childOption(ChannelOption.TCP_NODELAY,true)
                .childOption(ChannelOption.ALLOW_HALF_CLOSURE, false);

        if(writeBufferLowWaterMark>=0&&writeBufferHighWatermark>0){
            WriteBufferWaterMark waterMark=new WriteBufferWaterMark(writeBufferLowWaterMark,writeBufferHighWatermark);
            serverBootstrap.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,waterMark);
        }
    }

    public void start() {
        this.defaultEventExecutorGroup=new DefaultEventExecutorGroup(AVALIABLE_PROCESSORS, new ThreadFactory() {

            private AtomicInteger threadIndex=new AtomicInteger(0);

            public Thread newThread(Runnable r) {
                return new Thread(r,"NettyServerWorkerThread_"+this.threadIndex.incrementAndGet());
            }
        });
        if(isNativeEt()){
            serverBootstrap.channel(EpollServerSocketChannel.class);
        }else{
            serverBootstrap.channel(NioServerSocketChannel.class);
        }
        serverBootstrap.localAddress(new InetSocketAddress(this.nettyServerConfig.getListenPort())).childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                socketChannel.pipeline().addLast(
                        defaultEventExecutorGroup,
                        new IdleStateChecker(timer, READER_IDLE_TIME_SECONDS, 0, 0),
                        acceptorIdleStateTrigger,
                        new RemotingTransporterDecoder(),
                        new RemotingTransporterEncoder(),
                        new NettyServerHandler());
            }
        });
        try{
            logger.info("netty bind [{}] serverBootstrap start...",this.nettyServerConfig.getListenPort());
            this.serverBootstrap.bind().sync();
            logger.info("netty start success at port [{}]",this.nettyServerConfig.getListenPort());
        }catch (InterruptedException e){
            logger.error("start serverBootrap exception [{}]",e.getMessage());
            throw new RuntimeException("this.serverBootrap.bind().sync() InterruptedException",e);
        }
    }


    public void shutdown() {
        try{
            if(this.timer!=null){
                this.timer.stop();
            }

            this.boss.shutdownGracefully();
            this.worker.shutdownGracefully();
            if(this.defaultEventExecutorGroup!=null){
                this.defaultEventExecutorGroup.shutdownGracefully();
            }
        }catch (Exception e){
            logger.error("NettyRemotingServer shutdown exception,",e);
        }

        if(this.publicExecutor!=null){
            try{
                this.publicExecutor.shutdown();
            }catch (Exception e){
                logger.error("NettyRemotingServer shutdown exception,",e);
            }
        }
    }

    public void registerRPCHook(RPCHook rpcHook) {
        this.rpcHook=rpcHook;
    }

    public void registerProcessor(byte requestCode, NettyRequestProcessor processor, ExecutorService executorService) {
        ExecutorService executors=executorService;
        if(null==executorService){
            executors=this.publicExecutor;
        }
        Pair<NettyRequestProcessor,ExecutorService> pair=new Pair<NettyRequestProcessor,ExecutorService>(processor,executors);
        this.processorTable.put(requestCode,pair);
    }


    public void registerDefaultProcessor(NettyRequestProcessor processor, ExecutorService executor) {
        this.defaultRequestProcessor=new Pair<NettyRequestProcessor, ExecutorService>(processor,executor);
    }

    public void registerChannelInactiveProcessor(NettyChannelInactiveProcessor processor, ExecutorService executorService) {
        if(executorService==null){
            executorService=super.publicExecutor;
        }
        this.defaultChannelInactiveProcessor=new Pair<NettyChannelInactiveProcessor, ExecutorService>(processor,executorService);
    }

    public Pair<NettyRequestProcessor,ExecutorService> getProcessorPair(int reqeustCode){
        return processorTable.get(reqeustCode);
    }

    @Override
    protected RPCHook getRPCHook() {
        return rpcHook;
    }

    public RemotingTransporter invokeSync(Channel channel, RemotingTransporter request, long timeoutMs) throws InterruptedException, RemotingSendRequestException, RemotingTimeoutException {
        return super.invokeSyncImpl(channel,request,timeoutMs);
    }


    /**
     * 这两个方法存在于在NettyRemotingBase类，其实就是将这两个方法抽取出来了，因为客户端和服务端都会使用方法
     */
    class NettyServerHandler extends SimpleChannelInboundHandler<RemotingTransporter>{

        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, RemotingTransporter remotingTransporter) throws Exception {
            processMessageReceived(channelHandlerContext,remotingTransporter);
        }

        public void channelInactive(ChannelHandlerContext ctx) throws Exception{
            processChannelInactive(ctx);
        }
    }

}
