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

package com.silong.foundation.dj.hook;

import lombok.NonNull;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * 应用上下文，工具类
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-24 14:02
 */
public class SpringContext implements ApplicationContextAware {

  private static ApplicationContext applicationContext;

  /**
   * 获取指定类型的Bean
   *
   * @param t class类型
   * @return bean
   * @param <T> 类型
   */
  public static <T> T getBean(@NonNull Class<T> t) {
    return applicationContext.getBean(t);
  }

  /**
   * 获取指定名字的Bean
   *
   * @param name bean名字
   * @return bean
   * @param <T> bean类型
   */
  @SuppressWarnings("unchecked")
  public static <T> T getBean(@NonNull String name) {
    return (T) applicationContext.getBean(name);
  }

  @Override
  public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
    SpringContext.applicationContext = applicationContext;
  }
}
