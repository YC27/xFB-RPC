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
package com.ysc.rpc.netty;

import com.ysc.rpc.codec.RpcDecoder;
import com.ysc.rpc.codec.RpcEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
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

public abstract class ServerNode {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServerNode.class);

  protected final String serviceId;
  protected final int port;

  private EventLoopGroup bossGroup;
  private EventLoopGroup workerGroup;
  protected Channel serverChannel;

  private volatile boolean started = false;

  /** LoggingHandler is a Netty handler that logs all events for debugging purposes. */
  private static final LoggingHandler LOGGING_HANDLER = new LoggingHandler();

  public ServerNode(final String serviceId, final int port) {
    this.serviceId = serviceId;
    this.port = port;
  }

  public void start() {
    if (started) {
      LOGGER.warn("Server {} is already started", serviceId);
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

                addOtherHandlers(ch.pipeline());
              }
            });

    final ChannelFuture future = bootstrap.bind(port);

    future.addListener(
        (ChannelFutureListener)
            channelFuture -> {
              if (channelFuture.isSuccess()) {
                serverChannel = channelFuture.channel();
                started = true;
                LOGGER.info(
                    "RPC server started and listening on port {}, serviceId {}", port, serviceId);

                doSomeOtherInitialization();
              } else {
                LOGGER.warn(
                    "Failed to bind to port {}, cause: {}",
                    port,
                    channelFuture.cause().getMessage());
                throw new RuntimeException("Failed to bind to port " + port, channelFuture.cause());
              }
            });
  }

  protected abstract void addOtherHandlers(final ChannelPipeline pipeline);

  protected abstract void doSomeOtherInitialization();

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
