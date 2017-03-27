package com.apin.rpc.idle;

import com.apin.common.utils.SystemClock;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;

import java.util.concurrent.TimeUnit;

/**
 * 主要用于检测连接是否处于空闲状态
 *
 * 基于{@link HashedWheelTimer}的空闲链路监测.
 *
 * 相比较Netty4.x的默认链路监测方式:
 *
 * Netty4.x默认的链路检测使用的是eventLoop的delayQueue, delayQueue是一个优先级队列, 复杂度为O(log n),
 * 每个worker处理自己的链路监测, 可能有助于减少上下文切换, 但是网络IO操作与idle会相互影响.
 *
 * 这个实现使用{@link HashedWheelTimer}的复杂度为O(1), 而且网络IO操作与idle不会相互影响, 但是有上下文切换.
 *
 * 如果连接数小, 比如几万以内, 可以直接用Netty4.x默认的链路检测{@link io.netty.handler.timeout.IdleStateHandler},
 * 如果连接数较大, 建议使用这个实现.
 */
public class IdleStateChecker extends ChannelDuplexHandler{

    private static final long MIN_TIMEOUT_MILLTS=1;

    private final HashedWheelTimer timer;

    private final long readerIdleTimeMillis;
    private final long writerIdleTimeMillis;
    private final long allIdleTimeMillis;

    private volatile int state;
    private volatile boolean reading;

    private volatile Timeout readerIdleTimeout;
    private volatile long lastReadTime;
    private boolean firstReaderIdleEvent=true;

    private volatile Timeout writerIdleTimeout;
    private volatile long lastWriteTime;
    private boolean firstWriterIdleEvent=true;

    private volatile Timeout allIdleTimeout;
    private boolean firstAllIdleEvent=true;

    public IdleStateChecker(HashedWheelTimer timer,int readIdleTimeSeconds,int writeIdleTimeSeconds,int allIdleTimeSeconds){
        this(timer,readIdleTimeSeconds,writeIdleTimeSeconds,allIdleTimeSeconds, TimeUnit.SECONDS);
    }

    public IdleStateChecker(HashedWheelTimer timer,long readerIdleTime,long writeIdleTime,long allIdleTime,TimeUnit unit){
        if(unit==null){
            throw new NullPointerException("unit");
        }
        this.timer=timer;
        if(readerIdleTime<=0){
            readerIdleTimeMillis=0;
        }else{
            readerIdleTimeMillis=Math.max(unit.toMillis(readerIdleTime),MIN_TIMEOUT_MILLTS);
        }

        if(writeIdleTime<=0){
            writerIdleTimeMillis=0;
        }else{
            writerIdleTimeMillis=Math.max(unit.toMillis(writeIdleTime),MIN_TIMEOUT_MILLTS);
        }

        if(allIdleTime<=0){
            allIdleTimeMillis=0;
        }else{
            allIdleTimeMillis=Math.max(unit.toMillis(allIdleTime),MIN_TIMEOUT_MILLTS);
        }

    }

    private final ChannelFutureListener writeListener=new ChannelFutureListener(){

        public void operationComplete(ChannelFuture channelFuture) throws Exception {
            firstWriterIdleEvent=firstAllIdleEvent=true;
            lastWriteTime= SystemClock.millisClock().now();
        }
    };

    public void handlerAdded(ChannelHandlerContext ctx) throws Exception{
        if(ctx.channel().isActive()&&ctx.channel().isRegistered()){
            initialize(ctx);
        }else{

        }
    }

    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception{
        destroy();
    }

    public void channelRegistered(ChannelHandlerContext ctx) throws Exception{
        if(ctx.channel().isActive()){
            initialize(ctx);
        }
        super.channelRegistered(ctx);
    }

    public void channelActive(ChannelHandlerContext ctx) throws Exception{
        initialize(ctx);
        super.channelActive(ctx);
    }

    public void channelInactive(ChannelHandlerContext ctx) throws Exception{
        destroy();
        super.channelInactive(ctx);
    }

