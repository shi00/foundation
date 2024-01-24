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

package com.silong.foundation.duuid.server.configure.properties;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

/**
 * 基于etcdv3的workerId分配器配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-11 22:42
 */
@Data
@Validated
@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "只读初始配置")
public class EtcdProperties {

  /** 服务器地址 */
  private List<String> serverAddresses;

  /**
   * Trusted certificates for verifying the remote endpoint's certificate. The file should contain
   * an X.509 certificate collection in PEM format.
   */
  private String trustCertCollectionFile = EMPTY;

  /** a PKCS#8 private key file in PEM format */
  private String keyFile = EMPTY;

  /** an X.509 certificate chain file in PEM format */
  private String keyCertChainFile = EMPTY;

  /** 用户名 */
  private String userName = EMPTY;

  /** 密码 */
  private String password = EMPTY;
}
