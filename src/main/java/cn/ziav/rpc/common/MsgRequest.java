package cn.ziav.rpc.common;

import java.util.concurrent.atomic.AtomicLong;

/** @author Zavi */
public class MsgRequest<T> {
  private static final AtomicLong INVOKE_ID = new AtomicLong(0);

  public long id;
  /** 消息id */
  public int mId;

  /** 是否为双向请求 */
  public boolean mTwoWay = true;

  public T mData;

  public MsgRequest(long id, int mId) {
    this.id = id;
    this.mId = mId;
  }

  public MsgRequest() {
    this.id = newId();
  }

  private static long newId() {
    return INVOKE_ID.getAndIncrement();
  }
}
