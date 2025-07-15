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
    private HybridAllocationEngine hybridEngine;

    public EcologicalValueAllocationEngine getEngine(EcologicalValueAllocationModel type) {
        switch (type) {
            case PROPORTIONAL_DISTRIBUTION:
                return contributionBasedEngine;
            case HYBRID:
                return hybridEngine;
            case CURVE_BASED:
            default:
                return defaultEngine;
        }
    }

}
