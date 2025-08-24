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

/**
 * whisper_aheads 结构体是对 一组 whisper_ahead 实例的统一管理容器，核心作用是整合 “参与音频 - 文本对齐计算的多个注意力头（attention head）”
 * 的配置，为 DTW（动态时间规整）算法提供明确的 “注意力头集合”，从而实现更精细的令牌级时间戳（token-level timestamps）优化。
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-22 18:45
 */
public record WhisperAHeads(
    // nHeads 明确当前 whisper_aheads 容器中包含的 whisper_ahead 实例总数（即参与对齐计算的 “层 - 头” 组合数量）。
    long nHeads,

    // 指向一个 whisper_ahead 类型的数组，数组中的每个元素对应一个 “参与对齐的注意力头配置”（通过 whisper_ahead 的 nTextLayer
    // 确定层索引、nHead 确定头索引）。
    WhisperAHead[] heads) {}
