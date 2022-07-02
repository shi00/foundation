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

import com.google.protobuf.TextFormat;
import com.silong.foundation.devastator.Identity;
import com.silong.foundation.devastator.model.Devastator.ClusterNodeInfo;
import com.silong.foundation.devastator.utils.TypeConverter;
import edu.umd.cs.findbugs.annotations.NonNull;
import lombok.Getter;
import org.jgroups.Address;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.util.ByteArrayDataInputStream;
import org.jgroups.util.ByteArrayDataOutputStream;
import org.jgroups.util.UUID;

import java.io.*;
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
public class ClusterNodeUUID extends UUID implements Identity<Address>, Serializable {

  @Serial private static final long serialVersionUID = 8271975008899099415L;

  /** 类型转换器 */
  private static final TypeConverter<ClusterNodeUUID, byte[]> INSTANCE =
      new TypeConverter<>() {

        @Serial private static final long serialVersionUID = -1910864152755748200L;

        @Override
        public byte[] to(ClusterNodeUUID clusterNodeUUID) throws IOException {
          if (clusterNodeUUID == null) {
            return null;
          }
          ByteArrayDataOutputStream out =
              new ByteArrayDataOutputStream(clusterNodeUUID.serializedSize());
          clusterNodeUUID.writeTo(out);
          return out.buffer();
        }

        @Override
        public ClusterNodeUUID from(byte[] bytes) throws IOException {
          if (bytes == null || bytes.length == 0) {
            return null;
          }
          ClusterNodeUUID clusterNodeUUID = new ClusterNodeUUID();
          clusterNodeUUID.readFrom(new ByteArrayDataInputStream(bytes));
          return clusterNodeUUID;
        }
      };

  static {
    // it will need to get registered with the ClassConfigurator in order to marshal it correctly
    // Note that the ID should be chosen such that it doesn’t collide with any IDs defined in
    // jg-magic-map.xml
    ClassConfigurator.add((short) 5674, ClusterNodeUUID.class);
  }

  /** 节点信息 */
  @Getter private ClusterNodeInfo clusterNodeInfo;

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

  @Override
  @NonNull
  public ClusterNodeUUID uuid() {
    return this;
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
   * 对象序列化
   *
   * @return 二进制
   * @throws IOException 异常
   */
  public byte[] serialize() throws IOException {
    return INSTANCE.to(this);
  }

  /**
   * 反序列化
   *
   * @param bytes 二进制
   * @return 对象
   * @throws IOException 异常
   */
  public static ClusterNodeUUID deserialize(byte[] bytes) throws IOException {
    return INSTANCE.from(bytes);
  }

  public ClusterNodeUUID setClusterNodeInfo(ClusterNodeInfo clusterNodeInfo) {
    this.clusterNodeInfo = clusterNodeInfo;
    return this;
  }

  /**
   * 获取节点描述信息
   *
   * @return 描述信息
   */
  public String getClusterNodeInfoDesc() {
    return TextFormat.printer().printToString(this.clusterNodeInfo);
  }
}
