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

/**
  会话记录
 */
CREATE TABLE IF NOT EXISTS `chat_conversation`
(
    `id`
    VARCHAR
(
    32
) NOT NULL COMMENT '会话标识',
    `absrtact` VARCHAR
(
    128
) NOT NULL COMMENT '会话摘要',
    `system` TEXT NOT NULL COMMENT '系统消息',
    `user` TEXT NOT NULL COMMENT '用户消息',
    `assistant` TEXT COMMENT 'LLM响应消息',
    `prompt` TEXT NOT NULL COMMENT '提示词',
    `tool` TEXT COMMENT '工具信息',
    `order` INT NOT NULL COMMENT '会话内的对话轮次编号',
    `created_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '会话创建时间',
    `updated_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '会话更新时间',
    PRIMARY KEY
(
    `id`,
    `order`
),
    INDEX CONVERSATION_IDX
(
    `id`,
    `order`
)
    ) ENGINE = INNODB
    DEFAULT CHARSET = UTF8MB4;