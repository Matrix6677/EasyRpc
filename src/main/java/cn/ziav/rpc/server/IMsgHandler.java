package cn.ziav.rpc.server;

import cn.ziav.rpc.utils.NamedThreadFactory;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 消息处理接口
 *
 * @author Zavi
 */
public interface IMsgHandler<T, R> {
  ExecutorService DEFAULT_THREAD_EXEC =
      new ThreadPoolExecutor(
          200,
          200,
          1,
          TimeUnit.MINUTES,
          new ArrayBlockingQueue<>(200),
          new NamedThreadFactory("EasyRpcMsgHandler", true));

  /**
   * 处理业务
   *
   * @param t 请求参数
   * @return 返回参数
   * @throws Throwable 异常
   */
  R process(T t) throws Throwable;

  /**
   * 业务消息id
   *
   * @return
   */
  int msgId();

  /**
   * 自定义业务线程池，默认{@code DEFAULT_THREAD_EXEC}
   *
   * @return
   */
  default ExecutorService threadPool() {
    return DEFAULT_THREAD_EXEC;
  }
}
