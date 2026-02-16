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
package com.ysc.core.loadbanlance;

import com.ysc.core.RegistryManager;
import com.ysc.entity.ServiceInstance;
import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface LoadBalancer {

  Map<String, LoadBalancer> loadBalancerMap =
      Map.of(
          "round-robin",
          new RoundRobinBalancer(),
          "weighted-round-robin",
          new WeightedRoundRobinBalancer(),
          "random",
          new RandomBalancer());

  /**
   * select a service instance from the list of service instances for the given service id.
   *
   * @param serviceId the service id to select a service instance for
   * @return a service instance selected from the list of service instances for the given service id
   */
  ServiceInstance select(final String serviceId);

  default List<ServiceInstance> getServiceInstances(final String serviceId) {
    return RegistryManager.getInstanceList(serviceId);
  }

  static LoadBalancer getBalancer(final String balancerName) {
    return loadBalancerMap.get(balancerName);
  }
}
