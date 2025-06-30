package me.flyray.bsin.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * @author leonard
 * @date 2025/6/30 9:32
 * @desc
 */
public enum BondingCurveTokenStatus {

  /** 正常 */
  NORMAL("0", "正常"),
  /** 冻结 */
  FREEZED("1", "冻结"),
  /** 注销 */
  CANCELLED("2", "注销");

  private String code;

  private String desc;

  BondingCurveTokenStatus(String code, String desc) {
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
  public static BondingCurveTokenStatus getInstanceById(String id) {
    if (id == null) {
      return null;
    }
    for (BondingCurveTokenStatus status : values()) {
      if (id.equals(status.getCode())) {
        return status;
      }
    }
    return null;
  }
}
