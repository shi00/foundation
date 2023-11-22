/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package com.silong.foundation.dj.mixmaster.utils;

import org.jgroups.logging.CustomLogFactory;
import org.jgroups.logging.Log;
import org.jgroups.logging.Slf4jLogImpl;

/**
 * slf4j日志打印工厂
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-22 15:02
 */
public class Slf4jLogFactory implements CustomLogFactory {
  @Override
  public Log getLog(Class<?> clazz) {
    return new Slf4jLogImpl(clazz);
  }

  @Override
  public Log getLog(String category) {
    return new Slf4jLogImpl(category);
  }
}
