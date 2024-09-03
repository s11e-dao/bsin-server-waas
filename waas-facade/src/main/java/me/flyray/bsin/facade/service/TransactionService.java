package me.flyray.bsin.facade.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.flyray.bsin.domain.entity.Transaction;
import me.flyray.bsin.domain.request.TransactionDTO;
import me.flyray.bsin.domain.request.TransactionRequest;
import me.flyray.bsin.domain.response.TransactionVO;

import java.util.Map;

/**
* @author Admin
* @description 针对表【crm_transaction(交易记录;)】的数据库操作Service
* @createDate 2024-04-24 20:36:00
*/
public interface TransactionService  {

    /**
     * 创建交易（转出）
     * 1、参数校验
     * 2、记录交易信息
     * 3、调用风控方法
     * 4、风控未通过，则进入人工审核
     * @param transactionRequest
     * @return
     */
    void create(TransactionRequest transactionRequest);


    /**
     * 转出
     *
     * @param transactionDTO
     * @return
     */
    void transferOut(TransactionDTO transactionDTO) throws Exception;

    /**
     * 分页查询交易记录列表
     * @param transactionDTO
     * @return
     */
    Page<TransactionVO> getPageList(TransactionDTO transactionDTO);

    public Transaction getDetail(Map<String, Object> requestMap);

//    /**
//     * 转入通知
//     * @param transactionRequest
//     * @return
//     */
//    public BsinResultEntity transferNotification(TransactionRequest transactionRequest);
//
//    /**
//     * 转出通知
//     * @param transactionRequest
//     * @return
//     */
//    public BsinResultEntity transferOutNotification(TransactionRequest transactionRequest);

}
