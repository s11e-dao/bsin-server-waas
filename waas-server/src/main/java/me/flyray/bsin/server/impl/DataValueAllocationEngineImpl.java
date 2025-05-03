package me.flyray.bsin.server.impl;

import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.facade.engine.DataValueAllocationEngine;
import org.apache.shenyu.client.apache.dubbo.annotation.ShenyuDubboService;
import org.apache.shenyu.client.apidocs.annotations.ApiModule;

import java.util.Map;

@Slf4j
@ShenyuDubboService(path = "/dataValueAllocation", timeout = 6000)
@ApiModule(value = "dataValueAllocation")
public class DataValueAllocationEngineImpl implements DataValueAllocationEngine {


    /**
     * 参数：数据分类和数据级别
     * 根据数据分类和级别进行激励发放
     * 调用数字积分mint接口获取应该产出多少数字积分
     * @param requestMap
     */
    @Override
    public void excute(Map<String, Object> requestMap) {

    }

}
