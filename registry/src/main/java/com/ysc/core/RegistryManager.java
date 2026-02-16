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
package com.ysc.core;

import com.ysc.config.RegisterCenterOption;
import com.ysc.core.loadbanlance.LoadBalancer;
import com.ysc.entity.ServiceInstance;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegistryManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(RegistryManager.class);

  /** service registry map: serviceId -> list of service instances */
  private static final Map<String, List<ServiceInstance>> REGISTRY_MAP = new ConcurrentHashMap<>();

  /**
   * get a service instance by serviceId, with simple load balancing
   *
   * @param serviceId service's unique identifier, e.g., "com.ysc.api.UserService"
   * @return a service instance, or null if no instance is available
   */
  public ServiceInstance get(final String serviceId) {
    final List<ServiceInstance> instances = REGISTRY_MAP.get(serviceId);
    if (instances == null || instances.isEmpty()) {
      LOGGER.warn("No service instance found for serviceId: {}", serviceId);
      return null;
    }

    return LoadBalancer.getBalancer(RegisterCenterOption.LOAD_BALANCE_STRATEGY.value())
        .select(serviceId);
  }

  /**
   * get all service instances for a given serviceId
   *
   * @param serviceId service's unique identifier, e.g., "com.ysc.api.UserService"
   * @return list of service instances, or empty list if no instance is available
   */
  public static List<ServiceInstance> getInstanceList(final String serviceId) {
    final List<ServiceInstance> instances = REGISTRY_MAP.get(serviceId);

    if (instances == null || instances.isEmpty()) {
      return List.of();
    }
    return instances;
  }

  /**
   * register a service instance
   *
   * @param instance the service instance to register
   */
  public void register(final ServiceInstance instance) {
    REGISTRY_MAP
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
  public void remove(final String serviceId, final String host, final int port) {
    final List<ServiceInstance> instances = REGISTRY_MAP.get(serviceId);

    if (instances != null) {
      instances.removeIf(instance -> instance.getHost().equals(host) && instance.getPort() == port);
      LOGGER.info("Removed service instance: {}:{}:{}", serviceId, host, port);
    }
  }

  /**
   * remove all service instances for a given serviceId
   *
   * @param serviceId service's unique identifier, e.g., "com.ysc.api.UserService"
   */
  public void remove(final String serviceId) {
    REGISTRY_MAP.remove(serviceId);
    LOGGER.info("Removed all service instances for serviceId: {}", serviceId);
  }

  public Map<String, List<ServiceInstance>> getRegistryMap() {
    return REGISTRY_MAP;
  }

  private static class RegistryManagerHolder {
    private static final RegistryManager INSTANCE = new RegistryManager();
  }

  public static RegistryManager getInstance() {
    return RegistryManagerHolder.INSTANCE;
  }

  private RegistryManager() {
    // private constructor to prevent instantiation
  }
}
