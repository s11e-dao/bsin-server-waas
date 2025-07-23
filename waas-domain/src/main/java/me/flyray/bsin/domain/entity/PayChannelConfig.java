package me.flyray.bsin.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 应用支付接口参数配置表
 * @TableName waas_pay_channel_config
 */
@Data
@TableName(value ="waas_pay_channel_config")
public class PayChannelConfig implements Serializable {
    /**
     * ID
     */
    @TableId
    private String serialNo;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 应用ID
     */
    private String bizRoleAppId;

    /**
     * 商户号: 支付参数对应的商户号，平台应用为空，平台商户为商户ID
     */
    private String merchantNo;

    /**
     * 支付通道代码
     */
    private String payChannelCode;

    /**
     * 支付通道费率
     */
    private BigDecimal feeRatio;

    /**
     * 状态: 0-停用, 1-启用
     */
    private String status;

    /**
     * 是否支持普通商户模式
     */
    private Boolean isNormalMerchantMode;
    
    /**
     * 是否支持服务商子商户模式
     */
    private Boolean isIsvSubMerchantMode;

    /**
     * 普通商户参数: json
     *
     */
    private String normalMerchantParams;

    /**
     * 特约商户参数: json
     */
    private String specialMerchantParams;

    /**
     * 服务商参数
     */
    private String isvParams;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建者姓名
     */
    private String createBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新者姓名
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}