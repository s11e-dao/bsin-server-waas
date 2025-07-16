package me.flyray.bsin.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 
 * @TableName waas_bonding_curve_token_journal
 */

@Data
@TableName(value ="waas_bonding_curve_token_journal")
public class BondingCurveTokenJournal implements Serializable {
    /**
     * 铸造流水号
     */
    @TableId
    private String serialNo;

    /**
     * 积分当前供应量
     */
    private BigDecimal supply;

    /**
     * 储备金：这里特指劳动价值
     */
    private BigDecimal reserve;

    /**
     * 积分价格
     */
    private BigDecimal price;

    /**
     * 合约方法：mint|burn
     */
    private String method;


    /**
     * 劳动价值描述
     */
    private String description;

    /**
     * 交易hash
     */
    private String txHash;

    /**
     * 客户编号
     */
    private String customerNo;

    /**
     * 商户号
     */
    private String merchantNo;

    /**
     * 租户id
     */
    private String tenantId;

    /**
     * 创建时间
     */
    private Date createTime;

}