package me.flyray.bsin.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 交易让利分账流水
 * @TableName waas_profit_sharing_journal
 */

@Data
@TableName(value ="waas_profit_sharing_journal")
public class ProfitSharingJournal implements Serializable {
    /**
     * 
     */
    @TableId
    private String serialNo;

    /**
     * 所属租户
     */
    private String tenantId;

    /**
     * 分润商户
     */
    private Integer merchantNo;

    /**
     * 交易单号
     */
    private String transactionNo;

    /**
     * 外部(oms)订单号
     */
    private Integer outSerialNo;

    /**
     * 分润角色类型
     */
    private String bizRoleType;

    /**
     * 分润角色编号
     */
    private String bizRoleNo;

    /**
     * 资金接受者账号
     */
    private String receiverId;

    /**
     * 接受者的支付渠道
     */
    private String receiverChannel;

    /**
     * 分润金额
     */
    private String profitSharingAmount;

    /**
     * 状态
     */
    private String status;

    /**
     * 分润时间
     */
    private Date createdTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}