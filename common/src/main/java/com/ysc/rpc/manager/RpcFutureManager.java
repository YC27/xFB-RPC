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
package com.ysc.rpc.manager;

import io.netty.util.concurrent.Promise;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RpcFutureManager {

  /**
   * map of requestId to promise, used to correlate responses with requests and complete the
   * promises
   */
  public static final Map<Long, Promise<Object>> PROMISES = new ConcurrentHashMap<>();

  private RpcFutureManager() {
    // private constructor to prevent instantiation
  }

  public static void put(final Long requestId, final Promise<Object> promise) {
    PROMISES.put(requestId, promise);
  }

  public static Promise<Object> remove(final Long requestId) {
    return PROMISES.remove(requestId);
  }

  public static Promise<Object> get(final Long requestId) {
    return PROMISES.get(requestId);
  }
}
