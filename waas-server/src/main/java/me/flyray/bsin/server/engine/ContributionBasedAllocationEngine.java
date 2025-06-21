package me.flyray.bsin.server.engine;

import me.flyray.bsin.facade.engine.EcologicalValueAllocationEngine;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ContributionBasedAllocationEngine implements EcologicalValueAllocationEngine {

    /**
     * 1、排队免单
     * 2、商品分佣
     * 3、分销者分佣
     * @param requestMap
     * @throws Exception
     */
    @Override
    public void excute(Map<String, Object> requestMap) throws Exception {

    }

}
