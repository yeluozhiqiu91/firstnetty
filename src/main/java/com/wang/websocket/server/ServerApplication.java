package com.wang.websocket.server;

import com.wang.httpfile.server.FileServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * @Author: wangliujie
 * @Date: 2019/9/18 11:42
 */
public class ServerApplication {

    public void run(final int port) throws Exception {
        //配置服务端的nio线程组
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer <SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast("http-codec",new HttpServerCodec());
                    pipeline.addLast("http-aggregator",new HttpObjectAggregator(65536));
                    pipeline.addLast("http-chunked",new ChunkedWriteHandler());
                    pipeline.addLast("handler",new WebSocketServerHandler());
                }
            });
            //绑定端口，等待连接
            ChannelFuture f = b.bind(port).sync();
            System.out.println("Web socket started at port:"+port);
            System.out.println("Open your browse at http://localhost:"+port+"/");

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            //等待服务器监听端口关闭
            f.channel().closeFuture().sync();
        } finally {
            //优雅退出，释放线程池资源
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        int port;
        if (args !=null && args.length > 0) {
            port = Integer.parseInt(args[0]);
        } else {
            port = 8888;
        }
        new ServerApplication().run(port);
    }
}
