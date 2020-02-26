package cn.ziav.rpc.client;

import static cn.ziav.rpc.common.Constants.DEFAULT_CONNECT_TIMEOUT;
import static cn.ziav.rpc.common.Constants.DEFAULT_HEARTBEAT;
import static cn.ziav.rpc.exception.ExceptionCode.CLIENT_CONNECTED_FAILED;
import static cn.ziav.rpc.exception.ExceptionCode.CLIENT_HAS_CLOSED;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import cn.ziav.rpc.codec.RpcDecoder;
import cn.ziav.rpc.codec.RpcEncoder;
import cn.ziav.rpc.common.Constants;
import cn.ziav.rpc.common.MsgRequest;
import cn.ziav.rpc.exception.RemotingException;
import cn.ziav.rpc.utils.NamedThreadFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher.Event;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Zavi */
public class RpcClient {
  private static final Logger logger = LoggerFactory.getLogger(RpcClient.class);

  /** zk注册中心 */
  private final String zkAddr;
  /** 注册到具体的topic */
  private final String topic;

  /** 连接处理器 */
  private final RpcClientHandler rpcClientHandler;

  /** 客户端启动器 */
  private final Bootstrap bootstrap;
  /** netty client bootstrap */
  private static final NioEventLoopGroup NIO_EVENT_LOOP_GROUP =
      new NioEventLoopGroup(
          Constants.DEFAULT_IO_THREADS, new DefaultThreadFactory("NettyClientWorker", true));

  private ZooKeeper zk;

  private volatile boolean closed;
  /** 服务器节点列表 */
  private volatile List<String> nodeList = new ArrayList<>();

  private final Map<String, ServerNode> nodeMap = new ConcurrentHashMap<>();

  private Lock lock = new ReentrantLock();

  private int roundIdx = 0;

  private final ExecutorService SHARED_EXECUTOR =
      Executors.newCachedThreadPool(new NamedThreadFactory("EasyRpcSharedHandler", true));

  /**
   * 初始化Client端，并连接zookeeper
   *
   * @param zkAddr zk地址
   * @param topic 服务发现的topic
   * @throws Throwable
   */
  public RpcClient(String zkAddr, String topic) throws Throwable {
    this.zkAddr = zkAddr;
    this.topic = topic;
    bootstrap = new Bootstrap();
    bootstrap
        // 设置线程池组
        .group(NIO_EVENT_LOOP_GROUP)
        // 设置连接存活探测
        .option(ChannelOption.SO_KEEPALIVE, true)
        // 禁用Nagle算法
        .option(ChannelOption.TCP_NODELAY, true)
        // 设置缓冲区分配器
        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        // 设置连接超时
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, DEFAULT_CONNECT_TIMEOUT)
        // 设置非阻塞的连接套接字通道
        .channel(NioSocketChannel.class);

    this.rpcClientHandler = new RpcClientHandler();
    bootstrap.handler(
        new ChannelInitializer() {

          @Override
          protected void initChannel(Channel ch) throws Exception {
            ch.pipeline()
                // 解码器
                .addLast("decoder", new RpcDecoder())
                // 编码器
                .addLast("encoder", new RpcEncoder())
                // 心跳检测
                .addLast(
                    "client-idle-handler",
                    new IdleStateHandler(DEFAULT_HEARTBEAT * 3, 0, 0, MILLISECONDS))
                // 客户端请求处理器
                .addLast("handler", rpcClientHandler);
          }
        });

