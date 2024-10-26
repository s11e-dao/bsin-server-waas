package me.flyray.bsin.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 支付渠道表
 * @TableName waas_pay_way
 */
@Data
@TableName(value ="waas_pay_way")
public class PayWay implements Serializable {
    /**
     * 支付方式代码  例如： wxpay_jsapi
     */
    @TableId
    private String payWayCode;

    private String tenantId;

    /**
     * 支付渠道名称
     */
    private String payWayName;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 
     */
    private String serialNo;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}