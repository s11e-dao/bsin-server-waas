package me.flyray.bsin.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商户让利配置
 * @TableName waas_profit_sharing_config
 */

@Data
@TableName(value ="waas_profit_sharing_config")
public class ProfitSharingConfig implements Serializable {
    /**
     * 设置规则的租户
     */
    @TableId
    private String tenantId;

    /**
     * 
     */
    private String serialNo;

    /**
     * 运营平台分佣比例
     */
    private BigDecimal superTenantRate;

    /**
     * 租户平台分佣比例
     */
    private BigDecimal tenantRate;

    /**
     * 代理商分佣比例
     */
    private BigDecimal sysAgentRate;

    /**
     * 消费者返利比例
     */
    private BigDecimal customerRate;

    /**
     * 分销模型的分销者比例
     */
    private BigDecimal distributorRate;

    /**
     * 设置规则的商户
     */
    private String merchantNo;

    /**
     * 商户让利比例
     */
    private BigDecimal merchantSharingRate;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}