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
package com.ysc.netty;

import com.ysc.handler.GetInstanceRequestHandler;
import com.ysc.handler.RegisterRequestHandler;
import com.ysc.rpc.netty.ServerNode;
import io.netty.channel.ChannelPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcServer extends ServerNode {

  private static final Logger LOGGER = LoggerFactory.getLogger(RpcServer.class);

  /**
   * RegisterRequestHandler is a custom Netty handler that processes incoming service registration
   * requests and updates the ServerRegistry accordingly.
   */
  private static final RegisterRequestHandler REGISTER_REQUEST_HANDLER =
      new RegisterRequestHandler();

  private static final GetInstanceRequestHandler GET_INSTANCE_REQUEST_HANDLER =
      new GetInstanceRequestHandler();

  public RpcServer(String serviceId, int port) {
    super(serviceId, port);
  }

  @Override
  protected void addOtherHandlers(final ChannelPipeline pipeline) {
    pipeline.addLast(REGISTER_REQUEST_HANDLER);
    pipeline.addLast(GET_INSTANCE_REQUEST_HANDLER);
  }

  @Override
  protected void doSomeOtherInitialization() {
    // No additional initialization needed for the registry server
  }
}
