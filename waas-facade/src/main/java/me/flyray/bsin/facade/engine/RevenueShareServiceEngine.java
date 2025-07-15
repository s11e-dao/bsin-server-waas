package me.flyray.bsin.facade.engine;

import me.flyray.bsin.domain.entity.Transaction;

import java.util.Map;

/**
 * ┌───────────────────────────────┐
 * │         调用方（应用/服务）  │
 * └───────────────┬───────────────┘
 *                 │ HTTP/gRPC
 *                 ▼
 * ┌───────────────────────────────┐
 * │      核心分账协调器           │
 * │  • 验证交易数据                │
 * │  • 载入分账规则                │
 * │  • 调用分账策略 / 计算引擎     │
 * │  • 结果落库 & 事务控制         │
 * └───────────────┬───────────────┘
 *                 │
 *             ┌───┴──────────┐
 *             │   分账策略模块 │  ←—— 可插拔策略（Strategy Pattern）
 *             └───┬──────────┘
 *                 │
 *     ┌───────────┴───────────┐
 *     │ 分账规则提供者（DB/配置） │
 *     └───────────────────────┘
 *
 *     基于商户让利配分润进行价值分配
 */
public interface RevenueShareServiceEngine {

    /**
     * 获取让利规则配置
     * 根据规则进行计算分账（调用第三方）
     * 调用crm进行入账操作
     * 调用生态价值引擎进行生态贡献计算和价值分配
     */
    void excute(Transaction transaction) throws Exception;

}
