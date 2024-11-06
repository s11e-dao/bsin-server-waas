package me.flyray.bsin.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * @author leonard
 * @date 2024/11/06 0:02
 * @desc 支付通道代码: ??????????????
 */
public enum PayInterfaceCode {
  WXPAY("wxpay", "微信"),
  ALIPAY("alipay", "支付宝"),
  ;

  private String code;

  private String desc;

  PayInterfaceCode(String code, String desc) {
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
  public static PayInterfaceCode getInstanceById(String id) {
    if (id == null) {
      return null;
    }
    for (PayInterfaceCode status : values()) {
      if (id.equals(status.getCode())) {
        return status;
      }
    }
    return null;
  }
}
