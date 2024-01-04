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

package com.silong.foundation.utilities.nlloader;

import static com.silong.foundation.utilities.nlloader.NativeLibLoader.OS_NAME;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.zip.ZipFile.OPEN_READ;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import lombok.NonNull;
import lombok.SneakyThrows;

/**
 * jar文件工具类
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-01-03 9:19
 */
public final class JarUtils {

  /**
   * 把目标jar文件内的共享库抽取至给定目录
   *
   * @param jarFile jar文件
   * @param targetDir 目标目录
   */
  @SneakyThrows(IOException.class)
  public static void extractNativeLibs(@NonNull Path jarFile, @NonNull Path targetDir) {
    PlatformLibFormat format = PlatformLibFormat.match(OS_NAME);
    Files.createDirectories(targetDir);
    try (JarFile archive = new JarFile(jarFile.toFile(), true, OPEN_READ)) {
      // sort entries by name to always create folders first
      List<? extends ZipEntry> entries =
          archive.stream().sorted(Comparator.comparing(ZipEntry::getName)).toList();
      // copy each entry in the dest path
      for (ZipEntry entry : entries) {
        if (entry.isDirectory()) {
          continue;
        }

        String name = entry.getName();
        if (name.endsWith(format.libFormat)) {
          int index = name.lastIndexOf('/');
          Path entryDest =
              index == -1 ? targetDir.resolve(name) : targetDir.resolve(name.substring(index + 1));
          Files.copy(archive.getInputStream(entry), entryDest, REPLACE_EXISTING);
        }
      }
    }
  }

  /**
   * 根据jar包内包含的任一class，找到jar文件路径
   *
   * @param aClass jar包内包含的class
   * @return jar文件路径
   */
  @SneakyThrows(URISyntaxException.class)
  public static Path locateJarFile(@NonNull Class<?> aClass) {
    try {
      return byGetProtectionDomain(aClass);
    } catch (URISyntaxException e) {
      return byGetResource(aClass);
    }
  }

  @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "获取代码位置")
  static Path byGetProtectionDomain(Class<?> clazz) throws URISyntaxException {
    return Paths.get(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
  }

  static Path byGetResource(Class<?> clazz) throws URISyntaxException {
    URL classResource = clazz.getResource(String.format("%s.class", clazz.getSimpleName()));
    if (classResource == null) {
      throw new IllegalStateException(
          String.format("%s cannot be found from classpath jars.", clazz.getName()));
    }
    String url = classResource.toString();
    if (url.startsWith("jar:file:")) {
      // extract 'file:......jarName.jar' part from the url string
      String path = url.replaceAll("^jar:(file:.*[.]jar)!/.*", "$1");
      return Paths.get(URI.create(path));
    }
    throw new IllegalStateException(String.format("Invalid Jar File URL: %s", url));
  }

  /** forbidden */
  private JarUtils() {}
}
