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
 * whisper_ahead 结构体用于指定参与音频 - 文本对齐计算的注意力头（attention head）的具体位置，主要配合
 * DTW（动态时间规整）算法优化令牌级时间戳（token-level timestamps）的精度。
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-04-22 18:45
 */
public record WhisperAHead(
    // n_text_layer指定使用 Transformer 模型中 “文本编码器” 的第几层（从 0 开始计数）。
    int nTextLayer,
    // n_head指定在 nTextLayer 层中使用第几个注意力头（从 0 开始计数）。
    int nHead) {}
