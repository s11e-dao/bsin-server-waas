package me.flyray.bsin.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import me.flyray.bsin.domain.enums.TransactionStatus;
import me.flyray.bsin.entity.BaseEntity;
import me.flyray.bsin.enums.TransactionType;
import me.flyray.bsin.security.enums.BizRoleType;
import me.flyray.bsin.validate.AddGroup;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 交易记录;
 * @TableName crm_transaction
 */
@Data
@TableName(value ="waas_transaction")
public class Transaction extends BaseEntity implements Serializable {

    /**
     * ID
     */
    @TableId
    private String serialNo;

    /**
     * @see TransactionType
     */
    private String transactionType;
    /**
     * 交易hash
     */
    private String txHash;
    /**
     * 合约地址
     */
    private String contractAddress;
    /**
     * 执行的合约方法
     */
    private String contractMethod;
    /**
     * 合约方法调用类型 1、非合约调用 2、合约调用
     */
    private Integer methodInvokeWay;
    /**
     * 交易状态; 1、等待 2、成功 3、失败
     * @see TransactionStatus
     */
    private String transactionStatus;

    /**
     * 交易金额
     */
    private BigDecimal txAmount;

    /**
     * 实际消费gas费
     */
    private BigDecimal gasFee;

    /**
     * 手续费
     */
    private BigDecimal fee;

    /**
     * 源地址类型
     */
    private String fromAddressType;

    /**
     * 源地址
     * 源类型 1、结算账号 2、链地址
     */
    private String fromAddress;

    /**
     * 目标地址类型
     */
    private String toAddressType;

    /**
     * 目标地址
     * 地址类型 1、结算账号 2、链地址
     */
    private String toAddress;

    /**
     * 备注
     */
    private String comment;

    /**
     * 审核状态;1、待审核 2、审核成功 3、审核拒绝
     */
    private String auditStatus;

    /**
     * 交易完成时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String completedTime;

    /**
     * 商户业务唯一标识
     */
    private String outSerialNo;

    /**
     * 用户角色类型，1.系统运营 2.平台租户 3.商户 4.代理商 5.客户 6.门店 99.无
     * @see BizRoleType
     */
    private String bizRoleType;

    /**
     * 业务角色类型编号
     */
    private String bizRoleTypeNo;

    /**
     * 租户
     */
    private String tenantId;

    /**
     * 分账标识
     */
    private boolean profitSharing;

    /**
     * 分账状态; 1、待分账 2、成功 3、失败 99 无
     */
    private boolean profitSharingStatus;

}
