package me.flyray.bsin.facade.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import me.flyray.bsin.domain.entity.ProfitSharingReceiver;

import java.util.Map;

/**
* @description 针对表【waas_profit_sharing_receiver(参与分润的接受者绑定关系表)】的数据库操作Service
* @createDate 2025-07-22 13:57:46
*/
public interface ProfitSharingReceiverService {

    /**
     * 添加分账接收方
     * @param requestMap 请求参数
     * @return 分账接收方
     */
    ProfitSharingReceiver add(Map<String, Object> requestMap);

    /**
     * 删除分账接收方
     * @param requestMap 请求参数
     */
    void delete(Map<String, Object> requestMap);

    /**
     * 编辑分账接收方
     * @param requestMap 请求参数
     * @return 分账接收方
     */
    ProfitSharingReceiver edit(Map<String, Object> requestMap);

    /**
     * 获取分账接收方详情
     * @param requestMap 请求参数
     * @return 分账接收方
     */
    ProfitSharingReceiver getDetail(Map<String, Object> requestMap);

    /**
     * 分页查询分账接收方
     * @param requestMap 请求参数
     * @return 分页结果
     */
    IPage<?> getPageList(Map<String, Object> requestMap);

    /**
     * 根据接收方ID查询分账接收方
     * @param requestMap 请求参数
     * @return 分账接收方
     */
    ProfitSharingReceiver getByReceiverId(Map<String, Object> requestMap);

    /**
     * 根据商户号查询分账接收方列表
     * @param requestMap 请求参数
     * @return 分账接收方列表
     */
    java.util.List<ProfitSharingReceiver> getListByMerchantNo(Map<String, Object> requestMap);

    /**
     * 更新分账接收方状态
     * @param requestMap 请求参数
     * @return 分账接收方
     */
    ProfitSharingReceiver updateStatus(Map<String, Object> requestMap);
}
