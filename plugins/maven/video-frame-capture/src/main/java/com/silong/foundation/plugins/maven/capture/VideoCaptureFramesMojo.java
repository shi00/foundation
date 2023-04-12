/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.silong.foundation.plugins.maven.capture;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Path;
import java.util.Arrays;
import javax.imageio.ImageIO;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

/**
 * 定制Properties文件
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-28 22:47
 */
@Mojo(name = "capture-frames", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class VideoCaptureFramesMojo extends AbstractMojo {

  /** 视频抓帧后生成的图片目录 */
  @Parameter(property = "outputDirectory", readonly = true)
  @Getter
  @Setter
  private File outputDirectory;

  /** 视频所在目录，会递归目录下所有的视频文件进行抓帧处理 */
  @Parameter(property = "videosDirectory", required = true)
  @Getter
  @Setter
  private File videosDirectory;

  /** 需要抓取的视频帧序号，如果抓中间帧指定此值为-1 (defaults to 1) */
  @Parameter(property = "captureFrameNo", defaultValue = "1")
  @Getter
  @Setter
  private int captureFrameNo = 1;

  /** 抓取的视频格式列表，英文逗号分隔 (defaults to mp4,mov,avi,wmv,flv,mkv) */
  @Parameter(property = "videosFormats", defaultValue = "mp4,mov,avi,wmv,flv,mkv")
  @Getter
  @Setter
  private String videosFormats = "mp4,mov,avi,wmv,flv,mkv";

  /** 抓帧生成图片格式，(defaults to png) */
  @Parameter(property = "capturePictureFormat", defaultValue = "png")
  @Getter
  @Setter
  private String capturePictureFormat = "png";

  /** 忽略视频格式大小写 (defaults to true) */
  @Parameter(property = "ignoreCase", defaultValue = "true")
  @Getter
  @Setter
  private boolean ignoreCase = true;

  @SneakyThrows
  public void execute() {
    Log log = getLog();

    if (captureFrameNo < 0 && captureFrameNo != -1) {
      throw new IllegalArgumentException("frameNo must be greater than or equals to 0 or -1.");
    }

    // 文件输出目录不存在，则创建
    if (!outputDirectory.exists()) {
      if (outputDirectory.mkdirs()) {
        log.info(
            String.format(
                "Directory %s was successfully created.", outputDirectory.getCanonicalPath()));
      } else {
        throw new MojoFailureException(
            "Failed to create directory " + outputDirectory.getCanonicalPath());
      }
    }

    String[] formats =
        Arrays.stream(StringUtils.split(videosFormats, ','))
            .filter(StringUtils::isNotEmpty)
            .toArray(String[]::new);

    log.info(
        String.format(
            "video file formats that require frame capture: %s", String.join(", ", formats)));

    // 递归视频目录，开始抓帧处理
    PathUtils.walk(
            videosDirectory.toPath(),
            new SuffixFileFilter(formats, ignoreCase ? IOCase.INSENSITIVE : IOCase.SENSITIVE),
            Integer.MAX_VALUE,
            false,
            FileVisitOption.FOLLOW_LINKS)
        .map(Path::toFile)
        .parallel()
        .forEach(this::grabVideoFrame);
  }

  /**
   * 视频抓帧
   *
   * @param videoFile 视频文件
   */
  @SneakyThrows(IOException.class)
  private void grabVideoFrame(File videoFile) {
    try (FFmpegFrameGrabber ff = new FFmpegFrameGrabber(videoFile);
        Java2DFrameConverter converter = new Java2DFrameConverter()) {
      ff.start();
      int ffLength = ff.getLengthInFrames();
      int targetFrameNo = captureFrameNo == -1 ? ffLength / 2 : captureFrameNo;
      int i = 0;
      while (i < ffLength) {
        Frame f = ff.grabImage();
        if (i++ == targetFrameNo) {
          ImageIO.write(
              converter.getBufferedImage(f), capturePictureFormat, getOutputFile(videoFile));
          return;
        }
      }
    }
  }

  private File getOutputFile(File videoFile) {
    String name = videoFile.getName();
    return outputDirectory
        .toPath()
        .resolve(name.substring(0, name.lastIndexOf('.') + 1) + capturePictureFormat)
        .toFile();
  }
}
