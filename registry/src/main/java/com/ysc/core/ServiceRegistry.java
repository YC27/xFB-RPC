/* Licensed to the xFB-RPC under one or more
                                    * contributor license agreements. See the NOTICE file
                                    * distributed with this work for additional information
                                    * regarding copyright ownership. The xFB-RPC licenses this file
                                    * to you under the Apache License, Version 2.0 (the
                                    * "License"); you may not use this file except in compliance
                                    * with the License. You may obtain a copy of the License at
                                    *
                                    * http://www.apache.org/licenses/LICENSE-2.0
                                    *
                                    * Unless required by applicable law or agreed to in writing, software
                                    * distributed under the License is distributed on an "AS IS" BASIS,
                                    * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
                                    * See the License for the specific language governing permissions and
                                    * limitations under the License.
                                    */
package com.ysc.core;

import com.ysc.entity.ServiceInstance;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceRegistry {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRegistry.class);

  /** service registry map: serviceId -> list of service instances */
  private final Map<String, List<ServiceInstance>> registryMap = new ConcurrentHashMap<>();

  /**
   * get a service instance by serviceId, with simple load balancing
   *
   * @param serviceId service's unique identifier, e.g., "com.ysc.api.UserService"
   * @return a service instance, or null if no instance is available
   */
  public ServiceInstance getServiceInstance(final String serviceId) {
    final List<ServiceInstance> instances = registryMap.get(serviceId);
    if (instances == null || instances.isEmpty()) {
      LOGGER.warn("No service instance found for serviceId: {}", serviceId);
      return null;
    }

    // simple load balancing: randomly select one instance from the list
    // TODO: implement more advanced load balancing strategies, e.g., round-robin, weighted
    // random,
    // etc. also consider service instance health and availability when selecting an instance
    // for now, we just randomly select one instance from the list
    final int index = (int) (Math.random() * instances.size());

    return instances.get(index);
  }

  /**
   * register a service instance
   *
   * @param instance the service instance to register
   */
  public void registerServiceInstance(final ServiceInstance instance) {
    registryMap
        .computeIfAbsent(instance.getServiceId(), k -> new CopyOnWriteArrayList<>())
        .add(instance);
    LOGGER.info("Registered service instance: {}", instance);
  }

  /**
   * remove a service instance
   *
   * @param serviceId service's unique identifier, e.g., "com.ysc.api.UserService"
   * @param host service host, e.g., localhost or IP address
   * @param port service port, e.g., 8080
   */
  public void removeServiceInstance(final String serviceId, final String host, final int port) {
    final List<ServiceInstance> instances = registryMap.get(serviceId);

    if (instances != null) {
      instances.removeIf(instance -> instance.getHost().equals(host) && instance.getPort() == port);
      LOGGER.info("Removed service instance: {}:{}:{}", serviceId, host, port);
    }
  }

  private static class ServiceRegistryHolder {
    private static final ServiceRegistry INSTANCE = new ServiceRegistry();
  }

  public static ServiceRegistry getInstance() {
    return ServiceRegistryHolder.INSTANCE;
  }

  private ServiceRegistry() {
    // private constructor to prevent instantiation
  }
}
