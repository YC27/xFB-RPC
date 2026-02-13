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
package com.ysc.service;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class HeartbeatService implements IService {

  private final ScheduledExecutorService executorService;
  private final long interval;
  private final TimeUnit timeUnit;

  private final AtomicBoolean started = new AtomicBoolean(false);

  private volatile Future<?> future;

  public HeartbeatService(
      final ScheduledExecutorService executorService,
      final long interval,
      final TimeUnit timeUnit) {
    this.executorService = executorService;
    this.interval = interval;
    this.timeUnit = timeUnit;
  }

  @Override
  public synchronized void start() {
    if (!started.compareAndSet(false, true)) {
      return;
    }

    future = executorService.scheduleWithFixedDelay(this::execute, 0, interval, timeUnit);
  }

  private void execute() {}

  @Override
  public void stop() {
    if (!started.compareAndSet(true, false)) {
      return;
    }

    if (future != null) {
      future.cancel(true);
    }
  }
}
