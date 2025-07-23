package me.flyray.bsin.server.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.context.BsinServiceContext;
import me.flyray.bsin.domain.entity.PayChannelConfig;
import me.flyray.bsin.exception.BusinessException;
import me.flyray.bsin.facade.service.PayChannelConfigService;
import me.flyray.bsin.infrastructure.mapper.PayChannelConfigMapper;
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

import java.util.List;
import java.util.Map;

import static me.flyray.bsin.constants.ResponseCode.PAY_CHANNEL_CONFIG_NOT_EXIST;

/**
* @author rednet
* @description 针对表【waas_pay_channel_config(应用支付接口参数配置表)】的数据库操作Service实现
* @createDate 2024-10-26 10:15:50
*/
@Slf4j
@ShenyuDubboService(path = "/payChannelConfig", timeout = 6000)
@ApiModule(value = "payChannelConfig")
public class PayChannelConfigServiceImpl implements PayChannelConfigService {

    @Autowired
    private PayChannelConfigMapper payChannelConfigMapper;

    /**
     * 一个bizRoleAppId在一个payChannelCode中只会存在一条,无则添加，有则修改
     * @param requestMap
     * @return
     */
    @ApiDoc(desc = "add")
    @ShenyuDubboClient("/add")
    @Override
    public PayChannelConfig add(Map<String, Object> requestMap) {
        LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
        PayChannelConfig payChannelConfig = BsinServiceContext.getReqBodyDto(PayChannelConfig.class, requestMap);
        payChannelConfig.setTenantId(loginUser.getTenantId());
        
        // 构建查询条件
        LambdaQueryWrapper<PayChannelConfig> wrapper = new LambdaQueryWrapper<PayChannelConfig>()
                .eq(PayChannelConfig::getTenantId, loginUser.getTenantId())
                .eq(PayChannelConfig::getBizRoleAppId, payChannelConfig.getBizRoleAppId())
                .eq(PayChannelConfig::getPayChannelCode, payChannelConfig.getPayChannelCode());
        // 查询记录
        PayChannelConfig existingPayChannelConfig = payChannelConfigMapper.selectOne(wrapper);
        if (existingPayChannelConfig == null) {
            // 记录不存在，插入新记录
            payChannelConfig.setSerialNo(BsinSnowflake.getId());
            payChannelConfigMapper.insert(payChannelConfig);
        } else {
            // 记录存在，更新记录
            payChannelConfig.setSerialNo(existingPayChannelConfig.getSerialNo());
            payChannelConfigMapper.updateById(payChannelConfig);
        }
        return payChannelConfig;
    }

    @ApiDoc(desc = "delete")
    @ShenyuDubboClient("/delete")
    @Override
    public void delete(Map<String, Object> requestMap) {
        String serialNo = MapUtils.getString(requestMap, "serialNo");
        if (payChannelConfigMapper.deleteById(serialNo) == 0){
            throw new BusinessException(PAY_CHANNEL_CONFIG_NOT_EXIST);
        }
    }

    @ApiDoc(desc = "edit")
    @ShenyuDubboClient("/edit")
    @Override
    public PayChannelConfig edit(Map<String, Object> requestMap) {
        LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
        PayChannelConfig payChannelConfig = BsinServiceContext.getReqBodyDto(PayChannelConfig.class, requestMap);
        payChannelConfig.setTenantId(loginUser.getTenantId());
//        payChannelConfig.setSerialNo(serialNo);
        if (payChannelConfigMapper.updateById(payChannelConfig) == 0){
            throw new BusinessException(PAY_CHANNEL_CONFIG_NOT_EXIST);
        }
        return payChannelConfig;
    }

    @ApiDoc(desc = "getPageList")
    @ShenyuDubboClient("/getPageList")
    @Override
    public IPage<?> getPageList(Map<String, Object> requestMap) {
        LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
        Object paginationObj =  requestMap.get("pagination");
        Pagination pagination = new Pagination();
        BeanUtil.copyProperties(paginationObj,pagination);
        Page<PayChannelConfig> page = new Page<>(pagination.getPageNum(), pagination.getPageSize());
        PayChannelConfig payChannelConfig = BsinServiceContext.getReqBodyDto(PayChannelConfig.class, requestMap);
        LambdaQueryWrapper<PayChannelConfig> warapper = new LambdaQueryWrapper<>();
        warapper.orderByDesc(PayChannelConfig::getCreateTime);
        warapper.eq(PayChannelConfig::getTenantId, loginUser.getTenantId());
        IPage<PayChannelConfig> pageList = payChannelConfigMapper.selectPage(page, warapper);
        return pageList;
    }

    @ApiDoc(desc = "getList")
    @ShenyuDubboClient("/getList")
    @Override
    public List<?> getList(Map<String, Object> requestMap) {
        LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
        PayChannelConfig payChannelConfig = BsinServiceContext.getReqBodyDto(PayChannelConfig.class, requestMap);
        LambdaQueryWrapper<PayChannelConfig> warapper = new LambdaQueryWrapper<>();
        warapper.eq(ObjectUtil.isNotNull(payChannelConfig.getPayChannelCode()), PayChannelConfig::getPayChannelCode, payChannelConfig.getPayChannelCode());
        warapper.orderByDesc(PayChannelConfig::getCreateTime);
        warapper.eq(PayChannelConfig::getTenantId, loginUser.getTenantId());
        return payChannelConfigMapper.selectList(warapper);
    }

    /**
     * 支付配置详情
     * @param requestMap
     * @return
     */
    @ApiDoc(desc = "getDetail")
    @ShenyuDubboClient("/getDetail")
    @Override
    public PayChannelConfig getDetail(Map<String, Object> requestMap){
        String serialNo = MapUtils.getString(requestMap, "serialNo");
        PayChannelConfig payChannelConfig = payChannelConfigMapper.selectById(serialNo);
        return payChannelConfig;
    }

    /**
     * 查询应用支付配置详情
     * @param requestMap
     * @return
     */
    @ApiDoc(desc = "getBizRoleAppPayChannelConfig")
    @ShenyuDubboClient("/getBizRoleAppPayChannelConfig")
    @Override
    public PayChannelConfig getBizRoleAppPayChannelConfig(Map<String, Object> requestMap){
        String bizRoleAppId = MapUtils.getString(requestMap, "bizRoleAppId");
        String payChannelCode = MapUtils.getString(requestMap, "payChannelCode");
        LambdaQueryWrapper<PayChannelConfig> warapper = new LambdaQueryWrapper<>();
        warapper.eq(PayChannelConfig::getBizRoleAppId, bizRoleAppId);
        warapper.eq(PayChannelConfig::getPayChannelCode, payChannelCode);
        warapper.eq(PayChannelConfig::getTenantId, LoginInfoContextHelper.getLoginUser().getTenantId());
        warapper.orderByDesc(PayChannelConfig::getCreateTime);
        PayChannelConfig payChannelConfig = payChannelConfigMapper.selectOne(warapper);
        return payChannelConfig;
    }
}




