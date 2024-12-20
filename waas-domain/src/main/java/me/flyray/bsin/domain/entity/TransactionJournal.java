package me.flyray.bsin.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Data;

/**
 * 
 * @TableName waas_transaction_journal
 */
@TableName(value ="waas_transaction_journal")
@Data
public class TransactionJournal implements Serializable {
    /**
     * transaction编号
     */
    @TableId
    private String serialNo;

    /**
     * 支付金额
     */
    private BigDecimal payAmount;

    /**
     *  
     */
    private String payWayNo;

    /**
     *
     */
    private String payFee;

    /**
     * 交易单号
     */
    private String transactionNo;

    /**
     * 交易状态
     */
    private String status;


}