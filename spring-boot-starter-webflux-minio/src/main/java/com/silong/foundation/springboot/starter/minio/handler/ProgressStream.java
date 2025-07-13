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

package com.silong.foundation.springboot.starter.minio.handler;

import java.io.IOException;
import java.io.InputStream;
import me.tongfei.progressbar.DelegatingProgressBarConsumer;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import org.slf4j.Logger;

/**
 * 下载或上传进度流
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-03-27 21:33
 */
class ProgressStream extends InputStream {
  private final InputStream in;
  private final ProgressBar pb;

  /**
   * ProgressStream implements an extends InputStream while also writing out the read progress on
   * console. ProgressStream can be used as a direct replacement for any InputStream compatible
   * input.
   *
   * @param msg Custom message string.
   * @param size Size of the progress bar.
   * @param log logger
   * @param stream InputStream to be wrapped.
   */
  public ProgressStream(String msg, long size, Logger log, InputStream stream) {
    this.in = stream;
    this.pb =
        new ProgressBarBuilder()
            .setTaskName(msg)
            .setInitialMax(size)
            .setConsumer(new DelegatingProgressBarConsumer(log::info))
            .build();
  }

  @Override
  public int available() throws IOException {
    return this.in.available();
  }

  @Override
  public void close() throws IOException {
    in.close();
  }

  @Override
  public int read() throws IOException {
    this.pb.step();
    return this.in.read();
  }

  @Override
  public int read(byte[] toStore) throws IOException {
    int readBytes = this.in.read(toStore);
    this.pb.stepBy(readBytes); // Update progress bar.
    return readBytes;
  }

  @Override
  public int read(byte[] toStore, int off, int len) throws IOException {
    int readBytes = this.in.read(toStore, off, len);
    this.pb.stepBy(readBytes);
    return readBytes;
  }

  @Override
  public long skip(long n) throws IOException {
    this.pb.stepTo(n);
    return this.in.skip(n);
  }
}
