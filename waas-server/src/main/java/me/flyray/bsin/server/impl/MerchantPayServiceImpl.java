package me.flyray.bsin.server.impl;

import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.domain.entity.ProfitSharingConfig;
import me.flyray.bsin.facade.service.MerchantPayService;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.shenyu.client.apache.dubbo.annotation.ShenyuDubboService;
import org.apache.shenyu.client.apidocs.annotations.ApiModule;

import java.util.Map;

@Slf4j
@DubboService
@ApiModule(value = "merchantPay")
@ShenyuDubboService("/merchantPay")
public class MerchantPayServiceImpl implements MerchantPayService {


    @Override
    public void payApply(Map<String, Object> requestMap) {

    }

    @Override
    public void profitSharingConfig(Map<String, Object> requestMap) {

    }

    @Override
    public ProfitSharingConfig getProfitSharingConfig(Map<String, Object> requestMap) {

        return null;
    }

}
