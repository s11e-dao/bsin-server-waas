package me.flyray.bsin.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * @author leonard
 * @date 2024/1/15 9:32
 * @desc 类型;1、默认钱包 2、自定义钱包
 */
public enum WalletType {

  /** 默认钱包 */
  DEFAULT("1", "默认钱包"),
  /** 自定义钱包 */
  CUSTOMIZE("2", "自定义钱包");

  private String code;

  private String desc;

  WalletType(String code, String desc) {
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
  public static WalletType getInstanceById(String id) {
    if (id == null) {
      return null;
    }
    for (WalletType status : values()) {
      if (id.equals(status.getCode())) {
        return status;
      }
    }
    return null;
  }
}
