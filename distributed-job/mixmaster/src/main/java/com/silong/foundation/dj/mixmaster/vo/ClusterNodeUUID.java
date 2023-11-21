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
package com.silong.foundation.dj.mixmaster.vo;

import com.google.protobuf.TextFormat;
import com.silong.foundation.common.utils.BiConverter;
import com.silong.foundation.dj.mixmaster.Identity;
import com.silong.foundation.dj.mixmaster.generated.Messages.ClusterNodeInfo;
import edu.umd.cs.findbugs.annotations.NonNull;
import jakarta.annotation.Nullable;
import java.io.*;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jgroups.Address;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.util.ByteArrayDataOutputStream;
import org.jgroups.util.UUID;
import org.jgroups.util.Util;
import org.xerial.snappy.Snappy;

/**
 * 扩展UUID，作为节点信息
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-11 22:49
 */
@Getter
public class ClusterNodeUUID extends UUID implements Identity<Address>, Serializable {

  @Serial private static final long serialVersionUID = 5_046_218_932_880_379_606L;

  static {
    // it will need to get registered with the ClassConfigurator in order to marshal it correctly
    // Note that the ID should be chosen such that it doesn’t collide with any IDs defined in
    // jg-magic-map.xml
    ClassConfigurator.add((short) 5674, ClusterNodeUUID.class);
  }

  /** 类型转换器 */
  private static final BiConverter<UUID, byte[]> INSTANCE =
      new BiConverter<>() {

        @Override
        @Nullable
        public byte[] to(UUID uuid) {
          if (uuid == null) {
            return null;
          }
          ByteArrayDataOutputStream out = new ByteArrayDataOutputStream(UUID.SIZE);
          out.writeLong(uuid.getLeastSignificantBits());
          out.writeLong(uuid.getMostSignificantBits());
          return out.buffer();
        }

        @Override
        @Nullable
        public ClusterNodeUUID from(byte[] bytes) {
          if (bytes == null || bytes.length == 0) {
            return null;
          }
          return new ClusterNodeUUID(bytes);
        }
      };

  /** 节点信息 */
  @Setter
  @Accessors(fluent = true)
  private ClusterNodeInfo clusterNodeInfo;

  /** 节点权重，用于计算数据分区分布，不参与集群通讯的序列化内容 */
  @Getter
  @Accessors(fluent = true)
  private final transient ThreadLocal<Long> rendezvousWeight = new ThreadLocal<>();

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
    byte[] buf = clusterNodeInfo != null ? Snappy.compress(clusterNodeInfo.toByteArray()) : null;
    Util.writeByteBuffer(buf, 0, buf != null ? buf.length : -1, out);
  }

  @Override
  public void readFrom(DataInput in) throws IOException {
    super.readFrom(in);
    byte[] bytes = Util.readByteBuffer(in);
    clusterNodeInfo = bytes == null ? null : ClusterNodeInfo.parseFrom(Snappy.uncompress(bytes));
  }

  /**
   * 对象序列化，不包含ClusterNodeInfo
   *
   * @return 二进制
   */
  public byte[] serialize() {
    return INSTANCE.to(this);
  }

  /**
   * 反序列化，不包含clusterNodeInfo
   *
   * @param bytes 二进制
   * @return 对象
   */
  public static ClusterNodeUUID deserialize(byte[] bytes) {
    return (ClusterNodeUUID) INSTANCE.from(bytes);
  }

  /**
   * 获取附加属性
   *
   * @return 附加属性
   */
  public String printAttributes() {
    return TextFormat.printer().printToString(this.clusterNodeInfo);
  }
}
