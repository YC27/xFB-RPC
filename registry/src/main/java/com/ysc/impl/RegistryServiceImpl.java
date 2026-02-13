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
package com.ysc.impl;

import com.ysc.api.RegisterService;
import com.ysc.core.RegistryManager;
import com.ysc.entity.ServiceInstance;
import java.util.List;
import java.util.Map;

public class RegistryServiceImpl implements RegisterService {

  @Override
  public ServiceInstance get(final String serviceId) {
    return RegistryManager.getInstance().get(serviceId);
  }

  @Override
  public void register(final ServiceInstance instance) {
    RegistryManager.getInstance().register(instance);
  }

  public void remove(final String serviceId, final String host, final int port) {
    RegistryManager.getInstance().remove(serviceId, host, port);
  }

  @Override
  public void remove(final String serviceId) {
    RegistryManager.getInstance().remove(serviceId);
  }

  public Map<String, List<ServiceInstance>> getRegistryMap() {
    return RegistryManager.getInstance().getRegistryMap();
  }
}
