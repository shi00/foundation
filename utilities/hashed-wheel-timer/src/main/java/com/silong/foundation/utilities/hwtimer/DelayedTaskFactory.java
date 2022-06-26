/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.silong.foundation.utilities.hwtimer;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * 延时任务构造工厂
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-06-25 10:15
 */
class DelayedTaskFactory extends BasePooledObjectFactory<DefaultDelayedTask> {

  @Override
  public void destroyObject(PooledObject<DefaultDelayedTask> pooledObject) {
    DefaultDelayedTask object = pooledObject.getObject();
    object.stateRef = null;
    object.exception = null;
    object.name = null;
    object.callable = null;
    object.result = null;
    object.wheelTimer = null;
    object.signal = null;
  }

  @Override
  public DefaultDelayedTask create() {
    return new DefaultDelayedTask();
  }

  @Override
  public PooledObject<DefaultDelayedTask> wrap(DefaultDelayedTask delayedTask) {
    return new DefaultPooledObject<>(delayedTask);
  }

  /** When an object is returned to the pool, clear the task. */
  @Override
  public void passivateObject(PooledObject<DefaultDelayedTask> pooledObject) {
    DefaultDelayedTask object = pooledObject.getObject();
    object.deadLine = object.rounds = 0;
    object.callable = null;
    object.result = null;
    object.name = null;
    object.exception = null;
    object.wheelTimer = null;
    object.signal.reset();
    object.stateRef.set(DelayedTask.State.READY);
  }
}
