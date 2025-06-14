package me.flyray.bsin.server.engine;

import me.flyray.bsin.domain.enums.EcologicalValueAllocationType;
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
    private HybridAllocationEngine hybridEngine;

    public EcologicalValueAllocationEngine createEngine(EcologicalValueAllocationType type) {
        switch (type) {
            case CONTRIBUTION_BASED:
                return contributionBasedEngine;
            case HYBRID:
                return hybridEngine;
            case EQUAL_DISTRIBUTION:
            default:
                return defaultEngine;
        }
    }

}
