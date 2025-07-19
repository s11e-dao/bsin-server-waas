package me.flyray.bsin.server.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.context.BsinServiceContext;
import me.flyray.bsin.domain.entity.PayWay;
import me.flyray.bsin.domain.entity.ProfitSharingConfig;
import me.flyray.bsin.exception.BusinessException;
import me.flyray.bsin.facade.service.ProfitSharingConfigService;
import me.flyray.bsin.infrastructure.mapper.PayWayMapper;
import me.flyray.bsin.infrastructure.mapper.ProfitSharingConfigMapper;
import me.flyray.bsin.security.contex.LoginInfoContextHelper;
import me.flyray.bsin.security.domain.LoginUser;
import me.flyray.bsin.server.utils.Pagination;
import me.flyray.bsin.utils.BsinSnowflake;
import org.apache.commons.collections4.MapUtils;
import org.apache.shenyu.client.apache.dubbo.annotation.ShenyuDubboService;
import org.apache.shenyu.client.apidocs.annotations.ApiDoc;
import org.apache.shenyu.client.apidocs.annotations.ApiModule;
import org.apache.shenyu.client.dubbo.common.annotation.ShenyuDubboClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

@Slf4j
@ShenyuDubboService(path = "/profitSharingConfig", timeout = 6000)
@ApiModule(value = "profitSharingConfig")
public class ProfitSharingConfigServiceImpl implements ProfitSharingConfigService {

    @Autowired
    private ProfitSharingConfigMapper profitSharingConfigMapper;

    @ApiDoc(desc = "config")
    @ShenyuDubboClient("/config")
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProfitSharingConfig config(Map<String, Object> requestMap) {
        log.info("配置分账规则，参数: {}", requestMap);
        LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
        
        // 获取分账比例参数
        String superTenantRate = MapUtils.getString(requestMap, "superTenantRate");
        String tenantRate = MapUtils.getString(requestMap, "tenantRate");
        String sysAgentRate = MapUtils.getString(requestMap, "sysAgentRate");
        String customerRate = MapUtils.getString(requestMap, "customerRate");
        String distributorRate = MapUtils.getString(requestMap, "distributorRate");
        String exchangeDigitalPointsRate = MapUtils.getString(requestMap, "exchangeDigitalPointsRate");
        
        // 验证分账比例总和是否为100%
        BigDecimal totalRate = BigDecimal.ZERO;
        if (superTenantRate != null) totalRate = totalRate.add(new BigDecimal(superTenantRate));
        if (tenantRate != null) totalRate = totalRate.add(new BigDecimal(tenantRate));
        if (sysAgentRate != null) totalRate = totalRate.add(new BigDecimal(sysAgentRate));
        if (customerRate != null) totalRate = totalRate.add(new BigDecimal(customerRate));
        if (distributorRate != null) totalRate = totalRate.add(new BigDecimal(distributorRate));
        if (exchangeDigitalPointsRate != null) totalRate = totalRate.add(new BigDecimal(exchangeDigitalPointsRate));
        
        if (totalRate.compareTo(new BigDecimal("100")) != 0) {
            log.error("分账比例总和必须等于100%");
            throw new BusinessException("分账比例总和必须等于100%");
        }
        
        // 查询是否已存在配置
        LambdaQueryWrapper<ProfitSharingConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProfitSharingConfig::getTenantId, loginUser.getTenantId());
        ProfitSharingConfig existingConfig = profitSharingConfigMapper.selectOne(wrapper);
        
        ProfitSharingConfig profitSharingConfig;
        if (existingConfig != null) {
            // 更新现有配置
            profitSharingConfig = existingConfig;
            profitSharingConfig.setUpdateTime(new Date());
        } else {
            // 创建新配置
            profitSharingConfig = new ProfitSharingConfig();
            profitSharingConfig.setSerialNo(BsinSnowflake.getId());
            profitSharingConfig.setTenantId(loginUser.getTenantId());
            profitSharingConfig.setCreateTime(new Date());
        }
        
        // 设置分账比例
        if (superTenantRate != null) profitSharingConfig.setSuperTenantRate(new BigDecimal(superTenantRate));
        if (tenantRate != null) profitSharingConfig.setTenantRate(new BigDecimal(tenantRate));
        if (sysAgentRate != null) profitSharingConfig.setSysAgentRate(new BigDecimal(sysAgentRate));
        if (customerRate != null) profitSharingConfig.setCustomerRate(new BigDecimal(customerRate));
        if (distributorRate != null) profitSharingConfig.setDistributorRate(new BigDecimal(distributorRate));
        if (exchangeDigitalPointsRate != null) profitSharingConfig.setExchangeDigitalPointsRate(new BigDecimal(exchangeDigitalPointsRate));
        
        // 保存配置
        if (existingConfig != null) {
            profitSharingConfigMapper.updateById(profitSharingConfig);
        } else {
            profitSharingConfigMapper.insert(profitSharingConfig);
        }
        
        log.info("分账配置保存成功，配置ID: {}", profitSharingConfig.getSerialNo());
        return profitSharingConfig;
    }

    @ApiDoc(desc = "getDetail")
    @ShenyuDubboClient("/getDetail")
    @Override
    public ProfitSharingConfig getDetail(Map<String, Object> requestMap) {
        log.debug("获取分账配置详情，参数: {}", requestMap);
        LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
        
        LambdaQueryWrapper<ProfitSharingConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProfitSharingConfig::getTenantId, loginUser.getTenantId());
        
        ProfitSharingConfig profitSharingConfig = profitSharingConfigMapper.selectOne(wrapper);
        if (profitSharingConfig == null) {
            log.warn("未找到租户的分账配置，tenantId: {}", loginUser.getTenantId());
        }
        
        return profitSharingConfig;
    }

    @ApiDoc(desc = "getPageList")
    @ShenyuDubboClient("/getPageList")
    @Override
    public IPage<?> getPageList(Map<String, Object> requestMap) {
        log.debug("分页查询分账配置列表，参数: {}", requestMap);
        LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
        
        // 获取分页参数
        Object paginationObj = requestMap.get("pagination");
        Pagination pagination = new Pagination();
        BeanUtil.copyProperties(paginationObj, pagination);
        
        Page<ProfitSharingConfig> page = new Page<>(pagination.getPageNum(), pagination.getPageSize());
        
        // 构建查询条件
        LambdaQueryWrapper<ProfitSharingConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ProfitSharingConfig::getCreateTime);
        wrapper.eq(ProfitSharingConfig::getTenantId, loginUser.getTenantId());
        
        // 执行分页查询
        IPage<ProfitSharingConfig> pageList = profitSharingConfigMapper.selectPage(page, wrapper);
        
        log.debug("分账配置分页查询完成，总数: {}, 当前页: {}", pageList.getTotal(), pageList.getCurrent());
        return pageList;
    }

}
