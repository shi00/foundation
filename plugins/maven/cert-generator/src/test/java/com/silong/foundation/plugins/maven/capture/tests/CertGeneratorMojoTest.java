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
package com.silong.foundation.plugins.maven.capture.tests;

import com.silong.foundation.plugins.maven.cert.CertGeneratorMojo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

/**
 * 单元测试
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-23 19:52
 */
public class CertGeneratorMojoTest extends AbstractMojoTestCase {

  /** 测试pom */
  public static final String FORKED_POM_FILE = "src/test/resources/unit/pom.xml";

  public void test1() throws Exception {
    CertGeneratorMojo mojo = (CertGeneratorMojo) lookupMojo("generate-cert", FORKED_POM_FILE);
    assertNotNull(mojo);
    mojo.execute();
    Path outputCertPath = mojo.getOutputCertPath();
    assertTrue(outputCertPath.toFile().exists());

    String password = mojo.getPassword();
    assertNotNull(password);
    assertTrue(outputCertPath.toFile().exists());

    String certType = mojo.getCertType();
    assertNotNull(certType);

    var result = validate(outputCertPath, certType, password);
    assertTrue(result.isValid());
  }

  /** 校验 P12 证书 */
  public static ValidationResult validate(Path p12Path, String certType, String password) {
    ValidationResult result = new ValidationResult();

    try (var fis = Files.newInputStream(p12Path, StandardOpenOption.READ)) {
      // 加载证书
      KeyStore keyStore = KeyStore.getInstance(certType);
      keyStore.load(fis, password.toCharArray());

      // 获取证书链
      Enumeration<String> aliases = keyStore.aliases();
      if (!aliases.hasMoreElements()) {
        result.addError("证书文件为空");
        return result;
      }

      String alias = aliases.nextElement();
      Certificate[] certChain = keyStore.getCertificateChain(alias);
      if (certChain == null || certChain.length == 0) {
        result.addError("无法获取证书链");
        return result;
      }

      // 转换为 X509 证书列表
      List<X509Certificate> certificates = new ArrayList<>();
      for (Certificate cert : certChain) {
        if (cert instanceof X509Certificate) {
          certificates.add((X509Certificate) cert);
        } else {
          result.addError("证书链包含非 X509 证书");
          return result;
        }
      }

      // 验证证书链
      result.addCheckResult(validateCertificateChain(certificates));

      // 验证证书有效期
      result.addCheckResult(validateCertificateExpiry(certificates));

      // 验证证书签名
      result.addCheckResult(validateCertificateSignatures(certificates));

      result.setValid(true);
    } catch (KeyStoreException e) {
      result.addError("证书存储格式错误: " + e.getMessage());
    } catch (CertificateException e) {
      result.addError("证书格式错误: " + e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      result.addError("不支持的加密算法: " + e.getMessage());
    } catch (IOException e) {
      result.addError("读取证书文件失败: " + e.getMessage());
    } catch (Exception e) {
      result.addError("校验过程发生未知错误: " + e.getMessage());
    }

    return result;
  }

  /** 验证证书有效期 */
  private static CheckResult validateCertificateExpiry(List<X509Certificate> certificates) {
    CheckResult result = new CheckResult("证书有效期验证");

    for (X509Certificate cert : certificates) {
      try {
        cert.checkValidity();
      } catch (CertificateExpiredException e) {
        result.fail("证书已过期: " + cert.getSubjectX500Principal());
      } catch (CertificateNotYetValidException e) {
        result.fail("证书尚未生效: " + cert.getSubjectX500Principal());
      }
    }

    return result;
  }

  /** 验证证书签名链 */
  private static CheckResult validateCertificateSignatures(List<X509Certificate> certificates) {
    CheckResult result = new CheckResult("证书签名验证");

    for (int i = 0; i < certificates.size() - 1; i++) {
      try {
        certificates.get(i).verify(certificates.get(i + 1).getPublicKey());
      } catch (Exception e) {
        result.fail("证书签名验证失败: " + e.getMessage());
      }
    }

    // 验证根证书自签名
    if (!certificates.isEmpty()) {
      try {
        X509Certificate rootCert = certificates.get(certificates.size() - 1);
        rootCert.verify(rootCert.getPublicKey());
      } catch (Exception e) {
        result.fail("根证书自签名验证失败: " + e.getMessage());
      }
    }

    return result;
  }

  /** 验证证书链 */
  private static CheckResult validateCertificateChain(List<X509Certificate> certificates) {
    CheckResult result = new CheckResult("证书链验证");

    try {
      // 创建证书路径
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      CertPath certPath = cf.generateCertPath(certificates);

      // 创建信任库（包含根证书）
      KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
      trustStore.load(null, null);
      trustStore.setCertificateEntry("root", certificates.get(certificates.size() - 1));

      // 验证证书路径
      CertPathValidator validator = CertPathValidator.getInstance("PKIX");
      PKIXParameters params = new PKIXParameters(trustStore);
      params.setRevocationEnabled(false); // JDK 实现不支持在线撤销检查
      validator.validate(certPath, params);
    } catch (Exception e) {
      result.fail("证书链验证失败: " + e.getMessage());
    }

    return result;
  }

  /** 校验结果类 */
  public static class ValidationResult {
    private boolean valid;
    private List<String> errors = new ArrayList<>();
    private List<CheckResult> checkResults = new ArrayList<>();

    public boolean isValid() {
      return valid;
    }

    public void setValid(boolean valid) {
      this.valid = valid;
    }

    public List<String> getErrors() {
      return errors;
    }

    public void addError(String error) {
      this.errors.add(error);
    }

    public List<CheckResult> getCheckResults() {
      return checkResults;
    }

    public void addCheckResult(CheckResult result) {
      this.checkResults.add(result);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("证书校验结果: ").append(valid ? "通过" : "失败").append("\n");

      if (!errors.isEmpty()) {
        sb.append("错误:\n");
        for (String error : errors) {
          sb.append("  - ").append(error).append("\n");
        }
      }

      if (!checkResults.isEmpty()) {
        sb.append("检查详情:\n");
        for (CheckResult result : checkResults) {
          sb.append("  - ")
              .append(result.getName())
              .append(": ")
              .append(result.isPassed() ? "通过" : "失败")
              .append("\n");

          for (String message : result.getMessages()) {
            sb.append("    * ").append(message).append("\n");
          }
        }
      }

      return sb.toString();
    }
  }

  /** 单项检查结果类 */
  public static class CheckResult {
    private String name;
    private boolean passed = true;
    private List<String> messages = new ArrayList<>();

    public CheckResult(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public boolean isPassed() {
      return passed;
    }

    public void setPassed(boolean passed) {
      this.passed = passed;
    }

    public void fail(String message) {
      this.passed = false;
      this.messages.add(message);
    }

    public List<String> getMessages() {
      return messages;
    }

    public void addMessage(String message) {
      this.messages.add(message);
    }
  }
}
