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
import com.ysc.rpc.request.RpcRequest;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
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
import java.lang.reflect.Proxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ClientNode {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClientNode.class);

  protected static final String REGISTER_CENTER_HOST = "localhost";
  protected static final int REGISTER_CENTER_PORT = 9000;

  /**
   * optional service name for logging or future use, can be set to a default value or left empty
   */
  protected final String serviceId;

  /**
   * timeout for RPC calls in milliseconds, used to prevent hanging if the server does not respond
   */
  protected final long timeoutMillis = 5000;

  /** Netty components for managing the client connection */
  protected EventLoopGroup group;

  protected Bootstrap bootstrap;

  /** flag to indicate if the client has been started, used to prevent multiple starts/stops */
  protected volatile boolean started = false;

  /** shared logging handler instance, can be reused across channels since it's stateless */
  protected static final LoggingHandler LOGGING_HANDLER = new LoggingHandler();

  protected ClientNode(final String serviceId) {
    this.serviceId = serviceId;
  }

  public void start() {
    if (started) {
      LOGGER.warn("Client is already started");
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

                addOtherHandlers(ch.pipeline());
              }
            });

    started = true;
  }

  protected abstract void addOtherHandlers(ChannelPipeline pipeline);

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
   * @param <T> the type of the service interface, e.g., UserService
   * @return a proxy instance that implements the service interface, and sends RPC requests to the
   *     server when methods are invoked on the proxy instance
   * @throws IllegalStateException if the client is not connected to the server, or if the channel
   *     is not active
   * @throws RuntimeException if the RPC call fails, with the cause of the failure included in the
   *     exception message
   */
  @SuppressWarnings("unchecked")
  public <T> T getServiceProxy(final Class<T> serviceClass) {
    return (T)
        Proxy.newProxyInstance(
            serviceClass.getClassLoader(),
            new Class<?>[] {serviceClass},
            (proxy, method, args) -> {
              final RpcRequest request =
                  new RpcRequest(
                      "server",
                      serviceClass.getName(),
                      method.getName(),
                      method.getReturnType(),
                      method.getParameterTypes(),
                      args);

              try {
                return send(request);
              } catch (final Throwable e) {
                LOGGER.warn(
                    "RPC call failed for method: {}.{}",
                    serviceClass.getName(),
                    method.getName(),
                    e);
                throw new RuntimeException(
                    "RPC call failed for method: "
                        + serviceClass.getName()
                        + "."
                        + method.getName(),
                    e);
              }
            });
  }

  protected abstract Object send(RpcRequest request) throws Throwable;
}
