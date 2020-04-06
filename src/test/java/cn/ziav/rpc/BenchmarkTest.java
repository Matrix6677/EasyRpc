package cn.ziav.rpc;

import static cn.ziav.rpc.Constant.topic;
import static cn.ziav.rpc.Constant.zkAddr;

import cn.ziav.rpc.bean.User;
import cn.ziav.rpc.client.RpcClient;
import cn.ziav.rpc.handler.MsgId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

/** @author Zavi */
@State(Scope.Benchmark)
public class BenchmarkTest {
  public static final int CONCURRENCY = 32;

  private RpcClient client;

  private final AtomicInteger counter = new AtomicInteger(0);

  @Setup
  public void initClient() throws Throwable {
    client = new RpcClient(zkAddr, topic);
  }

  @TearDown
  public void close() {
    client.doClose();
  }

  @Benchmark
  @BenchmarkMode({Mode.Throughput, Mode.AverageTime, Mode.SampleTime})
  public void existUser() throws Throwable {
    String email = String.valueOf(counter.getAndIncrement());
    client.send(client.randomNode(), MsgId.EXIST_USER, email, 5000);
  }

  @Benchmark
  @BenchmarkMode({Mode.Throughput, Mode.AverageTime, Mode.SampleTime})
  public void createUser() throws Throwable {
    int id = counter.getAndIncrement();
    User user = new User();

    user.setId(id);
    user.setName("Doug Lea");
    user.setSex(1);
    user.setBirthday(LocalDate.of(1968, 12, 8));
    user.setEmail("dong.lea@gmail.com");
    user.setMobile("18612345678");
    user.setAddress("北京市 中关村 中关村大街1号 鼎好大厦 1605");
    user.setIcon("https://www.baidu.com/img/bd_logo1.png");
    user.setStatus(1);
    user.setCreateTime(LocalDateTime.now());
    user.setUpdateTime(user.getCreateTime());

    List<Integer> permissions =
        new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 19, 88, 86, 89, 90, 91, 92));

    user.setPermissions(permissions);
    client.send(client.randomNode(), MsgId.CREATE_USER, user, 5000);
  }

  @Benchmark
  @BenchmarkMode({Mode.Throughput, Mode.AverageTime, Mode.SampleTime})
  public void getUser() throws Throwable {
    int id = counter.getAndIncrement();
    client.send(client.randomNode(), MsgId.GET_USER, id, 5000);
  }

  @Benchmark
  @BenchmarkMode({Mode.Throughput, Mode.AverageTime, Mode.SampleTime})
  public void listUser() throws Throwable {
    int pageNo = counter.getAndIncrement();
    client.send(client.randomNode(), MsgId.LIST_USER, pageNo, 5000);
  }

  public static void main(String[] args) throws Throwable {
    Options opt =
        new OptionsBuilder()
            .timeUnit(TimeUnit.MILLISECONDS)
            .include(BenchmarkTest.class.getSimpleName())
            .warmupIterations(5) //
            .warmupTime(TimeValue.seconds(3)) //
            .measurementIterations(10) //
            .measurementTime(TimeValue.seconds(5)) //
            .threads(CONCURRENCY)
            .forks(1)
            .build();
    new Runner(opt).run();
  }
}
