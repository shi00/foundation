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

package com.silong.foundation.va.service;

import static java.lang.System.lineSeparator;
import static javax.sound.sampled.AudioFileFormat.Type.WAVE;
import static javax.sound.sampled.AudioSystem.isLineSupported;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.google.common.io.ByteSource;
import com.silong.foundation.utilities.whispercpp.Whisper;
import com.silong.foundation.va.configure.config.SoundRecorderProperties;
import com.silong.foundation.va.configure.config.VoiceAssistantProperties;
import com.silong.foundation.va.vad.VadDetector;
import dev.langchain4j.model.chat.ChatLanguageModel;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sound.sampled.*;
import lombok.extern.slf4j.Slf4j;
import org.jctools.queues.SpscArrayQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 语音助手服务
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-05-23 18:54
 */
@Slf4j
@RestController
@RequestMapping("/voice-assistant")
public class VoiceAssistantService {

  /**
   * 音频数据
   *
   * @author louis sin
   * @version 1.0.0
   * @since 2024-05-24 21:13
   * @param byteSource 音频数据
   * @param length 长度
   * @param isSpeech 是否包含语音
   */
  private record AudioBytes(ByteSource byteSource, int length, boolean isSpeech) {}

  /** 服务启动后是否自动启动语音交互 */
  private boolean autoStartUp;

  /** 音频格式，为Whisper cpp准备输入音频 */
  private AudioFormat audioFormat;

  /** chatgpt model */
  private ChatLanguageModel chatLanguageModel;

  /** 静音检测 */
  private VadDetector vadDetector;

  /** speech to text */
  private Whisper whisper;

  /** 单生产者，多消费者环状队列 */
  private final SpscArrayQueue<ByteArrayInputStream> audioInputStreamQueue =
      new SpscArrayQueue<>(1024);

  /** 录音启动标识 */
  private final AtomicBoolean startFlag = new AtomicBoolean(false);

  /** 停止标识 */
  private volatile boolean stopFlag;

  private int bufferSize;

  private int framesCountForTeminated;

