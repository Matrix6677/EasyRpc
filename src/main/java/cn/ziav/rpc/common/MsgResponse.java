package cn.ziav.rpc.common;

/** @author Zavi */
public class MsgResponse<R> {
  /** ok. */
  public static final byte OK = 20;
  /** client side timeout. */
  public static final byte CLIENT_TIMEOUT = 30;

  /** server side timeout. */
  public static final byte SERVER_TIMEOUT = 31;

  /** channel inactive, directly return the unfinished requests. */
  public static final byte CHANNEL_INACTIVE = 35;

  /** request format error. */
  public static final byte BAD_REQUEST = 40;

  /** response format error. */
  public static final byte BAD_RESPONSE = 50;

  /** service not found. */
  public static final byte SERVICE_NOT_FOUND = 60;

  /** service error. */
  public static final byte SERVICE_ERROR = 70;

  /** internal server error. */
  public static final byte SERVER_ERROR = 80;

  /** internal server error. */
  public static final byte CLIENT_ERROR = 90;

  /** server side threadpool exhausted and quick return. */
  public static final byte SERVER_THREADPOOL_EXHAUSTED_ERROR = 100;

  public long id;
  /** 消息id */
  public int mId;

  public R mData;

  public byte mStatus = OK;

  public MsgResponse(long id, int mId) {
    this.id = id;
    this.mId = mId;
  }

  public MsgResponse(long id, int mId, R mData) {
    this.id = id;
    this.mId = mId;
    this.mData = mData;
  }
}
