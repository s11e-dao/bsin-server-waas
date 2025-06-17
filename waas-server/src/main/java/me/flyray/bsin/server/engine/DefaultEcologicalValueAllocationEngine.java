package me.flyray.bsin.server.engine;


import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.domain.entity.ProfitSharingConfig;
import me.flyray.bsin.domain.enums.ValueAllocationType;
import me.flyray.bsin.domain.response.DistributionRoleAndRateDTO;
import me.flyray.bsin.facade.engine.DataValueAllocationEngine;
import me.flyray.bsin.facade.engine.EcologicalValueAllocationEngine;
import me.flyray.bsin.facade.engine.TransactionValueAllocationEngine;
import me.flyray.bsin.facade.service.DisInviteRelationService;
import me.flyray.bsin.facade.service.MerchantPayService;
import me.flyray.bsin.infrastructure.mapper.ProfitSharingConfigMapper;
import org.apache.commons.collections4.MapUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.shenyu.client.apache.dubbo.annotation.ShenyuDubboService;
import org.apache.shenyu.client.apidocs.annotations.ApiModule;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@Slf4j
@ShenyuDubboService(path = "/ecologicalValueAllocation", timeout = 6000)
@ApiModule(value = "ecologicalValueAllocation")
public class DefaultEcologicalValueAllocationEngine implements EcologicalValueAllocationEngine {

    @Autowired
    private TransactionValueAllocationEngine transactionValueAllocationEngine;
    @Autowired
    private DataValueAllocationEngine dataValueAllocationEngine;


    /**
     * 根据价值分配类型做贡献价值分配
     * @param requestMap
     * @throws Exception
     */
    @Override
    public void excute(Map<String, Object> requestMap) throws Exception {

        String allocationType = MapUtils.getString(requestMap, "allocationType");
        // 价值评估：判断根据不同类型进行不同价值计算和价值分配
        if (ValueAllocationType.TRANSACTION_TYPE.getCode().equals(allocationType)) {
            log.info("进入交易价值分配分支，参数：{}", requestMap);
            transactionValueAllocationEngine.excute(requestMap);
        } else {
            log.info("进入数据价值分配分支，allocationType={}，参数：{}", allocationType, requestMap);
            dataValueAllocationEngine.excute(requestMap);
        }

    }

}