  /**
   * 启动语音助手
   *
   * @return 响应
   */
  @PostMapping(
      value = "/start",
      consumes = APPLICATION_JSON_VALUE,
      produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(value = ACCEPTED)
  public Mono<Void> start() {
    doStart();
    return Mono.empty();
  }

  private void doStart() {
    if (startFlag.compareAndSet(false, true)) {
      CountDownLatch latch = new CountDownLatch(2);
      stopFlag = false;

      // 启动录音线程
      new Thread(
              () -> {
                try {
                  recordAudio();
                } catch (Exception e) {
                  log.error("Failed to record audio.", e);
                } finally {
                  latch.countDown();
                }
              },
              "Audio-Recorder")
          .start();

      // 启动语音识别线程
      new Thread(
              () -> {
                try {
                  while (!stopFlag) {
                    ByteArrayInputStream inputStream = audioInputStreamQueue.poll();
                    if (inputStream != null) {
                      String[] text = whisper.speech2Text(inputStream);
                      log.info(
                          "Speech Recognition: {}",
                          text != null ? String.join(lineSeparator(), text) : null);
                    } else {
                      Thread.onSpinWait();
                    }
                  }
                } catch (Exception e) {
                  log.error("Speech recognition error occurred: ", e);
                } finally {
                  latch.countDown();
                }
              },
              "Whisper-S2T")
          .start();

      // 启动监控线程
      new Thread(
              () -> {
                try {
                  latch.await();
                  startFlag.compareAndSet(true, false);
                } catch (InterruptedException e) {
                  log.error("Service internal error.", e);
                }
              },
              "Audio-Processor-Watcher")
          .start();

      log.info("Service started successfully.");
    } else {
      log.info("Service has been started.");
    }
  }

  /**
   * 停止语音助手
   *
   * @return 响应
   */
  @PostMapping(
      value = "/stop",
      consumes = APPLICATION_JSON_VALUE,
      produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(value = ACCEPTED)
  public Mono<Void> stop() {
    stopFlag = true;
    while (startFlag.get()) {
      Thread.onSpinWait();
    }
    return Mono.empty();
  }

  /**
   * 启动录音
   *
   * @throws Exception 异常
   */
  private void recordAudio() throws Exception {
    DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
    if (!isLineSupported(info)) {
      throw new UnsupportedOperationException("Line not supported: " + info);
    }

    try (TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info)) {
      LinkedList<AudioBytes> cache = new LinkedList<>();
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024 * 1024);
      microphone.open(audioFormat);
      microphone.start(); // start capturing

      log.info("Start listening to the user voice ... ");

      // Main loop to continuously read data and apply Voice Activity Detection
      while (!stopFlag && microphone.isOpen()) {
        byte[] buf = new byte[bufferSize];
        int numBytesRead = microphone.read(buf, 0, buf.length);
        if (numBytesRead <= 0) {
          log.warn("Reading data length from microphone is {}", numBytesRead);
          continue;
        }

        byte[] buffer;
        if (numBytesRead != buf.length) {
          buffer = new byte[numBytesRead];
          System.arraycopy(buf, 0, buffer, 0, numBytesRead);
        } else {
          buffer = buf;
        }

        // 静音检测，如果没有语音对话
        boolean isSpeech = vadDetector.isSpeech(buffer, 0, buffer.length);
        cache.add(new AudioBytes(ByteSource.wrap(buffer), buffer.length, isSpeech));
        if (!isSpeech && isFinish(cache)) {
          try (AudioInputStream ais =
              new AudioInputStream(
                  ByteSource.concat(
                          cache.stream()
                              .limit(cache.size() - framesCountForTeminated) // 忽略最后两秒的静音
                              .map(AudioBytes::byteSource)
                              .toArray(ByteSource[]::new))
                      .openBufferedStream(),
                  audioFormat,
                  cache.stream().mapToInt(audioBytes -> audioBytes.length).sum())) {
            AudioSystem.write(ais, WAVE, byteArrayOutputStream);
            audioInputStreamQueue.offer(
                new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
          } finally {
            byteArrayOutputStream.reset();
            cache.clear();
          }
        }
      }
    } finally {
      log.info("Stop listening to the user's voice.");
    }
  }

  /**
   * 是否已经结束输入
   *
   * @param cache 输入缓存
   * @return true or false
   */
  private boolean isFinish(LinkedList<AudioBytes> cache) {
    return cache.size() > framesCountForTeminated
        && cache.stream().skip(cache.size() - framesCountForTeminated).noneMatch(e -> e.isSpeech);
  }

  /** 是否执行自启动 */
  @PostConstruct
  void postProcess() {
    if (autoStartUp) {
      doStart();
    }
  }

  @Autowired
  public void setVoiceAssistantProperties(VoiceAssistantProperties voiceAssistantProperties) {
    SoundRecorderProperties soundRecorder = voiceAssistantProperties.getSoundRecorder();
    this.bufferSize =
        (Float.valueOf(soundRecorder.getSamplingRate()).intValue()
                * soundRecorder.getSampleSizeInBits()
                * soundRecorder.getChannels())
            / Byte.SIZE
            * 2;
    this.autoStartUp = voiceAssistantProperties.isAutoStart();

    // 输入终结帧数
    this.framesCountForTeminated = 1;
    this.audioFormat =
        new AudioFormat(
            soundRecorder.getSamplingRate(),
            soundRecorder.getSampleSizeInBits(),
            soundRecorder.getChannels(),
            soundRecorder.isSigned(),
            soundRecorder.isBigEndian());
  }

  @Autowired
  public void setVadDetector(VadDetector vadDetector) {
    this.vadDetector = vadDetector;
  }

  @Autowired
  public void setWhisper(Whisper whisper) {
    this.whisper = whisper;
  }

  @Autowired
  public void setChatLanguageModel(ChatLanguageModel chatLanguageModel) {
    this.chatLanguageModel = chatLanguageModel;
  }
}
