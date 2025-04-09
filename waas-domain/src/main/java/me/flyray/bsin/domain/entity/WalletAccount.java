package me.flyray.bsin.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import me.flyray.bsin.entity.BaseEntity;
import me.flyray.bsin.validate.AddGroup;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 钱包账户;
 * @TableName crm_wallet_account
 */
@Data
@TableName(value ="waas_wallet_account")
public class WalletAccount extends BaseEntity implements Serializable {
    /**
     * 地址
     */
    @NotBlank(message = "链地址不能为空" ,groups = AddGroup.class)
    private String address;

    /**
     * 签名公钥
     */
    @NotBlank(message = "签名公钥不能为空" ,groups = AddGroup.class)
    private String pubKey;
    /**
     * 链上货币id
     */
    @NotBlank(message = "链币种编号不能为空" ,groups = AddGroup.class)
    private String chainCoinNo;

    /**
     * 账户状态 1、正常 2、冻结
     */
    private Integer status;

    /**
     * 余额
     */
    private BigDecimal balance;

    /**
     * 钱包id
     */
    @NotBlank(message = "钱包编号不能为空" ,groups = AddGroup.class)
    private String walletNo;
    /**
     * 租户ID
     */
    @NotBlank(message = "租户ID不能为空" ,groups = AddGroup.class)
    private String tenantId;

}
