package me.flyray.bsin.server.impl;

import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.domain.entity.Platform;
import me.flyray.bsin.domain.entity.ProfitSharingConfig;
import me.flyray.bsin.domain.entity.Transaction;
import me.flyray.bsin.domain.enums.EcologicalValueAllocationModel;
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
    
    // 依赖注入
    @Autowired
    private MerchantPayService merchantPayService;
    @Autowired
    private BsinWxPayServiceUtil bsinWxPayServiceUtil;
    @Autowired
    private EcologicalValueAllocationEngineFactory ecologicalValueEngineFactory;
    @Autowired
    private TransactionMapper transactionMapper;

    @DubboReference(version = "${dubbo.provider.version}")
    private PlatformService platformService;

    /**
     * 执行生态价值分配操作
     * 主要包含以下步骤：
     * 1. 验证交易信息
     * 2、判断是直接分配固定汇率积分还是数字积分
     * 3. 执行生态价值分配阶段（独立事务 + 异步处理）
     * @param transaction 交易信息
     * @throws IllegalArgumentException 当必要参数缺失时抛出
     * @throws Exception 其他业务异常
     */
    @Override
    public void excute(Transaction transaction) throws Exception {
        validateTransaction(transaction);
        
        final String serialNo = transaction.getSerialNo();
        log.info("开始执行生态价值分配流程，交易号：{}", serialNo);

        // 判断价值分配方式
        EcologicalValueAllocationModel allocationModel = determineValueAllocationType(transaction);
        log.info("确定价值分配方式：{}，交易号：{}", allocationModel, serialNo);
        
        // 生态价值分配阶段（独立事务 + 异步处理）
        scheduleEcologicalValueAllocation(transaction, allocationModel);
        
        log.info("生态价值分配流程执行完成，交易号：{}", serialNo);
    }

    /**
     * 判断价值分配方式
     * @param transaction 交易信息
     * @return 价值分配类型
     */
    private EcologicalValueAllocationModel determineValueAllocationType(Transaction transaction) {
        final String serialNo = transaction.getSerialNo();
        
        try {
            // 1. 首先从平台配置中获取默认的价值分配模型
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("tenantId", transaction.getTenantId());
            
            Optional<Platform> platformOpt = Optional.ofNullable(
                platformService.getEcologicalValueAllocationModel(requestMap));
            
            if (platformOpt.isPresent()) {
                Platform platform = platformOpt.get();
                EcologicalValueAllocationModel allocationType = EcologicalValueAllocationModel
                    .getInstanceById(platform.getEcoValueAllocationModel());
                
                if (allocationType != null) {
                    log.info("从平台配置获取价值分配方式：{}，交易号：{}", allocationType, serialNo);
                    return allocationType;
                }
            }
            
            // 2. 如果平台未配置，则根据交易金额等比例入账
            return EcologicalValueAllocationModel.PROPORTIONAL_DISTRIBUTION;

        } catch (Exception e) {
            log.error("判断价值分配方式失败，使用默认方式，交易号：{}, 错误：{}", serialNo, e.getMessage());
            return EcologicalValueAllocationModel.PROPORTIONAL_DISTRIBUTION; // 默认使用等比例价值分配模型
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
     * 异步执行生态价值分配
     */
//    @Async("ecologicalValueExecutor")
    private void scheduleEcologicalValueAllocation(Transaction transaction, EcologicalValueAllocationModel allocationModel) throws Exception {
        final String serialNo = transaction.getSerialNo();
        log.info("开始执行生态价值分配，交易号：{}", serialNo);
        
        Map<String, Object> requestMap = buildValueAllocationRequestMap(transaction);
        log.info("请求平台配置参数: {}，交易号：{}", requestMap, serialNo);
        
        log.info("开始执行生态价值分配，分配模型：{}，交易号：{}", allocationModel, serialNo);
        ecologicalValueEngineFactory.getEngine(allocationModel).excute(requestMap);

        log.info("生态价值分配执行完成，交易号：{}", serialNo);
    }

    /**
     * 构建生态价值分配请求参数
     */
    private Map<String, Object> buildValueAllocationRequestMap(Transaction transaction) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("transaction", transaction);
        requestMap.put("tenantId", transaction.getTenantId());
        return requestMap;
    }
}
