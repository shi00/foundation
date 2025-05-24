/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 *  under the License.
 *
 */

package com.silong.foundation.va.vad;

import static ai.onnxruntime.OnnxTensor.createTensor;
import static ai.onnxruntime.OrtEnvironment.getEnvironment;
import static ai.onnxruntime.OrtSession.SessionOptions.OptLevel.ALL_OPT;

import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OrtSession.SessionOptions;
import com.silong.foundation.va.configure.config.SileroVadProperties.Mode;
import com.silong.foundation.va.configure.config.VoiceAssistantProperties;
import java.io.IOException;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

/**
 * Created by Georgiy Konovalov on 6/1/2023.
 *
 * <p>The Silero VAD algorithm, based on DNN, analyzes the audio signal to determine whether it
 * contains speech or non-speech segments. It offers higher accuracy in differentiating speech from
 * background noise compared to the WebRTC VAD algorithm.
 *
 * <p>The Silero VAD supports the following parameters:
 *
 * <p>Sample Rates:
 *
 * <p>8000Hz, 16000Hz
 *
 * <p>Frame Sizes (per sample rate):
 *
 * <p>For 8000Hz: 80, 160, 240 For 16000Hz: 160, 320, 480
 *
 * <p>Mode:
 *
 * <p>NORMAL, LOW_BITRATE, AGGRESSIVE, VERY_AGGRESSIVE
 *
 * <p>Please note that the VAD class supports these specific combinations of sample rates and frame
 * sizes, and the classifiers determine the aggressiveness of the voice activity detection
 * algorithm.
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-17 17:01
 */
@Slf4j
public class SlieroVadDetector implements VadDetector {

  private static final float SAMPLE_RATES_16KHZ = 16000.0f;
  private static final String INPUT = "input";
  private static final String SR = "sr";
  private static final String H = "h";
  private static final String C = "c";

  private static final int OUTPUT = 0;
  private static final int HN = 1;
  private static final int CN = 2;

  private float[][][] h;
  private float[][][] c;

  private int speechFramesCount;
  private int silenceFramesCount;

  private final int maxSpeechFramesCount;
  private final int maxSilenceFramesCount;
  private final long[] srArray;
  private final OrtSession session;
  private final int frameSize;
  private final float threshold;

  private final int silenceDurationMs;
  private final int speechDurationMs;

  /**
   * 构造方法
   *
   * @param properties 配置
   */
  @SneakyThrows({OrtException.class, IOException.class})
  public SlieroVadDetector(VoiceAssistantProperties properties) {
    var sr = properties.getSoundRecorder().getSamplingRate();
    if (sr != SAMPLE_RATES_16KHZ) {
      throw new IllegalArgumentException("Sampling rate must be " + SAMPLE_RATES_16KHZ);
    }

    this.srArray = new long[] {Float.valueOf(sr).longValue()};
    var vad = properties.getVad();
    this.frameSize = vad.getFrameSize().getValue();
    this.threshold = threshold(vad.getMode());
    this.session =
        getEnvironment()
            .createSession(
                new ClassPathResource(vad.getModelPath()).getContentAsByteArray(),
                buildSessionOptions());
    var sampleRate = Float.valueOf(sr).intValue();
    this.maxSilenceFramesCount =
        getFramesCount(sampleRate, frameSize, this.silenceDurationMs = vad.getSilenceDurationMs());
    this.maxSpeechFramesCount =
        getFramesCount(sampleRate, frameSize, this.speechDurationMs = vad.getSpeechDurationMs());
    this.h = new float[2][1][64];
    this.c = new float[2][1][64];
  }

  private SessionOptions buildSessionOptions() throws OrtException {
    var sessionOptions = new SessionOptions();
    sessionOptions.setIntraOpNumThreads(1);
    sessionOptions.setInterOpNumThreads(1);
    sessionOptions.addCPU(
        true); // Add a CPU device, setting to false disables CPU execution optimization
    sessionOptions.setOptimizationLevel(ALL_OPT);
    return sessionOptions;
  }

