package me.flyray.bsin.server.engine;

import me.flyray.bsin.facade.engine.EcologicalValueAllocationEngine;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HybridAllocationEngine implements EcologicalValueAllocationEngine {

    /**
     * 排队免单模式
     * @param requestMap
     * @throws Exception
     */
    @Override
    public void excute(Map<String, Object> requestMap) throws Exception {

    }

}
