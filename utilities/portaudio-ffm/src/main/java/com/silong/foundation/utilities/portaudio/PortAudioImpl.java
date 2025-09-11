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

package com.silong.foundation.utilities.portaudio;

import static com.silong.foundation.utilities.portaudio.Utils.free;
import static com.silong.foundation.utilities.portaudio.generated.PortAudio.*;
import static com.silong.foundation.utilities.portaudio.generated.PortAudio.Pa_GetErrorText;
import static java.lang.foreign.MemorySegment.NULL;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import com.silong.foundation.utilities.portaudio.generated.PaStreamParameters;
import java.lang.foreign.*;
import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.jctools.queues.SpmcArrayQueue;
import org.jctools.queues.SpscArrayQueue;

/**
 * portaudio实现
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-09-11 14:52
 */
@Slf4j
class PortAudioImpl implements PortAudio {

  private static final int QUEUE_CAPACITY;

  private static final int AUDIO_CHUNK_WORKER_NUM;

  static final PortAudioImpl INSTANCE = new PortAudioImpl();

  private ExecutorService executor;

  private Queue<MemorySegment> audioChunkQueue;

  private final AtomicBoolean isRunning = new AtomicBoolean(true);

  private final AtomicInteger counter = new AtomicInteger(0);

  private final ResettableCountDownLatch latch = new ResettableCountDownLatch(1);

  static {
    QUEUE_CAPACITY = Integer.parseInt(System.getProperty("audio.chunk.queue.capacity", "128"));
    AUDIO_CHUNK_WORKER_NUM =
        Math.max(
            Integer.parseInt(System.getProperty("audio.chunk.workers.num", "4")),
            Runtime.getRuntime().availableProcessors() / 2);
  }

  /** 禁止实例化 */
  private PortAudioImpl() {}

  @Override
  public void start(
      int sampleRate,
      int channels,
      Duration audioChunkDuration,
      boolean parallel,
      AudioChunkProcessor callback)
      throws Exception {
    log.info(
        "sampleRate:{}, channels:{}, audioChunkDuration:{}, parallel:{}",
        sampleRate,
        channels,
        audioChunkDuration,
        parallel);

    // 重置资源
    isRunning.set(true);
    counter.set(0);
    latch.reset();
    executor = buildThreadPool(parallel);
    audioChunkQueue = buildAudioChunckQueue(parallel);

    // 启动录音线程
    new Thread(
            () -> {
              log.info("Starting microphone recording ......");
              try (var arena = Arena.ofConfined()) {
                int errCode = Pa_Initialize();
                if (errCode != paNoError()) {
                  throw new IllegalStateException(
                      "Failed to initialize PortAudio: " + getErrorText(errCode));
                }

                var inputParamsPtr = buildInputParameters(arena, channels);

                MemorySegment paStreamPtrPtr = arena.allocate(C_POINTER);

                errCode =
                    Pa_OpenStream(
                        paStreamPtrPtr,
                        inputParamsPtr,
                        NULL,
                        sampleRate,
                        1024,
                        paClipOff(),
                        NULL,
                        NULL);
                if (errCode != paNoError()) {
                  throw new IllegalStateException(
                      "Failed to open stream: " + getErrorText(errCode));
                }

                errCode = Pa_StartStream(paStreamPtrPtr);
                if (errCode != paNoError()) {
                  throw new IllegalStateException(
                      "Failed to start stream: " + getErrorText(errCode));
                }

                while (isRunning.compareAndSet(true, true)) {
                  Pa_ReadStream();
                }

                latch.countDown();
              } finally {
                Pa_Terminate();
              }
            },
            "Audio-Recorder")
        .start();
  }

  private static MemorySegment buildInputParameters(Arena arena, int channels) {
    int device = Pa_GetDefaultInputDevice();
    if (device == paNoDevice()) {
      throw new IllegalStateException("No default input device.");
    }

    var inputParamsPtr = PaStreamParameters.allocateArray(1, arena);
    var inputParams = PaStreamParameters.asSlice(inputParamsPtr, 0);

    PaStreamParameters.device(inputParams, device);
    PaStreamParameters.channelCount(inputParams, channels);
    PaStreamParameters.sampleFormat(inputParams, paFloat32());
    PaStreamParameters.suggestedLatency(inputParams, defaultLowInputLatency(device));
    PaStreamParameters.hostApiSpecificStreamInfo(inputParams, NULL);
    return inputParamsPtr;
  }

  private static Queue<MemorySegment> buildAudioChunckQueue(boolean parallel) {
    return parallel ? new SpmcArrayQueue<>(QUEUE_CAPACITY) : new SpscArrayQueue<>(QUEUE_CAPACITY);
  }

  private ExecutorService buildThreadPool(boolean parallel) {
    return parallel
        ? newFixedThreadPool(
            AUDIO_CHUNK_WORKER_NUM,
            t -> new Thread(t, "Audio-Chunk-Processor" + counter.getAndIncrement()))
        : newSingleThreadExecutor(t -> new Thread(t, "Audio-Chunk-Processor"));
  }

  /**
   * 按照采样率，声道数，块时长计算样本数
   *
   * @param sampleRate 采样率
   * @param channels 声道数
   * @param chunkDurationMs 音频块时长，单位毫秒
   * @return 样本数
   */
  private static int calculateChunkSize(int sampleRate, int channels, int chunkDurationMs) {
    return sampleRate * chunkDurationMs / 1000 * channels;
  }

  private static String getErrorText(int errCode) {
    MemorySegment msgPtr = Pa_GetErrorText(errCode);
    try {
      return msgPtr.getString(0, UTF_8);
    } finally {
      free(msgPtr);
    }
  }

  private static double defaultLowInputLatency(int device) {
    MemorySegment deviceInfoPtr = Pa_GetDeviceInfo(device);
    try {
      return com.silong.foundation.utilities.portaudio.generated.PaDeviceInfo
          .defaultLowInputLatency(deviceInfoPtr);
    } finally {
      free(deviceInfoPtr);
    }
  }

  /** 停止录音，释放资源 */
  @Override
  public void close() throws Exception {
    isRunning.set(false); // 设置信号，停止录音
    latch.await(); // 等待录音停止后执行清理工作
    if (audioChunkQueue != null) {
      audioChunkQueue.clear();
      audioChunkQueue = null;
    }
    if (executor != null) {
      executor.shutdown();
      executor = null;
    }
  }
}
