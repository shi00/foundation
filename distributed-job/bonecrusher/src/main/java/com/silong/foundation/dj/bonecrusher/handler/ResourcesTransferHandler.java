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
import static org.apache.commons.lang3.StringUtils.replaceChars;

import com.silong.foundation.dj.bonecrusher.configure.config.BonecrusherProperties;
import com.silong.foundation.dj.bonecrusher.message.Messages.*;
import com.silong.foundation.dj.bonecrusher.utils.ErrorCode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.udt.nio.NioUdtProvider;
import io.netty.handler.stream.ChunkedStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.util.unit.DataSize;

/**
 * 资源传输处理器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-20 22:13
 */
@Slf4j
@Sharable
public class ResourcesTransferHandler extends ChannelInboundHandlerAdapter {

  private static final Result CLASS_NOT_FOUND =
      Result.newBuilder()
          .setCode(ErrorCode.CLASS_NOT_FOUND.getCode()) // 找不到指定class，返回错误
          .setDesc(ErrorCode.CLASS_NOT_FOUND.getDesc())
          .build();

  /** 文件保存目录 */
  private final Path dataStorePath;

  private final DataSize dataBlockSize;

  /**
   * 构造方法
   *
   * @param properties 配置
   */
  public ResourcesTransferHandler(@NonNull BonecrusherProperties properties) {
    this.dataStorePath = properties.getDataStorePath();
    this.dataBlockSize = properties.getDataBlockSize();
  }

  /** 导出分区数据 */
  private Path exportPartition2Files() {
    // TODO 分区数据导出待实现
    return SystemUtils.getJavaIoTmpDir().toPath();
  }

  private void handleSyncDataReq(ChannelHandlerContext ctx, SyncDataReq request, String requestId) {

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
        case DATA_SYNC_REQ -> handleSyncDataReq(ctx, request.getSyncData(), request.getUuid());
        case LOADING_CLASS_REQ -> handleLoadingClassReq(
            ctx, request.getLoadingClass(), request.getUuid());
      }
    } else {
      ctx.fireChannelRead(msg);
    }
  }

  private int calculateTotalBlocks(int readableBytes, int chunkSize) {
    return readableBytes % chunkSize == 0
        ? readableBytes / chunkSize
        : readableBytes / chunkSize + 1;
  }

  private void handleLoadingClassReq(
      ChannelHandlerContext ctx, LoadingClassReq request, String requestId) throws IOException {
    String classFqdn = request.getClassFqdn();
    try (InputStream inputStream = getClass().getResourceAsStream(classFqdn2Path(classFqdn))) {
      if (inputStream == null) {
        ctx.writeAndFlush(
            Unpooled.wrappedBuffer(
                Response.newBuilder()
                    .setType(LOADING_CLASS_RESP)
                    .setResult(CLASS_NOT_FOUND)
                    .setUuid(requestId)
                    .build()
                    .toByteArray()));
        return;
      }

      // 分块数据发送
      int chunkSize = (int) dataBlockSize.toKilobytes();
      ctx.writeAndFlush(
              new ChunkedStream(inputStream, chunkSize) {

                /** 数据块总量 */
                private final int totalBlocks =
                    calculateTotalBlocks(inputStream.available(), chunkSize);

                /** 数据块计数 */
                private final AtomicInteger dataBlockNoCount = new AtomicInteger(0);

                private ByteBuf attachRespType(
                    ByteBufAllocator allocator, ByteBuf fileDataBlock, Type respType) {
                  if (fileDataBlock != null) {
                    Response response =
                        Response.newBuilder()
                            .setType(respType)
                            .setUuid(requestId)
                            .setDataBlockMetadata(
                                DataBlockMetadata.newBuilder()
                                    .setDataUuid(classFqdn)
                                    .setTotalBlocks(totalBlocks)
                                    .setBlockSize(chunkSize)
                                    .setBlockNo(dataBlockNoCount.getAndIncrement()))
                            .build();

                    // 拼装组合bytebuf，第一个组件为protobuf响应
                    fileDataBlock =
                        allocator
                            .compositeBuffer(2)
                            .addComponents(
                                true,
                                Unpooled.wrappedBuffer(response.toByteArray()),
                                fileDataBlock);
                  }
                  return fileDataBlock;
                }

                @Override
                public ByteBuf readChunk(ByteBufAllocator allocator) throws Exception {
                  return attachRespType(allocator, super.readChunk(allocator), LOADING_CLASS_RESP);
                }
              })
          .addListener(
              future -> {
                if (future.isSuccess()) {
                  log.info(
                      "The class[{}] transfer completed successfully from {} to {} by channel[id:{}].",
                      classFqdn,
                      ctx.channel().localAddress(),
                      ctx.channel().remoteAddress(),
                      ctx.channel().id());
                } else {
                  log.info(
                      "Failed to transfer the class[{}] from {} to {} by channel[id:{}].",
                      classFqdn,
                      ctx.channel().localAddress(),
                      ctx.channel().remoteAddress(),
                      ctx.channel().id());
                }
              });
    }
  }

  /**
   * 拼装class文件路径
   *
   * @param classFqdn fqdn
   * @return class文件加载路径
   */
  private String classFqdn2Path(String classFqdn) {
    return "/" + replaceChars(classFqdn, '.', '/') + ".class";
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