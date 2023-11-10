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
import com.silong.foundation.dj.mixmaster.message.Messages.MemberInfo;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.*;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.jgroups.Address;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.util.ByteArrayDataInputStream;
import org.jgroups.util.ByteArrayDataOutputStream;
import org.jgroups.util.UUID;
import org.jgroups.util.Util;

/**
 * 扩展UUID，作为节点信息
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-11 22:49
 */
@Getter
public class ClusterMemberUUID extends UUID implements Identity<Address>, Serializable {

  static {
    // it will need to get registered with the ClassConfigurator in order to marshal it correctly
    // Note that the ID should be chosen such that it doesn’t collide with any IDs defined in
    // jg-magic-map.xml
    ClassConfigurator.add((short) 5674, ClusterMemberUUID.class);
  }

  @Serial private static final long serialVersionUID = -3_208_132_295_684_059_186L;

  /** 类型转换器 */
  private static final BiConverter<ClusterMemberUUID, byte[]> INSTANCE =
      new BiConverter<>() {

        @Override
        @SneakyThrows
        public byte[] to(ClusterMemberUUID uuid) {
          if (uuid == null) {
            return null;
          }
          ByteArrayDataOutputStream out = new ByteArrayDataOutputStream(uuid.serializedSize());
          uuid.writeTo(out);
          return out.buffer();
        }

        @Override
        @SneakyThrows
        public ClusterMemberUUID from(byte[] bytes) {
          if (bytes == null || bytes.length == 0) {
            return null;
          }
          ClusterMemberUUID uuid = new ClusterMemberUUID();
          uuid.readFrom(new ByteArrayDataInputStream(bytes));
          return uuid;
        }
      };

  /** 节点信息 */
  @Setter
  @Accessors(fluent = true)
  private MemberInfo memberInfo;

  /** 默认构造方法 */
  public ClusterMemberUUID() {
    super();
  }

  /**
   * 构造方法
   *
   * @param uuid uuid
   */
  public ClusterMemberUUID(byte[] uuid) {
    super(uuid);
  }

  @Override
  @NonNull
  public ClusterMemberUUID uuid() {
    return this;
  }

  /**
   * 随机uuid生成对象
   *
   * @return @{@code ClusterNodeUUID}
   */
  public static ClusterMemberUUID random() {
    return new ClusterMemberUUID(generateRandomBytes());
  }

  /**
   * 创建实例
   *
   * @return 实例
   */
  @Override
  public Supplier<? extends UUID> create() {
    return ClusterMemberUUID::new;
  }

  @Override
  public int serializedSize() {
    return super.serializedSize() + (memberInfo == null ? 0 : memberInfo.getSerializedSize());
  }

  @Override
  public void writeTo(DataOutput out) throws IOException {
    super.writeTo(out);
    byte[] buf = memberInfo != null ? memberInfo.toByteArray() : null;
    Util.writeByteBuffer(buf, 0, buf != null ? buf.length : -1, out);
  }

  @Override
  public void readFrom(DataInput in) throws IOException {
    super.readFrom(in);
    byte[] bytes = Util.readByteBuffer(in);
    memberInfo = bytes == null ? null : MemberInfo.parseFrom(bytes);
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
  public static ClusterMemberUUID deserialize(byte[] bytes) throws IOException {
    return INSTANCE.from(bytes);
  }

  /**
   * 获取节点描述信息
   *
   * @return 描述信息
   */
  public String memberInfoDesc() {
    return TextFormat.printer().printToString(this.memberInfo);
  }
}
