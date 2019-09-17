package com.wang.client;

import com.wang.clienthandler.DiscardClientHandler;
import com.wang.clienthandler.FirstClientHandler;
import com.wang.clienthandler.TimeClientHandler;
import com.wang.server.DiscardServer;
import com.wang.serverhandler.TimeServerHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;

/**
 * @Author: wangliujie
 * @Date: 2019/5/29 11:40
 */
public final class DiscardClient {

    public void connect(String host,int port) throws Exception{
        //配置客户端nio线程组
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY,true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new FirstClientHandler());
//                            .addLast(new FixedLengthFrameDecoder(7));
                        }
                    });

            // Make the connection attempt.
            ChannelFuture f = b.connect(host, port).sync();

            // Wait until the connection is closed.
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 8888;
        if (args !=null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            }catch (NumberFormatException e){
                //
                port = 8888;
            }

        }
        new DiscardClient().connect("127.0.0.1",8888);
    }
}
