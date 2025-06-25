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

package com.silong.foundation.plugins.maven.cert;

import static java.nio.file.StandardOpenOption.*;
import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_TEST_RESOURCES;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.ToString;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * 自签名证书生成器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-28 22:47
 */
@ToString
@Mojo(name = "generate-cert", defaultPhase = GENERATE_TEST_RESOURCES, threadSafe = true)
@SuppressFBWarnings(
    value = {"PATH_TRAVERSAL_IN", "HARD_CODE_PASSWORD"},
    justification = "读取的maven配置")
public class CertGeneratorMojo extends AbstractMojo {

  /**
   * @since 1.2
   */
  @ToString.Exclude
  @Parameter(readonly = true, defaultValue = "${project}")
  private MavenProject project;

  /** 是否忽略执行 */
  @Parameter(property = "cert.skip", defaultValue = "false")
  protected boolean skip;

  /** 生成证书输出路径，默认：${project.build.testOutputDirectory}/resources/certs */
  @Getter
  @Parameter(property = "cert.outputDir")
  private File outputDir;

  /** 证书别名，默认：test-cert */
  @Getter
  @Parameter(property = "cert.alias", defaultValue = "test-cert")
  private String alias = "test-cert";

  /** 证书subject，默认："CN=localhost, OU=Dev, O=SiLong, L=ShenZheng, ST=GuangDong, C=CN" */
  @Getter
  @Parameter(
      property = "cert.subject",
      defaultValue = "CN=localhost, OU=Dev, O=SiLong, L=ShenZheng, ST=GuangDong, C=CN")
  private String subject = "CN=localhost, OU=Dev, O=SiLong, L=ShenZheng, ST=GuangDong, C=CN";

  /** 证书密码 */
  @Getter
  @ToString.Exclude
  @Parameter(property = "cert.password", required = true)
  private String password;

  /** 证书格式，默认：PKCS12 */
  @Getter
  @Parameter(property = "cert.certType", defaultValue = "PKCS12")
  private String certType = "PKCS12";

  /** 证书名 */
  @Getter
  @Parameter(property = "cert.certName", required = true)
  private String certName;

  /** key长度，默认：2048 */
  @Getter
  @Parameter(property = "cert.keySize", defaultValue = "2048")
  private int keySize = 2048;

  /** 证书有效期，单位：天。默认：365 */
  @Getter
  @Parameter(property = "cert.validityDays", defaultValue = "365")
  private int validityDays = 365;

  @Getter private Path outputCertPath;

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  @Override
  public void execute() throws MojoExecutionException {

    if (skip) {
      getLog().info("Skipping generate cert...");
      return;
    }

    try {
      Path path = prepareOutputDir();
      validateParameters();
      outputCertPath = path.resolve(certName);

      getLog().info("parameters: " + this);

      KeyPair keyPair = generateKeyPair();
      X509Certificate cert = generateCertificate(keyPair);
      outputKeyStore(keyPair.getPrivate(), cert);
    } catch (Exception e) {
      throw new MojoExecutionException("Failed to generate cert.", e);
    }
  }

  private Path prepareOutputDir() throws IOException {
    if (outputDir == null) {
      if (project != null) {
        String testOutputDirectory = project.getBuild().getTestOutputDirectory();
        outputDir = Paths.get(testOutputDirectory).resolve("resources").resolve("certs").toFile();
      } else {
        outputDir = new File("target/test-classes/resources/certs");
      }
    }

    Path path = outputDir.toPath();
    Files.createDirectories(path);
    return path;
  }

  private void validateParameters() {
    if (validityDays <= 0) {
      throw new IllegalArgumentException("validityDays must be greater than 0.");
    }
    if (alias == null || alias.isEmpty()) {
      throw new IllegalArgumentException("alias must not be null or empty.");
    }
    if (password == null || password.isEmpty()) {
      throw new IllegalArgumentException("password must not be null or empty.");
    }
    if (subject == null || subject.isEmpty()) {
      throw new IllegalArgumentException("subject must not be null or empty.");
    }
    if (certName == null || certName.isEmpty()) {
      throw new IllegalArgumentException("certName must not be null or empty.");
    }
    if (certType == null || certType.isEmpty()) {
      throw new IllegalArgumentException("certType must not be null or empty.");
    }
    if (keySize <= 0) {
      throw new IllegalArgumentException("keySize must be greater than 0.");
    }
  }

  private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(keySize);
    return generator.generateKeyPair();
  }

  private X509Certificate generateCertificate(KeyPair keyPair) throws Exception {
    X500Name subject = new X500Name(this.subject);
    java.util.Date startDate = new java.util.Date();
    java.util.Date endDate =
        new java.util.Date(startDate.getTime() + TimeUnit.DAYS.toMillis(validityDays));

    X509v3CertificateBuilder builder =
        new JcaX509v3CertificateBuilder(
            subject,
            BigInteger.valueOf(System.currentTimeMillis()),
            startDate,
            endDate,
            subject,
            keyPair.getPublic());

    ContentSigner signer =
        new JcaContentSignerBuilder("SHA256WithRSA").setProvider("BC").build(keyPair.getPrivate());

    return new JcaX509CertificateConverter()
        .setProvider("BC")
        .getCertificate(builder.build(signer));
  }

  private void outputKeyStore(PrivateKey privateKey, X509Certificate cert) throws Exception {
    KeyStore keyStore = KeyStore.getInstance(certType);
    keyStore.load(null, password.toCharArray());

    KeyStore.PrivateKeyEntry privateKeyEntry =
        new KeyStore.PrivateKeyEntry(privateKey, new X509Certificate[] {cert});

    KeyStore.PasswordProtection protection =
        new KeyStore.PasswordProtection(password.toCharArray());

    keyStore.setEntry(alias, privateKeyEntry, protection);

    try (var fos = Files.newOutputStream(outputCertPath, CREATE, WRITE, TRUNCATE_EXISTING)) {
      keyStore.store(fos, password.toCharArray());
    }

    getLog().info("Certificate generated successfully.");
  }
}
