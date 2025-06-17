package me.flyray.bsin.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * @author leonard
 * @date 2024/10/30 00:32
 * @desc 交易状态; 1、等待 2、成功 3、失败
 */
public enum TransactionStatus {

  /** 等待 */
  PENDING("1", "等待"),

  /** 成功 */
  SUCCESS("2", "成功"),

  /** 失败 */
  FAIL("3", "失败");

  private String code;

  private String desc;

  TransactionStatus(String code, String desc) {
    this.code = code;
    this.desc = desc;
  }

  public String getCode() {
    return code;
  }

  public String getDesc() {
    return desc;
  }

  /** Json 枚举序列化 */
  @JsonCreator
  public static TransactionStatus getInstanceById(String id) {
    if (id == null) {
      return null;
    }
    for (TransactionStatus status : values()) {
      if (id.equals(status.getCode())) {
        return status;
      }
    }
    return null;
  }

}
