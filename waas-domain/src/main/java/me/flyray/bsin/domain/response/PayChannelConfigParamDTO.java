package me.flyray.bsin.domain.response;

import lombok.Data;
import me.flyray.bsin.domain.entity.Wallet;
import me.flyray.bsin.domain.enums.PayMerchantModeEnum;

@Data
public class PayChannelConfigParamDTO extends Wallet {

  /** 商户模式 */
  public PayMerchantModeEnum payMerchantMode;

  /** 普通商户模式参数: json */
  public String normalMerchantParams;

  /** 特约商户模式参数: json */
  public String specialMerchantParams;

  /** 服务商子商户模式参数: json */
  public String isvSubMerchantParams;
}
