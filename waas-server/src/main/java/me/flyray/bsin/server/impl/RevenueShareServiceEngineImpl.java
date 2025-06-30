package me.flyray.bsin.server.impl;

import com.github.binarywang.wxpay.bean.profitsharing.request.ProfitSharingReceiverRequest;
import com.github.binarywang.wxpay.bean.profitsharing.request.ProfitSharingRequest;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.constant.WxPayConstants;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.ProfitSharingService;
import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.domain.entity.Platform;
import me.flyray.bsin.domain.entity.ProfitSharingConfig;
import me.flyray.bsin.domain.entity.Transaction;
import me.flyray.bsin.domain.enums.EcologicalValueAllocationType;
import me.flyray.bsin.facade.engine.RevenueShareServiceEngine;
import me.flyray.bsin.facade.service.MerchantPayService;
import me.flyray.bsin.facade.service.PlatformService;
import me.flyray.bsin.infrastructure.mapper.TransactionMapper;
import me.flyray.bsin.payment.BsinWxPayServiceUtil;
import me.flyray.bsin.server.engine.EcologicalValueAllocationEngineFactory;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.shenyu.client.apache.dubbo.annotation.ShenyuDubboService;
import org.apache.shenyu.client.apidocs.annotations.ApiModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 收益分享服务引擎实现类
 * 负责处理交易的分账分润和生态价值分配
 */
@Slf4j
@ShenyuDubboService(path = "/revenueShare", timeout = 6000)
@ApiModule(value = "revenueShare")
public class RevenueShareServiceEngineImpl implements RevenueShareServiceEngine {

    // 常量定义
    private static final String ORDER_TYPE = "1";
    private static final String PRODUCT_TYPE = "2";
    
    // 依赖注入
    @Autowired
    private MerchantPayService merchantPayService;
    @Autowired
    private BsinWxPayServiceUtil bsinWxPayServiceUtil;
    @Autowired
    private EcologicalValueAllocationEngineFactory ecologicalValueEngineFactory;
    @Autowired
    private TransactionMapper transactionMapper;
    
    @DubboReference(version = "dev")
    private PlatformService platformService;

    /**
     * 执行分账分润操作
     * 主要包含以下步骤：
     * 1. 验证交易信息并获取分账配置
     * 2. 执行支付通道分账
     * 3. 执行生态价值分配
     *
     * @param transaction 交易信息
     * @throws IllegalArgumentException 当必要参数缺失时抛出
     * @throws Exception 其他业务异常
     */
    @Override
    public void excute(Transaction transaction) throws Exception {
        validateTransaction(transaction);
        
        final String serialNo = transaction.getSerialNo();
        log.info("开始执行分账分润流程，交易号：{}", serialNo);
        
        try {
            // 获取分账配置并执行分账
            Optional<ProfitSharingConfig> configOpt = getProfitSharingConfig(transaction);
            configOpt.ifPresent(config -> {
                try {
                    executeProfitSharing(transaction, config);
                } catch (WxPayException e) {
                    log.error("支付分账执行失败，交易号：{}", serialNo, e);
                    throw new RuntimeException("支付分账执行失败", e);
                }
            });
            
            // 执行生态价值分配
            executeEcologicalValueAllocation(transaction, configOpt.orElse(null));
            
            log.info("分账分润流程执行完成，交易号：{}", serialNo);
        } catch (Exception e) {
            log.error("分账分润流程执行失败，交易号：{}", serialNo, e);
            throw new RuntimeException("分账分润执行失败：" + e.getMessage(), e);
        }
    }

    /**
     * 验证交易信息
     */
    private void validateTransaction(Transaction transaction) {
        Assert.notNull(transaction, "交易信息不能为空");
        Assert.hasText(transaction.getSerialNo(), "交易流水号不能为空");
        Assert.hasText(transaction.getTenantId(), "租户ID不能为空");
    }

    /**
     * 获取分账配置
     */
    private Optional<ProfitSharingConfig> getProfitSharingConfig(Transaction transaction) {
        Map<String, Object> requestMap = buildConfigRequestMap(transaction);
        ProfitSharingConfig config = merchantPayService.getProfitSharingConfig(requestMap);
        
        if (config == null) {
            log.info("未找到分账配置，跳过支付分账，交易号：{}", transaction.getSerialNo());
        }
        
        return Optional.ofNullable(config);
    }

