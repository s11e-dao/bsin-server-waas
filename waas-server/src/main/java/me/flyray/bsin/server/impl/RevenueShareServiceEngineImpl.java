package me.flyray.bsin.server.impl;

import com.github.binarywang.wxpay.bean.profitsharing.request.ProfitSharingReceiverRequest;
import com.github.binarywang.wxpay.bean.profitsharing.request.ProfitSharingRequest;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.constant.WxPayConstants;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.ProfitSharingService;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.domain.entity.Platform;
import me.flyray.bsin.domain.entity.ProfitSharingConfig;
import me.flyray.bsin.domain.entity.Transaction;
import me.flyray.bsin.domain.enums.EcologicalValueAllocationType;
import me.flyray.bsin.facade.engine.EcologicalValueAllocationEngine;
import me.flyray.bsin.facade.engine.RevenueShareServiceEngine;
import me.flyray.bsin.facade.service.DisInviteRelationService;
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

import java.util.HashMap;
import java.util.Map;

@Slf4j
@ShenyuDubboService(path = "/revenueShare", timeout = 6000)
@ApiModule(value = "revenueShare")
public class RevenueShareServiceEngineImpl implements RevenueShareServiceEngine {

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
     * 1. 判断交易是否需要进行分账，根据商户让利配置进行分账
     * 2. 调用支付通道分账服务
     * 3. 根据不同生态价值分配模型进行生态价值分配
     *
     * @param transaction 交易信息
     * @throws IllegalArgumentException 当必要参数缺失时抛出
     * @throws Exception 其他业务异常
     */
    @Override
    public void excute(Transaction transaction) throws Exception {
        Assert.notNull(transaction, "交易信息不能为空");
        log.info("开始执行分账分润，交易号：{}", transaction.getSerialNo());
        
        try {
            // 1. 获取并验证分账配置
            Map<String, Object> requestMap =  new HashMap<>();
            requestMap.put("serialNo", transaction.getSerialNo());
            ProfitSharingConfig profitSharingConfig = merchantPayService.getProfitSharingConfig(requestMap);
            if (profitSharingConfig != null) {
                // 2. 根据不同支付通道，执行支付分账
                executeProfitSharing(transaction, profitSharingConfig);
            }
            
            // 3. 执行生态价值计算分配
            executeEcologicalValueAllocation(transaction, profitSharingConfig);
            
            log.info("分账分润执行完成，交易号：{}", transaction.getSerialNo());
        } catch (Exception e) {
            log.error("分账分润执行失败，交易号：{}", transaction.getSerialNo(), e);
            throw e;
        }
    }

    /**
     * 构建请求参数Map
     */
    private Map<String, Object> buildValueAllocationRequestMap(Transaction transaction, ProfitSharingConfig profitSharingConfig) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("transaction", transaction);
        requestMap.put("profitSharingConfig", profitSharingConfig);
        return requestMap;
    }

    /**
     * 执行支付分账
     * 1、添加分账接受方
     * 2、执行分账
     * 3、更新分账状态
     * @param transaction 交易信息
     * @param profitSharingConfig 分账配置
     */
    private void executeProfitSharing(Transaction transaction, ProfitSharingConfig profitSharingConfig) throws WxPayException {
        log.info("开始执行支付分账，交易号：{}", transaction.getSerialNo());

        WxPayConfig wxPayConfig = new WxPayConfig();
        wxPayConfig.setSignType(WxPayConstants.SignType.MD5);
        wxPayConfig.setUseSandboxEnv(false);
        ProfitSharingService profitSharingService = bsinWxPayServiceUtil.getProfitSharingService(wxPayConfig);

        ProfitSharingReceiverRequest receiverRequest = new ProfitSharingReceiverRequest();
        profitSharingService.addReceiver(receiverRequest);

        // TODO 根据支付方式，调用不同支付通道分账
        ProfitSharingRequest profitSharingRequest = buildProfitSharingRequest(transaction, profitSharingConfig);
        profitSharingService.multiProfitSharing(profitSharingRequest);

        //TODO  更新分账状态
        transaction.setProfitSharingStatus(true);
        transactionMapper.updateById(transaction);

        log.info("支付分账执行完成，交易号：{}", transaction.getSerialNo());
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
     * @param transaction 交易信息
     */
    private void executeEcologicalValueAllocation(Transaction transaction, ProfitSharingConfig profitSharingConfig) throws Exception {
        log.info("开始执行生态价值分配，交易号：{}", transaction.getSerialNo());
        
        Map<String, Object> requestMap = buildValueAllocationRequestMap(transaction, profitSharingConfig);
        Platform platform = platformService.getEcologicalValueAllocationModel(requestMap);
        if (platform == null) {
            log.warn("未找到平台配置，交易号：{}", transaction.getSerialNo());
            return;
        }

        EcologicalValueAllocationType allocationType = EcologicalValueAllocationType.getInstanceById(
            platform.getEcoValueAllocationModel());
            
        // 获取生态价值计算引擎，根据不同计算模型进行价值计算分配
        ecologicalValueEngineFactory.getEngine(allocationType)
                .excute(requestMap);

        log.info("生态价值分配执行完成，交易号：{}", transaction.getSerialNo());
    }
}
