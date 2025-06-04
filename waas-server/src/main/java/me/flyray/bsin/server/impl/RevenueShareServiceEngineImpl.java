package me.flyray.bsin.server.impl;

import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.facade.engine.EcologicalValueAllocationEngine;
import me.flyray.bsin.facade.engine.RevenueShareServiceEngine;
import org.apache.shenyu.client.apache.dubbo.annotation.ShenyuDubboService;
import org.apache.shenyu.client.apidocs.annotations.ApiModule;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

@Slf4j
@ShenyuDubboService(path = "/revenueShare", timeout = 6000)
@ApiModule(value = "revenueShare")
public class RevenueShareServiceEngineImpl implements RevenueShareServiceEngine {


    @Autowired
    private EcologicalValueAllocationEngine ecologicalValueAllocationEngine;

    /**
     * 执行分账分润
     * @param requestMap
     */
    @Override
    public void excute(Map<String, Object> requestMap) throws Exception {

        // 根据分账配置进行分账，逻辑实现



        // 生态贡献计算和价值分配
        ecologicalValueAllocationEngine.excute(requestMap);

    }

}
