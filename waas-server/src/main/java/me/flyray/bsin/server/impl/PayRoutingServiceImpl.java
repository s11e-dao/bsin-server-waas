package me.flyray.bsin.server.impl;

import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.facade.service.PayRoutingService;
import org.apache.shenyu.client.apache.dubbo.annotation.ShenyuDubboService;
import org.apache.shenyu.client.apidocs.annotations.ApiDoc;
import org.apache.shenyu.client.apidocs.annotations.ApiModule;
import org.apache.shenyu.client.dubbo.common.annotation.ShenyuDubboClient;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 第三方支付路由处理
 */

@Slf4j
@ShenyuDubboService(path = "/payRouting", timeout = 6000)
@ApiModule(value = "payRouting")
@Service
public class PayRoutingServiceImpl implements PayRoutingService {


    @ApiDoc(desc = "pay")
    @ShenyuDubboClient("/pay")
    @Override
    public Map<String, Object> pay(Map<String, Object> requestMap) {
        return null;
    }
}
