package me.flyray.bsin.server.impl;

import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.facade.engine.DigitalPointsServiceEngine;
import me.flyray.bsin.facade.engine.TransactionValueAllocationEngine;
import me.flyray.bsin.facade.service.BondingCurveTokenService;
import org.apache.shenyu.client.apache.dubbo.annotation.ShenyuDubboService;
import org.apache.shenyu.client.apidocs.annotations.ApiModule;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

@Slf4j
@ShenyuDubboService(path = "/transactionValueAllocation", timeout = 6000)
@ApiModule(value = "transactionValueAllocation")
public class TransactionValueAllocationEngineImpl implements TransactionValueAllocationEngine {

    @Autowired
    private BondingCurveTokenService bondingCurveTokenService;
    @Autowired
    private DigitalPointsServiceEngine digitalPointsServiceEngine;

    /**
     * 获取交易数据进行分账操作
     * @param requestMap
     */
    @Override
    public void excute(Map<String, Object> requestMap) {

    }

}
