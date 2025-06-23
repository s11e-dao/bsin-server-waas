package me.flyray.bsin.domain.request;

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
public class ParticipantRoleAllocationDTO implements Serializable {

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
     * 运营平台分佣金额
     */
    private BigDecimal superTenantValue;

    /**
     * 租户平台分佣比例
     */
    private BigDecimal tenantValue;

    /**
     * 代理商分佣比例
     */
    private BigDecimal sysAgentValue;

    /**
     * 消费者返利比例
     */
    private BigDecimal customerValue;

    /**
     * 分销模型的分销者比例
     */
    private BigDecimal distributorValue;

    /**
     * 佣金兑换数字积分比例
     */
    private BigDecimal exchangeDigitalPointsRate;


}