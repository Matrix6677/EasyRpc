package cn.ziav.rpc.client;

import static cn.ziav.rpc.exception.ExceptionCode.TIME_OUT;
import static cn.ziav.rpc.exception.ExceptionCode.UNKNOWN_ERROR;

import cn.ziav.rpc.common.MsgRequest;
import cn.ziav.rpc.common.MsgResponse;
import cn.ziav.rpc.exception.RemotingException;
import cn.ziav.rpc.timer.HashedWheelTimer;
import cn.ziav.rpc.timer.Timeout;
import cn.ziav.rpc.timer.Timer;
import cn.ziav.rpc.timer.TimerTask;
import cn.ziav.rpc.utils.NamedThreadFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RpcFuture
 *
 * @author Zavi
 */
public class RpcFuture<R> extends CompletableFuture<R> {
  private static final Logger logger = LoggerFactory.getLogger(RpcFuture.class);

  /** <reqId, future> */
  private static final Map<Long, RpcFuture> FUTURES = new ConcurrentHashMap<>();

  public final long id;
  public final MsgRequest request;
  private final int timeout;
  private volatile long sent;
  private Timeout timeoutCheckTask;
  public ExecutorService executor;
  public EasyRpcCallback<R> callback;

  /** 超时计时器 */
  public static final Timer TIME_OUT_TIMER =
      new HashedWheelTimer(
          new NamedThreadFactory("easyRpc-future-timeout", true), 30, TimeUnit.MILLISECONDS);

  private ServerNode serverNode;

  public RpcFuture(MsgRequest request, int timeout) {
    this.id = request.id;
    this.request = request;
    this.timeout = timeout;
    FUTURES.put(id, this);
  }

  public static <R> RpcFuture<R> newFuture(
      MsgRequest request,
      EasyRpcCallback<R> callback,
      int timeout,
      ServerNode serverNode,
      ExecutorService executor) {
    final RpcFuture<R> future = new RpcFuture<>(request, timeout);
    future.executor = executor;
    future.callback = callback;
    future.serverNode = serverNode;
    // 启动超时检查任务
    timeoutCheck(future);
    return future;
  }

  private static void timeoutCheck(RpcFuture future) {
    // 初始化任务
    TimeoutCheckTask task = new TimeoutCheckTask(future.id);
    // 启动任务
    future.timeoutCheckTask =
        TIME_OUT_TIMER.newTimeout(task, future.timeout, TimeUnit.MILLISECONDS);
  }

  public static RpcFuture getFuture(long id) {
    return FUTURES.get(id);
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    MsgResponse errorResult = new MsgResponse<>(id, request.mId);
    errorResult.mStatus = MsgResponse.CLIENT_ERROR;
    doReceived(errorResult);
    FUTURES.remove(id);
    return true;
  }

  public static void received(MsgResponse response, boolean timeout) {
    RpcFuture future = FUTURES.remove(response.id);
    if (future != null) {
      Timeout t = future.timeoutCheckTask;
      // 这里已经拿到结果了，如果不是超时的话，就cancel掉之前的超时任务
      if (!timeout) {
        t.cancel();
      }

      future.doReceived(response);
    } else {
      logger.warn(
          "The timeout response finally returned at {}",
          new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
    }
  }

  private void doReceived(MsgResponse<R> res) {
    if (res == null) {
      throw new IllegalStateException("response cannot be null");
    }

    if (serverNode != null && isSent()) {
      long cost = System.currentTimeMillis() - sent;
      serverNode.addCost(cost);
    }

    if (res.mStatus == MsgResponse.OK) {
      complete(res.mData);
      return;
    }
    if (res.mStatus == MsgResponse.CLIENT_TIMEOUT) {
      completeExceptionally(new RemotingException(TIME_OUT, "client timeout"));
    }
    if (res.mStatus == MsgResponse.SERVER_TIMEOUT) {
      completeExceptionally(new RemotingException(TIME_OUT, "server timeout"));
      return;
    }

    if (res.mData instanceof Throwable) {
      this.completeExceptionally((Throwable) res.mData);
      return;
    }

    this.completeExceptionally(
        new RemotingException(UNKNOWN_ERROR, "statusCode=" + res.mStatus + " data=" + res.mData));
  }

  @Override
  public boolean complete(R value) {
    if (callback != null) {
      callback.success(value);
    }
    return super.complete(value);
  }

  @Override
  public boolean completeExceptionally(Throwable ex) {
    if (callback != null) {
      callback.fail(ex);
    }
    return super.completeExceptionally(ex);
  }

  public static void sent(MsgRequest request) {
    RpcFuture future = FUTURES.get(request.id);
    if (future != null) {
      future.doSent();
    }
  }

  private void doSent() {
    sent = System.currentTimeMillis();
  }

  /**
   * 是否已发送
   *
   * @return
   */
  private boolean isSent() {
    return sent > 0;
  }

  /** 超时任务 */
  private static class TimeoutCheckTask implements TimerTask {

    private final Long requestID;

    TimeoutCheckTask(Long requestID) {
      this.requestID = requestID;
    }

    @Override
    public void run(Timeout timeout) {
      RpcFuture future = RpcFuture.getFuture(requestID);
      // 判断future是否已完成
      if (future == null || future.isDone()) {
        return;
      }

      if (future.executor != null) {
        future.executor.execute(() -> notifyTimeout(future));
      } else {
        notifyTimeout(future);
      }
    }

    /**
     * 通知超时
     *
     * @param future
     */
    private void notifyTimeout(RpcFuture future) {
      // 1. 创建异常响应对象
      MsgResponse timeoutResponse = new MsgResponse(future.id, future.getMid());
      // 2. 设置超时状态码
      timeoutResponse.mStatus =
          future.isSent() ? MsgResponse.SERVER_TIMEOUT : MsgResponse.CLIENT_TIMEOUT;
      // 3. 处理响应
      RpcFuture.received(timeoutResponse, true);
    }
  }

  private int getMid() {
    return request.mId;
  }
}
