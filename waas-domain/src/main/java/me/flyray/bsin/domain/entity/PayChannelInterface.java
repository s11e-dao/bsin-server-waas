package me.flyray.bsin.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import me.flyray.bsin.domain.enums.PayChannelInterfaceEnum;
import me.flyray.bsin.domain.enums.PayWayEnum;

import java.io.Serializable;
import java.util.Date;

/**
 * 支付渠道具体接口定义表
 * @TableName waas_pay_channel_interface
 */
@Data
@TableName(value ="waas_pay_channel_interface")
public class PayChannelInterface implements Serializable {
    /**
     * 支付通道接口代码 全小写  wxpay alipay
     * @see PayChannelInterfaceEnum
     */
    private String payChannelCode;

    /**
     * 支付通道接口名称
     */
    private String payChannelName;

    /**
     * 支付参数配置页面类型:1-JSON渲染,2-自定义
     */
    private Integer configPageType;

    /**
     * 普通商户接口配置定义描述,json字符串
     * 定义支付接口需要配置的参数，在config里面根据定义赋值具体的值
     */
    private String params;

    /**
     * 支持的支付方式 ["wxpay_jsapi", "wxpay_bar"]
     * @see PayWayEnum
     */
    private String wayCode;

    /**
     * 页面展示：卡片-图标
     */
    private String icon;

    /**
     * 状态: 0-停用, 1-启用
     */
    private Integer status;

    /**
     * 是否支持普通商户模式
     */
    private Boolean isNormalMerchanMode;
    
    /**
     * 是否支持服务商子商户模式
     */
    private Boolean isServiceSubMerchantMode;

    /**
     * 普通商户模式参数
     */
    private String normalMerchantParams;

    /**
     * 特殊商户模式参数
     */
    private String specialMerchantParams;

    /**
     * 服务商子商户模式参数
     */
    private String serviceSubMerchantParams;

    /**
     * 备注
     */
    private String remark;

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
    private String tenantId;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}