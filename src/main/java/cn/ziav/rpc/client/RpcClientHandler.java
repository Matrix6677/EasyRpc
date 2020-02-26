package cn.ziav.rpc.client;

import cn.ziav.rpc.common.MsgRequest;
import cn.ziav.rpc.common.MsgResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundInvoker;
import io.netty.channel.ChannelPromise;
import io.netty.handler.timeout.IdleStateEvent;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Zavi */
@io.netty.channel.ChannelHandler.Sharable
public class RpcClientHandler extends ChannelDuplexHandler {
  private static final Logger logger = LoggerFactory.getLogger(RpcClientHandler.class);
  /** <ip:port, channel> */
  private final Map<String, Channel> CHANNEL_MAP = new ConcurrentHashMap<>();

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    String addr = toRemoteAddrString(ctx.channel());
    getOrAddChannel(addr, ctx.channel());
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    String addr = toRemoteAddrString(ctx.channel());
    CHANNEL_MAP.remove(addr);
  }

  /**
   * 将Channel转换成host:port字符串
   *
   * @param channel
   * @return
   */
  private String toRemoteAddrString(Channel channel) {
    InetSocketAddress socketAddress = (InetSocketAddress) channel.remoteAddress();
    String host = socketAddress.getHostString();
    int port = socketAddress.getPort();
    return host + ":" + port;
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    super.write(ctx, msg, promise);
    final boolean isRequest = msg instanceof MsgRequest;
    promise.addListener(
        future -> {
          if (future.isSuccess()) {
            return;
          }

          Throwable t = future.cause();
          if (t != null && isRequest) {
            MsgRequest request = (MsgRequest) msg;
            MsgResponse response = buildErrorResponse(request, t);
            RpcFuture rpcFuture = RpcFuture.getFuture(response.id);
            if (rpcFuture.executor == null) {
              RpcFuture.received((response), false);
            } else {
              rpcFuture.executor.submit(() -> RpcFuture.received(response, false));
            }
          }
        });
  }

  /**
   * 构建一个请求异常的响应
   *
   * @param request 请求体
   * @param t 大多都是序列化的异常
   * @return
   */
  private static MsgResponse buildErrorResponse(MsgRequest request, Throwable t) {
    MsgResponse<Throwable> response = new MsgResponse<>(request.id, request.mId, t);
    response.mStatus = MsgResponse.BAD_REQUEST;
    return response;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof MsgResponse) {
      RpcFuture future = RpcFuture.getFuture(((MsgResponse) msg).id);
      if (future == null) {
        logger.warn("future has been removed, mId={}", ((MsgResponse) msg).mId);
        return;
      }
      if (future.executor == null) {
        RpcFuture.received(((MsgResponse) msg), false);
      } else {
        future.executor.submit(() -> RpcFuture.received(((MsgResponse) msg), false));
      }
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    Channel channel = ctx.channel();
    removeChannelIfDisconnected(channel);
    logger.error("", cause);
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    // send heartbeat when read idle.
    if (evt instanceof IdleStateEvent) {
      Channel channel = ctx.channel();
      try {
        MsgRequest req = new MsgRequest();
        req.mId = -1;
        req.mTwoWay = true;
        // 心跳处理
        channel.writeAndFlush(req);
      } finally {
        removeChannelIfDisconnected(channel);
      }
      return;
    }
    super.userEventTriggered(ctx, evt);
  }

  /**
   * 如果失联了则移除Channel
   *
   * @param channel
   */
  private void removeChannelIfDisconnected(Channel channel) {
    if (channel != null && !channel.isActive()) {
      String address = toRemoteAddrString(channel);
      CHANNEL_MAP.remove(address);
    }
  }

  /**
   * 获取Channel
   *
   * @param addr
   * @return
   */
  public Channel getChannel(String addr) {
    return CHANNEL_MAP.get(addr);
  }

  /**
   * 获取或添加Channel
   *
   * @param addr
   * @param channel
   * @return
   */
  public Channel getOrAddChannel(String addr, Channel channel) {
    Channel pre = CHANNEL_MAP.putIfAbsent(addr, channel);
    return pre != null ? pre : channel;
  }

  public void closeChannel() {
    CHANNEL_MAP.values().forEach(ChannelOutboundInvoker::close);
  }
}
