package me.flyray.bsin.facade.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import me.flyray.bsin.domain.entity.DisCommissionConfig;
import me.flyray.bsin.domain.entity.PayWay;
import me.flyray.bsin.domain.entity.ProfitSharingConfig;

import java.util.Map;

public interface ProfitSharingConfigService {


    /**
     * 平台利益分配配置
     * @param requestMap
     * @return
     */
    public ProfitSharingConfig config(Map<String, Object> requestMap);

    /**
     * 详情
     */
    public ProfitSharingConfig getDetail(Map<String, Object> requestMap);

    public DisCommissionConfig getDetailForCrm(Map<String, Object> requestMap);

    public IPage<?> getPageList(Map<String, Object> requestMap);

}
