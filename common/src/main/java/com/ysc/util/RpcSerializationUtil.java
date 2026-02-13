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
package com.ysc.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ysc.rpc.request.RpcRequest;
import com.ysc.rpc.response.RpcResponse;

/** RPC 序列化工具 处理 RpcRequest 和 RpcResponse 的序列化/反序列化 */
public class RpcSerializationUtil {

  private static final Gson gson = new GsonBuilder().create();

  /** 将 RpcRequest 序列化为 JSON 字符串 */
  public static String serializeRequest(RpcRequest request) {
    JsonObject json = new JsonObject();
    json.addProperty("requestId", request.getRequestId());
    // include both interfaceName and serviceId for compatibility
    json.addProperty("interfaceName", request.getInterfaceName());
    json.addProperty("serviceId", request.getServiceId());
    json.addProperty("methodName", request.getMethodName());

    // 序列化参数类型
    if (request.getParamTypes() != null) {
      JsonArray paramTypesArray = new JsonArray();
      for (Class<?> paramType : request.getParamTypes()) {
        paramTypesArray.add(paramType.getName());
      }
      json.add("paramTypes", paramTypesArray);
    }

    // 序列化参数值
    if (request.getParamValues() != null) {
      JsonArray paramValuesArray = new JsonArray();
      for (Object paramValue : request.getParamValues()) {
        if (paramValue == null) {
          paramValuesArray.add((String) null);
        } else {
          paramValuesArray.add(gson.toJsonTree(paramValue));
        }
      }
      json.add("paramValues", paramValuesArray);
    }

    return json.toString();
  }

  /** 将 JSON 字符串反序列化为 RpcRequest */
  public static RpcRequest deserializeRequest(String json) throws Exception {
    JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();

    RpcRequest request = new RpcRequest();
    if (jsonObject.has("requestId") && !jsonObject.get("requestId").isJsonNull()) {
      try {
        request.setRequestId(jsonObject.get("requestId").getAsLong());
      } catch (Exception ignored) {
      }
    }
    if (jsonObject.has("interfaceName") && !jsonObject.get("interfaceName").isJsonNull()) {
      request.setInterfaceName(jsonObject.get("interfaceName").getAsString());
    }
    request.setServiceId(jsonObject.get("serviceId").getAsString());
    request.setMethodName(jsonObject.get("methodName").getAsString());

    // 反序列化参数类型
    if (jsonObject.has("paramTypes") && !jsonObject.get("paramTypes").isJsonNull()) {
      JsonArray paramTypesArray = jsonObject.getAsJsonArray("paramTypes");
      Class<?>[] paramTypes = new Class<?>[paramTypesArray.size()];
      for (int i = 0; i < paramTypesArray.size(); i++) {
        String className = paramTypesArray.get(i).getAsString();
        paramTypes[i] = resolveClassName(className);
      }
      request.setParamTypes(paramTypes);
    }

    // 反序列化参数值
    if (jsonObject.has("paramValues") && !jsonObject.get("paramValues").isJsonNull()) {
      JsonArray paramValuesArray = jsonObject.getAsJsonArray("paramValues");
      if (request.getParamTypes() != null) {
        Object[] paramValues = new Object[paramValuesArray.size()];
        for (int i = 0; i < paramValuesArray.size(); i++) {
          if (paramValuesArray.get(i).isJsonNull()) {
            paramValues[i] = null;
          } else {
            Class<?> paramType = request.getParamTypes()[i];
            paramValues[i] = gson.fromJson(paramValuesArray.get(i), paramType);
          }
        }
        request.setParamValues(paramValues);
      }
    }

    return request;
  }

  /** 将 RpcResponse 序列化为 JSON 字符串 */
  public static String serializeResponse(RpcResponse response) {
    return gson.toJson(response);
  }

  /** 将 JSON 字符串反序列化为 RpcResponse */
  public static RpcResponse deserializeResponse(String json) {
    return gson.fromJson(json, RpcResponse.class);
  }

  /** 根据类名解析 Class 对象 */
  private static Class<?> resolveClassName(String className) throws ClassNotFoundException {
    // 处理基本数据类型
    switch (className) {
      case "int":
        return int.class;
      case "long":
        return long.class;
      case "float":
        return float.class;
      case "double":
        return double.class;
      case "boolean":
        return boolean.class;
      case "byte":
        return byte.class;
      case "short":
        return short.class;
      case "char":
        return char.class;
      case "void":
        return void.class;
      default:
        return Class.forName(className);
    }
  }
}
