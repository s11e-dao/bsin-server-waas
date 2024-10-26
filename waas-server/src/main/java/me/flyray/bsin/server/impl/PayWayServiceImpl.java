package me.flyray.bsin.server.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.context.BsinServiceContext;
import me.flyray.bsin.domain.entity.Event;
import me.flyray.bsin.domain.entity.PayWay;
import me.flyray.bsin.exception.BusinessException;
import me.flyray.bsin.facade.service.PayWayService;
import me.flyray.bsin.infrastructure.mapper.PayWayMapper;
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
* @description 针对表【waas_pay_way(支付渠道表)】的数据库操作Service实现
* @createDate 2024-10-26 10:16:00
*/
@Slf4j
@ShenyuDubboService(path = "/payWay", timeout = 6000)
@ApiModule(value = "payWay")
@Service
public class PayWayServiceImpl implements PayWayService {


    @Autowired
    private PayWayMapper payWayMapper;

    @ApiDoc(desc = "add")
    @ShenyuDubboClient("/add")
    @Override
    public PayWay add(Map<String, Object> requestMap) {
        LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
        PayWay payWay = BsinServiceContext.getReqBodyDto(PayWay.class, requestMap);
        payWay.setTenantId(loginUser.getTenantId());
        payWay.setSerialNo(BsinSnowflake.getId());
        payWayMapper.insert(payWay);
        return payWay;
    }

    @ApiDoc(desc = "delete")
    @ShenyuDubboClient("/delete")
    @Override
    public void delete(Map<String, Object> requestMap) {
        String serialNo = MapUtils.getString(requestMap, "serialNo");
        if (payWayMapper.deleteById(serialNo) == 0){
            throw new BusinessException(GRADE_NOT_EXISTS);
        }
    }

    @ApiDoc(desc = "edit")
    @ShenyuDubboClient("/edit")
    @Override
    public PayWay edit(Map<String, Object> requestMap) {
        LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
        PayWay payWay = BsinServiceContext.getReqBodyDto(PayWay.class, requestMap);
        payWay.setTenantId(loginUser.getTenantId());
        if (payWayMapper.updateById(payWay) == 0){
            throw new BusinessException(GRADE_NOT_EXISTS);
        }
        return payWay;
    }

    @ApiDoc(desc = "getPageList")
    @ShenyuDubboClient("/getPageList")
    @Override
    public IPage<?> getPageList(Map<String, Object> requestMap) {
        LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
        Object paginationObj =  requestMap.get("pagination");
        Pagination pagination = new Pagination();
        BeanUtil.copyProperties(paginationObj,pagination);
        Page<PayWay> page = new Page<>(pagination.getPageNum(), pagination.getPageSize());
        PayWay payWay = BsinServiceContext.getReqBodyDto(PayWay.class, requestMap);
        LambdaQueryWrapper<PayWay> warapper = new LambdaQueryWrapper<>();
        warapper.orderByDesc(PayWay::getCreateTime);
        warapper.eq(PayWay::getTenantId, loginUser.getTenantId());
        IPage<PayWay> pageList = payWayMapper.selectPage(page, warapper);
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
    public PayWay getDetail(Map<String, Object> requestMap){
        String serialNo = MapUtils.getString(requestMap, "serialNo");
        PayWay payWay = payWayMapper.selectById(serialNo);
        return payWay;
    }

}




