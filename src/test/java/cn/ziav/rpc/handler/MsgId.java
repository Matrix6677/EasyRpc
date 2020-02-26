package cn.ziav.rpc.handler;

/** @author Zavi */
public interface MsgId {
  int HELLO = 1;
  int TIMEOUT = 2;
  int EXCEPTION = 3;
  int MULTI_THREAD = 4;
  int GET_USER = 5;
  int LIST_USER = 6;
  int CREATE_USER = 7;
  int EXIST_USER = 8;
}
