package me.flyray.bsin.server.engine;

import me.flyray.bsin.domain.enums.EcologicalValueAllocationModel;
import me.flyray.bsin.facade.engine.EcologicalValueAllocationEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EcologicalValueAllocationEngineFactory {

    @Autowired
    private DefaultEcologicalValueAllocationEngine defaultEngine;

    @Autowired
    private ContributionBasedAllocationEngine contributionBasedEngine;

    @Autowired
    private QueueFreeBillAllocationEngine queueFreeBillAllocationEngine;

    public EcologicalValueAllocationEngine getEngine(EcologicalValueAllocationModel type) {
        switch (type) {
            // 等比例价值分配
            case PROPORTIONAL_DISTRIBUTION:
                return contributionBasedEngine;
            case QUEUE_FREE_BILLING:
                return queueFreeBillAllocationEngine;
            // 曲线价值分配
            case CURVE_BASED:
            default:
                return defaultEngine;
        }
    }

}
