package me.flyray.bsin.facade.service;

import me.flyray.bsin.domain.entity.PayChannelInterface;

import java.util.Map;

/**
 * 处理微信、支付宝、火源钱包支付路由逻辑
 */

public interface PayRoutingService {

    /**
     * 根据支付方式进行第三调用
     * @param requestMap
     * @return
     */
    public Map<String, Object> pay(Map<String, Object> requestMap);

}
