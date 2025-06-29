package me.flyray.bsin.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * @author leonard
 * @date 2024/1/15 9:32
 * @desc 类型;1、bancor curve 2、sigmoid curve
 */
public enum BondingCurveTokenType {

  /** 默认钱包 */
  BANCOR_CURVE("0", "bancor curve"),
  /** 自定义钱包 */
  SIGMOID_CURVE("1", "sigmoid curve");

  private String type;

  private String desc;

  BondingCurveTokenType(String type, String desc) {
    this.type = type;
    this.desc = desc;
  }

  public String getType() {
    return type;
  }

  public String getDesc() {
    return desc;
  }

  /** Json 枚举序列化 */
  @JsonCreator
  public static BondingCurveTokenType getInstanceById(String id) {
    if (id == null) {
      return null;
    }
    for (BondingCurveTokenType status : values()) {
      if (id.equals(status.getType())) {
        return status;
      }
    }
    return null;
  }
}
