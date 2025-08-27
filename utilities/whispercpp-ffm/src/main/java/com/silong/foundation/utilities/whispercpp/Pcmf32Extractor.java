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

import static com.silong.foundation.utilities.whispercpp.WhisperCpp.SUPPORTED_CHANNELS;
import static com.silong.foundation.utilities.whispercpp.WhisperCpp.SUPPORTED_SAMPLED_RATE;

import java.io.*;
import lombok.extern.slf4j.Slf4j;
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

  // 复用的线程本地缓冲，降低短期分配与 GC 压力
  private static final int IO_BUF_SIZE = 8192;
  private static final int PCM_BATCH = 32768;
  private static final ThreadLocal<short[]> TL_S16_BATCH =
      ThreadLocal.withInitial(() -> new short[PCM_BATCH]);

  // 用于受限头部探测与回推的缓冲
  private static final int PEEK_BUF_SIZE = 64 * 1024;
  private static final ThreadLocal<byte[]> TL_PEEK_BUF =
      ThreadLocal.withInitial(() -> new byte[PEEK_BUF_SIZE]);

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
   * 把输入的音频文件流转为pcmf32格式
   *
   * @param inputStream 音频输入流
   * @return pcmf32格式的音频数据
   * @throws IOException 异常
   */
  public static float[] extract(InputStream inputStream) throws IOException {
    final int sampleRate = SUPPORTED_SAMPLED_RATE; // 16000
    final int channels = SUPPORTED_CHANNELS; // 1
    final float S16_TO_F32 = 1.0f / 32768.0f;
    final int BATCH = PCM_BATCH;

    long t0 = System.currentTimeMillis();

    // 流式快速路径：仅读取并缓存头部（<=64KB），判断 WAV/16k/mono 后直接边读边转换
    PushbackInputStream pin =
        new PushbackInputStream(new BufferedInputStream(inputStream, IO_BUF_SIZE), PEEK_BUF_SIZE);
    float[] fast = fastWavStreamToPcmf32If16kMono(pin);
    if (fast != null) {
      long ms = Math.max(1, System.currentTimeMillis() - t0);
      if (log.isDebugEnabled()) {
        log.debug(
            "convertToPcmf32: mode=fast-wav(stream), samples={}, time={}ms, thr={} samp/ms",
            fast.length,
            ms,
            fast.length / ms);
      }
      return fast;
    }

    // 回退路径：未命中快速路径，回推已探测字节后直接交给 FFmpeg（仅一次解码 + 重采样）
    float[] pcmf32;
    int total = 0;
    final short[] sbuf = TL_S16_BATCH.get();

    try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(pin)) {
      // 轻量探测 + 多线程解码
      grabber.setOption("analyzeduration", "0");
      grabber.setOption("probesize", "32k");
      grabber.setOption("threads", "0");
      // 强制音频滤镜输出 s16/mono/16k（提升兼容性）
      grabber.setOption(
          "af", "aresample=osf=s16:ocl=mono:osr=" + sampleRate + ":filter_size=32:cutoff=0.99");
      grabber.setSampleRate(sampleRate);
      grabber.setAudioChannels(channels);
      grabber.start();

      long durUs = grabber.getLengthInTime(); // 微秒
      int est =
          durUs > 0
              ? (int)
                  Math.min(Integer.MAX_VALUE, (durUs * sampleRate) / 1_000_000L * 11 / 10) // +10%
              : sampleRate * 12;
      pcmf32 = new float[Math.max(est, BATCH)];

      Frame frame;
      while ((frame = grabber.grabSamples()) != null) {
        if (frame.samples == null || frame.samples.length == 0) continue;
        java.nio.Buffer buf = frame.samples[0];

        if (buf instanceof java.nio.ShortBuffer sb) {
          while (sb.hasRemaining()) {
            int len = Math.min(sb.remaining(), BATCH);
            sb.get(sbuf, 0, len);

            if (total + len > pcmf32.length) {
              int newCap = Math.max(pcmf32.length << 1, total + len);
              float[] tmp = new float[newCap];
              System.arraycopy(pcmf32, 0, tmp, 0, total);
              pcmf32 = tmp;
            }
            for (int i = 0; i < len; i++) {
              pcmf32[total + i] = sbuf[i] * S16_TO_F32;
            }
            total += len;
          }
        } else if (buf instanceof java.nio.FloatBuffer fb) {
          // 罕见：直接浮点输出（夹紧到 [-1,1]）
          while (fb.hasRemaining()) {
            int len = Math.min(fb.remaining(), BATCH);
            if (total + len > pcmf32.length) {
              int newCap = Math.max(pcmf32.length << 1, total + len);
              float[] tmp = new float[newCap];
              System.arraycopy(pcmf32, 0, tmp, 0, total);
              pcmf32 = tmp;
            }
            for (int i = 0; i < len; i++) {
              float v = fb.get();
              pcmf32[total + i] = (v > 1f ? 1f : (v < -1f ? -1f : v));
            }
            total += len;
          }
        }
      }
    }

    if (total != pcmf32.length) {
      float[] trimmed = new float[total];
      System.arraycopy(pcmf32, 0, trimmed, 0, total);
      pcmf32 = trimmed;
    }

    long ms = Math.max(1, System.currentTimeMillis() - t0);
    if (log.isDebugEnabled()) {
      log.debug(
          "convertToPcmf32: mode=ffmpeg, samples={}, time={}ms, thr={} samp/ms",
          pcmf32.length,
          ms,
          pcmf32.length / ms);
    }
    return pcmf32;
  }

  // 流式 WAV 快速路径（仅缓存头部<=64KB，数据段边读边转），支持 PCM s16 与 IEEE f32
  private static float[] fastWavStreamToPcmf32If16kMono(PushbackInputStream in) throws IOException {
    final byte[] scratch = TL_PEEK_BUF.get();
    // int used = 0;
    final int[] usedRef = {0}; // 使用可变引用包装，供 lambda 与外层共同维护

    // 工具：读取 n 字节到 scratch（用于可回推），不足或溢出即失败
    java.util.function.IntPredicate readToScratch =
        (n) -> {
          if (usedRef[0] + n > scratch.length) return false;
          int off = usedRef[0], need = n;
          try {
            while (need > 0) {
              int r = in.read(scratch, off, need);
              if (r < 0) return false;
              off += r;
              need -= r;
            }
          } catch (IOException e) {
            return false;
          }
          usedRef[0] += n;
          return true;
        };

    // 读取 RIFF/WAVE 头
    if (!readToScratch.test(12)) {
      in.unread(scratch, 0, usedRef[0]);
      return null;
    }
    if (!(scratch[0] == 'R' && scratch[1] == 'I' && scratch[2] == 'F' && scratch[3] == 'F')
        || !(scratch[8] == 'W' && scratch[9] == 'A' && scratch[10] == 'V' && scratch[11] == 'E')) {
      in.unread(scratch, 0, usedRef[0]);
      return null;
    }

    Integer audioFormat = null, numChannels = null, sampleRate = null, bitsPerSample = null;
    Integer dataSize = null;

    // 解析 chunk，直到拿到 fmt 与 data
    while (true) {
      if (!readToScratch.test(8)) { // chunk header
        in.unread(scratch, 0, usedRef[0]);
        return null;
      }
      int base = usedRef[0] - 8;
      int id0 = scratch[base] & 0xFF,
          id1 = scratch[base + 1] & 0xFF,
          id2 = scratch[base + 2] & 0xFF,
          id3 = scratch[base + 3] & 0xFF;
      int chunkSize =
          (scratch[base + 4] & 0xFF)
              | ((scratch[base + 5] & 0xFF) << 8)
              | ((scratch[base + 6] & 0xFF) << 16)
              | ((scratch[base + 7] & 0xFF) << 24);

      boolean isFmt = (id0 == 'f' && id1 == 'm' && id2 == 't' && id3 == ' ');
      boolean isData = (id0 == 'd' && id1 == 'a' && id2 == 't' && id3 == 'a');

      if (isFmt) {
        int need = Math.min(16, chunkSize);
        if (!readToScratch.test(need)) {
          in.unread(scratch, 0, usedRef[0]);
          return null;
        }
        int off = usedRef[0] - need;
        audioFormat = (scratch[off] & 0xFF) | ((scratch[off + 1] & 0xFF) << 8);
        numChannels = (scratch[off + 2] & 0xFF) | ((scratch[off + 3] & 0xFF) << 8);
        sampleRate =
            (scratch[off + 4] & 0xFF)
                | ((scratch[off + 5] & 0xFF) << 8)
                | ((scratch[off + 6] & 0xFF) << 16)
                | ((scratch[off + 7] & 0xFF) << 24);
        bitsPerSample = (scratch[off + 14] & 0xFF) | ((scratch[off + 15] & 0xFF) << 8);
        int skip = chunkSize - need + (chunkSize & 1);
        if (skip > 0 && !readToScratch.test(skip)) {
          in.unread(scratch, 0, usedRef[0]);
          return null;
        }
      } else if (isData) {
        dataSize = chunkSize;
        break; // data 内容不读入 scratch，保持流位置在 data 起始
      } else {
        int skip = chunkSize + (chunkSize & 1);
        if (skip > 0 && !readToScratch.test(skip)) {
          in.unread(scratch, 0, usedRef[0]);
          return null;
        }
      }
    }

    if (audioFormat == null
        || numChannels == null
        || sampleRate == null
        || bitsPerSample == null
        || dataSize == null) {
      in.unread(scratch, 0, usedRef[0]);
      return null;
    }
    if (numChannels != 1 || sampleRate != SUPPORTED_SAMPLED_RATE || dataSize <= 0) {
      in.unread(scratch, 0, usedRef[0]);
      return null;
    }

    // ... existing code ...
    // 非目标编码：回推已读头部，交给 FFmpeg
    in.unread(scratch, 0, usedRef[0]);
    return null;
  }
}
