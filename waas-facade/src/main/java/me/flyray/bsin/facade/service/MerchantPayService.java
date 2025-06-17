package me.flyray.bsin.facade.service;

import me.flyray.bsin.domain.entity.ProfitSharingConfig;

import java.util.Map;

public interface MerchantPayService {

    /**

     * ┌───────────────────────┐
     * │ 商户支付入网申请          │
     * └────────┬──────────────┘
     *          │
     *          ▼
     * ┌────────────────────────────────────────────┐
     * │ 通过 WaaS 支付模块调用第三方进件入网            │
     * └────────┬───────────────────────────────────┘
     *          │
     *          ▼
     * ┌──────────────────────────────┐
     * │ 自动配置商户支付通道开通支付        │
     * └──────────────────────────────┘
     * @param requestMap
     */
    public void payApply(Map<String, Object> requestMap);

    /**
     * 商户分账规则配置
     */
    public void profitSharingConfig(Map<String, Object> requestMap);

    /**
     * 获取商户的分账配置
     * @param requestMap
     */
    public ProfitSharingConfig getProfitSharingConfig(Map<String, Object> requestMap);

}
