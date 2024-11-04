package me.flyray.bsin.facade.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import me.flyray.bsin.domain.entity.PayChannelConfig;
import me.flyray.bsin.domain.entity.PayWay;

import java.util.List;
import java.util.Map;

/**
* @author rednet
* @description 针对表【waas_pay_channel_config(应用支付接口参数配置表)】的数据库操作Service
* @createDate 2024-10-26 10:15:50
*/
public interface PayChannelConfigService {

    /**
     * 添加
     */
    public PayChannelConfig add(Map<String, Object> requestMap);

    /**
     * 删除
     */
    public void delete(Map<String, Object> requestMap);

    /**
     * 编辑
     */
    public PayChannelConfig edit(Map<String, Object> requestMap);


    /**
     * 详情
     */
    public PayChannelConfig getDetail(Map<String, Object> requestMap);

    /**
     * 查询应用支付配置详情
     */
    public PayChannelConfig getBizRoleAppPayChannelConfig(Map<String, Object> requestMap);


    /**
     * 租户下分页所有
     */
    public IPage<?> getPageList(Map<String, Object> requestMap);

    /**
     * 租户下所有
     */
    public List<?> getList(Map<String, Object> requestMap);

}
