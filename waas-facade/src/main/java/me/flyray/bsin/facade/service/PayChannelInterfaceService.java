package me.flyray.bsin.facade.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import me.flyray.bsin.domain.entity.PayChannelConfig;
import me.flyray.bsin.domain.entity.PayChannelInterface;

import java.util.Map;

/**
* @author rednet
* @description 针对表【waas_pay_channel_interface(支付渠道具体接口定义表)】的数据库操作Service
* @createDate 2024-10-26 10:15:56
*/
public interface PayChannelInterfaceService {

    /**
     * 添加
     */
    public PayChannelInterface add(Map<String, Object> requestMap);

    /**
     * 删除
     */
    public void delete(Map<String, Object> requestMap);

    /**
     * 编辑
     */
    public PayChannelInterface edit(Map<String, Object> requestMap);


    /**
     * 详情
     */
    public PayChannelInterface getDetail(Map<String, Object> requestMap);

    /**
     * 租户下所有
     */
    public IPage<?> getPageList(Map<String, Object> requestMap);

}
