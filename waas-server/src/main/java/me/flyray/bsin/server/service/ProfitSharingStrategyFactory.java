package me.flyray.bsin.server.service;

import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.facade.service.ProfitSharingStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * 分账策略工厂
 * 用于管理不同支付渠道的分账策略
 */
@Slf4j
@Service
public class ProfitSharingStrategyFactory {

    private final Map<String, ProfitSharingStrategy> strategyMap = new HashMap<>();

    @Autowired
    private Map<String, ProfitSharingStrategy> profitSharingStrategies;

    @PostConstruct
    public void init() {
        // 自动注册所有策略实现
        for (Map.Entry<String, ProfitSharingStrategy> entry : profitSharingStrategies.entrySet()) {
            String beanName = entry.getKey();
            ProfitSharingStrategy strategy = entry.getValue();
            String payChannelType = strategy.getPayChannelType();
            
            strategyMap.put(payChannelType, strategy);
            log.info("注册分账策略：{} -> {}", payChannelType, beanName);
        }
        
        log.info("分账策略工厂初始化完成，支持渠道：{}", strategyMap.keySet());
    }

    /**
     * 获取分账策略
     * @param payChannelType 支付渠道类型
     * @return 分账策略
     */
    public ProfitSharingStrategy getStrategy(String payChannelType) {
        ProfitSharingStrategy strategy = strategyMap.get(payChannelType);
        if (strategy == null) {
            log.warn("未找到支付渠道 {} 的分账策略", payChannelType);
            throw new IllegalArgumentException("不支持的支付渠道类型: " + payChannelType);
        }
        return strategy;
    }

    /**
     * 注册分账策略
     * @param payChannelType 支付渠道类型
     * @param strategy 分账策略
     */
    public void registerStrategy(String payChannelType, ProfitSharingStrategy strategy) {
        strategyMap.put(payChannelType, strategy);
        log.info("注册分账策略成功，支付渠道：{}", payChannelType);
    }

    /**
     * 获取所有支持的支付渠道类型
     * @return 支付渠道类型集合
     */
    public java.util.Set<String> getSupportedPayChannels() {
        return strategyMap.keySet();
    }
} 