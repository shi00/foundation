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

package com.silong.foundation.springboot.starter.simpleauth;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * 测试rest接口
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-11-27 20:19
 */
@Slf4j
@RestController
@RequestMapping("/test")
public class TestService {

  private static final Faker FAKER = new Faker();

  public record User(long id, String name) {

    @Override
    public boolean equals(Object object) {
      if (this == object) return true;
      if (object == null || getClass() != object.getClass()) return false;
      User user = (User) object;
      return id == user.id;
    }

    @Override
    public int hashCode() {
      return Objects.hash(id);
    }
  }

  @GetMapping(value = "/a/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<User> queryUser(@PathVariable long id) {
    return Mono.just(new User(id, FAKER.animal().name()));
  }

  @DeleteMapping(value = "/b/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<Long> deleteUser(@PathVariable long id) {
    return Mono.just(id);
  }
}
