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

package com.silong.foundation.dj.bonecrusher.handler;

import static com.silong.foundation.dj.bonecrusher.message.Messages.Type.LOADING_CLASS_RESP;

import com.silong.foundation.dj.bonecrusher.message.ErrorCode;
import com.silong.foundation.dj.bonecrusher.message.Messages.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.udt.nio.NioUdtProvider;
import io.netty.handler.stream.ChunkedStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

/**
 * 文件加载处理器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-20 22:13
 */
@Slf4j
@Sharable
public class FileLoaderHandler extends ChannelInboundHandlerAdapter {

  /** 找不到指定class */
  private static final ByteBuf CLASS_NOT_FOUND =
      Unpooled.wrappedBuffer(
          Response.newBuilder()
              .setType(LOADING_CLASS_RESP)
              .setResult(
                  Result.newBuilder()
                      .setCode(ErrorCode.CLASS_NOT_FOUND.getCode())
                      .setDesc(ErrorCode.CLASS_NOT_FOUND.getDesc()))
              .build()
              .toByteArray());

  /** 文件保存目录 */
  private final Path dataStorePath;

  /**
   * 构造方法
   *
   * @param dataStorePath 数据存储目录
   */
  public FileLoaderHandler(@NonNull Path dataStorePath) {
    this.dataStorePath = dataStorePath;
  }

  /** 导出分区数据 */
  private Path exportPartition2Files() {
    // TODO 分区数据导出待实现
    return SystemUtils.getJavaIoTmpDir().toPath();
  }

  private void handleSyncDataReq(ChannelHandlerContext ctx, SyncDataReq request) {

    //    RandomAccessFile raf = null;
    //    long length = -1;
    //    try {
    //      raf = new RandomAccessFile(msg, "r");
    //      length = raf.length();
    //    } catch (Exception e) {
    //      ctx.writeAndFlush("ERR: " + e.getClass().getSimpleName() + ": " + e.getMessage() +
    // '\n');
    //      return;
    //    } finally {
    //      if (length < 0 && raf != null) {
    //        raf.close();
    //      }
    //    }
    //
    //    ctx.write("OK: " + raf.length() + '\n');
    //    if (ctx.pipeline().get(SslHandler.class) == null) {
    //      // 传输文件使用了 DefaultFileRegion 进行写入到 NioSocketChannel 中
    //      ctx.write(new DefaultFileRegion(raf.getChannel(), 0, length));
    //    } else {
    //      // SSL enabled - cannot use zero-copy file transfer.
    //      ctx.write(new ChunkedFile(raf));
    //    }
    //    ctx.writeAndFlush("\n");
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof Request request) {
      switch (request.getType()) {
        case DATA_SYNC_REQ -> handleSyncDataReq(ctx, request.getSyncData());
        case LOADING_CLASS_REQ -> handleLoadingClassReq(ctx, request.getLoadingClass());
      }
    } else {
      ctx.fireChannelRead(msg);
    }
  }

  private void handleLoadingClassReq(ChannelHandlerContext ctx, LoadingClassReq request)
      throws IOException {
    String classFqdn = request.getClassFqdn();
    try (InputStream inputStream = getClass().getResourceAsStream(fqdn2Path(classFqdn))) {
      if (inputStream == null) {
        ctx.writeAndFlush(CLASS_NOT_FOUND); // 找不到指定class，返回错误
        return;
      }

      log.info(
          "The transfer of class[{}] is about to begin from {} to {} by channel[id:{}].",
          classFqdn,
          ctx.channel().localAddress(),
          ctx.channel().remoteAddress(),
          ctx.channel().id());
      ctx.writeAndFlush(new ChunkedStream(inputStream));
    }
  }

  /**
   * 拼装class文件路径
   *
   * @param classFqdn fqdn
   * @return class文件加载路径
   */
  private String fqdn2Path(String classFqdn) {
    return "/" + StringUtils.replaceChars(classFqdn, '.', '/') + ".class";
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    log.error(
        "The channel[id:{}] is closed between client:{} and server:{}. {}details:{}",
        ctx.channel().id(),
        ctx.channel().remoteAddress(),
        ctx.channel().localAddress(),
        System.lineSeparator(),
        NioUdtProvider.socketUDT(ctx.channel()).toStringOptions(),
        cause);
    ctx.close();
  }
}
