package me.flyray.bsin.server.engine;


import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.domain.entity.ProfitSharingConfig;
import me.flyray.bsin.domain.enums.ValueAllocationType;
import me.flyray.bsin.domain.response.DistributionRoleAndRateDTO;
import me.flyray.bsin.facade.engine.DigitalPointsServiceEngine;
import me.flyray.bsin.facade.engine.EcologicalValueAllocationEngine;
import me.flyray.bsin.facade.service.BondingCurveTokenService;
import me.flyray.bsin.facade.service.DisInviteRelationService;
import org.apache.commons.collections4.MapUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DefaultEcologicalValueAllocationEngine implements EcologicalValueAllocationEngine {

    @Autowired
    private BondingCurveTokenService bondingCurveTokenService;
    @Autowired
    private DigitalPointsServiceEngine digitalPointsServiceEngine;
    @DubboReference(version = "dev")
    private DisInviteRelationService disInviteRelationService;

    /**
     * 根据价值分配类型做贡献价值分配
     * 1、进行不同参与者角色分配：运营平台、租户平台，推广者角色（分销模型参与人员）、客户：根据平台配置
     * 2、进行推广者角色（基于分销模型的参与人员分配）进行分配
     * 3、劳动贡献价值转换成曲线价值，曲线价值转换成数字积分价值入账
     * @param requestMap
     */
    @Override
    public void excute(Map<String, Object> requestMap) throws Exception {
        log.info("开始执行交易价值分配，参数：{}", requestMap);

        String allocationType = MapUtils.getString(requestMap, "allocationType");
        if(ValueAllocationType.TRANSACTION_TYPE.getCode().equals(allocationType)){
            // 1. 查询平台配置利益分配规则
            ProfitSharingConfig profitSharingConfig = (ProfitSharingConfig) MapUtils.getObject(requestMap, "profitSharingConfig");
            if (profitSharingConfig == null) {
                log.warn("未找到分账配置，参数：{}", requestMap);
                return;
            }

            // 2. 根据不同参与者角色进行利益分配进行分账
            executeParticipantRoleAllocation(requestMap, profitSharingConfig);

            // 3. 根据分销模型对应角色进行分销者利益分配
            executeDistributorAllocation(requestMap, profitSharingConfig);

            // 4. 价值转换：劳动贡献价值转换成曲线价值，曲线价值转换成数字积分价值入账
            executeValueConversion(requestMap);
        }else {

        }


        log.info("交易价值分配执行完成，参数：{}", requestMap);
    }

    /**
     * 执行参与者角色分配
     */
    private void executeParticipantRoleAllocation(Map<String, Object> requestMap, ProfitSharingConfig profitSharingConfig) {
        log.info("开始执行参与者角色分配，分账配置：{}", profitSharingConfig);
        // TODO 根据不同参与者角色进行利益分配进行分账
        log.info("参与者角色分配执行完成");
    }

    /**
     * 执行分销者分配
     */
    private void executeDistributorAllocation(Map<String, Object> requestMap, ProfitSharingConfig profitSharingConfig) {
        log.info("开始执行分销者分配，分销者比例：{}", profitSharingConfig.getDistributorRate());

        // 根据分销者总激励和各角色占比进行利益配置
        // 根据订单涉及的分销模型关系向CRM获取利益分配角色及对应角色的分润比例
        List<DistributionRoleAndRateDTO> distributionRoleAndRateList = disInviteRelationService.getDistributionRoleAndRateList(requestMap);
        if (distributionRoleAndRateList == null || distributionRoleAndRateList.isEmpty()) {
            log.warn("未获取到分销角色及分润比例，参数：{}", requestMap);
            return;
        }

        // TODO 根据分销者总激励和各角色占比进行利益配置
        log.info("分销者分配执行完成，分销角色数量：{}", distributionRoleAndRateList.size());
    }

    /**
     * 执行价值转换： 劳动贡献价值转换成曲线价值，曲线价值转换成数字积分价值入账
     */
    private void executeValueConversion(Map<String, Object> requestMap) throws Exception {
        log.info("开始执行价值转换");
        // TODO 价值转换
        bondingCurveTokenService.calculateCurveValue(requestMap);

        digitalPointsServiceEngine.calculateValueOfLabor(requestMap);

        log.info("价值转换执行完成");
    }


}
