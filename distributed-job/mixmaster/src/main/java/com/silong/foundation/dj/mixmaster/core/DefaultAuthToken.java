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
package com.silong.foundation.dj.mixmaster.core;

import com.silong.foundation.dj.hook.auth.JwtAuthenticator;
import com.silong.foundation.dj.hook.auth.JwtAuthenticator.Result;
import com.silong.foundation.dj.mixmaster.configure.config.MixmasterProperties;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;
import org.jgroups.Message;
import org.jgroups.auth.AuthToken;
import org.jgroups.util.Util;

/**
 * 基于JWT的集群加入鉴权token
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-07-10 00:12
 */
@Accessors(fluent = true)
@NoArgsConstructor
public class DefaultAuthToken extends AuthToken {

  private static final String PARTITION = "partition";

  private static final String BACKUP_NUM = "backupNum";

  private static final String CLUSTER = "cluster";

  /** jwt token */
  private String jwtToken;

  private MixmasterProperties properties;

  private JwtAuthenticator jwtAuthenticator;

  /**
   * 初始化
   *
   * @param jwtAuthenticator 鉴权
   * @param properties 配置
   */
  public void initialize(
      @NonNull JwtAuthenticator jwtAuthenticator, @NonNull MixmasterProperties properties) {
    this.jwtAuthenticator = jwtAuthenticator;
    this.properties = properties;
    this.jwtToken =
        jwtAuthenticator.generate(
            Map.of(
                PARTITION,
                properties.getPartitions(),
                BACKUP_NUM,
                properties.getBackupNum(),
                CLUSTER,
                properties.getClusterName()));
  }

  @Override
  public String getName() {
    return DefaultAuthToken.class.getName();
  }

  @Override
  public int size() {
    return Util.size(Objects.requireNonNull(this.jwtToken));
  }

  @Override
  public boolean authenticate(AuthToken token, Message msg) {
    if (token instanceof DefaultAuthToken defaultAuthToken) {
      try {
        return jwtAuthenticator
            .verify(
                defaultAuthToken.jwtToken,
                claims ->
                    (properties.getBackupNum() == claims.get(BACKUP_NUM).as(Integer.class)
                            && properties.getPartitions() == claims.get(PARTITION).as(Integer.class)
                            && Objects.equals(
                                properties.getClusterName(), claims.get(CLUSTER).as(String.class)))
                        ? Result.VALID
                        : new Result(false, null))
            .isValid();
      } catch (Exception e) {
        log.error("Authentication failed.", e);
      }
    }
    return false;
  }

  @Override
  public void writeTo(DataOutput out) throws IOException {
    Util.writeString(this.jwtToken, out);
  }

  @Override
  public void readFrom(DataInput in) throws IOException {
    this.jwtToken = Util.readString(in);
  }
}
