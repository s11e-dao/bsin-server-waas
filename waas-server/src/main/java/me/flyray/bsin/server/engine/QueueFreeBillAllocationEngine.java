package me.flyray.bsin.server.engine;

import me.flyray.bsin.facade.engine.EcologicalValueAllocationEngine;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 排队免单价值分配模型
 */
@Component
public class QueueFreeBillAllocationEngine implements EcologicalValueAllocationEngine {

    /**
     * 排队免单模式
     * @param requestMap
     * @throws Exception
     */
    @Override
    public void excute(Map<String, Object> requestMap) throws Exception {

    }

}
