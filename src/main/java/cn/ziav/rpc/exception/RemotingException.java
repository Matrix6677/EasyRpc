package cn.ziav.rpc.exception;

/**
 * 统一异常
 *
 * @author Zavi
 */
public class RemotingException extends Exception {
  /** 异常码 */
  public final int code;

  public RemotingException(int code) {
    this.code = code;
  }

  public RemotingException(int code, String message) {
    super(message);
    this.code = code;
  }

  public RemotingException(int code, String message, Throwable cause) {
    super(message, cause);
    this.code = code;
  }

  public RemotingException(int code, Throwable cause) {
    super(cause);
    this.code = code;
  }

  protected RemotingException(
      int code,
      String message,
      Throwable cause,
      boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
    this.code = code;
  }
}
