package me.flyray.bsin.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * @author leonard
 * @date 2024/1/15 9:32
 * @desc 钱包分类： 1、MVP 2、多签
 */
public enum WalletCategory {

  /** MPC */
  MPC("1", "MPC"),

  /** 多签 */
  MULTIPLE_SIGNATURES("2", "多签"),
  /** 分层不切定性钱包 */
  HD("3", "HD"),
  /** 数字积分 */
  DIGITAL_TOKEN("4", "数字积分");

  private String code;

  private String desc;

  WalletCategory(String code, String desc) {
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
  public static WalletCategory getInstanceById(String id) {
    if (id == null) {
      return null;
    }
    for (WalletCategory status : values()) {
      if (id.equals(status.getCode())) {
        return status;
      }
    }
    return null;
  }
}
