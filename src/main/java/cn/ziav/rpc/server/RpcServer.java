package cn.ziav.rpc.server;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import cn.ziav.rpc.codec.RpcDecoder;
import cn.ziav.rpc.codec.RpcEncoder;
import cn.ziav.rpc.common.Constants;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Zavi */
public class RpcServer {

  private static final Logger logger = LoggerFactory.getLogger(RpcServer.class);
  /** 主机地址 */
  private final String host;
  /** 端口 */
  private final int port;
  /** zookeeper连接地址 */
  private final String zkAddr;

  private final String topic;

  /** worker通道 <ip:port,channel> */
  private Map<String, Channel> channels;
  /** Server启动器 */
  private ServerBootstrap bootstrap;
  /** boss通道：用于接收连接和分发给worker通道 */
  private Channel channel;
  /** boss线程池 */
  private EventLoopGroup bossGroup;
  /** worker线程池 */
  private EventLoopGroup workerGroup;
  /** Netty Server处理器 */
  private RpcDispatcher rpcDispatcher;

  /**
   * 初始化一个Server
   *
   * @param zkAddr 注册中心地址
   * @param topic 需要注册的topic
   * @param host 主机地址
   * @param port 端口
   * @throws Throwable
   */
  public RpcServer(String zkAddr, String topic, String host, int port) throws Throwable {
    this.zkAddr = zkAddr;
    this.host = host;
    this.port = port;
    this.topic = topic;
    this.bootstrap = new ServerBootstrap();

    this.bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("NettyServerBoss", false));
    this.workerGroup =
        new NioEventLoopGroup(
            Constants.DEFAULT_IO_THREADS, new DefaultThreadFactory("NettyServerWorker", true));

    this.rpcDispatcher = new RpcDispatcher();
    this.channels = rpcDispatcher.getChannels();
    this.start();
  }

  /** 启动服务器 */
  private void start() throws Throwable {
    bootstrap
        // 设置线程池组
        .group(bossGroup, workerGroup)
        // 设置非阻塞的连接套接字通道
        .channel(NioServerSocketChannel.class)
        // 禁用Nagle算法
        .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
        // 端口复用
        .childOption(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
        // 设置缓冲区分配器
        .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        // 设置处理请求的Handler
        .childHandler(
            new ChannelInitializer<NioSocketChannel>() {
              @Override
              protected void initChannel(NioSocketChannel ch) throws Exception {
                // 心跳检测时间3分钟
                int idleTimeout = Constants.DEFAULT_HEARTBEAT * 3;
                ch.pipeline()
                    // 解码器
                    .addLast("decoder", new RpcDecoder())
                    // 编码器
                    .addLast("encoder", new RpcEncoder())
                    // 心跳检测
                    .addLast(
                        "server-idle-handler",
                        new IdleStateHandler(0, 0, idleTimeout, MILLISECONDS))
                    // 消息分发器
                    .addLast("dispatcher", rpcDispatcher);
              }
            });
    // 绑定主机和端口
    ChannelFuture channelFuture = bootstrap.bind(host, port);
    channelFuture.syncUninterruptibly();
    channel = channelFuture.channel();
    registerToZookeeper();
  }

  private void registerToZookeeper() throws Throwable {
    CountDownLatch latch = new CountDownLatch(1);
    // 初始化ZooKeeper
    ZooKeeper zk =
        new ZooKeeper(
            zkAddr,
            Constants.ZK_SESSION_TIMEOUT,
            event -> {
              KeeperState state = event.getState();
              if (state == KeeperState.SyncConnected) {
                latch.countDown();
                return;
              }

              if (state == KeeperState.Disconnected || state == KeeperState.Expired) {
                try {
                  // 自动重连
                  registerToZookeeper();
                } catch (Throwable throwable) {
                  logger.error("reconnect zk failed", throwable);
                }
              }
            });
    latch.await();

    // 创建/easy-rpc节点
    String path = Constants.ZK_REGISTRY_PATH;
    Stat s = zk.exists(path, false);
    if (s == null) {
      zk.create(path, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    }

    // 创建/easy-rpc/{topic}节点
    path += "/" + topic;
    s = zk.exists(path, false);
    if (s == null) {
      zk.create(path, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    }

    // 创建/easy-rpc/{topic}/{ip:port}节点
    path += "/" + host + ":" + port;
    s = zk.exists(path, false);
    if (s == null) {
      zk.create(path, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
    }
  }

  public void doClose() {
    try {
      if (channel != null) {
        // unbind.
        channel.close();
      }

      channels.forEach((addr, channel1) -> channel1.close());

      if (bootstrap != null) {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
      }

      if (channels != null) {
        channels.clear();
      }
    } catch (Throwable e) {
      logger.warn(e.getMessage(), e);
    }
  }

  public void register(IMsgHandler handler) {
    rpcDispatcher.register(handler);
  }
}
