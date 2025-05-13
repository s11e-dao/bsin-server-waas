package me.flyray.bsin.server.impl;


import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.facade.engine.DidProfileServiceEngine;
import org.apache.shenyu.client.apache.dubbo.annotation.ShenyuDubboService;
import org.apache.shenyu.client.apidocs.annotations.ApiDoc;
import org.apache.shenyu.client.apidocs.annotations.ApiModule;
import org.apache.shenyu.client.dubbo.common.annotation.ShenyuDubboClient;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@ShenyuDubboService(path = "/dataAssets", timeout = 6000)
@ApiModule(value = "dataAssets")
public class DidProfileServiceEngineImpl implements DidProfileServiceEngine {

    @ShenyuDubboClient("/create")
    @ApiDoc(desc = "create")
    @Override
    public void create(Map<String, Object> requestMap) {

    }

    @Override
    public void getDetail(Map<String, Object> requestMap) {

    }


}
