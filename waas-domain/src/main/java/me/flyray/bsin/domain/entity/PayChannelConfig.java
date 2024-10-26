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

    private String tenantId;

    /**
     * 服务商号/商户号/应用ID
     */
    private String bizRoleAppId;

    /**
     * 支付接口代码
     */
    private String payServiceCode;

    /**
     * 接口配置参数,json字符串
     */
    private String params;

    /**
     * 支付接口费率
     */
    private BigDecimal feeRatio;

    /**
     * 状态: 0-停用, 1-启用
     */
    private Integer state;

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