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
package com.silong.foundation.devastator.core;

import com.silong.foundation.devastator.model.Devastator.ClusterNodeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.util.UUID;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.function.Supplier;

import static org.jgroups.util.Util.readByteBuffer;
import static org.jgroups.util.Util.writeByteBuffer;

/**
 * 扩展UUID，作为节点信息
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-11 22:49
 */
public class ClusterNodeUUID extends UUID {
  static {
    // it will need to get registered with the ClassConfigurator in order to marshal it correctly
    // Note that the ID should be chosen such that it doesn’t collide with any IDs defined in
    // jg-magic-map.xml
    ClassConfigurator.add((short) 5674, ClusterNodeUUID.class);
  }

  /** 节点信息 */
  @Getter
  @Setter
  @Accessors(fluent = true)
  private ClusterNodeInfo clusterNodeInfo;

  /** 默认构造方法 */
  public ClusterNodeUUID() {
    super();
  }

  /**
   * 构造方法
   *
   * @param uuid uuid
   */
  public ClusterNodeUUID(byte[] uuid) {
    super(uuid);
  }

  /**
   * 随机uuid生成对象
   *
   * @return @{@code ClusterNodeUUID}
   */
  public static ClusterNodeUUID random() {
    return new ClusterNodeUUID(generateRandomBytes());
  }

  /**
   * 创建实例
   *
   * @return 实例
   */
  @Override
  public Supplier<? extends UUID> create() {
    return ClusterNodeUUID::new;
  }

  @Override
  public int serializedSize() {
    return super.serializedSize()
        + (clusterNodeInfo == null ? 0 : clusterNodeInfo.getSerializedSize());
  }

  @Override
  public void writeTo(DataOutput out) throws IOException {
    super.writeTo(out);
    byte[] buf = clusterNodeInfo != null ? clusterNodeInfo.toByteArray() : null;
    writeByteBuffer(buf, 0, buf != null ? buf.length : -1, out);
  }

  @Override
  public void readFrom(DataInput in) throws IOException {
    super.readFrom(in);
    byte[] bytes = readByteBuffer(in);
    clusterNodeInfo = bytes == null ? null : ClusterNodeInfo.parseFrom(bytes);
  }

  /**
   * 获取节点打印信息
   *
   * @return 打印信息
   */
  public String printClusterNodeInfo() {
    return String.format("[%s]", clusterNodeInfo);
  }
}