  /**
   * Calculates and returns the threshold value based on the value of detection mode. The threshold
   * value represents the confidence level required for VAD to make proper decision. ex.
   * Mode.VERY_AGGRESSIVE requiring a very high prediction accuracy from the model.
   *
   * @return threshold Float value.
   */
  private static float threshold(Mode mode) {
    return switch (mode) {
      case NORMAL -> 0.5f;
      case AGGRESSIVE -> 0.8f;
      case VERY_AGGRESSIVE -> 0.95f;
      default -> 0.0f;
    };
  }

  @Override
  public boolean isSpeech(byte[] audioData) throws Exception {
    if (audioData == null) {
      throw new IllegalArgumentException("audioData must not be null.");
    }
    return isSpeech(audioData, 0, audioData.length);
  }

  @Override
  public boolean isSpeech(byte[] audioData, int offset, int length) throws Exception {
    if (audioData == null) {
      throw new IllegalArgumentException("audioData must not be null.");
    }
    if (offset < 0 || offset >= length) {
      throw new IllegalArgumentException(
          "offset must be greater than or equal to 0 and less than " + length);
    }
    if (length > audioData.length) {
      throw new IllegalArgumentException(
          "length must be less than or equal to " + audioData.length);
    }
    return isContinuousSpeech(predict(toFloatArray(audioData, offset, length)));
  }

  /**
   * This method designed to detect long utterances without returning false positive results when
   * user makes pauses between sentences.
   *
   * @param isSpeech predicted frame result.
   * @return 'true' if speech is detected, 'false' otherwise.
   */
  private boolean isContinuousSpeech(boolean isSpeech) {
    if (isSpeech) {
      if (speechFramesCount <= maxSpeechFramesCount) {
        speechFramesCount++;
      }
      if (speechFramesCount > maxSpeechFramesCount) {
        silenceFramesCount = 0;
        return true;
      }
    } else {
      if (silenceFramesCount <= maxSilenceFramesCount) {
        silenceFramesCount++;
      }
      if (silenceFramesCount > maxSilenceFramesCount) {
        speechFramesCount = 0;
        return false;
      } else if (speechFramesCount > maxSpeechFramesCount) {
        return true;
      }
    }
    return false;
  }

  /**
   * Determines if the provided audio data contains speech based on the inference result. The audio
   * data is passed to the model for prediction. The result is obtained and compared with the
   * threshold value to determine if it represents speech.
   *
   * @param audioData audio data to analyze.
   * @throws OrtException if there was an error in creating the tensors or getting the
   *     OrtEnvironment.
   * @return 'true' if speech is detected, 'false' otherwise.
   */
  private boolean predict(float[] audioData) throws OrtException {
    float[][] inputAudioData = new float[1][frameSize];
    inputAudioData[0] = audioData;
    try (var env = getEnvironment();
        var input = createTensor(env, inputAudioData);
        var sr = createTensor(env, srArray);
        var h = createTensor(env, this.h);
        var c = createTensor(env, this.c);
        var ortOutputs = session.run(Map.of(INPUT, input, SR, sr, H, h, C, c))) {

      float[][] output = (float[][]) ortOutputs.get(OUTPUT).getValue();
      this.h = (float[][][]) ortOutputs.get(HN).getValue();
      this.c = (float[][][]) ortOutputs.get(CN).getValue();
      return output[0][0] > threshold;
    }
  }

  /**
   * Calculates the frame count based on the duration in milliseconds, frequency and frame size.
   *
   * @param durationMs duration in milliseconds.
   * @return frame count.
   */
  private static int getFramesCount(int sampleRate, int frameSize, int durationMs) {
    return durationMs / (frameSize / (sampleRate / 1000));
  }

  private static float[] toFloatArray(byte[] data, int offset, int length) {
    float[] floats = new float[(length - offset) / 2];
    for (int i = 0, j = offset; i < floats.length; i++, j++) {
      floats[i] = ((data[j * 2] & 0xff) | (data[j * 2 + 1] << 8)) / 32767.0f;
    }
    return floats;
  }

  @Override
  public void close() throws OrtException {
    session.close();
  }
}
