package me.flyray.bsin.server.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.context.BsinServiceContext;
import me.flyray.bsin.domain.entity.Event;
import me.flyray.bsin.domain.entity.PayChannelInterface;
import me.flyray.bsin.exception.BusinessException;
import me.flyray.bsin.facade.service.PayChannelInterfaceService;
import me.flyray.bsin.infrastructure.mapper.PayChannelInterfaceMapper;
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
import org.springframework.stereotype.Service;

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
@Service
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
        String serialNo = MapUtils.getString(requestMap, "serialNo");
        if (payChannelInterfaceMapper.deleteById(serialNo) == 0){
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
        if (payChannelInterfaceMapper.updateById(payChannelInterface) == 0){
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
        warapper.orderByDesc(PayChannelInterface::getCreateTime);
        warapper.eq(PayChannelInterface::getTenantId, loginUser.getTenantId());
        IPage<PayChannelInterface> pageList = payChannelInterfaceMapper.selectPage(page, warapper);
        return pageList;
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
        String serialNo = MapUtils.getString(requestMap, "serialNo");
        PayChannelInterface payChannelInterface = payChannelInterfaceMapper.selectById(serialNo);
        return payChannelInterface;
    }

}




