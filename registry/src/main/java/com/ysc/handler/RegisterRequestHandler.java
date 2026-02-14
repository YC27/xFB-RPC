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
package com.ysc.handler;

import com.ysc.core.RegistryManager;
import com.ysc.entity.ServiceInstance;
import com.ysc.rpc.request.RegisterRequest;
import com.ysc.rpc.response.RegisterResponse;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public class RegisterRequestHandler extends SimpleChannelInboundHandler<RegisterRequest> {

  private static final Logger LOGGER = LoggerFactory.getLogger(RegisterRequestHandler.class);

  @Override
  protected void channelRead0(
      ChannelHandlerContext channelHandlerContext, RegisterRequest registerRequest) {
    final ServiceInstance instance =
        new ServiceInstance(
            registerRequest.serviceId(), registerRequest.host(), registerRequest.port());

    RegistryManager.getInstance().register(instance);
    LOGGER.info("Received register request: {}", instance);

    channelHandlerContext.writeAndFlush(
        new RegisterResponse(true, "Service registered successfully"));
  }
}
