/* Licensed to the xFB-RPC under one or more
 * contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The xFB-RPC licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ysc.rpc.server;

import com.ysc.api.UserService;
import com.ysc.rpc.RpcDecoder;
import com.ysc.rpc.RpcEncoder;
import com.ysc.rpc.server.handler.RpcRequestHandler;
import com.ysc.rpc.server.impl.UserServiceImpl;
import com.ysc.rpc.server.registry.ServerRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcServer {

  private static final Logger LOGGER = LoggerFactory.getLogger(RpcServer.class);

  private final int port;

  private EventLoopGroup bossGroup;
  private EventLoopGroup workerGroup;
  private Channel serverChannel;

  private volatile boolean started = false;

  /** LoggingHandler is a Netty handler that logs all events for debugging purposes. */
  private static final LoggingHandler LOGGING_HANDLER = new LoggingHandler();

  /**
   * RpcRequestHandler is a custom Netty handler that processes incoming RPC requests and generates
   * responses.
   */
  private static final RpcRequestHandler RPC_REQUEST_HANDLER = new RpcRequestHandler();

  public RpcServer(final int port) {
    this.port = port;
  }

  public synchronized void start() {
    if (started) {
      return;
    }

    final boolean epoll = Epoll.isAvailable();
    bossGroup =
        epoll
            ? new EpollEventLoopGroup(1)
            : new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);
    workerGroup =
        epoll
            ? new EpollEventLoopGroup(1)
            : new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);

    final ServerBootstrap bootstrap = new ServerBootstrap();
    bootstrap
        .group(bossGroup, workerGroup)
        .channel(epoll ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
        .option(ChannelOption.SO_BACKLOG, 1024)
        .childOption(ChannelOption.TCP_NODELAY, true)
        .childOption(ChannelOption.SO_KEEPALIVE, true)
        .childHandler(
            new ChannelInitializer<SocketChannel>() {
              @Override
              protected void initChannel(final SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4));
                ch.pipeline().addLast(new RpcDecoder());
                ch.pipeline().addLast(new LengthFieldPrepender(4));
                ch.pipeline().addLast(new RpcEncoder());
                ch.pipeline().addLast(LOGGING_HANDLER);
                ch.pipeline().addLast(RPC_REQUEST_HANDLER);
              }
            });

    final ChannelFuture future = bootstrap.bind(port);

    ServerRegistry.registerService(UserService.class, new UserServiceImpl());

    future.addListener(
        (ChannelFutureListener)
            channelFuture -> {
              if (channelFuture.isSuccess()) {
                serverChannel = channelFuture.channel();
                started = true;
                LOGGER.info("RPC server started and listening on port {}", port);
              } else {
                LOGGER.warn(
                    "Failed to bind to port {}, cause: {}",
                    port,
                    channelFuture.cause().getMessage());
                throw new RuntimeException("Failed to bind to port " + port, channelFuture.cause());
              }
            });
  }

  public synchronized void stop() {
    if (!started) {
      return;
    }

    if (serverChannel != null) {
      serverChannel.close();
    }

    bossGroup.shutdownGracefully();
    workerGroup.shutdownGracefully();

    started = false;
  }
}
