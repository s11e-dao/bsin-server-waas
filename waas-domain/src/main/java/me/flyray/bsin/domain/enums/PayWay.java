package me.flyray.bsin.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * @author leonard
 * @date 2024/10/30 00:32
 * @desc 支付方式;1、默认钱包 2、自定义钱包
 */
public enum PayWay {

  /** 微信支付 */
  WX("wx", "微信支付"),
  /** 微信支付 */
  DEFAULT("aliPay", "支付宝支付"),
  /** 品牌积分支付 */
  BRAND_POINT("brandsPoint", "品牌积分支付"),

  /** 火钻支付 */
  FIRE_DIAMOND("fireDiamond", "火钻支付");

  private String code;

  private String desc;

  PayWay(String code, String desc) {
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
  public static PayWay getInstanceById(String id) {
    if (id == null) {
      return null;
    }
    for (PayWay status : values()) {
      if (id.equals(status.getCode())) {
        return status;
      }
    }
    return null;
  }
}
