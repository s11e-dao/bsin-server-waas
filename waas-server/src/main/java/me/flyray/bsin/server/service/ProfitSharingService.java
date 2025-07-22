package me.flyray.bsin.server.service;

import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.domain.entity.Transaction;
import me.flyray.bsin.facade.service.ProfitSharingStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 分账服务
 * 作为分账功能的统一入口，根据支付渠道类型选择对应的分账策略
 */
@Slf4j
@Service
public class ProfitSharingService {

    @Autowired
    private ProfitSharingStrategyFactory strategyFactory;

    /**
     * 执行分账
     * @param transaction 交易信息
     * @param payChannelType 支付渠道类型
     * @return 分账结果
     */
    public ProfitSharingStrategy.ProfitSharingResult executeProfitSharing(Transaction transaction, String payChannelType) {
        log.info("开始执行分账，交易号：{}，支付渠道：{}", transaction.getSerialNo(), payChannelType);
        
        try {
            ProfitSharingStrategy strategy = strategyFactory.getStrategy(payChannelType);
            return strategy.executeProfitSharing(transaction);
        } catch (Exception e) {
            log.error("分账执行失败，交易号：{}，支付渠道：{}", transaction.getSerialNo(), payChannelType, e);
            return new ProfitSharingStrategy.ProfitSharingResult(false, "分账失败：" + e.getMessage());
        }
    }

    /**
     * 添加分账接收方
     * @param receiverRequest 接收方请求
     * @param payChannelType 支付渠道类型
     * @return 添加结果
     */
    public boolean addProfitSharingReceiver(ProfitSharingStrategy.ProfitSharingReceiverRequest receiverRequest, String payChannelType) {
        log.info("添加分账接收方，接收方ID：{}，支付渠道：{}", receiverRequest.getReceiverId(), payChannelType);
        
        try {
            ProfitSharingStrategy strategy = strategyFactory.getStrategy(payChannelType);
            return strategy.addProfitSharingReceiver(receiverRequest);
        } catch (Exception e) {
            log.error("添加分账接收方失败，接收方ID：{}，支付渠道：{}", receiverRequest.getReceiverId(), payChannelType, e);
            return false;
        }
    }

    /**
     * 查询分账结果
     * @param transactionNo 交易单号
     * @param payChannelType 支付渠道类型
     * @return 分账结果
     */
    public ProfitSharingStrategy.ProfitSharingResult queryProfitSharingResult(String transactionNo, String payChannelType) {
        log.info("查询分账结果，交易单号：{}，支付渠道：{}", transactionNo, payChannelType);
        
        try {
            ProfitSharingStrategy strategy = strategyFactory.getStrategy(payChannelType);
            return strategy.queryProfitSharingResult(transactionNo);
        } catch (Exception e) {
            log.error("查询分账结果失败，交易单号：{}，支付渠道：{}", transactionNo, payChannelType, e);
            return new ProfitSharingStrategy.ProfitSharingResult(false, "查询失败：" + e.getMessage());
        }
    }

    /**
     * 分账回退
     * @param returnRequest 回退请求
     * @param payChannelType 支付渠道类型
     * @return 回退结果
     */
    public boolean returnProfitSharing(ProfitSharingStrategy.ProfitSharingReturnRequest returnRequest, String payChannelType) {
        log.info("分账回退，订单ID：{}，支付渠道：{}", returnRequest.getOrderId(), payChannelType);
        
        try {
            ProfitSharingStrategy strategy = strategyFactory.getStrategy(payChannelType);
            return strategy.returnProfitSharing(returnRequest);
        } catch (Exception e) {
            log.error("分账回退失败，订单ID：{}，支付渠道：{}", returnRequest.getOrderId(), payChannelType, e);
            return false;
        }
    }

    /**
     * 解冻剩余资金
     * @param unfreezeRequest 解冻请求
     * @param payChannelType 支付渠道类型
     * @return 解冻结果
     */
    public boolean unfreezeRemainingFunds(ProfitSharingStrategy.ProfitSharingUnfreezeRequest unfreezeRequest, String payChannelType) {
        log.info("解冻剩余资金，交易ID：{}，支付渠道：{}", unfreezeRequest.getTransactionId(), payChannelType);
        
        try {
            ProfitSharingStrategy strategy = strategyFactory.getStrategy(payChannelType);
            return strategy.unfreezeRemainingFunds(unfreezeRequest);
        } catch (Exception e) {
            log.error("解冻剩余资金失败，交易ID：{}，支付渠道：{}", unfreezeRequest.getTransactionId(), payChannelType, e);
            return false;
        }
    }

    /**
     * 获取支持的支付渠道类型
     * @return 支付渠道类型集合
     */
    public java.util.Set<String> getSupportedPayChannels() {
        return strategyFactory.getSupportedPayChannels();
    }
} 