package me.flyray.bsin.facade.service;

import org.springframework.http.ResponseEntity;

import java.util.Map;

/**
 * 微信分账API接口服务
 * 提供后台管理UI所需的分账操作接口
 * 
 * @author flyray
 * @version 1.0
 */
public interface ProfitSharingApiService {

    /**
     * 请求分账
     * 
     * @param requestMap 请求参数
     * @return 分账结果
     */
    ResponseEntity<Map<String, Object>> requestProfitShare(Map<String, Object> requestMap);

    /**
     * 查询分账结果
     * 
     * @param requestMap 请求参数
     * @return 查询结果
     */
    ResponseEntity<Map<String, Object>> queryProfitShareResult(Map<String, Object> requestMap);

    /**
     * 请求分账回退
     * 
     * @param requestMap 请求参数
     * @return 回退结果
     */
    ResponseEntity<Map<String, Object>> requestProfitShareReturn(Map<String, Object> requestMap);

    /**
     * 查询分账回退结果
     * 
     * @param requestMap 请求参数
     * @return 查询结果
     */
    ResponseEntity<Map<String, Object>> queryProfitShareReturnResult(Map<String, Object> requestMap);

    /**
     * 解冻剩余资金
     * 
     * @param requestMap 请求参数
     * @return 解冻结果
     */
    ResponseEntity<Map<String, Object>> unfreezeRemainingFunds(Map<String, Object> requestMap);

    /**
     * 查询剩余待分金额
     * 
     * @param requestMap 请求参数
     * @return 查询结果
     */
    ResponseEntity<Map<String, Object>> queryRemainingAmount(Map<String, Object> requestMap);

    /**
     * 添加分账接收方
     * 
     * @param requestMap 请求参数
     * @return 添加结果
     */
    ResponseEntity<Map<String, Object>> addProfitShareReceiver(Map<String, Object> requestMap);

    /**
     * 删除分账接收方
     * 
     * @param requestMap 请求参数
     * @return 删除结果
     */
    ResponseEntity<Map<String, Object>> deleteProfitShareReceiver(Map<String, Object> requestMap);

    /**
     * 申请分账账单
     * 
     * @param requestMap 请求参数
     * @return 申请结果
     */
    ResponseEntity<Map<String, Object>> applyProfitShareBill(Map<String, Object> requestMap);

    /**
     * 下载账单
     * 
     * @param requestMap 请求参数
     * @return 下载结果
     */
    ResponseEntity<Map<String, Object>> downloadProfitShareBill(Map<String, Object> requestMap);
} 