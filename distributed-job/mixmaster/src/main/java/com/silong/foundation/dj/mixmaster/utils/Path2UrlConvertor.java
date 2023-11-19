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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import lombok.NonNull;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * 类型转换器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-24 16:40
 */
@Component
@ConfigurationPropertiesBinding
@SuppressFBWarnings({"PATH_TRAVERSAL_IN"})
public class Path2UrlConvertor implements Converter<String, URL> {
  @Override
  public URL convert(@NonNull String configFile) {
    try {
      return locateConfig(configFile);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException(String.format("Failed to load %s.", configFile), e);
    }
  }

  private URL locateConfig(String confFile) throws MalformedURLException {
    try {
      return new URI(confFile).toURL();
    } catch (Exception e) {
      File file = new File(confFile);
      if (file.exists() && file.isFile()) {
        return file.toURI().toURL();
      } else {
        return getClass().getClassLoader().getResource(confFile);
      }
    }
  }
}
