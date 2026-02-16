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
package com.ysc.rpc;

import com.ysc.api.UserService;
import com.ysc.rpc.config.Options;
import com.ysc.rpc.impl.UserServiceImpl;
import com.ysc.rpc.netty.RpcServer;
import java.util.List;

public class ServerApplication {
  public static void main(String[] args) throws InterruptedException {
    final Options options = new Options();
    options.logAllOptions();

    final RpcServer server = new RpcServer("server", 8080);
    server.start();

    server.registerService(List.of(UserService.class), List.of(new UserServiceImpl()));

    Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

    Thread.currentThread().join();
  }
}
