package com.wang.httpfile.server;

import com.wang.demo2.server.TimeServerHander;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * @Author: wangliujie
 * @Date: 2019/9/18 11:01
 */
public class ServerApplication {
    public static final String DEFAULT_URL = "/src/com/phei/netty/";

    public void run(final int port ,final String url) throws Exception {
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
                    ch.pipeline().addLast("http-decoder",new HttpRequestDecoder());
                    ch.pipeline().addLast("http-aggregator",new HttpObjectAggregator(65535));
                    ch.pipeline().addLast("http-encoder",new HttpResponseEncoder());
                    ch.pipeline().addLast("http-chunked",new ChunkedWriteHandler());
                    ch.pipeline().addLast("fileServerHandler",new FileServerHandler());
                }
            });
            //绑定端口，等待连接
            ChannelFuture f = b.bind("127.0.0.1",port).sync();
            System.out.println("Http 文件目录服务器启动，网址是："+"http://127.0.0.1:"+port+url);

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
        String url = DEFAULT_URL;
        new ServerApplication().run(port,url);
    }
}
