package cn.ziav.rpc.exception;

/** @author Zavi */
public interface ExceptionCode {
  /** 客户端连接失败 */
  int CLIENT_CONNECTED_FAILED = -1;
  /** 超时 */
  int TIME_OUT = -2;
  /** 请求格式错误 */
  int BAD_REQUEST = -3;
  /** 响应格式错误 */
  int BAD_RESPONSE = -4;
  /** 客户端已关闭 */
  int CLIENT_HAS_CLOSED = -5;
  /** 没有对应的handler */
  int NO_SUCH_HANDLER = -6;
  /** 未知错误 */
  int UNKNOWN_ERROR = -255;
}
