package cn.ziav.rpc.common;

import cn.ziav.rpc.utils.Bytes;

/** @author Zavi */
public interface Constants {
  /** 默认IO线程池数 */
  int DEFAULT_IO_THREADS = Math.min(Runtime.getRuntime().availableProcessors() + 1, 32);

  /** 默认心跳时间 */
  int DEFAULT_HEARTBEAT = 60 * 1000;
  /** 8M */
  int DEFAULT_PAYLOAD = 8 * 1024 * 1024;

  /** 协议头长度 */
  int HEADER_LENGTH = 20;
  /** 魔数 */
  short MAGIC = (short) 0xdabb;

  byte MAGIC_HIGH = Bytes.short2bytes(MAGIC)[0];
  byte MAGIC_LOW = Bytes.short2bytes(MAGIC)[1];
  /** 请求标识 */
  byte FLAG_REQUEST = (byte) 0x80;
  /** 往返请求标识 */
  byte FLAG_TWOWAY = (byte) 0x40;

  /** 默认客户端连接超时 */
  int DEFAULT_CONNECT_TIMEOUT = 3000;

  /** ZK默认连接超时 */
  int ZK_SESSION_TIMEOUT = 5000;

  String ZK_REGISTRY_PATH = "/easy-rpc";
}
