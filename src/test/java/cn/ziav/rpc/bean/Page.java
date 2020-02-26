package cn.ziav.rpc.bean;

import java.io.Serializable;
import java.util.List;

/** @author Zavi */
public class Page<T> implements Serializable {

  private int pageNo;
  private int total;
  private List<T> result;

  public int getPageNo() {
    return pageNo;
  }

  public void setPageNo(int pageNo) {
    this.pageNo = pageNo;
  }

  public int getTotal() {
    return total;
  }

  public void setTotal(int total) {
    this.total = total;
  }

  public List<T> getResult() {
    return result;
  }

  public void setResult(List<T> result) {
    this.result = result;
  }

  @Override
  public String toString() {
    return "Page [pageNo=" + pageNo + ", total=" + total + ", result=" + result + "]";
  }
}
