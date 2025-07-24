package me.flyray.bsin.facade.service;

import java.util.Map;

/**
 * 支付商户进件
 */

public interface PayMerchantEntryService {

    /**
     * 支付渠道进件申请
     */
    public Map<String, Object> apply(Map<String, Object> requestMap);

    /**
     * 进件状态查询
     */
    public Map<String, Object> getApplyStatus(Map<String, Object> requestMap);

}
