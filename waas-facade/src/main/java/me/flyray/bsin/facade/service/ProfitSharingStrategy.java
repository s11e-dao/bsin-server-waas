package me.flyray.bsin.facade.service;

import me.flyray.bsin.domain.entity.Transaction;

import java.math.BigDecimal;

/**
 * 分账策略接口
 * 支持不同支付渠道的分账功能
 */
public interface ProfitSharingStrategy {

    /**
     * 执行分账
     * @param transaction 交易信息
     * @return 分账结果
     */
    ProfitSharingResult executeProfitSharing(Transaction transaction);

    /**
     * 添加分账接收方
     * @param receiverRequest 接收方请求
     * @return 添加结果
     */
    boolean addProfitSharingReceiver(ProfitSharingReceiverRequest receiverRequest);

    /**
     * 查询分账结果
     * @param transactionNo 交易单号
     * @return 分账结果
     */
    ProfitSharingResult queryProfitSharingResult(String transactionNo);

    /**
     * 分账回退
     * @param returnRequest 回退请求
     * @return 回退结果
     */
    boolean returnProfitSharing(ProfitSharingReturnRequest returnRequest);

    /**
     * 解冻剩余资金
     * @param unfreezeRequest 解冻请求
     * @return 解冻结果
     */
    boolean unfreezeRemainingFunds(ProfitSharingUnfreezeRequest unfreezeRequest);

    /**
     * 获取支付渠道类型
     * @return 支付渠道类型
     */
    String getPayChannelType();

    /**
     * 分账结果
     */
    class ProfitSharingResult {
        private boolean success;
        private String message;
        private String orderId;
        private String state;
        private BigDecimal amount;

        public ProfitSharingResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public ProfitSharingResult(boolean success, String message, String orderId, String state, BigDecimal amount) {
            this.success = success;
            this.message = message;
            this.orderId = orderId;
            this.state = state;
            this.amount = amount;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }

    /**
     * 分账接收方请求
     */
    class ProfitSharingReceiverRequest {
        private String receiverId;
        private String receiverName;
        private String receiverType;
        private String relationType;
        private String customRelation;
        private String payChannelCode;

        // Getters and Setters
        public String getReceiverId() { return receiverId; }
        public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
        public String getReceiverName() { return receiverName; }
        public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
        public String getReceiverType() { return receiverType; }
        public void setReceiverType(String receiverType) { this.receiverType = receiverType; }
        public String getRelationType() { return relationType; }
        public void setRelationType(String relationType) { this.relationType = relationType; }
        public String getCustomRelation() { return customRelation; }
        public void setCustomRelation(String customRelation) { this.customRelation = customRelation; }
        public String getPayChannelCode() { return payChannelCode; }
        public void setPayChannelCode(String payChannelCode) { this.payChannelCode = payChannelCode; }
    }

    /**
     * 分账回退请求
     */
    class ProfitSharingReturnRequest {
        private String orderId;
        private String outReturnNo;
        private String returnMchid;
        private BigDecimal amount;
        private String description;

        // Getters and Setters
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getOutReturnNo() { return outReturnNo; }
        public void setOutReturnNo(String outReturnNo) { this.outReturnNo = outReturnNo; }
        public String getReturnMchid() { return returnMchid; }
        public void setReturnMchid(String returnMchid) { this.returnMchid = returnMchid; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    /**
     * 解冻请求
     */
    class ProfitSharingUnfreezeRequest {
        private String transactionId;
        private String outOrderNo;
        private String description;

        // Getters and Setters
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        public String getOutOrderNo() { return outOrderNo; }
        public void setOutOrderNo(String outOrderNo) { this.outOrderNo = outOrderNo; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
} 