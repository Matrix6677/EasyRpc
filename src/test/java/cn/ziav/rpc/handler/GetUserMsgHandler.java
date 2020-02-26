package cn.ziav.rpc.handler;

import cn.ziav.rpc.bean.User;
import cn.ziav.rpc.server.IMsgHandler;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** @author Zavi */
public class GetUserMsgHandler implements IMsgHandler<Integer, User> {

  @Override
  public User process(Integer id) throws Throwable {
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

    return user;
  }

  @Override
  public int msgId() {
    return MsgId.GET_USER;
  }
}
