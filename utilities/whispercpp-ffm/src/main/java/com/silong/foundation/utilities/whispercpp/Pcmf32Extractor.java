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

package com.silong.foundation.utilities.whispercpp;

import static com.silong.foundation.utilities.whispercpp.WhisperCpp.*;

import java.io.*;
import java.nio.*;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;

/**
 * 音频数据转pcmf32格式工具类
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-22 18:45
 */
@Slf4j
class Pcmf32Extractor {

  /** IO缓冲区大小 */
  private static final int IO_BUF_SIZE = 8192;

  private static final int DEFAULT_SIZE = SUPPORTED_CHANNELS * SUPPORTED_SAMPLED_RATE * 10;

  /** 十秒缓存 */
  private static final ThreadLocal<float[]> PCM_BUF =
      ThreadLocal.withInitial(() -> new float[DEFAULT_SIZE]);

  /** 禁止实例化 */
  private Pcmf32Extractor() {}

  /**
   * 把输入的音频文件转为pcmf32格式
   *
   * @param audioFile 音频输入流
   * @return pcmf32格式的音频数据
   * @throws IOException 异常
   */
  public static float[] extract(File audioFile) throws IOException {
    try (InputStream in = new BufferedInputStream(new FileInputStream(audioFile), IO_BUF_SIZE)) {
      return extract(in);
    }
  }

  /**
   * 将音频文件转换为whisper.cpp所需的pcmf32数组（兼容新版本JavaCV）
   *
   * @param inputStream 音频文件路径
   * @return 转换后的float数组
   * @throws IOException 处理过程中的异常
   */
  public static float[] extract(InputStream inputStream) throws IOException {
    try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputStream)) {
      // 设置音频参数以匹配whisper.cpp要求
      grabber.setAudioChannels(SUPPORTED_CHANNELS); // 单声道
      grabber.setSampleRate(SUPPORTED_SAMPLED_RATE); // 16kHz采样率
      grabber.setAudioBitrate(SUPPORT_BIT_RATE); // 比特率
      grabber.setSampleFormat(avutil.AV_SAMPLE_FMT_FLT); // 浮点格式
      grabber.setOption("threads", "0");
      grabber.start();

      // 预估数组大小以减少重新分配
      int estimatedSize = (int) (grabber.getLengthInTime() * grabber.getSampleRate() / 1000_1000);
      float[] pcmData = estimatedSize > DEFAULT_SIZE ? new float[estimatedSize] : PCM_BUF.get();
      int offset = 0;

      Frame frame;
      while ((frame = grabber.grabSamples()) != null) {
        if (frame.samples != null && frame.samples[0] instanceof FloatBuffer buffer) {
          int remaining = buffer.remaining();

          // 如果数组不够大，则扩容
          if (offset + remaining > pcmData.length) {
            float[] newArray =
                new float[Math.max(((int) (pcmData.length * 1.1)), offset + remaining)];
            System.arraycopy(pcmData, 0, newArray, 0, offset);
            pcmData = newArray;
          }

          // 直接从buffer获取数据
          buffer.get(pcmData, offset, remaining);
          offset += remaining;
        }
      }

      grabber.stop();

      // 返回实际大小的数组
      if (offset < pcmData.length) {
        float[] result = new float[offset];
        System.arraycopy(pcmData, 0, result, 0, offset);
        return result;
      }

      return pcmData;
    }
  }
}
