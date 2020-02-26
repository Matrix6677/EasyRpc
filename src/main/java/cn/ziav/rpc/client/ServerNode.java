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

  public void addCost(long time) {
    totalRespTime.add(time);
    totalReqTimes.increment();
  }

  public double calAvgRespTime() {
    return totalRespTime.sum() / totalReqTimes.sum();
  }
}
