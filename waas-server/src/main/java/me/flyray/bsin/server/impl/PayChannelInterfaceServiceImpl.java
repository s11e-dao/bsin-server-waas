package me.flyray.bsin.server.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.context.BsinServiceContext;
import me.flyray.bsin.domain.entity.PayChannelInterface;
import me.flyray.bsin.exception.BusinessException;
import me.flyray.bsin.facade.service.PayChannelInterfaceService;
import me.flyray.bsin.infrastructure.mapper.PayChannelInterfaceMapper;
import me.flyray.bsin.security.contex.LoginInfoContextHelper;
import me.flyray.bsin.security.domain.LoginUser;
import me.flyray.bsin.server.utils.Pagination;
import org.apache.commons.collections4.MapUtils;
import org.apache.shenyu.client.apache.dubbo.annotation.ShenyuDubboService;
import org.apache.shenyu.client.apidocs.annotations.ApiDoc;
import org.apache.shenyu.client.apidocs.annotations.ApiModule;
import org.apache.shenyu.client.dubbo.common.annotation.ShenyuDubboClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static me.flyray.bsin.constants.ResponseCode.GRADE_NOT_EXISTS;

/**
* @author rednet
* @description 针对表【waas_pay_channel_interface(支付渠道具体接口定义表)】的数据库操作Service实现
* @createDate 2024-10-26 10:15:56
*/
@Slf4j
@ShenyuDubboService(path = "/payChannelInterface", timeout = 6000)
@ApiModule(value = "payChannelInterface")
public class PayChannelInterfaceServiceImpl implements PayChannelInterfaceService {


    @Autowired
    private PayChannelInterfaceMapper payChannelInterfaceMapper;

    @ApiDoc(desc = "add")
    @ShenyuDubboClient("/add")
    @Override
    public PayChannelInterface add(Map<String, Object> requestMap) {
        LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
        PayChannelInterface payChannelInterface = BsinServiceContext.getReqBodyDto(PayChannelInterface.class, requestMap);
        payChannelInterface.setTenantId(loginUser.getTenantId());
        payChannelInterfaceMapper.insert(payChannelInterface);
        return payChannelInterface;
    }

    @ApiDoc(desc = "delete")
    @ShenyuDubboClient("/delete")
    @Override
    public void delete(Map<String, Object> requestMap) {
        String payChannelCode = MapUtils.getString(requestMap, "payChannelCode");
        LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
        
        // 使用复合主键条件进行删除
        LambdaQueryWrapper<PayChannelInterface> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PayChannelInterface::getPayChannelCode, payChannelCode);
        wrapper.eq(PayChannelInterface::getTenantId, loginUser.getTenantId());
        
        if (payChannelInterfaceMapper.delete(wrapper) == 0){
            throw new BusinessException(GRADE_NOT_EXISTS);
        }
    }

    @ApiDoc(desc = "edit")
    @ShenyuDubboClient("/edit")
    @Override
    public PayChannelInterface edit(Map<String, Object> requestMap) {
        LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
        PayChannelInterface payChannelInterface = BsinServiceContext.getReqBodyDto(PayChannelInterface.class, requestMap);
        payChannelInterface.setTenantId(loginUser.getTenantId());
        
        // 使用复合主键条件进行更新，避免主键冲突
        LambdaQueryWrapper<PayChannelInterface> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PayChannelInterface::getPayChannelCode, payChannelInterface.getPayChannelCode());
        wrapper.eq(PayChannelInterface::getTenantId, loginUser.getTenantId());
        
        if (payChannelInterfaceMapper.update(payChannelInterface, wrapper) == 0){
            throw new BusinessException(GRADE_NOT_EXISTS);
        }
        return payChannelInterface;
    }

    @ApiDoc(desc = "getPageList")
    @ShenyuDubboClient("/getPageList")
    @Override
    public IPage<?> getPageList(Map<String, Object> requestMap) {
        LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
        Object paginationObj =  requestMap.get("pagination");
        Pagination pagination = new Pagination();
        BeanUtil.copyProperties(paginationObj,pagination);
        Page<PayChannelInterface> page = new Page<>(pagination.getPageNum(), pagination.getPageSize());
        PayChannelInterface payChannelInterface = BsinServiceContext.getReqBodyDto(PayChannelInterface.class, requestMap);
        LambdaQueryWrapper<PayChannelInterface> warapper = new LambdaQueryWrapper<>();
        warapper.eq(ObjectUtil.isNotNull(payChannelInterface.getPayChannelCode()), PayChannelInterface::getPayChannelCode, payChannelInterface.getPayChannelCode());
        warapper.eq(ObjectUtil.isNotNull(payChannelInterface.getPayChannelName()), PayChannelInterface::getPayChannelName, payChannelInterface.getPayChannelName());
        warapper.eq(ObjectUtil.isNotNull(payChannelInterface.getWayCode()), PayChannelInterface::getWayCode, payChannelInterface.getWayCode());
        warapper.eq(ObjectUtil.isNotNull(payChannelInterface.getStatus()), PayChannelInterface::getStatus, payChannelInterface.getStatus());
        warapper.orderByDesc(PayChannelInterface::getCreateTime);
        warapper.eq(PayChannelInterface::getTenantId, loginUser.getTenantId());
        IPage<PayChannelInterface> pageList = payChannelInterfaceMapper.selectPage(page, warapper);
        return pageList;
    }

    @ApiDoc(desc = "getList")
    @ShenyuDubboClient("/getList")
    @Override
    public List<?> getList(Map<String, Object> requestMap) {
        LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
        PayChannelInterface payChannelInterface = BsinServiceContext.getReqBodyDto(PayChannelInterface.class, requestMap);
        LambdaQueryWrapper<PayChannelInterface> warapper = new LambdaQueryWrapper<>();
        warapper.eq(ObjectUtil.isNotNull(payChannelInterface.getPayChannelCode()), PayChannelInterface::getPayChannelCode, payChannelInterface.getPayChannelCode());
        warapper.eq(ObjectUtil.isNotNull(payChannelInterface.getPayChannelName()), PayChannelInterface::getPayChannelName, payChannelInterface.getPayChannelName());
        warapper.eq(ObjectUtil.isNotNull(payChannelInterface.getWayCode()), PayChannelInterface::getWayCode, payChannelInterface.getWayCode());
        warapper.eq(ObjectUtil.isNotNull(payChannelInterface.getStatus()), PayChannelInterface::getStatus, payChannelInterface.getStatus());
        warapper.orderByDesc(PayChannelInterface::getCreateTime);
        warapper.eq(PayChannelInterface::getTenantId, loginUser.getTenantId());
        return payChannelInterfaceMapper.selectList(warapper);
    }

    /**
     * 事件详情
     * @param requestMap
     * @return
     */
    @ApiDoc(desc = "getDetail")
    @ShenyuDubboClient("/getDetail")
    @Override
    public PayChannelInterface getDetail(Map<String, Object> requestMap){
        String payChannelCode = MapUtils.getString(requestMap, "payChannelCode");
        LambdaQueryWrapper<PayChannelInterface> warapper = new LambdaQueryWrapper<>();
        warapper.eq(PayChannelInterface::getPayChannelCode, payChannelCode);
        warapper.eq(PayChannelInterface::getTenantId, LoginInfoContextHelper.getLoginUser().getTenantId());
        PayChannelInterface payChannelInterface = payChannelInterfaceMapper.selectOne(warapper);
        return payChannelInterface;
    }

}




