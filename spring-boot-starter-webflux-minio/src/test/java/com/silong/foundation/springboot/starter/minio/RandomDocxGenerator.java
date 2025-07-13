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

package com.silong.foundation.springboot.starter.minio;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;
import lombok.SneakyThrows;
import org.apache.poi.xwpf.usermodel.*;

/**
 * 随机生成docx文档
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-03-27 21:33
 */
public final class RandomDocxGenerator {
  private RandomDocxGenerator() {}

  private static final String CHARS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 "; // 可扩展字符集
  private static final Random random = new Random();

  @SneakyThrows
  public static void generateDocx(File file, long targetSizeBytes) {
    try (XWPFDocument document = new XWPFDocument();
        FileOutputStream fos = new FileOutputStream(file)) {

      long currentSize = 0;
      // 生成内容直到达到或超过目标大小
      while (currentSize < targetSizeBytes) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        String randomText = generateRandomText(1000); // 每段1000字符
        run.setText(randomText);

        // 估算当前文档大小
        ByteArrayOutputStream tempStream = new ByteArrayOutputStream();
        document.write(tempStream);
        currentSize = tempStream.size();
        tempStream.reset();
      }
      document.write(fos);
    }
  }

  // 生成随机字符串（可调整长度）
  private static String generateRandomText(int length) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < length; i++) {
      sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
    }
    return sb.toString();
  }
}
