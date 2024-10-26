package me.flyray.bsin.facade.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import me.flyray.bsin.domain.entity.PayWay;

import java.util.Map;

/**
* @author rednet
* @description 针对表【waas_pay_way(支付渠道表)】的数据库操作Service
* @createDate 2024-10-26 10:16:00
*/
public interface PayWayService {

    /**
     * 添加
     */
    public PayWay add(Map<String, Object> requestMap);

    /**
     * 删除
     */
    public void delete(Map<String, Object> requestMap);

    /**
     * 编辑
     */
    public PayWay edit(Map<String, Object> requestMap);


    /**
     * 详情
     */
    public PayWay getDetail(Map<String, Object> requestMap);

    /**
     * 租户下所有
     */
    public IPage<?> getPageList(Map<String, Object> requestMap);

}