    // 初始化Zookeeper
    initZooKeeper(zkAddr);
    // 更新服务器节点信息
    updateNodeList();
  }

  /**
   * 初始化Zookeeper
   *
   * @param zkAddr
   * @throws IOException
   * @throws InterruptedException
   */
  private void initZooKeeper(String zkAddr) throws IOException, InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);

    ZooKeeper zk =
        new ZooKeeper(
            zkAddr,
            Constants.ZK_SESSION_TIMEOUT,
            event -> {
              if (event.getState() == Event.KeeperState.SyncConnected) {
                latch.countDown();
              }
            });
    latch.await();
    this.zk = zk;
  }

  /** 更新服务器节点信息 */
  private void updateNodeList() {
    lock.lock();
    try {
      List<String> oldNodeList = this.nodeList;
      this.nodeList =
          zk.getChildren(
              Constants.ZK_REGISTRY_PATH + "/" + topic,
              event -> {
                if (event.getType() == Event.EventType.NodeChildrenChanged) {
                  updateNodeList();
                }
              });
      // 新列表和老列表完全相同，那么什么都不需要做
      if (this.nodeList.containsAll(oldNodeList) && this.nodeList.size() == oldNodeList.size()) {
        return;
      }

      Set<String> set = new HashSet<>(this.nodeList);
      // 新老列表存在交集或没有交集
      boolean flag = set.retainAll(oldNodeList);
      if (flag) {
        // 移除交集部分，剩下的就是需要被删除的
        oldNodeList.removeAll(set);
        oldNodeList.forEach(this.nodeMap::remove);
      }

      // 其他情况，直接putIfAbsent
      this.nodeList.forEach(addr -> this.nodeMap.putIfAbsent(addr, new ServerNode(addr)));
    } catch (KeeperException | InterruptedException e) {
      logger.error("", e);
    } finally {
      lock.unlock();
    }
  }

  public <T, R> R send(String addr, int msgId, T body, int timeout) throws Throwable {
    // 判断client是否已关闭
    if (closed) {
      throw new RemotingException(CLIENT_HAS_CLOSED);
    }
    // 根据远程服务器地址获取连接通道
    Channel channel = getOrCreateChannel(addr);
    // 构建请求
    MsgRequest<T> request = new MsgRequest<>();
    request.mTwoWay = false;
    request.mData = body;
    request.mId = msgId;
    // 代理给RpcFuture处理结果
    RpcFuture<R> rpcFuture =
        RpcFuture.newFuture(request, null, timeout, nodeMap.get(addr), SHARED_EXECUTOR);
    try {
      // 写入缓冲区
      channel.writeAndFlush(request);
      // 标记请求已发送
      RpcFuture.sent(request);
      return rpcFuture.get();
    } catch (Throwable e) {
      // 异常取消
      rpcFuture.cancel(true);
      if (e instanceof ExecutionException) {
        throw e.getCause();
      }
      throw e;
    }
  }

  /**
   * 获取或创建Channel
   *
   * @param addr
   * @return
   * @throws RemotingException
   */
  private Channel getOrCreateChannel(String addr) throws RemotingException {
    // 根据Server端的地址获取连接Channel
    Channel channel = rpcClientHandler.getChannel(addr);
    if (channel != null) {
      return channel;
    }

    String[] split = addr.split(":");
    // 建立连接
    ChannelFuture future = bootstrap.connect(split[0], Integer.parseInt(split[1]));
    // 阻塞等待结果
    boolean ret = future.awaitUninterruptibly(DEFAULT_CONNECT_TIMEOUT, MILLISECONDS);
    if (ret && future.isSuccess()) {
      Channel newChannel = future.channel();
      channel = rpcClientHandler.getOrAddChannel(addr, future.channel());
      // 如果旧的连接跟新连接是同一个对象，那么就把新的给关掉
      if (newChannel != channel) {
        newChannel.close();
      }
    } else if (future.cause() != null) {
      // 连接异常
      throw new RemotingException(CLIENT_CONNECTED_FAILED, future.cause());
    } else {
      // 连接发生未知异常
      throw new RemotingException(CLIENT_CONNECTED_FAILED, "unknown error");
    }
    return channel;
  }

  public <T, R> void sendAsync(
      String addr, int msgId, T body, int timeout, EasyRpcCallback<R> callback) {
    // 判断client是否已关闭
    if (closed) {
      callback.fail(new RemotingException(CLIENT_HAS_CLOSED));
      return;
    }
    Channel channel;
    try {
      channel = getOrCreateChannel(addr);
    } catch (RemotingException e) {
      closed = true;
      callback.fail(e);
      return;
    }

    // 构建请求对象
    MsgRequest<T> request = new MsgRequest<>();
    request.mTwoWay = false;
    request.mId = msgId;
    request.mData = body;
    // 代理给RpcFuture处理结果
    RpcFuture rpcFuture =
        RpcFuture.newFuture(request, callback, timeout, nodeMap.get(addr), SHARED_EXECUTOR);
    try {
      // 写入缓冲区
      channel.writeAndFlush(request);
      // 标记请求已发送
      RpcFuture.sent(request);
    } catch (Exception e) {
      closed = true;
      rpcFuture.cancel(true);
      callback.fail(e);
    }
  }

  public String randomNode() {
    lock.lock();
    try {
      int i = ThreadLocalRandom.current().nextInt(nodeList.size());
      return nodeList.get(i);
    } finally {
      lock.unlock();
    }
  }

  public String roundNode() {
    lock.lock();
    try {
      if (roundIdx > nodeList.size() - 1) {
        roundIdx = 0;
      }

      return nodeList.get(roundIdx++);
    } finally {
      lock.unlock();
    }
  }

  public String hashNode(Object object) {
    int mod = object.hashCode() % nodeList.size();
    return nodeList.get(Math.abs(mod));
  }

  public String lowLatencyNode() {
    ServerNode serverNode =
        this.nodeMap.values().stream()
            .min(Comparator.comparingDouble(ServerNode::calAvgRespTime))
            .get();
    return serverNode.addr;
  }

  public void doClose() {
    closed = true;
    rpcClientHandler.closeChannel();
    SHARED_EXECUTOR.shutdownNow();
  }
}
