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
package com.silong.foundation.dj.mixmaster;

import com.silong.foundation.dj.mixmaster.message.Messages.MemberRole;
import java.util.Map;
import org.jgroups.Address;

/**
 * 集群成员
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-07 21:33
 * @param <T> 唯一标识类型
 */
public interface ClusterMember<T extends Comparable<T>> extends Identity<T> {

  /**
   * 节点角色
   *
   * @return 角色
   */
  MemberRole role();

  /**
   * 获取集群成员名称
   *
   * @return 成员名称
   */
  String name();

  /**
   * 获取集群成员本地地址
   *
   * @return local地址，非IP
   */
  Address localAddress();

  /**
   * 获取节点属性
   *
   * @param attributeKey 属性key
   * @return 属性值
   */
  Object attribute(String attributeKey);

  /**
   * 获取节点属性集合
   *
   * @return 属性集合
   */
  Map<String, Object> attributes();
}
