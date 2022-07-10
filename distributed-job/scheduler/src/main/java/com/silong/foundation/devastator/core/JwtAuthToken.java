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

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.silong.foundation.devastator.config.DevastatorConfig;
import edu.umd.cs.findbugs.annotations.NonNull;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jgroups.Message;
import org.jgroups.auth.AuthToken;
import org.jgroups.util.Util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;

/**
 * 基于JWT的集群加入鉴权token
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-07-10 00:12
 */
@Accessors(fluent = true)
@NoArgsConstructor
public class JwtAuthToken extends AuthToken {

  public static final String PARTITIONS = "partitions";
  public static final String BACKUP_NUM = "backupNum";

  /** jwt token payload */
  private String jwtToken;

  /** 集群配置 */
  private DevastatorConfig config;

  /** token校验器 */
  private JWTVerifier verifier;

  public void setConfig(@NonNull DevastatorConfig config) {
    this.jwtToken = generate(config);
  }

  private String generate(DevastatorConfig config) {
    var algorithm = Algorithm.HMAC256(config.authTokenConfig().authKey());
    String clusterName = config.clusterName();
    this.config = config;
    this.verifier = JWT.require(algorithm).withIssuer(clusterName).build();
    return this.jwtToken =
            JWT.create()
                    .withIssuer(clusterName)
                    .withPayload(
                            Map.of(PARTITIONS, config.partitionCount(), BACKUP_NUM, config.backupNums()))
                    .sign(algorithm);
  }

  @Override
  public String getName() {
    return JwtAuthToken.class.getName();
  }

  @Override
  public int size() {
    return Util.size(jwtToken);
  }

  @Override
  public boolean authenticate(AuthToken token, Message msg) {
    if (token instanceof JwtAuthToken jwtAuthToken) {
      try {
        DecodedJWT jwt = verifier.verify(jwtAuthToken.jwtToken);
        return config.clusterName().equals(jwt.getIssuer())
            && config.backupNums() == jwt.getClaim(BACKUP_NUM).as(Integer.class)
            && config.partitionCount() == jwt.getClaim(PARTITIONS).as(Integer.class);
      } catch (Exception e) {
        log.error("Failed to authenticate with JWT-Token.", e);
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
