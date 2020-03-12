package cn.ziav.rpc.client;

import java.util.concurrent.atomic.LongAdder;

/** @author Zavi */
public class ServerNode {
  public final String addr;
  /** 总响应时间（毫秒） */
  private LongAdder totalRespTime = new LongAdder();

  /** 总请求次数 */
  private LongAdder totalReqTimes = new LongAdder();

  public ServerNode(String addr) {
    this.addr = addr;
  }

  /**
   * 累计总响应时间
   *
   * @param time
   */
  public void addCost(long time) {
    totalRespTime.add(time);
    totalReqTimes.increment();
  }

  /**
   * 平均耗时
   *
   * @return
   */
  public double calAvgRespTime() {
    long sumRespTime = totalRespTime.sum();
    if (sumRespTime == 0) {
      return 0;
    }
    long sumReqTimes = totalReqTimes.sum();
    if (sumReqTimes == 0) {
      return 0;
    }

    return sumRespTime * 1.0f / sumReqTimes;
  }
}
