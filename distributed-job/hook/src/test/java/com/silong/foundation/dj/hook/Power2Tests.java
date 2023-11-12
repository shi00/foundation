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

package com.silong.foundation.dj.hook;

import com.silong.foundation.dj.hook.validation.Power2;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-12 17:36
 */
public class Power2Tests {

  private static Validator validator;

  @BeforeAll
  public static void setUpValidator() {
    try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
      validator = validatorFactory.getValidator();
    }
  }

  private record TestObj(@Power2 int value) {}

  @Test
  public void test1() {
    TestObj testObj = new TestObj(3);
    Set<ConstraintViolation<TestObj>> constraintViolations = validator.validate(testObj);
    constraintViolations.stream()
        .findFirst()
        .ifPresent(
            constraintViolation ->
                Assertions.assertEquals(
                    constraintViolation.getMessage(), "The value(3) can only be a power of 2."));
  }

  @Test
  public void test2() {
    TestObj testObj = new TestObj(1);
    Set<ConstraintViolation<TestObj>> constraintViolations = validator.validate(testObj);
    Assertions.assertTrue(constraintViolations.isEmpty());
  }

  @Test
  public void test3() {
    TestObj testObj = new TestObj(-11111);
    Set<ConstraintViolation<TestObj>> constraintViolations = validator.validate(testObj);
    constraintViolations.stream()
        .findFirst()
        .ifPresent(
            constraintViolation ->
                Assertions.assertEquals(
                    constraintViolation.getMessage(),
                    "The value(-11111) can only be a power of 2."));
  }

  @Test
  public void test4() {
    TestObj testObj = new TestObj(1024);
    Set<ConstraintViolation<TestObj>> constraintViolations = validator.validate(testObj);
    Assertions.assertTrue(constraintViolations.isEmpty());
  }
}
