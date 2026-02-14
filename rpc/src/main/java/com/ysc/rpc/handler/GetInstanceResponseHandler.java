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
package com.ysc.rpc.handler;

import com.ysc.rpc.manager.RegisterFutureManager;
import com.ysc.rpc.response.GetInstanceResponse;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Promise;

@ChannelHandler.Sharable
public class GetInstanceResponseHandler extends SimpleChannelInboundHandler<GetInstanceResponse> {
  @Override
  protected void channelRead0(
      ChannelHandlerContext channelHandlerContext, GetInstanceResponse msg) {
    final Promise<Object> promise = RegisterFutureManager.remove(msg.requestId());

    if (promise != null) {
      if (msg.success()) {
        promise.setSuccess(msg.instance());
      } else {
        promise.setFailure(new RuntimeException(msg.message()));
      }
    }
  }
}
