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

import lombok.extern.slf4j.Slf4j;
import oshi.software.os.OperatingSystem;

/**
 * 系统常量
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-21 22:17
 */
@Slf4j
public abstract class SystemInfo {

  /** 主机名 */
  public static final String HOST_NAME;

  /** 操作系统 */
  public static final String OS_NAME;

  /** 硬件uuid */
  public static final String HARDWARE_UUID;

  static {
    oshi.SystemInfo systemInfo = new oshi.SystemInfo();
    HARDWARE_UUID = systemInfo.getHardware().getComputerSystem().getHardwareUUID();
    OperatingSystem operatingSystem = systemInfo.getOperatingSystem();
    HOST_NAME = operatingSystem.getNetworkParams().getHostName();
    OS_NAME = systemInfo.getOperatingSystem().toString();
    log.info("hostName:{}, OS:{}, hardware_uuid:{}", HOST_NAME, OS_NAME, HARDWARE_UUID);
  }

  /** forbidden */
  private SystemInfo() {}
}
