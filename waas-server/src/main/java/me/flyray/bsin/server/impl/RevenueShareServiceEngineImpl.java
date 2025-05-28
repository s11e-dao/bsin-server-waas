package me.flyray.bsin.server.impl;

import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.facade.engine.RevenueShareServiceEngine;
import org.apache.shenyu.client.apache.dubbo.annotation.ShenyuDubboService;
import org.apache.shenyu.client.apidocs.annotations.ApiModule;

import java.util.Map;

@Slf4j
@ShenyuDubboService(path = "/revenueShare", timeout = 6000)
@ApiModule(value = "revenueShare")
public class RevenueShareServiceEngineImpl implements RevenueShareServiceEngine {

    /**
     * 执行分账分润
     * @param requestMap
     */
    @Override
    public void excute(Map<String, Object> requestMap) {

    }

}