    /**
     * 构建分账配置请求参数
     */
    private Map<String, Object> buildConfigRequestMap(Transaction transaction) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("tenantId", transaction.getTenantId());
        requestMap.put("type", transaction.getProfitSharingType());
        return requestMap;
    }

    /**
     * 执行支付分账
     * 包括计算分账金额、配置支付服务、执行分账请求、更新交易状态
     */
    private void executeProfitSharing(Transaction transaction, ProfitSharingConfig config) throws WxPayException {
        final String serialNo = transaction.getSerialNo();
        log.info("开始执行支付分账，交易号：{}", serialNo);

        // 计算分账金额
        BigDecimal profitSharingAmount = calculateProfitSharingAmount(transaction, config);
        log.info("计算得出分账金额：{}，交易号：{}", profitSharingAmount, serialNo);

        // 执行分账流程
        ProfitSharingService profitSharingService = createProfitSharingService();
        addProfitSharingReceiver(profitSharingService);
        executeProfitSharingRequest(profitSharingService, transaction, config);
        updateTransactionStatus(transaction);

        log.info("支付分账执行完成，交易号：{}", serialNo);
    }

    /**
     * 计算分账金额
     * 订单类型：使用预设分账金额
     * 商品类型：交易金额 × 商户分润比例
     */
    private BigDecimal calculateProfitSharingAmount(Transaction transaction, ProfitSharingConfig config) {
        if (ORDER_TYPE.equals(transaction.getProfitSharingType())) {
            return transaction.getProfitSharingAmount();
        } else {
            return transaction.getTxAmount().multiply(config.getMerchantSharingRate());
        }
    }

    /**
     * 创建分账服务
     */
    private ProfitSharingService createProfitSharingService() throws WxPayException {
        WxPayConfig wxPayConfig = new WxPayConfig();
        wxPayConfig.setSignType(WxPayConstants.SignType.MD5);
        wxPayConfig.setUseSandboxEnv(false);
        return bsinWxPayServiceUtil.getProfitSharingService(wxPayConfig);
    }

    /**
     * 添加分账接收方
     */
    private void addProfitSharingReceiver(ProfitSharingService profitSharingService) throws WxPayException {
        ProfitSharingReceiverRequest receiverRequest = new ProfitSharingReceiverRequest();
        receiverRequest.setReceiver("");
        profitSharingService.addReceiver(receiverRequest);
    }

    /**
     * 执行分账请求
     */
    private void executeProfitSharingRequest(ProfitSharingService profitSharingService, 
                                           Transaction transaction, 
                                           ProfitSharingConfig config) throws WxPayException {
        ProfitSharingRequest profitSharingRequest = buildProfitSharingRequest(transaction, config);
        profitSharingService.multiProfitSharing(profitSharingRequest);
    }

    /**
     * 更新交易状态
     */
    private void updateTransactionStatus(Transaction transaction) {
        transaction.setProfitSharingStatus(true);
        transactionMapper.updateById(transaction);
    }

    /**
     * 构建分账请求对象
     */
    private ProfitSharingRequest buildProfitSharingRequest(Transaction transaction, ProfitSharingConfig config) {
        ProfitSharingRequest request = new ProfitSharingRequest();
        // TODO: 根据实际业务需求设置分账请求参数
        return request;
    }

    /**
     * 执行生态价值分配
     */
    private void executeEcologicalValueAllocation(Transaction transaction, ProfitSharingConfig profitSharingConfig) throws Exception {
        final String serialNo = transaction.getSerialNo();
        log.info("开始执行生态价值分配，交易号：{}", serialNo);
        
        Map<String, Object> requestMap = buildValueAllocationRequestMap(transaction, profitSharingConfig);
        log.info("请求平台配置参数: {}，交易号：{}", requestMap, serialNo);
        
        Optional<Platform> platformOpt = Optional.ofNullable(
            platformService.getEcologicalValueAllocationModel(requestMap));
            
        if (!platformOpt.isPresent()) {
            log.warn("未找到平台配置，跳过生态价值分配，交易号：{}", serialNo);
            return;
        }

        Platform platform = platformOpt.get();
        EcologicalValueAllocationType allocationType = EcologicalValueAllocationType
            .getInstanceById(platform.getEcoValueAllocationModel());
            
        // 执行生态价值计算分配
        ecologicalValueEngineFactory.getEngine(allocationType).excute(requestMap);

        log.info("生态价值分配执行完成，交易号：{}", serialNo);
    }

    /**
     * 构建生态价值分配请求参数
     */
    private Map<String, Object> buildValueAllocationRequestMap(Transaction transaction, ProfitSharingConfig profitSharingConfig) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("transaction", transaction);
        requestMap.put("profitSharingConfig", profitSharingConfig);
        requestMap.put("tenantId", transaction.getTenantId());
        return requestMap;
    }
}
