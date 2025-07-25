package me.flyray.bsin.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * @author leonard
 * @date 2024/10/30 00:32
 * @desc 支付接口：
 */
public enum PayChannelInterfaceEnum {

  /** 微信支付 */
  WXPAY("wxPay", "微信支付"),

  /** 支付宝支付 */
  ALIPAY("aliPay", "支付宝支付"),

  /** 品牌积分支付 */
  BRAND_POINT("brandsPoint", "品牌积分支付"),

  /** 火钻支付 */
  FIRE_DIAMOND("fireDiamond", "火钻支付");

  private String code;

  private String desc;

  PayChannelInterfaceEnum(String code, String desc) {
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
  public static PayChannelInterfaceEnum getInstanceById(String id) {
    if (id == null) {
      return null;
    }
    for (PayChannelInterfaceEnum status : values()) {
      if (id.equals(status.getCode())) {
        return status;
      }
    }
    return null;
  }
}
