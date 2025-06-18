package me.flyray.bsin.server.impl;

import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.domain.entity.ProfitSharingConfig;
import me.flyray.bsin.domain.response.DistributionRoleAndRateDTO;
import me.flyray.bsin.facade.engine.DigitalPointsServiceEngine;
import me.flyray.bsin.facade.engine.TransactionValueAllocationEngine;
import me.flyray.bsin.facade.service.BondingCurveTokenService;
import me.flyray.bsin.facade.service.DisInviteRelationService;
import org.apache.commons.collections4.MapUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.shenyu.client.apache.dubbo.annotation.ShenyuDubboService;
import org.apache.shenyu.client.apidocs.annotations.ApiModule;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@ShenyuDubboService(path = "/transactionValueAllocation", timeout = 6000)
@ApiModule(value = "transactionValueAllocation")
public class TransactionValueAllocationEngineImpl implements TransactionValueAllocationEngine {

    @Autowired
    private BondingCurveTokenService bondingCurveTokenService;
    @Autowired
    private DigitalPointsServiceEngine digitalPointsServiceEngine;
    @DubboReference(version = "dev")
    private DisInviteRelationService disInviteRelationService;

    /**
     * 1、进行不同参与者角色分配：运营平台、租户平台，推广者角色（分销模型参与人员）、客户：根据平台配置
     * 2、进行推广者角色（基于分销模型的参与人员分配）进行分配
     * 2、劳动贡献价值转换成曲线价值，曲线价值转换成数字积分价值入账
     * @param requestMap
     */
    @Override
    public void excute(Map<String, Object> requestMap) {

        // 查询平台配置利益分配规则
        ProfitSharingConfig profitSharingConfig = (ProfitSharingConfig) MapUtils.getObject(requestMap, "profitSharingConfig");
        // TODO 根据不同参与者角色进行利益分配进行分账


        // TODO 根据分销模型对应角色进行分销者利益分配
        BigDecimal distributorRate = profitSharingConfig.getDistributorRate();
        // 根据分销者总激励和各角色占比进行利益配置
        // 根据订单涉及的分销模型关系向CRM获取利益分配角色及对应角色的分润比例
        List<DistributionRoleAndRateDTO> distributionRoleAndRateList = disInviteRelationService.getDistributionRoleAndRateList(requestMap);


    }

}
