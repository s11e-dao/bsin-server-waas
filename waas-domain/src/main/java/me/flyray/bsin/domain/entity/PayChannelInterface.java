package me.flyray.bsin.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import me.flyray.bsin.domain.enums.PayInterfaceCode;

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
     * 接口代码 全小写  wxpay alipay
     * @see PayInterfaceCode
     */
    @TableId
    private String payChannelCode;

    /**
     * 接口名称
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
     * @see me.flyray.bsin.domain.enums.PayWayEnum
     */
    private Object wayCode;

    /**
     * 页面展示：卡片-图标
     */
    private String icon;

    /**
     * 状态: 0-停用, 1-启用
     */
    private Integer status;

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