    public void channelRead(ChannelHandlerContext ctx,Object msg) throws Exception{
        if(readerIdleTimeMillis>0||allIdleTimeMillis>0){
            firstReaderIdleEvent=firstAllIdleEvent=true;
            reading=true;
        }
        ctx.fireChannelRead(msg);
    }

    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception{
        if(readerIdleTimeMillis>0||allIdleTimeMillis>0){
            lastReadTime=SystemClock.millisClock().now();
            reading=false;
        }
        ctx.fireChannelReadComplete();
    }

    public void write(ChannelHandlerContext ctx,Object msg,ChannelPromise promise) throws Exception{
        if(writerIdleTimeMillis>0||allIdleTimeMillis>0){
            if(promise.isVoid()){
                firstWriterIdleEvent=firstAllIdleEvent=true;
                lastWriteTime=SystemClock.millisClock().now();
            }else{
                promise.addListener(writeListener);
            }
        }
        ctx.write(msg, promise);
    }

    private void initialize(ChannelHandlerContext ctx){
        switch (state){
            case 1:
            case 2:
                return;
        }
        state=1;
        lastReadTime=lastWriteTime=SystemClock.millisClock().now();
        if(readerIdleTimeMillis>0){
            readerIdleTimeout=timer.newTimeout(new ReaderIdleTimeoutTask(ctx),readerIdleTimeMillis, TimeUnit.MILLISECONDS);
        }
        if(writerIdleTimeMillis>0){
            writerIdleTimeout=timer.newTimeout(new WriterIdleTimeoutTask(ctx),writerIdleTimeMillis,TimeUnit.MILLISECONDS);
        }
        if(allIdleTimeMillis>0){
            allIdleTimeout=timer.newTimeout(new AllIdleTimeoutTask(ctx),allIdleTimeMillis,TimeUnit.MILLISECONDS);
        }
    }

    private void destroy(){
        state=2;

        if(readerIdleTimeout!=null){
            readerIdleTimeout.cancel();
            readerIdleTimeout=null;
        }

        if(writerIdleTimeout!=null){
            writerIdleTimeout.cancel();
            writerIdleTimeout=null;
        }

        if(allIdleTimeout!=null){
            allIdleTimeout.cancel();
            allIdleTimeout=null;
        }
    }

    protected void channelIdle(ChannelHandlerContext ctx,IdleStateEvent evt) throws Exception{
        ctx.fireUserEventTriggered(evt);
    }

    private final class ReaderIdleTimeoutTask implements TimerTask{

        private final ChannelHandlerContext ctx;

        ReaderIdleTimeoutTask(ChannelHandlerContext ctx){
            this.ctx=ctx;
        }

        public void run(Timeout timeout) throws Exception {
            if(timeout.isCancelled()||!ctx.channel().isOpen()){
                return;
            }

            long lastReadTime=IdleStateChecker.this.lastReadTime;
            long nextDelay=readerIdleTimeMillis;
            if(!reading){
                nextDelay-=SystemClock.millisClock().now()-lastReadTime;
            }

            if(nextDelay<=0){
                readerIdleTimeout=timer.newTimeout(this,readerIdleTimeMillis,TimeUnit.MILLISECONDS);
                try{
                    IdleStateEvent event;
                    if(firstReaderIdleEvent){
                        firstReaderIdleEvent=false;
                        event=IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT;
                    }else{
                        event=IdleStateEvent.READER_IDLE_STATE_EVENT;
                    }
                    channelIdle(ctx,event);
                }catch (Throwable e){
                    ctx.fireExceptionCaught(e);
                }
            }
        }
    }


    private final class WriterIdleTimeoutTask implements TimerTask {
        private final ChannelHandlerContext ctx;

        WriterIdleTimeoutTask(ChannelHandlerContext ctx){
            this.ctx=ctx;
        }

