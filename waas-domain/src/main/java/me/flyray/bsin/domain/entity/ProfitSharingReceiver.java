package me.flyray.bsin.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 参与分润的接受者绑定关系表
 * @TableName waas_profit_sharing_receiver
 */

@Data
@TableName(value ="waas_profit_sharing_receiver")
public class ProfitSharingReceiver implements Serializable {
    /**
     * 
     */
    @TableId
    private String serialNo;

    /**
     * 租户号
     */
    private String tenantId;

    /**
     * 商户号:分账方商户ID
     */
    private String senderMerchantNo;

    /**
     * 型: 1-普通商户, 2-特约商户(服务商模式)
     */
    private String type;

    /**
     * 资金接受者名称
     */
    private String receiverName;

    /**
     * 资金接受者
     */
    private String receiverId;

    /**
     * 绑定状态:（本系统状态，并不调用上游关联关系）: 1-正常分账, 0-暂停分账
     */
    private String status;

    /**
     * 绑定时间
     */
    private Date createTime;

    /**
     * 支付通道代码：全小写  wxpay alipay
     */
    private String payChannelCode;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}