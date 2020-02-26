package cn.ziav.rpc.client;

/**
 * 回调接口
 *
 * @author Zavi
 */
public interface EasyRpcCallback<R> {
  /**
   * 成功
   *
   * @param result
   */
  void success(R result);

  /**
   * 失败
   *
   * @param throwable
   */
  void fail(Throwable throwable);
}
