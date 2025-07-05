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

package com.silong.llm.chatbot.daos;

import static com.silong.llm.chatbot.providers.LdapUserProvider.*;
import static com.silong.llm.chatbot.providers.LdapUserProvider.DESCRIPTION_ATTRIBUTION;
import static com.silong.llm.chatbot.providers.LdapUserProvider.MAIL_ATTRIBUTION;

import com.silong.foundation.springboot.starter.jwt.security.SimpleTokenAuthentication;
import com.silong.llm.chatbot.pos.User;
import java.util.List;
import lombok.NonNull;

/**
 * 数据库服务的常量
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:19
 */
public interface RepoHelper {

  /** 多值分隔符 */
  String DELIMITER = "\\u001D";

  /** 有效 */
  byte VALID = 1;

  /** 无效 */
  byte INVALID = 0;

  /**
   * 根据用户据鉴权信息生成用户对象
   *
   * @param auth 用户鉴权信息
   * @return 用户
   */
  @NonNull
  static User map2User(@NonNull SimpleTokenAuthentication auth) {
    List<String> memberOf = auth.getAttribute(MEMBER_OF_ATTRIBUTION);
    List<String> displayName = auth.getAttribute(DISPLAY_NAME_ATTRIBUTION);
    List<String> mobiles = auth.getAttribute(MOBILE_ATTRIBUTION);
    List<String> mails = auth.getAttribute(MAIL_ATTRIBUTION);
    List<String> desc = auth.getAttribute(DESCRIPTION_ATTRIBUTION);
    return User.builder()
        .name(auth.getUserName())
        .roles(memberOf)
        .mobiles(mobiles)
        .mails(mails)
        .displayName(displayName == null ? null : displayName.getFirst())
        .desc(desc == null ? null : desc.getFirst())
        .build();
  }
}
