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

import com.ysc.entity.ServiceInstance;
import com.ysc.rpc.handler.GetInstanceResponseHandler;
import com.ysc.rpc.handler.RegisterResponseHandler;
import com.ysc.rpc.handler.RpcResponseHandler;
import com.ysc.rpc.manager.RegisterFutureManager;
import com.ysc.rpc.manager.RpcFutureManager;
import com.ysc.rpc.request.GetInstanceRequest;
import com.ysc.rpc.request.RegisterRequest;
import com.ysc.rpc.request.RpcRequest;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPipeline;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcClient extends ClientNode {

  private static final Logger LOGGER = LoggerFactory.getLogger(RpcClient.class);

  /**
   * shared RPC response handler instance, can be reused across channels since it uses a concurrent
   * map for promises
   */
  private static final RpcResponseHandler RPC_RESPONSE_HANDLER = new RpcResponseHandler();

  /**
   * shared register response handler instance, can be reused across channels since it only logs
   * responses and does not maintain any state
   */
  private static final RegisterResponseHandler REGISTER_RESPONSE_HANDLER =
      new RegisterResponseHandler();

  /**
   * shared get instance response handler instance, can be reused across channels since it only
   * processes responses for get instance requests and does not maintain any state
   */
  private static final GetInstanceResponseHandler GET_INSTANCE_RESPONSE_HANDLER =
      new GetInstanceResponseHandler();

  /**
   * key: server address in the format "host:port", value: Netty channel connected to the server.
   * this map is used to manage channels for different servers.
   */
  private static final Map<String, Channel> SERVER_CHANNEL_MAP = new ConcurrentHashMap<>();

  public RpcClient(final String serviceId) {
    super(serviceId);
  }

  @Override
  protected void addOtherHandlers(final ChannelPipeline pipeline) {
    pipeline.addLast(RPC_RESPONSE_HANDLER);
    pipeline.addLast(REGISTER_RESPONSE_HANDLER);
    pipeline.addLast(GET_INSTANCE_RESPONSE_HANDLER);
  }

  @Override
  public void start() {
    super.start();

    getChannel(REGISTER_CENTER_HOST, REGISTER_CENTER_PORT)
        .writeAndFlush(new RegisterRequest("server", "localhost", 8080))
        .addListener(
            future -> {
              if (future.isSuccess()) {
                LOGGER.info("Service registration request sent successfully");
              } else {
                LOGGER.warn("Failed to send service registration request", future.cause());
              }
            });
  }

  public Channel getChannel(final String host, final int port) {
    final String serverKey = host + ":" + port;
    Channel channel;

    if (SERVER_CHANNEL_MAP.containsKey(serverKey)) {
      channel = SERVER_CHANNEL_MAP.get(serverKey);
      if (channel != null && channel.isActive()) {
        return channel;
      }
    }

    channel = doConnect(host, port);
    if (channel != null) {
      SERVER_CHANNEL_MAP.put(serverKey, channel);
      return channel;
    } else {
      LOGGER.warn("Failed to connect to server {}:{}", host, port);
    }

    SERVER_CHANNEL_MAP.remove(serverKey);
    return null;
  }

  private Channel doConnect(final String serverHost, final int serverPort) {
    final AtomicReference<Channel> channel = new AtomicReference<>();
    try {
      final ChannelFuture future = bootstrap.connect(serverHost, serverPort).sync();
      channel.set(future.channel());
      LOGGER.info("Connected to server {}:{}", serverHost, serverPort);

      channel
          .get()
          .closeFuture()
          .addListener(
              (ChannelFutureListener)
                  closeFuture -> {
                    LOGGER.warn("Connection to server closed, reconnecting...");
                    channel.set(null);
                    scheduleReconnect();
                  });
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      LOGGER.warn("Failed to connect to server {}:{}", serverHost, serverPort, e);
      scheduleReconnect();
    }

    return channel.get();
  }

  private void scheduleReconnect() {
    LOGGER.info("Reconnecting to server in 5 seconds...");
    // TODO: implement exponential backoff for reconnection attempts
  }

  public synchronized void stop() {
    if (!started) {
      return;
    }

    group.shutdownGracefully();
    started = false;
  }

  protected Object send(final RpcRequest request) throws Throwable {
    final Channel registerCenterChannel = getRegisterChannel();

    final DefaultPromise<Object> promise = new DefaultPromise<>(registerCenterChannel.eventLoop());
    sendToRegisterCenter(
        new GetInstanceRequest(
            request.getServiceId(),
            request.getRequestId(),
            REGISTER_CENTER_HOST,
            REGISTER_CENTER_PORT),
        promise);

    final boolean completed = promise.awaitUninterruptibly(timeoutMillis);

    if (!completed) {
      throw new RuntimeException(
          "Time out, failed to get service instance from register center for request: " + request);
    }

    if (!promise.isSuccess()) {
      throw promise.cause();
    }

    final ServiceInstance instance = (ServiceInstance) promise.getNow();
    LOGGER.info(
        "Received service instance from register center for request {}: {}",
        request.getRequestId(),
        instance);

    final Channel serverChannel = getChannel(instance.getHost(), instance.getPort());
    final DefaultPromise<Object> rpcPromise = new DefaultPromise<>(serverChannel.eventLoop());
    sendRequest(instance, request, rpcPromise);

    final boolean rpcCompleted = rpcPromise.awaitUninterruptibly(timeoutMillis);

    if (!rpcCompleted) {
      throw new RuntimeException("RPC request timed out");
    }

    if (rpcPromise.isSuccess()) {
      return rpcPromise.getNow();
    } else {
      throw rpcPromise.cause();
    }
  }

  private void sendToRegisterCenter(
      final GetInstanceRequest request, final DefaultPromise<Object> promise) {
    RegisterFutureManager.put(request.requestId(), promise);

    getRegisterChannel()
        .writeAndFlush(request)
        .addListener(
            (ChannelFutureListener)
                future -> {
                  if (!future.isSuccess()) {
                    LOGGER.warn(
                        "Failed to send RPC request to register center: {}",
                        request,
                        future.cause());
                    RegisterFutureManager.remove(request.requestId());
                    promise.tryFailure(future.cause());
                  }
                });

    getRegisterChannel()
        .eventLoop()
        .schedule(
            () -> {
              final Promise<Object> timeoutPromise = RegisterFutureManager.get(request.requestId());

              if (timeoutPromise != null && !timeoutPromise.isDone()) {
                LOGGER.warn("RPC request to register center timed out: {}", request);
                timeoutPromise.setFailure(
                    new TimeoutException("RPC request to register center timed out"));
              }
            },
            timeoutMillis,
            TimeUnit.MILLISECONDS);
  }

  private Channel getRegisterChannel() {
    final Channel channel =
        getChannel(ClientNode.REGISTER_CENTER_HOST, ClientNode.REGISTER_CENTER_PORT);

    if (channel == null || !channel.isActive()) {
      throw new IllegalStateException(
          "Not connected to server "
              + ClientNode.REGISTER_CENTER_HOST
              + ":"
              + ClientNode.REGISTER_CENTER_PORT);
    }

    return channel;
  }

  private void sendRequest(
      final ServiceInstance instance,
      final RpcRequest request,
      final DefaultPromise<Object> promise) {
    RpcFutureManager.put(request.getRequestId(), promise);

    getChannel(instance.getHost(), instance.getPort())
        .writeAndFlush(request)
        .addListener(
            (ChannelFutureListener)
                future -> {
                  if (!future.isSuccess()) {
                    LOGGER.warn("Failed to send RPC request: {}", request, future.cause());
                    RpcFutureManager.remove(request.getRequestId());
                    promise.tryFailure(future.cause());
                  }
                });

    getChannel(instance.getHost(), instance.getPort())
        .eventLoop()
        .schedule(
            () -> {
              final Promise<Object> timeoutPromise = RpcFutureManager.get(request.getRequestId());

              if (timeoutPromise != null && !timeoutPromise.isDone()) {
                LOGGER.warn("RPC request timed out: {}", request);
                timeoutPromise.setFailure(new TimeoutException("RPC request timed out"));
              }
            },
            timeoutMillis,
            TimeUnit.MILLISECONDS);
  }
}
