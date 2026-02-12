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
package com.ysc.rpc;

import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

public class RpcRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  private static final AtomicLong REQUEST_ID_GEN = new AtomicLong(0);

  /** 请求的唯一标识 */
  private Long requestId;

  /** 被调用的服务标识 */
  private String serviceId;

  /** 被调用的接口名 */
  private String interfaceName;

  /** 被调用的方法名 */
  private String methodName;

  /** 方法返回值类型 */
  private Class<?> returnType;

  /** 方法参数类型数组 */
  private Class<?>[] paramTypes;

  /** 方法参数值数组 */
  private Object[] paramValues;

  public RpcRequest() {}

  public RpcRequest(
      final String interfaceName,
      final String methodName,
      final Class<?> returnType,
      final Class<?>[] paramTypes,
      final Object[] paramValues) {
    this.requestId = REQUEST_ID_GEN.incrementAndGet();
    this.interfaceName = interfaceName;
    this.methodName = methodName;
    this.returnType = returnType;
    this.paramTypes = paramTypes;
    this.paramValues = paramValues;
  }

  public Long getRequestId() {
    return requestId;
  }

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(final String serviceId) {
    this.serviceId = serviceId;
  }

  public String getMethodName() {
    return methodName;
  }

  public void setMethodName(final String methodName) {
    this.methodName = methodName;
  }

  public Class<?>[] getParamTypes() {
    return paramTypes;
  }

  public void setParamTypes(final Class<?>[] paramTypes) {
    this.paramTypes = paramTypes;
  }

  public Object[] getParamValues() {
    return paramValues;
  }

  public void setParamValues(final Object[] paramValues) {
    this.paramValues = paramValues;
  }

  public void setRequestId(final Long requestId) {
    this.requestId = requestId;
  }

  public String getInterfaceName() {
    return interfaceName;
  }

  public void setInterfaceName(final String interfaceName) {
    this.interfaceName = interfaceName;
  }

  public Class<?> getReturnType() {
    return returnType;
  }

  public void setReturnType(Class<?> returnType) {
    this.returnType = returnType;
  }

  @Override
  public String toString() {
    return "RpcRequest{"
        + "requestId="
        + requestId
        + ", serviceId='"
        + serviceId
        + '\''
        + ", methodName='"
        + methodName
        + '\''
        + ", paramTypes="
        + Arrays.toString(paramTypes)
        + ", paramValues="
        + Arrays.toString(paramValues)
        + '}';
  }
}
