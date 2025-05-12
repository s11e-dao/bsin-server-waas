package me.flyray.bsin.server.impl;


import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.facade.engine.TrustedDataAssetsServiceEngine;
import org.apache.shenyu.client.apache.dubbo.annotation.ShenyuDubboService;
import org.apache.shenyu.client.apidocs.annotations.ApiDoc;
import org.apache.shenyu.client.apidocs.annotations.ApiModule;
import org.apache.shenyu.client.dubbo.common.annotation.ShenyuDubboClient;

import java.util.Map;

@Slf4j
@ShenyuDubboService(path = "/dataAssets", timeout = 6000)
@ApiModule(value = "dataAssets")
public class DataAssetsServiceEngineImpl implements TrustedDataAssetsServiceEngine {

    @ShenyuDubboClient("/register")
    @ApiDoc(desc = "register")
    @Override
    public void register(Map<String, Object> requestMap) {

    }

    /**
     * 详情
     * @param requestMap
     * @return
     */
    @ApiDoc(desc = "getDetail")
    @ShenyuDubboClient("/getDetail")
    @Override
    public void getDetail(Map<String, Object> requestMap){

    }

}
