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
package com.ysc.api;

import com.ysc.entity.ServiceInstance;

public interface RegisterService {

  /**
   * get a service instance by serviceId, with simple load balancing
   *
   * @param serviceId service's unique identifier, e.g., "com.ysc.api.UserService"
   * @return a service instance, or null if no instance is available
   */
  ServiceInstance get(String serviceId);

  /**
   * register a service instance to the registry
   *
   * @param serviceInstance service instance to be registered
   */
  void register(ServiceInstance serviceInstance);

  /**
   * remove a service instance from the registry
   *
   * @param serviceId service's unique identifier, e.g., "com.ysc.api.UserService"
   * @param host service host, e.g., localhost or IP address
   * @param port service port, e.g., 8080
   */
  void remove(final String serviceId, final String host, final int port);

  /**
   * remove all service instances of a service from the registry, e.g., when all the services are
   * shut down
   *
   * @param serviceId service's unique identifier, e.g., "com.ysc.api.UserService"
   */
  void remove(final String serviceId);
}
