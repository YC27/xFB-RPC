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
package com.ysc.rpc.server.handler;

import com.ysc.rpc.RpcRequest;
import com.ysc.rpc.RpcResponse;
import com.ysc.rpc.server.registry.ServerRegistry;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ChannelHandler.Sharable
public class RpcRequestHandler extends SimpleChannelInboundHandler<RpcRequest> {

  private static final ExecutorService RPC_REQUEST_HANDLER_POOL = Executors.newFixedThreadPool(16);

  @Override
  protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcRequest rpcRequest) {
    RPC_REQUEST_HANDLER_POOL.submit(
        () -> {
          RpcResponse response;
          long startTime = 0L;
          long endTime;

          try {
            startTime = System.currentTimeMillis();

            final Object instance = ServerRegistry.getService(rpcRequest.getInterfaceName());

            if (instance == null) {
              throw new RuntimeException("Service not found: " + rpcRequest.getInterfaceName());
            }

            final Object result =
                instance
                    .getClass()
                    .getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes())
                    .invoke(instance, rpcRequest.getParamValues());
            endTime = System.currentTimeMillis();
            response = new RpcResponse(rpcRequest.getRequestId(), result, endTime - startTime);
          } catch (final Exception e) {
            endTime = System.currentTimeMillis();
            response =
                new RpcResponse(rpcRequest.getRequestId(), e.getMessage(), endTime - startTime);
          }

          channelHandlerContext.writeAndFlush(response);
        });
  }
}
