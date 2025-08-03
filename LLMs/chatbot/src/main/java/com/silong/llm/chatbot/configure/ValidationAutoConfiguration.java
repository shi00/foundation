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

package com.silong.llm.chatbot.configure;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.HibernateValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

/**
 * 参数校验自动配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:21
 */
@Configuration
public class ValidationAutoConfiguration {

  @Bean(destroyMethod = "close")
  ValidatorFactory validatorFactory() {
    return Validation.byProvider(HibernateValidator.class)
        .configure()
        .failFast(true) // 开启快速失败模式
        .buildValidatorFactory();
  }

  /**
   * 配置并创建一个 Hibernate Validator 实例，开启快速失败模式。
   *
   * @return 配置好的 Validator 实例
   */
  @Bean
  public Validator validator(ValidatorFactory validatorFactory) {
    return validatorFactory.getValidator();
  }

  /**
   * 配置 MethodValidationPostProcessor，并注入已创建的 Validator。 这样可以避免在每次创建 MethodValidationPostProcessor
   * 时重复创建 Validator 实例。
   *
   * @param validator 已配置好的 Validator 实例
   * @return 配置好的 MethodValidationPostProcessor 实例
   */
  @Bean
  public MethodValidationPostProcessor methodValidationPostProcessor(Validator validator) {
    MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
    processor.setValidator(validator);
    return processor;
  }
}
