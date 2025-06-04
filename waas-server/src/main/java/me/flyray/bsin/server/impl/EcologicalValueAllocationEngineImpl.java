package me.flyray.bsin.server.impl;


import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.facade.engine.DataValueAllocationEngine;
import me.flyray.bsin.facade.engine.EcologicalValueAllocationEngine;
import me.flyray.bsin.facade.engine.TransactionValueAllocationEngine;
import org.apache.shenyu.client.apache.dubbo.annotation.ShenyuDubboService;
import org.apache.shenyu.client.apidocs.annotations.ApiModule;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

@Slf4j
@ShenyuDubboService(path = "/ecologicalValueAllocation", timeout = 6000)
@ApiModule(value = "ecologicalValueAllocation")
public class EcologicalValueAllocationEngineImpl implements EcologicalValueAllocationEngine {

    @Autowired
    private TransactionValueAllocationEngine transactionValueAllocationEngine;
    @Autowired
    private DataValueAllocationEngine dataValueAllocationEngine;

    @Override
    public void excute(Map<String, Object> requestMap) throws Exception {

        // 判断根据不同类型进行不同价值计算和价值分配
        transactionValueAllocationEngine.excute(requestMap);

        dataValueAllocationEngine.excute(requestMap);

    }

}
