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

import static com.silong.foundation.utilities.nlloader.NativeLibLoader.getOSDetectedClassifier;
import static com.silong.foundation.utilities.nlloader.NativeLibLoader.loadLibrary;
import static com.silong.foundation.utilities.portaudio.Utils.*;
import static com.silong.foundation.utilities.portaudio.generated.PortAudio.*;
import static com.silong.foundation.utilities.portaudio.generated.PortAudio.Pa_GetErrorText;
import static java.lang.foreign.MemorySegment.NULL;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.silong.foundation.utilities.portaudio.generated.PaStreamParameters;
import java.lang.foreign.*;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
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

  private static final int DEFAULT_QUEUE_CAPACITY = 256;

  private static final int DEFAULT_FRAMES_PER_READ = 1024;

  static final PortAudioImpl INSTANCE = new PortAudioImpl();

  private SpscArrayQueue<AudioChunk> audioChunkQueue;

  private final AtomicBoolean isRunning = new AtomicBoolean(true);

  private final ResettableCountDownLatch latch = new ResettableCountDownLatch(2);

  static {
    loadLibrary("libportaudio", "native-libs/" + getOSDetectedClassifier());
  }

  /** 禁止实例化 */
  private PortAudioImpl() {}

  @Override
  public void start(
      int sampleRate,
      SampleFormat sampleFormat,
      int channels,
      Duration audioChunkDuration,
      AudioChunkProcessor processor) {
    start(
        sampleRate,
        sampleFormat,
        channels,
        audioChunkDuration,
        DEFAULT_FRAMES_PER_READ,
        DEFAULT_QUEUE_CAPACITY,
        processor);
  }

  @Override
  public void start(
      int sampleRate,
      SampleFormat sampleFormat,
      int channels,
      Duration audioChunkDuration,
      int framesPerRead,
      int ringBufferSize,
      AudioChunkProcessor processor) {
    validateParameters(
        sampleRate,
        sampleFormat,
        channels,
        audioChunkDuration,
        framesPerRead,
        ringBufferSize,
        processor);

    log.info(
        "sampleRate:{}, channels:{}, audioChunkDuration:{}.",
        sampleRate,
        channels,
        audioChunkDuration);

    // 重置资源
    isRunning.set(true);
    latch.reset();
    audioChunkQueue = new SpscArrayQueue<>(ringBufferSize);
    int chunkSamples =
        calculateChunkSize(sampleRate, channels, (int) audioChunkDuration.toMillis());
    Arena auto = Arena.ofAuto();

    // 启动录音线程
    new Thread(
            () -> {
              log.info("Starting microphone recording......");
              MemorySegment streamPtr = NULL;
              int paNoError = paNoError();
              try (Arena arena = Arena.ofConfined()) {
                // 初始化portaudio
                int errCode = Pa_Initialize();
                if (errCode != paNoError) {
                  throw new IllegalStateException(
                      "Failed to initialize PortAudio: " + getErrorMsg(errCode));
                }

                // 获取默认输入设备
                int device = Pa_GetDefaultInputDevice();
                if (device == paNoDevice()) {
                  throw new IllegalStateException("No default input device.");
                }

                var inputParamsPtr = buildInputParameterPtr(arena, sampleFormat, device, channels);
                streamPtr = arena.allocate(C_POINTER); // 创建stream指针
                errCode =
                    Pa_OpenStream(
                        MemorySegment.ofAddress(streamPtr.address()), // 创建指针的指针
                        inputParamsPtr, // MIC输入参数指针
                        NULL, // 无输出
                        sampleRate, // 采样率
                        framesPerRead, // 缓冲区大小
                        paClipOff(), // 不进行音频裁剪
                        NULL, // 无回调函数，阻塞模式
                        NULL // 无回调函数数据
                        );
                if (errCode != paNoError) {
                  throw new IllegalStateException("Failed to open stream: " + getErrorMsg(errCode));
                }

                // 开启流
                errCode = Pa_StartStream(streamPtr);
                if (errCode != paNoError) {
                  throw new IllegalStateException(
                      "Failed to start stream: " + getErrorMsg(errCode));
                }

                log.info("Microphone recording started successfully.");

                var readBuf = arena.allocate(C_FLOAT, (long) framesPerRead * channels);

                var chunkBuf = auto.allocate(C_FLOAT, chunkSamples);
                int chunkOffset = 0;
                while (isRunning.compareAndSet(true, true)) {
                  errCode = Pa_ReadStream(streamPtr, readBuf, framesPerRead);
                  if (errCode == paInputOverflowed()) {
                    // 丢帧情况，继续
                    log.warn(
                        "Audio input overflow (code: {}). DeviceNo: '{}', SampleRate: {}Hz, Buffer: {} frames. "
                            + "Suggest: increase buffer or check CPU/device usage.",
                        paInputOverflowed(),
                        device,
                        sampleRate,
                        framesPerRead);
                    continue;
                  } else if (errCode != paNoError) {
                    log.error("Read error: {}", getErrorMsg(errCode));
                    break;
                  }

                  int got = framesPerRead * channels;
                  int cursor = 0;
                  while (cursor < got) {
                    int need = chunkSamples - chunkOffset;
                    int copy = Math.min(got - cursor, need);

                    MemorySegment.copy(
                        readBuf,
                        (long) cursor * Float.BYTES, // 源偏移（按字节计算）
                        chunkBuf,
                        (long) chunkOffset * Float.BYTES, // 目标偏移
                        (long) copy * Float.BYTES // 复制字节数
                        );

                    chunkOffset += copy;
                    cursor += copy;

                    if (chunkOffset == chunkSamples) {
                      AudioChunk audioChunk = new AudioChunk(chunkBuf, chunkSamples);
                      while (!audioChunkQueue.offer(audioChunk)) {
                        Thread.onSpinWait();
                      }
                      chunkBuf = auto.allocate(C_FLOAT, chunkSamples);
                      chunkOffset = 0;
                    }
                  }
                }

                // flush 残留
                if (chunkOffset > 0) {
                  AudioChunk audioChunk = new AudioChunk(chunkBuf, chunkOffset);
                  while (!audioChunkQueue.offer(audioChunk)) {
                    Thread.onSpinWait();
                  }
                }

                latch.countDown();
                log.info("Microphone recording stopped.");
              } catch (Throwable e) {
                log.error("Error occurred during audio recording: ", e);
              } finally {
                if (!NULL.equals(streamPtr)) {
                  Pa_StopStream(streamPtr);
                  Pa_CloseStream(streamPtr);
                }
                Pa_Terminate();
              }
            },
            "Audio-Recorder")
        .start();

    new Thread(
            () -> {
              while (isRunning.compareAndSet(true, true)) {
                audioChunkQueue.drain(processor::accept);
                Thread.onSpinWait();
              }

              // 处理残留的数据
              AudioChunk audioChunk;
              while ((audioChunk = audioChunkQueue.poll()) != null) {
                processor.accept(audioChunk);
              }
              latch.countDown();
            },
            "Audio-Processor")
        .start();
  }

  private static MemorySegment buildInputParameterPtr(
      Arena arena, SampleFormat sampleFormat, int device, int channels) {
    var ptr = PaStreamParameters.allocateArray(1, arena);
    var inputParams = PaStreamParameters.asSlice(ptr, 0);
    PaStreamParameters.device(inputParams, device);
    PaStreamParameters.channelCount(inputParams, channels);
    PaStreamParameters.sampleFormat(inputParams, sampleFormat.value());
    PaStreamParameters.suggestedLatency(inputParams, defaultLowInputLatency(device));
    PaStreamParameters.hostApiSpecificStreamInfo(inputParams, NULL);
    return ptr;
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

  private static String getErrorMsg(int errCode) {
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
    if (!isRunning.compareAndSet(true, false)) {
      Thread.onSpinWait();
    }
    latch.await(); // 等待录音停止后执行清理工作
    if (audioChunkQueue != null) {
      audioChunkQueue = null;
    }
  }
}
