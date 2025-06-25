package me.flyray.bsin.server.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.context.BsinServiceContext;
import me.flyray.bsin.domain.entity.PayWay;
import me.flyray.bsin.domain.entity.ProfitSharingConfig;
import me.flyray.bsin.facade.service.ProfitSharingConfigService;
import me.flyray.bsin.infrastructure.mapper.PayWayMapper;
import me.flyray.bsin.infrastructure.mapper.ProfitSharingConfigMapper;
import me.flyray.bsin.security.contex.LoginInfoContextHelper;
import me.flyray.bsin.security.domain.LoginUser;
import me.flyray.bsin.server.utils.Pagination;
import org.apache.shenyu.client.apache.dubbo.annotation.ShenyuDubboService;
import org.apache.shenyu.client.apidocs.annotations.ApiDoc;
import org.apache.shenyu.client.apidocs.annotations.ApiModule;
import org.apache.shenyu.client.dubbo.common.annotation.ShenyuDubboClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

@Slf4j
@ShenyuDubboService(path = "/profitSharingConfig", timeout = 6000)
@ApiModule(value = "profitSharingConfig")
public class ProfitSharingConfigServiceImpl implements ProfitSharingConfigService {

    @Autowired
    private ProfitSharingConfigMapper profitSharingConfigMapper;

    @ApiDoc(desc = "getPageList")
    @ShenyuDubboClient("/getPageList")
    @Override
    public IPage<?> getPageList(Map<String, Object> requestMap) {
        LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
        Object paginationObj =  requestMap.get("pagination");
        Pagination pagination = new Pagination();
        BeanUtil.copyProperties(paginationObj,pagination);
        Page<ProfitSharingConfig> page = new Page<>(pagination.getPageNum(), pagination.getPageSize());
        ProfitSharingConfig profitSharingConfig = BsinServiceContext.getReqBodyDto(ProfitSharingConfig.class, requestMap);
        LambdaQueryWrapper<ProfitSharingConfig> warapper = new LambdaQueryWrapper<>();
        warapper.orderByDesc(ProfitSharingConfig::getCreateTime);
        warapper.eq(ProfitSharingConfig::getTenantId, loginUser.getTenantId());
        IPage<ProfitSharingConfig> pageList = profitSharingConfigMapper.selectPage(page, warapper);
        return pageList;
    }

}
