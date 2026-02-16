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

import com.ysc.rpc.codec.decoder.RpcDecoder;
import com.ysc.rpc.codec.encoder.RpcEncoder;
import com.ysc.rpc.config.RpcClientOption;
import com.ysc.rpc.config.RpcServerOption;
import com.ysc.rpc.handler.RpcRequestHandler;
import com.ysc.rpc.manager.ServiceManager;
import io.netty.channel.ChannelPipeline;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcServer extends ServerNode {

  private static final Logger LOGGER = LoggerFactory.getLogger(RpcServer.class);

  /**
   * RpcRequestHandler is a custom Netty handler that processes incoming RPC requests and generates
   * responses.
   */
  private static final RpcRequestHandler RPC_REQUEST_HANDLER = new RpcRequestHandler();

  public RpcServer(final String serviceId, final int port) {
    super(serviceId, port);
  }

  @Override
  protected void addEncoder(final ChannelPipeline pipeline) {
    pipeline.addLast(new RpcEncoder(RpcClientOption.ENCODE_TYPE.value()));
  }

  @Override
  protected void addDecoder(final ChannelPipeline pipeline) {
    pipeline.addLast(new RpcDecoder(RpcServerOption.DECODE_TYPE.value()));
  }

  @Override
  protected void addOtherHandlers(final ChannelPipeline pipeline) {
    pipeline.addLast(RPC_REQUEST_HANDLER);
  }

  @Override
  protected void doSomeOtherInitialization() {
    // No additional initialization needed for the RPC server
  }

  public void registerService(final Class<?> clazz, final Object serviceInstance) {
    ServiceManager.registerService(clazz, serviceInstance);
  }

  public void registerService(final List<Class<?>> classes, final List<Object> serviceInstances) {
    if (classes.size() != serviceInstances.size()) {
      throw new IllegalArgumentException(
          "Classes and serviceInstances lists must have the same size.");
    }
    for (int i = 0; i < classes.size(); i++) {
      registerService(classes.get(i), serviceInstances.get(i));
    }
  }
}
