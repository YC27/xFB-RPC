/* Licensed to the xFB-RPC under one or more
                                    * contributor license agreements. See the NOTICE file
                                    * distributed with this work for additional information
                                    * regarding copyright ownership. The xFB-RPC licenses this file
                                    * to you under the Apache License, Version 2.0 (the
                                    * "License"); you may not use this file except in compliance
                                    * with the License. You may obtain a copy of the License at
                                    *
                                    * http://www.apache.org/licenses/LICENSE-2.0
                                    *
                                    * Unless required by applicable law or agreed to in writing, software
                                    * distributed under the License is distributed on an "AS IS" BASIS,
                                    * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
                                    * See the License for the specific language governing permissions and
                                    * limitations under the License.
                                    */
package com.ysc.rpc.client;

import com.ysc.rpc.RpcDecoder;
import com.ysc.rpc.RpcEncoder;
import com.ysc.rpc.RpcRequest;
import com.ysc.rpc.client.handler.RpcResponseHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultPromise;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(RpcClient.class);

  /** host and port of the RPC server to connect to */
  private final String host;

  private final int port;

  /** Netty components for managing the client connection */
  private EventLoopGroup group;

  private Bootstrap bootstrap;
  private volatile Channel channel;

  /** flag to indicate if the client has been started, used to prevent multiple starts/stops */
  private volatile boolean started = false;

  /** shared logging handler instance, can be reused across channels since it's stateless */
  private static final LoggingHandler LOGGING_HANDLER = new LoggingHandler();

  /**
   * shared RPC response handler instance, can be reused across channels since it uses a concurrent
   * map for promises
   */
  private static final RpcResponseHandler RPC_RESPONSE_HANDLER = new RpcResponseHandler();

  public RpcClient(final String host, final int port) {
    this.host = host;
    this.port = port;
  }

  public synchronized void start() {
    if (started) {
      return;
    }

    final boolean epoll = Epoll.isAvailable();

    group =
        epoll
            ? new EpollEventLoopGroup(1)
            : new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);
    bootstrap = new Bootstrap();

    bootstrap
        .group(group)
        .channel(epoll ? EpollSocketChannel.class : NioSocketChannel.class)
        .option(ChannelOption.TCP_NODELAY, true)
        .handler(
            new ChannelInitializer<SocketChannel>() {
              @Override
              protected void initChannel(final SocketChannel ch) {
                ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4));
                ch.pipeline().addLast(new RpcDecoder());
                ch.pipeline().addLast(new LengthFieldPrepender(4));
                ch.pipeline().addLast(new RpcEncoder());
                ch.pipeline().addLast(LOGGING_HANDLER);
                ch.pipeline().addLast(RPC_RESPONSE_HANDLER);
              }
            });

    doConnect();

    started = true;
  }

  private void doConnect() {
    try {
      final ChannelFuture future = bootstrap.connect(host, port).sync();
      channel = future.channel();
      LOGGER.info("Connected to server {}:{}", host, port);

      channel
          .closeFuture()
          .addListener(
              (ChannelFutureListener)
                  closeFuture -> {
                    LOGGER.warn("Connection to server closed, reconnecting...");
                    channel = null;
                    scheduleReconnect();
                  });
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      LOGGER.warn("Failed to connect to server {}:{}", host, port, e);
      scheduleReconnect();
    }
  }

  private void scheduleReconnect() {
    LOGGER.info("Reconnecting to server in 5 seconds...");
    group.schedule(this::doConnect, 5, TimeUnit.SECONDS);
  }

  public Channel getChannel() {
    if (channel != null && channel.isActive()) {
      return channel;
    }

    LOGGER.warn("Channel is not active. Please ensure the client is connected.");
    throw new IllegalStateException(
        "Channel is not active. Please ensure the client is connected.");
  }

  public synchronized void stop() {
    if (!started) {
      return;
    }

    if (channel != null) {
      channel.close();
    }

    group.shutdownGracefully();
    started = false;
  }

  /**
   * create a dynamic proxy for the given service interface, which will send RPC requests to the
   * server when methods are invoked on the proxy instance. the proxy will block until a response is
   * received from the server, and return the result or throw an exception if the RPC call failed.
   * the proxy uses the RpcResponseHandler to manage the promises for pending RPC requests, and the
   * RpcRequest class to encapsulate the details of the RPC request. the proxy also uses the channel
   * to send the RPC request to the server, and waits for the response using a DefaultPromise. if
   * the RPC call is successful, the result is returned to the caller. if the RPC call fails, an
   * exception is thrown
   *
   * @param serviceClass the service interface class, e.g., UserService.class
   * @return a proxy instance that implements the service interface, and sends RPC requests to the
   *     server when methods are invoked on the proxy instance
   * @param <T> the type of the service interface, e.g., UserService
   * @throws IllegalStateException if the client is not connected to the server, or if the channel
   *     is not active
   * @throws RuntimeException if the RPC call fails, with the cause of the failure included in the
   *     exception message
   */
  public <T> T getServiceProxy(final Class<T> serviceClass) {
    final ClassLoader loader = serviceClass.getClassLoader();
    final Class<?>[] interfaces = new Class<?>[] {serviceClass};

    final Object o =
        Proxy.newProxyInstance(
            loader,
            interfaces,
            (proxy, method, args) -> {
              final RpcRequest request =
                  new RpcRequest(
                      serviceClass.getName(),
                      method.getName(),
                      method.getReturnType(),
                      method.getParameterTypes(),
                      args);

              final DefaultPromise<Object> promise = new DefaultPromise<>(getChannel().eventLoop());
              RpcResponseHandler.PROMISES.put(request.getRequestId(), promise);
              getChannel().writeAndFlush(request);

              promise.awaitUninterruptibly();

              if (promise.isSuccess()) {
                return promise.getNow();
              } else {
                LOGGER.warn("RPC call failed for request: {}", request, promise.cause());
                throw new RuntimeException(promise.cause());
              }
            });

    return (T) o;
  }
}
