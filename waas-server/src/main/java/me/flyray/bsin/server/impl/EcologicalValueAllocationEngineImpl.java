package me.flyray.bsin.server.impl;


import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.facade.engine.EcologicalValueAllocationEngine;
import org.apache.shenyu.client.apache.dubbo.annotation.ShenyuDubboService;
import org.apache.shenyu.client.apidocs.annotations.ApiModule;

import java.util.Map;

@Slf4j
@ShenyuDubboService(path = "/ecologicalValueAllocation", timeout = 6000)
@ApiModule(value = "ecologicalValueAllocation")
public class EcologicalValueAllocationEngineImpl implements EcologicalValueAllocationEngine {

    @Override
    public void excute(Map<String, Object> requestMap) {
        
    }
}
