package com.wang.websocket.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.soap.Text;
import java.util.Date;

import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;

/**
 * @Author: wangliujie
 * @Date: 2019/9/17 16:24
 */
public class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {
    private int counter;
    private WebSocketServerHandshaker handshaker;
    private static final Logger logger = LoggerFactory.getLogger(WebSocketServerHandler.class);

    /*@Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String body = (String) msg;
        *//*ByteBuf buf =(ByteBuf)msg;
        byte[] req = new byte[buf.readableBytes()];
        buf.readBytes(req);
        String body = new String(req,"UTF-8")
                .substring(0,req.length-System.getProperty("line.separator").length());*//*
        System.out.println("The TimeServer receive order: "+body+"; the counter is:"+(++counter));
        //对于windows,换行符，即System.getProperty("line.separator")值为\r\n
        //如果接受到的body是“QUERY TIME ORDER”，就将当前时间+换行符发送给客户端，否则发送"BAD ORDER"+换行符给客户端
        String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body)?new Date(System.currentTimeMillis()).toString():"BAD ORDER";
        currentTime = currentTime +System.getProperty("line.separator");
        ByteBuf resp = Unpooled.copiedBuffer(currentTime.getBytes());
        ctx.writeAndFlush(resp);
    }*/

    @Override
    protected void messageReceived(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        if(o instanceof FullHttpRequest){
            handlerHttpRequest(channelHandlerContext,(FullHttpRequest) o);
        }else if( o instanceof WebSocketFrame){
            handlerWebSocketFrame(channelHandlerContext,(WebSocketFrame) o);
        }
    }

    private void handlerWebSocketFrame(ChannelHandlerContext channelHandlerContext, WebSocketFrame frame) {
        //是否是关闭链路指令
        if(frame instanceof CloseWebSocketFrame){
            handshaker.close(channelHandlerContext.channel(),((CloseWebSocketFrame) frame).retain());
            return;
        }
        //是否是Ping消息
        if(frame instanceof PingWebSocketFrame){
            channelHandlerContext.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        //本例子只支持文本消息
        if(!(frame instanceof TextWebSocketFrame)){
            throw new UnsupportedOperationException(String.format("%s frame types not supported",frame.getClass().getName()));
        }
        //返回应答消息
        String request = ((TextWebSocketFrame) frame).text();
        logger.info("{} received {}",channelHandlerContext.channel(),request);
        channelHandlerContext.channel().write(new TextWebSocketFrame(request+" 欢迎使用WebSocket服务，现在时间是："+new Date().toString()));


    }

    private void handlerHttpRequest(ChannelHandlerContext channelHandlerContext, FullHttpRequest req) {
        if(!req.getDecoderResult().isSuccess()
                ||!"websocket".equals(req.headers().get("Upgrade"))){
            sendHttpResponse(channelHandlerContext,req,new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.BAD_REQUEST));
            return;
        }
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory("ws://localhost:8888/websocket",null,false);
        handshaker = wsFactory.newHandshaker(req);
        if(handshaker == null){
            WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(channelHandlerContext.channel());
        }else{
            handshaker.handshake(channelHandlerContext.channel(),req);
        }
    }

    private void sendHttpResponse(ChannelHandlerContext channelHandlerContext, FullHttpRequest req, DefaultFullHttpResponse resp) {
        if(resp.getStatus().code()!=200){
            ByteBuf buf = Unpooled.copiedBuffer(resp.getStatus().toString(),CharsetUtil.UTF_8);
            resp.content().writeBytes(buf);
            buf.release();
            setContentLength(resp,resp.content().readableBytes());
        }
        //如果是非Keep-alive，关闭连接
        ChannelFuture future = channelHandlerContext.channel().writeAndFlush(resp);
        if(!isKeepAlive(req)||resp.getStatus().code()!=200){
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
