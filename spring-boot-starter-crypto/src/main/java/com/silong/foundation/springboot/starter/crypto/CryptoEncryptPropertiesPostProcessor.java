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

package com.silong.foundation.springboot.starter.crypto;

import com.silong.foundation.crypto.RootKey;
import com.silong.foundation.crypto.aes.AesGcmToolkit;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.*;

/**
 * crypto加密属性后处理器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-05-21 13:41
 */
@Slf4j
public class CryptoEncryptPropertiesPostProcessor implements EnvironmentPostProcessor, Ordered {

  private static final String CRYPTO_WORK_KEY_NAME =
      System.getProperty("crypto.work-key.property-name", "crypto.work-key").trim();

  static {
    RootKey.initialize();
  }

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    if (!environment.containsProperty(CRYPTO_WORK_KEY_NAME)) {
      log.debug(
          "The working key that crypto depends on cannot be found, and subsequent processing is skipped. keyName: {}",
          CRYPTO_WORK_KEY_NAME);
      return;
    }

    String workKey = environment.getProperty(CRYPTO_WORK_KEY_NAME);

    // 读取所有配置
    var map = new HashMap<>();
    for (PropertySource<?> propertySource : environment.getPropertySources()) {
      if (propertySource instanceof MapPropertySource source) {
        map.putAll(source.getSource());
      }
    }

    var props = new Properties();
    map.entrySet().stream()
        .filter(
            e ->
                !CRYPTO_WORK_KEY_NAME.equals(e.getKey().toString().trim())
                    && e.getValue() instanceof CharSequence v
                    && v.toString().trim().startsWith("security:"))
        .map(e -> convert(e, workKey))
        .filter(Objects::nonNull)
        .forEach(e -> props.put(e.getKey(), e.getValue()));

    if (!props.isEmpty()) {
      MutablePropertySources propertySources = environment.getPropertySources();
      propertySources.addFirst(new PropertiesPropertySource("crypto-decrypt-sources", props));
    }
  }

  private static SimpleEntry<Object, String> convert(Map.Entry<Object, Object> e, String workKey) {
    try {
      return new SimpleEntry<>(
          e.getKey().toString().trim(),
          AesGcmToolkit.decrypt(e.getValue().toString().trim(), workKey));
    } catch (Exception ex) {
      log.error(String.format("Failed to covert %s to decrypt property.", e), ex);
      return null;
    }
  }

  @Override
  public int getOrder() {
    return Integer.MAX_VALUE;
  }
}
