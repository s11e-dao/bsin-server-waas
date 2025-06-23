package me.flyray.bsin.server.engine;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.domain.entity.Account;
import me.flyray.bsin.domain.entity.BondingCurveTokenParam;
import me.flyray.bsin.domain.entity.ProfitSharingConfig;
import me.flyray.bsin.domain.entity.Transaction;
import me.flyray.bsin.domain.enums.ValueAllocationType;
import me.flyray.bsin.domain.request.DistributorValueAllocationDTO;
import me.flyray.bsin.domain.request.ParticipantRoleAllocationDTO;
import me.flyray.bsin.domain.response.DistributionRoleAndRateDTO;
import me.flyray.bsin.facade.engine.DigitalPointsServiceEngine;
import me.flyray.bsin.facade.engine.EcologicalValueAllocationEngine;
import me.flyray.bsin.facade.service.AccountService;
import me.flyray.bsin.facade.service.BondingCurveTokenService;
import me.flyray.bsin.facade.service.DisInviteRelationService;
import me.flyray.bsin.infrastructure.mapper.BondingCurveTokenParamMapper;
import me.flyray.bsin.security.enums.BizRoleType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class DefaultEcologicalValueAllocationEngine implements EcologicalValueAllocationEngine {

    // 常量定义
    private static final String ALLOCATION_TYPE_KEY = "allocationType";
    private static final String TRANSACTION_KEY = "transaction";
    private static final String PROFIT_SHARING_CONFIG_KEY = "profitSharingConfig";
    private static final String TENANT_ID_KEY = "tenantId";
    
    // 角色类型常量

    private static final String ROLE_DISTRIBUTOR = "distributor";
    
    // 账户类型常量
    private static final String ACCOUNT_TYPE_DISCOUNT = "discount";
    private static final String ACCOUNT_TYPE_CURVE = "curve";

    @Autowired
    private BondingCurveTokenService bondingCurveTokenService;
    @Autowired
    private DigitalPointsServiceEngine digitalPointsServiceEngine;
    @DubboReference(version = "dev")
    private DisInviteRelationService disInviteRelationService;
    @Autowired
    private BondingCurveTokenParamMapper bondingCurveTokenParamMapper;
    @DubboReference(version = "dev")
    private AccountService accountService;

    /**
     * 根据价值分配类型做贡献价值分配
     * 1. 查询平台配置利益分配规则
     * 2、进行不同参与者角色贡献收益计算：运营平台、租户平台，推广者角色（分销模型参与人员）、客户：根据平台配置
     * 3、进行推广者角色（基于分销模型的参与人员分配）进行贡献收益计算
     * 4、劳动贡献价值转换成曲线价值，曲线价值转换成数字积分价值入账
     * @param requestMap 请求参数
     */
    @Override
    public void excute(Map<String, Object> requestMap) throws Exception {
        log.info("开始执行交易价值分配，参数：{}", requestMap);

        try {

            String allocationType = MapUtils.getString(requestMap, ALLOCATION_TYPE_KEY);
            Transaction transaction = (Transaction) MapUtils.getObject(requestMap, TRANSACTION_KEY);
            ProfitSharingConfig profitSharingConfig = (ProfitSharingConfig) MapUtils.getObject(requestMap, PROFIT_SHARING_CONFIG_KEY);

            ParticipantRoleAllocationDTO participantRoleAllocation = null;
            List<DistributorValueAllocationDTO> distributorAllocationList = null;
            
            if (ValueAllocationType.TRANSACTION_TYPE.getCode().equals(allocationType)) {
                // 处理交易类型的价值分配
                participantRoleAllocation = handleTransactionTypeAllocation(requestMap, transaction);
                if (participantRoleAllocation != null) {
                    // 基于分销模型的贡献价值计算
                    distributorAllocationList = executeDistributorContributionCalculate(participantRoleAllocation.getDistributorValue());
                }
            } else {
                // TODO 根据数据贡献价值量化配置计算
                log.info("暂不支持的分配类型：{}", allocationType);
            }

            // 4. 价值转换和收益发放：将计算出来的劳动贡献价值按积分兑换比列直接入账和转换成曲线价值，曲线价值转换成数字积分价值入账
            if (participantRoleAllocation != null || CollectionUtils.isNotEmpty(distributorAllocationList)) {
                executeValueConversion(profitSharingConfig, participantRoleAllocation, distributorAllocationList);
            }

            log.info("交易价值分配执行完成，参数：{}", requestMap);
        } catch (Exception e) {
            log.error("价值分配执行失败，参数：{}", requestMap, e);
            throw e;
        }
    }

    /**
     * 处理交易类型的价值分配
     */
    private ParticipantRoleAllocationDTO handleTransactionTypeAllocation(Map<String, Object> requestMap, Transaction transaction) {
        // 1. 查询平台配置利益分配规则
        ProfitSharingConfig profitSharingConfig = (ProfitSharingConfig) MapUtils.getObject(requestMap, PROFIT_SHARING_CONFIG_KEY);
        if (profitSharingConfig == null) {
            log.warn("未找到分账配置，参数：{}", requestMap);
            return null;
        }

        // 2. 根据不同参与者角色配置进行贡献收益计算
        return calculateParticipantRoleContribution(transaction, profitSharingConfig);
    }

    /**
     * 计算参与者角色贡献收益
     * @param transaction 交易信息
     * @param profitSharingConfig 分账配置
     * @return 参与者角色分配结果
     */
    private ParticipantRoleAllocationDTO calculateParticipantRoleContribution(Transaction transaction, ProfitSharingConfig profitSharingConfig) {
        log.info("开始执行参与者角色分配，分账配置：{}", profitSharingConfig);
        
        try {
            // TODO 根据不同参与者角色进行利益分配进行分账
            // 这里应该实现具体的分配逻辑
            ParticipantRoleAllocationDTO participantRoleAllocationDTO = new ParticipantRoleAllocationDTO();
            // 设置各角色的分配值，交易金额乘以分配比例
            
            log.info("参与者角色分配执行完成");
            return participantRoleAllocationDTO;
        } catch (Exception e) {
            log.error("参与者角色分配计算失败，交易：{}，配置：{}", transaction, profitSharingConfig, e);
            return null;
        }
    }

    /**
     * 执行基于分销模型的分销者贡献收益计算
     * @param distributorContributionValue 分销者贡献价值
     * @return 分销者分配列表
     */
    private List<DistributorValueAllocationDTO> executeDistributorContributionCalculate(BigDecimal distributorContributionValue) {
        if (distributorContributionValue == null || distributorContributionValue.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("分销者贡献价值无效：{}", distributorContributionValue);
            return null;
        }
        
        log.info("开始执行分销者分配，分销者金额：{}", distributorContributionValue);

        try {
            // 根据分销者总激励和各角色占比进行利益配置
            // 根据订单涉及的分销模型关系向CRM获取利益分配角色及对应角色的分润比例
            Map<String, Object> requestMap = new HashMap<>();
            // TODO 填充必要的请求参数

            List<DistributionRoleAndRateDTO> distributionRoleAndRateList = disInviteRelationService.getDistributionRoleAndRateList(requestMap);
            if (CollectionUtils.isEmpty(distributionRoleAndRateList)) {
                log.warn("未获取到分销角色及分润比例，参数：{}", distributorContributionValue);
                return null;
            }

            // TODO 根据分销者总激励和各角色占比进行利益配置
            // 实现具体的分配计算逻辑
            
            log.info("分销者分配执行完成，分销角色数量：{}", distributionRoleAndRateList.size());
            return null; // 应该返回实际的分配结果
        } catch (Exception e) {
            log.error("分销者贡献收益计算失败，贡献值：{}", distributorContributionValue, e);
            return null;
        }
    }

    /**
     * 计算直接入账金额和价值转换金额
     * 执行价值转换： 劳动贡献价值转换成曲线价值，曲线价值转换成数字积分价值入账
     */
    private void executeValueConversion(ProfitSharingConfig profitSharingConfig, ParticipantRoleAllocationDTO participantRoleAllocation,
                                      List<DistributorValueAllocationDTO> allocationParticipatorDTOList) throws Exception {
        log.info("开始执行价值转换");

        try {
            // 参与者价值转换并入账
            if (participantRoleAllocation != null) {
                processParticipantValueConversion(profitSharingConfig, participantRoleAllocation);
            }

            // 分销者价值转换并入账
            if (CollectionUtils.isNotEmpty(allocationParticipatorDTOList)) {
                processDistributorValueConversion(profitSharingConfig, allocationParticipatorDTOList);
            }

            // 检查并处理积分释放
            processDigitalPointsRelease();

            log.info("价值转换执行完成");
        } catch (Exception e) {
            log.error("价值转换执行失败", e);
            throw e;
        }
    }

    /**
     * 处理参与者价值转换
     */
    private void processParticipantValueConversion(ProfitSharingConfig profitSharingConfig, ParticipantRoleAllocationDTO participantRoleAllocation) throws Exception {
        // 根据 profitSharingConfig exchangeDigitalPointsRate 计算价值转换金额
        if (profitSharingConfig == null || profitSharingConfig.getExchangeDigitalPointsRate() == null) {
            log.warn("分账配置或兑换比例为空，跳过价值转换");
            return;
        }

        BigDecimal exchangeDigitalPointsRate = profitSharingConfig.getExchangeDigitalPointsRate();
        
        // 超级租户价值转换并入账
        if (participantRoleAllocation.getSuperTenantValue() != null) {
            processRoleValueAllocation(BizRoleType.SYS.getCode(), participantRoleAllocation.getSuperTenantValue(), exchangeDigitalPointsRate);
        }

        // 租户价值转换并入账
        if (participantRoleAllocation.getTenantValue() != null) {
            processRoleValueAllocation(BizRoleType.TENANT.getCode(), participantRoleAllocation.getTenantValue(), exchangeDigitalPointsRate);
        }

        // 系统代理价值转换并入账
        if (participantRoleAllocation.getSysAgentValue() != null) {
            processRoleValueAllocation(BizRoleType.SYS_AGENT.getCode(), participantRoleAllocation.getSysAgentValue(), exchangeDigitalPointsRate);
        }

        // 客户价值转换并入账
        if (participantRoleAllocation.getCustomerValue() != null) {
            processRoleValueAllocation(BizRoleType.CUSTOMER.getCode(), participantRoleAllocation.getCustomerValue(), exchangeDigitalPointsRate);
        }
    }

    /**
     * 处理单个角色的价值分配
     * @param roleType 角色类型
     * @param totalValue 总价值
     * @param exchangeDigitalPointsRate 数字积分兑换比例
     */
    private void processRoleValueAllocation(String roleType, BigDecimal totalValue, BigDecimal exchangeDigitalPointsRate) throws Exception {
        if (totalValue == null || totalValue.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("角色 {} 的价值为空或小于等于0，跳过处理", roleType);
            return;
        }

        // 计算价值转换金额（需要转换为曲线价值的部分）
        BigDecimal convertValue = totalValue.multiply(exchangeDigitalPointsRate);
        
        // 计算直接入账金额（优惠账户部分）
        BigDecimal discountValue = totalValue.multiply(BigDecimal.ONE.subtract(exchangeDigitalPointsRate));

        // 价值转换并入账（曲线价值）
        if (convertValue.compareTo(BigDecimal.ZERO) > 0) {
            processValueConversionAndAccount(roleType, convertValue);
        }

        // 优惠账户部分直接入账
        if (discountValue.compareTo(BigDecimal.ZERO) > 0) {
            processDirectAccount(roleType, discountValue);
        }
    }

    /**
     * 处理直接入账
     * @param roleType 角色类型
     * @param discountValue 优惠价值
     */
    private void processDirectAccount(String roleType, BigDecimal discountValue) throws Exception {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("roleType", roleType);
        requestMap.put("discountPoints", discountValue);
        requestMap.put("accountType", ACCOUNT_TYPE_DISCOUNT);
        // TODO 添加其他必要的参数（tenantId, merchantNo, customerNo等）
        
        try {
            accountService.inAccount(requestMap);
            log.debug("角色 {} 优惠金额 {} 直接入账成功", roleType, discountValue);
        } catch (Exception e) {
            log.error("角色 {} 优惠金额 {} 直接入账失败", roleType, discountValue, e);
            throw e;
        }
    }

    /**
     * 处理分销者价值转换
     */
    private void processDistributorValueConversion(ProfitSharingConfig profitSharingConfig, List<DistributorValueAllocationDTO> allocationParticipatorDTOList) throws Exception {
        if (profitSharingConfig == null || profitSharingConfig.getExchangeDigitalPointsRate() == null) {
            log.warn("分账配置或兑换比例为空，跳过分销者价值转换");
            return;
        }

        BigDecimal exchangeDigitalPointsRate = profitSharingConfig.getExchangeDigitalPointsRate();
        
        for (DistributorValueAllocationDTO valueAllocationParticipatorDTO : allocationParticipatorDTOList) {
            if (valueAllocationParticipatorDTO.getLaborValue() != null && valueAllocationParticipatorDTO.getLaborValue().compareTo(BigDecimal.ZERO) > 0) {
                // 计算价值转换金额
                BigDecimal convertValue = valueAllocationParticipatorDTO.getLaborValue().multiply(exchangeDigitalPointsRate);
                
                // 计算直接入账金额
                BigDecimal discountValue = valueAllocationParticipatorDTO.getLaborValue().multiply(BigDecimal.ONE.subtract(exchangeDigitalPointsRate));
                
                // 价值转换（曲线价值）
                if (convertValue.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal distributorBondingCurveValue = bondingCurveTokenService.calculateCurveValue(convertValue);
                    Map<String, Object> distributorRequestMap = buildAccountRequestMap(ROLE_DISTRIBUTOR, distributorBondingCurveValue, valueAllocationParticipatorDTO);
                    accountService.inAccount(distributorRequestMap);
                }
                
                // 直接入账（优惠部分）
                if (discountValue.compareTo(BigDecimal.ZERO) > 0) {
                    Map<String, Object> discountRequestMap = buildDiscountAccountRequestMap(ROLE_DISTRIBUTOR, discountValue, valueAllocationParticipatorDTO);
                    accountService.inAccount(discountRequestMap);
                }
            }
        }
    }

    /**
     * 构建优惠账户入账请求参数
     */
    private Map<String, Object> buildDiscountAccountRequestMap(String roleType, BigDecimal discountValue, DistributorValueAllocationDTO distributorInfo) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("roleType", roleType);
        requestMap.put("discountPoints", discountValue);
        requestMap.put("accountType", ACCOUNT_TYPE_DISCOUNT);
        if (distributorInfo != null) {
            requestMap.put("distributorInfo", distributorInfo);
        }
        // TODO 添加其他必要的参数
        return requestMap;
    }

    /**
     * 通用的价值转换和入账处理
     */
    private void processValueConversionAndAccount(String roleType, BigDecimal value) throws Exception {
        if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal bondingCurveValue = bondingCurveTokenService.calculateCurveValue(value);
            Map<String, Object> requestMap = buildAccountRequestMap(roleType, bondingCurveValue, null);
            accountService.inAccount(requestMap);
        }
    }

    /**
     * 构建入账请求参数
     */
    private Map<String, Object> buildAccountRequestMap(String roleType, BigDecimal bondingCurveValue, DistributorValueAllocationDTO distributorInfo) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("roleType", roleType);
        requestMap.put("bondingCurveValue", bondingCurveValue);
        if (distributorInfo != null) {
            requestMap.put("distributorInfo", distributorInfo);
        }
        // TODO 添加其他必要的参数
        return requestMap;
    }

    /**
     * 处理数字积分释放
     */
    private void processDigitalPointsRelease() throws Exception {
        try {
            // 判断曲线价值池中曲线价值是否达到配置的释放积分阀值
            QueryWrapper<BondingCurveTokenParam> bondingCurveTokenParamQueryWrapper = new QueryWrapper<>();
            // TODO 应该从上下文中获取实际的tenantId
            bondingCurveTokenParamQueryWrapper.eq(TENANT_ID_KEY, TENANT_ID_KEY);
            BondingCurveTokenParam bondingCurveTokenParam = bondingCurveTokenParamMapper.selectOne(bondingCurveTokenParamQueryWrapper);

            if (bondingCurveTokenParam != null && shouldReleaseDigitalPoints(bondingCurveTokenParam)) {
                releaseDigitalPointsToParticipants(bondingCurveTokenParam);
            }
        } catch (Exception e) {
            log.error("处理数字积分释放失败", e);
            throw e;
        }
    }

    /**
     * 释放数字积分给参与者
     */
    private void releaseDigitalPointsToParticipants(BondingCurveTokenParam bondingCurveTokenParam) throws Exception {
        try {
            // 计算出阀值对应的数字积分是多少，同时对数字积分进行释放
            BigDecimal digitalPointsValue = digitalPointsServiceEngine.calculateValueOfLabor(bondingCurveTokenParam.getReleaseThreshold().intValue());
            
            // TODO 将数字积分分配给持有待分配积分的曲线价值的参与者，同时清除待分配积分的曲线价值
            log.info("释放数字积分，价值：{}", digitalPointsValue);
            
            // TODO 实现实际的分配逻辑：
            // 1. 查询持有曲线价值的参与者
            // 2. 按比例分配数字积分
            // 3. 清除待分配的曲线价值
            
        } catch (Exception e) {
            log.error("释放数字积分给参与者失败，参数：{}", bondingCurveTokenParam, e);
            throw e;
        }
    }

    /**
     * 判断是否应该释放数字积分
     */
    private boolean shouldReleaseDigitalPoints(BondingCurveTokenParam bondingCurveTokenParam) {
        if (bondingCurveTokenParam == null) {
            log.debug("联合曲线参数为空，不进行数字积分释放");
            return false;
        }
        
        if (bondingCurveTokenParam.getReleaseThreshold() == null) {
            log.debug("释放阈值为空，不进行数字积分释放");
            return false;
        }
        
        boolean shouldRelease = bondingCurveTokenParam.getReleaseThreshold().compareTo(0) > 0;
        log.debug("是否应该释放数字积分：{}，阈值：{}", shouldRelease, bondingCurveTokenParam.getReleaseThreshold());
        
        // TODO 实现实际的判断逻辑，而不是仅仅检查阈值
        // 比如：检查当前曲线价值池的总值是否达到释放阈值
        return shouldRelease;
    }
}
