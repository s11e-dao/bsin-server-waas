package me.flyray.bsin.server.impl;


import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.blockchain.tds.TrustedDataSpaceConnector;
import me.flyray.bsin.facade.engine.DidProfileServiceEngine;
import org.apache.shenyu.client.apache.dubbo.annotation.ShenyuDubboService;
import org.apache.shenyu.client.apidocs.annotations.ApiDoc;
import org.apache.shenyu.client.apidocs.annotations.ApiModule;
import org.apache.shenyu.client.dubbo.common.annotation.ShenyuDubboClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@ShenyuDubboService(path = "/didProfile", timeout = 6000)
@ApiModule(value = "didProfile")
public class DidProfileServiceEngineImpl implements DidProfileServiceEngine {

    @Value("${bsin.oss.ipfs.gateway}")
    private String tdsUrl;

    /**
     * 调用可信数据空间创建可信身份
     * @param requestMap
     */
    @ShenyuDubboClient("/create")
    @ApiDoc(desc = "create")
    @Override
    public void create(Map<String, Object> requestMap) {

        TrustedDataSpaceConnector trustedDataSpaceClient = TrustedDataSpaceConnector.builder()
                .endpointUrl(tdsUrl)
                .apiKey("your-api-key-here")
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .maxRetries(5)
                .addCustomHeader("X-Tenant-ID", "tenant-123")
                .addCustomHeader("X-Client-Version", "1.0.0")
                .build();

        trustedDataSpaceClient.createDidProfile(requestMap);


    }


    @Override
    public void getDetail(Map<String, Object> requestMap) {

    }

}
