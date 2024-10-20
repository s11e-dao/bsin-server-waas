package me.flyray.bsin.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * @author leonard
 * @date 2024/1/15 9:32
 * @desc 状态;1、正常 2、冻结 3、注销
 */
public enum WalletStatus {

  /** 正常 */
  NORMAL("1", "正常"),
  /** 冻结 */
  FREEZED("2", "冻结"),
  /** 注销 */
  CANCELLED("2", "注销");

  private String code;

  private String desc;

  WalletStatus(String code, String desc) {
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
  public static WalletStatus getInstanceById(String id) {
    if (id == null) {
      return null;
    }
    for (WalletStatus status : values()) {
      if (id.equals(status.getCode())) {
        return status;
      }
    }
    return null;
  }
}
