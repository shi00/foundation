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
package com.silong.foundation.devastator.core;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.LiteBlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.silong.foundation.devastator.event.JobMsgPayloadEvent;
import com.silong.foundation.devastator.model.Devastator.JobMsgPayload;
import com.silong.foundation.utilities.concurrent.SimpleThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.Serial;
import java.io.Serializable;

import static com.lmax.disruptor.dsl.ProducerType.MULTI;
import static com.silong.foundation.devastator.core.DefaultViewChangedHandler.powerOf2;

/**
 * 消息事件处理器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-29 23:40
 */
@Slf4j
class DefaultMessageHandler
    implements EventHandler<JobMsgPayloadEvent>, AutoCloseable, Serializable {

  @Serial private static final long serialVersionUID = 5687637299110748013L;

  /** 消息事件处理器线程名 */
  private static final String MESSAGE_EVENT_PROCESSOR = "cluster-message-processor-";

  /** 分布式引擎 */
  private DefaultDistributedEngine engine;

  /** 事件处理器 */
  private Disruptor<JobMsgPayloadEvent> disruptor;

  /** 环状队列 */
  private RingBuffer<JobMsgPayloadEvent> ringBuffer;

  /**
   * 事件处理器
   *
   * @param engine 分布式引擎
   */
  public DefaultMessageHandler(DefaultDistributedEngine engine) {
    if (engine == null) {
      throw new IllegalArgumentException("engine must not be null.");
    }
    this.engine = engine;
    this.disruptor = buildMessageEventDisruptor(engine.config().messageEventQueueSize());
    this.ringBuffer = disruptor.start();
  }

  private Disruptor<JobMsgPayloadEvent> buildMessageEventDisruptor(int queueSize) {
    Disruptor<JobMsgPayloadEvent> disruptor =
        new Disruptor<>(
            JobMsgPayloadEvent::new,
            powerOf2(queueSize),
            new SimpleThreadFactory(MESSAGE_EVENT_PROCESSOR),
            MULTI,
            new LiteBlockingWaitStrategy());
    disruptor.handleEventsWith(this);
    return disruptor;
  }

  @Override
  public void onEvent(JobMsgPayloadEvent event, long sequence, boolean endOfBatch) {
    if (log.isDebugEnabled()) {
      log.debug(
          "Start processing {} with sequence:{} and endOfBatch:{}.", event, sequence, endOfBatch);
    }

    try {

      JobMsgPayload payload = event.jobMsgPayload();

    } finally {
      if (log.isDebugEnabled()) {
        log.debug(
            "End processing {} with sequence:{} and endOfBatch:{}.", event, sequence, endOfBatch);
      }
    }
  }

  /**
   * 处理收到的任务消息
   *
   * @param payload 任务消息
   */
  public void handle(JobMsgPayload payload) {
    if (payload == null) {
      log.error("payload must not be null.");
      return;
    }
    long sequence = ringBuffer.next();
    try {
      JobMsgPayloadEvent event = ringBuffer.get(sequence).jobMsgPayload(payload);
      if (log.isDebugEnabled()) {
        log.debug("Enqueue {} with sequence:{}.", event, sequence);
      }
    } finally {
      ringBuffer.publish(sequence);
    }
  }

  @Override
  public void close() {
    if (this.disruptor != null) {
      this.disruptor.shutdown();
      this.disruptor = null;
    }
    this.ringBuffer = null;
    this.engine = null;
  }
}
