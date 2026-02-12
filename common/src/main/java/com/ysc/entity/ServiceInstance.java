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
package com.ysc.entity;

import java.io.Serial;
import java.io.Serializable;

public class ServiceInstance implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  /** service id, e.g., "com.ysc.api.UserService" */
  private final String serviceId;

  /** service host, e.g., localhost or IP address */
  private final String host;

  /** service port, e.g., 8080 */
  private final int port;

  /** service weight, used for load balancing, default is 1 */
  private final int weight;

  /** service availability, true if the service is healthy and can receive requests */
  private boolean available;

  /** service registration timestamp, used for service expiration and cleanup */
  private final long timestamp;

  /**
   * create a service instance with default weight 1
   *
   * @param serviceId service id, e.g., "com.ysc.api.UserService"
   * @param host service host, e.g., localhost or IP address
   * @param port service port, e.g., 8080
   */
  public ServiceInstance(final String serviceId, final String host, int port) {
    this(serviceId, host, port, 1);
  }

  /**
   * create a service instance with specified weight
   *
   * @param serviceId service id, e.g., "com.ysc.api.UserService"
   * @param host service host, e.g., localhost or IP address
   * @param port service port, e.g., 8080
   * @param weight service weight, used for load balancing, default is 1
   */
  public ServiceInstance(final String serviceId, final String host, int port, int weight) {
    this.serviceId = serviceId;
    this.host = host;
    this.port = port;
    this.weight = weight > 0 ? weight : 1;
    this.available = true;
    this.timestamp = System.currentTimeMillis();
  }

  public String getServiceId() {
    return serviceId;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public int getWeight() {
    return weight;
  }

  public boolean isAvailable() {
    return available;
  }

  public void setAvailable(final boolean available) {
    this.available = available;
  }

  public long getTimestamp() {
    return timestamp;
  }
}
