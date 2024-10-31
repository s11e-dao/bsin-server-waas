package me.flyray.bsin.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * @author leonard
 * @date 2024/10/30 00:32
 * @desc 交易类型： 1、充值 2、转账 3、提现 4、退款 5、消费
 */
public enum TransactionType {

  /** 充值 */
  RECHARGE("1", "充值"),

  /** 转账 */
  TRANSFER("2", "转账"),

  /** 提现 */
  WITHDRAWAL("3", "提现"),

  /** 退款 */
  REFUND("4", "退款"),

  /** 消费 */
  CONSUMPTION("5", "消费");

  private String code;

  private String desc;

  TransactionType(String code, String desc) {
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
  public static TransactionType getInstanceById(String id) {
    if (id == null) {
      return null;
    }
    for (TransactionType status : values()) {
      if (id.equals(status.getCode())) {
        return status;
      }
    }
    return null;
  }
}