        public void run(Timeout timeout) throws Exception {
            if(timeout.isCancelled()||!ctx.channel().isOpen()){
                return;
            }
            long lastWriteTime=IdleStateChecker.this.lastWriteTime;
            long nextDelay=writerIdleTimeMillis-(SystemClock.millisClock().now()-lastWriteTime);
            if(nextDelay<=0){
                writerIdleTimeout=timer.newTimeout(this,writerIdleTimeMillis,TimeUnit.MILLISECONDS);
                try{
                    IdleStateEvent event;
                    if(firstWriterIdleEvent){
                        firstWriterIdleEvent=false;
                        event=IdleStateEvent.FIRST_WRITER_IDLE_STATE_EVENT;
                    }else{
                        event=IdleStateEvent.WRITER_IDLE_STATE_EVENT;
                    }
                    channelIdle(ctx,event);
                }catch (Throwable e){
                    ctx.fireExceptionCaught(e);
                }
            }else{
                writerIdleTimeout=timer.newTimeout(this,nextDelay,TimeUnit.MILLISECONDS);
            }
        }
    }

    private final class AllIdleTimeoutTask implements TimerTask{

        private final ChannelHandlerContext ctx;

        AllIdleTimeoutTask(ChannelHandlerContext ctx){
            this.ctx=ctx;
        }

        public void run(Timeout timeout) throws Exception {
            if(timeout.isCancelled()||!ctx.channel().isOpen()){
                return;
            }

            long nextDelay=allIdleTimeMillis;
            if(!reading){
                long lastIoTime=Math.max(lastReadTime,lastWriteTime);
                nextDelay-=SystemClock.millisClock().now()-lastIoTime;
            }

            if(nextDelay<=0){
                allIdleTimeout=timer.newTimeout(this,allIdleTimeMillis,TimeUnit.MILLISECONDS);
                try{
                    IdleStateEvent event;
                    if(firstAllIdleEvent){
                        firstAllIdleEvent=false;
                        event=IdleStateEvent.FIRST_ALL_IDLE_STATE_EVENT;
                    }else{
                        event=IdleStateEvent.ALL_IDLE_STATE_EVENT;
                    }
                    channelIdle(ctx,event);
                }catch (Throwable e){
                    ctx.fireExceptionCaught(e);
                }
            }else{
                allIdleTimeout=timer.newTimeout(this,nextDelay,TimeUnit.MILLISECONDS);
            }
        }
    }


    public HashedWheelTimer getTimer() {
        return timer;
    }

    public long getReaderIdleTimeMillis() {
        return readerIdleTimeMillis;
    }

    public long getWriterIdleTimeMillis() {
        return writerIdleTimeMillis;
    }

    public long getAllIdleTimeMillis() {
        return allIdleTimeMillis;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public boolean isReading() {
        return reading;
    }

    public void setReading(boolean reading) {
        this.reading = reading;
    }

    public Timeout getReaderIdleTimeout() {
        return readerIdleTimeout;
    }

    public void setReaderIdleTimeout(Timeout readerIdleTimeout) {
        this.readerIdleTimeout = readerIdleTimeout;
    }

    public long getLastReadTime() {
        return lastReadTime;
    }

    public void setLastReadTime(long lastReadTime) {
        this.lastReadTime = lastReadTime;
    }

    public boolean isFirstReaderIdleEvent() {
        return firstReaderIdleEvent;
    }

    public void setFirstReaderIdleEvent(boolean firstReaderIdleEvent) {
        this.firstReaderIdleEvent = firstReaderIdleEvent;
    }

    public Timeout getWriterIdleTimeout() {
        return writerIdleTimeout;
    }

    public void setWriterIdleTimeout(Timeout writerIdleTimeout) {
        this.writerIdleTimeout = writerIdleTimeout;
    }

    public long getLastWriteTime() {
        return lastWriteTime;
    }

    public void setLastWriteTime(long lastWriteTime) {
        this.lastWriteTime = lastWriteTime;
    }

    public boolean isFirstWriterIdleEvent() {
        return firstWriterIdleEvent;
    }

    public void setFirstWriterIdleEvent(boolean firstWriterIdleEvent) {
        this.firstWriterIdleEvent = firstWriterIdleEvent;
    }

    public Timeout getAllIdleTimeout() {
        return allIdleTimeout;
    }

    public void setAllIdleTimeout(Timeout allIdleTimeout) {
        this.allIdleTimeout = allIdleTimeout;
    }

    public boolean isFirstAllIdleEvent() {
        return firstAllIdleEvent;
    }

    public void setFirstAllIdleEvent(boolean firstAllIdleEvent) {
        this.firstAllIdleEvent = firstAllIdleEvent;
    }


}
