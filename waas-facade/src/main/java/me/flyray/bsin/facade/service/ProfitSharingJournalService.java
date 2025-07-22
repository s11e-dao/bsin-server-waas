package me.flyray.bsin.facade.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import me.flyray.bsin.domain.entity.ProfitSharingJournal;

import java.util.Map;

/**
* @description 针对表【waas_profit_sharing_journal(交易让利分账流水)】的数据库操作Service
* @createDate 2025-07-22 13:57:41
*/
public interface ProfitSharingJournalService {

    /**
     * 添加分账流水记录
     * @param requestMap 请求参数
     * @return 分账流水记录
     */
    ProfitSharingJournal add(Map<String, Object> requestMap);

    /**
     * 删除分账流水记录
     * @param requestMap 请求参数
     */
    void delete(Map<String, Object> requestMap);

    /**
     * 编辑分账流水记录
     * @param requestMap 请求参数
     * @return 分账流水记录
     */
    ProfitSharingJournal edit(Map<String, Object> requestMap);

    /**
     * 获取分账流水记录详情
     * @param requestMap 请求参数
     * @return 分账流水记录
     */
    ProfitSharingJournal getDetail(Map<String, Object> requestMap);

    /**
     * 分页查询分账流水记录
     * @param requestMap 请求参数
     * @return 分页结果
     */
    IPage<?> getPageList(Map<String, Object> requestMap);

    /**
     * 根据交易单号查询分账流水记录
     * @param requestMap 请求参数
     * @return 分账流水记录列表
     */
    java.util.List<ProfitSharingJournal> getListByTransactionNo(Map<String, Object> requestMap);
}
