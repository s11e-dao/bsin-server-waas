package me.flyray.bsin.server;

import com.alibaba.fastjson.JSONObject;
import com.github.binarywang.wxpay.bean.notify.SignatureHeader;
import com.github.binarywang.wxpay.bean.profitsharing.notify.ProfitSharingNotifyV3Result;
import com.github.binarywang.wxpay.bean.profitsharing.request.*;
import com.github.binarywang.wxpay.bean.profitsharing.result.*;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.service.ProfitSharingService;
import com.github.binarywang.wxpay.service.WxPayService;
import com.github.binarywang.wxpay.service.impl.WxPayServiceImpl;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 微信支付分账服务实现类 (基于微信支付SDK 4.7.0版本)
 * 提供服务商模式分账相关的业务功能，包括：
 * - 添加/删除分账接收方
 * - 执行分账操作
 * - 查询分账结果
 * - 分账完结
 * - 分账回退
 * - 查询分账比例等
 *
 * @author flyray
 * @version 2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class WechatProfitSharingServiceDemo {

    /**
     * 微信支付API V3密钥
     */
    @Value("${wx.pay.apiV3Key:111}")
    private String apiV3Key;

    /**
     * 微信支付商户号（服务商商户号）
     */
    @Value("${wx.pay.mchId:11}")
    private String mchId;

    /**
     * 子商户号
     */
    @Value("${wx.pay.subMchId:11}")
    private String subMchId;

    /**
     * 应用ID
     */
    @Value("${wx.pay.appId:11}")
    private String appId;

    /**
     * 子应用ID
     */
    @Value("${wx.pay.subAppId:11}")
    private String subAppId;

    /**
     * 私钥文件路径
     */
    @Value("${wx.pay.privateKeyPath:/apiclient_key.pem}")
    private String privateKeyPath;

    /**
     * 证书文件路径
     */
    @Value("${wx.pay.privateCertPath:/apiclient_cert.pem}")
    private String privateCertPath;

    /**
     * 证书序列号 (4.7.0版本推荐配置)
     */
    @Value("${wx.pay.certSerialNo:}")
    private String certSerialNo;

    /**
     * Gson实例
     */
    private final Gson gson = new GsonBuilder().create();

    /**
     * 微信支付服务实例
     */
    private WxPayService wxPayService;

    /**
     * 初始化微信支付服务配置
     */
    @PostConstruct
    public void initWxPayService() {
        try {
            this.wxPayService = createWxPayService();
            log.info("微信支付分账服务初始化完成，服务商商户号：{}，子商户号：{}", mchId, subMchId);
        } catch (Exception e) {
            log.error("微信支付分账服务初始化失败：{}", e.getMessage(), e);
            throw new RuntimeException("微信支付分账服务初始化失败", e);
        }
    }

    // ==================== 服务商分账接收方管理 ====================

    /**
     * 服务商代子商户添加分账接收方 (传统API)
     *
     * @param type 分账接收方类型
     * @param account 分账接收方账号
     * @param name 分账接收方名称
     * @param relationType 关系类型
     * @param customRelation 自定义关系
     * @return 添加结果
     * @throws Exception 添加异常
     */
    public ProfitSharingReceiverResult addReceiver(String type, String account, String name, 
                                                   String relationType, String customRelation) throws Exception {
        log.info("开始添加分账接收方，子商户：{}，类型：{}，账号：{}", subMchId, type, maskAccount(account));

        try {
            ProfitSharingService profitSharingService = wxPayService.getProfitSharingService();
            
            ProfitSharingReceiverRequest request = new ProfitSharingReceiverRequest();
            request.setSubMchId(subMchId);
            request.setSubAppId(subAppId);

            ProfitSharingReceiverResult result = profitSharingService.addReceiver(request);
            log.info("添加分账接收方成功：{}", result);
            return result;

        } catch (Exception e) {
            log.error("添加分账接收方失败：{}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 添加分账接收方 (V3 API)
     *
     * @param type 分账接收方类型
     * @param account 分账接收方账号
     * @param name 分账接收方名称
     * @param relationType 关系类型
     * @param customRelation 自定义关系
     * @return 添加结果
     * @throws Exception 添加异常
     */
    public ProfitSharingReceiverV3Result addReceiverV3(String type, String account, String name, 
                                                        String relationType, String customRelation) throws Exception {
        log.info("开始添加分账接收方V3，子商户：{}，类型：{}，账号：{}", subMchId, type, maskAccount(account));

        try {
            ProfitSharingService profitSharingService = wxPayService.getProfitSharingService();
            
            ProfitSharingReceiverV3Request request = new ProfitSharingReceiverV3Request();
            request.setAppid(appId);
            request.setSubAppid(subAppId);
            request.setType(type);
            request.setAccount(account);
            request.setName(name);
            request.setRelationType(relationType);
            request.setCustomRelation(customRelation);

            ProfitSharingReceiverV3Result result = profitSharingService.addReceiverV3(request);
            log.info("添加分账接收方V3成功：{}", result);
            return result;

        } catch (Exception e) {
            log.error("添加分账接收方V3失败：{}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 服务商代子商户删除分账接收方 (传统API)
     *
     * @param type 分账接收方类型
     * @param account 分账接收方账号
     * @return 删除结果
     * @throws Exception 删除异常
     */
    public ProfitSharingReceiverResult removeReceiver(String type, String account) throws Exception {
        log.info("开始删除分账接收方，子商户：{}，类型：{}，账号：{}", subMchId, type, maskAccount(account));

        try {
            ProfitSharingService profitSharingService = wxPayService.getProfitSharingService();
            
            ProfitSharingReceiverRequest request = new ProfitSharingReceiverRequest();
            request.setSubMchId(subMchId);
            request.setSubAppId(subAppId);

            ProfitSharingReceiverResult result = profitSharingService.removeReceiver(request);
            log.info("删除分账接收方成功：{}", result);
            return result;

        } catch (Exception e) {
            log.error("删除分账接收方失败：{}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 删除分账接收方 (V3 API)
     *
     * @param type 分账接收方类型
     * @param account 分账接收方账号
     * @return 删除结果
     * @throws Exception 删除异常
     */
    public ProfitSharingReceiverV3Result removeReceiverV3(String type, String account) throws Exception {
        log.info("开始删除分账接收方V3，子商户：{}，类型：{}，账号：{}", subMchId, type, maskAccount(account));

        try {
            ProfitSharingService profitSharingService = wxPayService.getProfitSharingService();
            
            ProfitSharingReceiverV3Request request = new ProfitSharingReceiverV3Request();
            request.setAppid(appId);
            request.setSubAppid(subAppId);
            request.setType(type);
            request.setAccount(account);

            ProfitSharingReceiverV3Result result = profitSharingService.removeReceiverV3(request);
            log.info("删除分账接收方V3成功：{}", result);
            return result;

        } catch (Exception e) {
            log.error("删除分账接收方V3失败：{}", e.getMessage(), e);
            throw e;
        }
    }

    // ==================== 分账操作 ====================

    /**
     * 服务商代子商户发起多次分账请求 (传统API)
     *
     * @param transactionId 微信订单号
     * @param receivers 分账接收方列表
     * @return 分账结果
     * @throws Exception 分账异常
     */
    public ProfitSharingResult multiProfitSharing(@NotBlank(message = "微信订单号不能为空") String transactionId,
                                                  List<ProfitSharingV3Request.Receiver> receivers) throws Exception {
        log.info("开始执行多次分账，子商户：{}，订单号：{}", subMchId, transactionId);

        try {
            ProfitSharingService profitSharingService = wxPayService.getProfitSharingService();
            
            ProfitSharingRequest request = new ProfitSharingRequest();
            request.setSubMchId(subMchId);
            request.setSubAppId(subAppId);
            request.setTransactionId(transactionId);
            request.setOutOrderNo(generateOutOrderNo());
            
            // 转换接收方列表为JSON字符串
            String receiversJson = gson.toJson(receivers);
            request.setReceivers(receiversJson);

            ProfitSharingResult result = profitSharingService.multiProfitSharing(request);
            log.info("多次分账执行成功：{}", result);
            return result;

        } catch (Exception e) {
            log.error("多次分账执行失败：{}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 单次分账请求 (传统API)
     *
     * @param transactionId 微信订单号
     * @param receivers 分账接收方列表
     * @return 分账结果
     * @throws Exception 分账异常
     */
    public ProfitSharingResult profitSharing(@NotBlank(message = "微信订单号不能为空") String transactionId,
                                             List<ProfitSharingV3Request.Receiver> receivers) throws Exception {
        log.info("开始执行单次分账，子商户：{}，订单号：{}", subMchId, transactionId);

        try {
            ProfitSharingService profitSharingService = wxPayService.getProfitSharingService();
            
            ProfitSharingRequest request = new ProfitSharingRequest();
            request.setSubMchId(subMchId);
            request.setSubAppId(subAppId);
            request.setTransactionId(transactionId);
            request.setOutOrderNo(generateOutOrderNo());
            
            // 转换接收方列表为JSON字符串
            String receiversJson = gson.toJson(receivers);
            request.setReceivers(receiversJson);

            ProfitSharingResult result = profitSharingService.profitSharing(request);
            log.info("单次分账执行成功：{}", result);
            return result;

        } catch (Exception e) {
            log.error("单次分账执行失败：{}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 请求分账API (V3 API)
     *
     * @param transactionId 微信订单号
     * @param receivers 分账接收方列表
     * @return 分账结果
     * @throws Exception 分账异常
     */
    public ProfitSharingV3Result profitSharingV3(@NotBlank(message = "微信订单号不能为空") String transactionId,
                                                 List<ProfitSharingV3Request.Receiver> receivers) throws Exception {
        log.info("开始执行分账V3，子商户：{}，订单号：{}", subMchId, transactionId);

        try {
            ProfitSharingService profitSharingService = wxPayService.getProfitSharingService();
            
            ProfitSharingV3Request request = new ProfitSharingV3Request();
            request.setAppid(appId);
            request.setSubAppid(subAppId);
            request.setTransactionId(transactionId);
            request.setOutOrderNo(generateOutOrderNo());
            request.setReceivers(receivers);
            request.setUnfreezeUnsplit(true); // 是否解冻剩余未分资金

            ProfitSharingV3Result result = profitSharingService.profitSharingV3(request);
            log.info("分账V3执行成功：{}", result);
            return result;

        } catch (Exception e) {
            log.error("分账V3执行失败：{}", e.getMessage(), e);
            throw e;
        }
    }

    // ==================== 分账查询 ====================

    /**
     * 查询分账结果 (传统API)
     *
     * @param outOrderNo 商户分账单号
     * @param transactionId 微信订单号
     * @return 查询结果
     * @throws Exception 查询异常
     */
    public ProfitSharingQueryResult profitSharingQuery(@NotBlank(message = "商户分账单号不能为空") String outOrderNo,
                                                       String transactionId) throws Exception {
        log.info("开始查询分账结果，子商户：{}，分账单号：{}，订单号：{}", subMchId, outOrderNo, transactionId);

        try {
            ProfitSharingService profitSharingService = wxPayService.getProfitSharingService();
            
            ProfitSharingQueryRequest request = new ProfitSharingQueryRequest();
            request.setSubMchId(subMchId);
            request.setTransactionId(transactionId);
            request.setOutOrderNo(outOrderNo);

            ProfitSharingQueryResult result = profitSharingService.profitSharingQuery(request);
            log.info("查询分账结果成功：{}", result);
            return result;

        } catch (Exception e) {
            log.error("查询分账结果失败：{}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 查询分账结果API (V3商户平台)
     *
     * @param outOrderNo 商户分账单号
     * @param transactionId 微信订单号
     * @return 查询结果
     * @throws Exception 查询异常
     */
    public ProfitSharingV3Result profitSharingQueryV3(@NotBlank(message = "商户分账单号不能为空") String outOrderNo,
                                                      String transactionId) throws Exception {
        log.info("开始查询分账结果V3，分账单号：{}，订单号：{}", outOrderNo, transactionId);

        try {
            ProfitSharingService profitSharingService = wxPayService.getProfitSharingService();
            
            ProfitSharingV3Result result = profitSharingService.profitSharingQueryV3(outOrderNo, transactionId);
            log.info("查询分账结果V3成功：{}", result);
            return result;

        } catch (Exception e) {
            log.error("查询分账结果V3失败：{}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 查询分账结果API (V3服务商平台)
     *
     * @param outOrderNo 商户分账单号
     * @param transactionId 微信订单号
     * @param subMchId 子商户号
     * @return 查询结果
     * @throws Exception 查询异常
     */
    public ProfitSharingV3Result profitSharingQueryV3ForPartner(@NotBlank(message = "商户分账单号不能为空") String outOrderNo,
                                                               String transactionId,
                                                               @NotBlank(message = "子商户号不能为空") String subMchId) throws Exception {
        log.info("开始查询分账结果V3服务商，分账单号：{}，订单号：{}，子商户：{}", outOrderNo, transactionId, subMchId);

        try {
            ProfitSharingService profitSharingService = wxPayService.getProfitSharingService();
            
            ProfitSharingV3Result result = profitSharingService.profitSharingQueryV3(outOrderNo, transactionId, subMchId);
            log.info("查询分账结果V3服务商成功：{}", result);
            return result;

        } catch (Exception e) {
            log.error("查询分账结果V3服务商失败：{}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 查询订单剩余待分金额 (传统API)
     *
     * @param transactionId 微信订单号
     * @return 查询结果
     * @throws Exception 查询异常
     */
    public ProfitSharingOrderAmountQueryResult profitSharingOrderAmountQuery(@NotBlank(message = "微信订单号不能为空") String transactionId) throws Exception {
        log.info("开始查询订单剩余待分金额，子商户：{}，订单号：{}", subMchId, transactionId);

        try {
            ProfitSharingService profitSharingService = wxPayService.getProfitSharingService();
            
            ProfitSharingOrderAmountQueryRequest request = new ProfitSharingOrderAmountQueryRequest();
            request.setSubMchId(subMchId);
            request.setTransactionId(transactionId);

            ProfitSharingOrderAmountQueryResult result = profitSharingService.profitSharingOrderAmountQuery(request);
            log.info("查询订单剩余待分金额成功：{}", result);
            return result;

        } catch (Exception e) {
            log.error("查询订单剩余待分金额失败：{}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 查询剩余待分金额API (V3 API)
     *
     * @param transactionId 微信订单号
     * @return 查询结果
     * @throws Exception 查询异常
     */
    public ProfitSharingOrderAmountQueryV3Result profitSharingUnsplitAmountQueryV3(@NotBlank(message = "微信订单号不能为空") String transactionId) throws Exception {
        log.info("开始查询剩余待分金额V3，订单号：{}", transactionId);

        try {
            ProfitSharingService profitSharingService = wxPayService.getProfitSharingService();
            
            ProfitSharingOrderAmountQueryV3Result result = profitSharingService.profitSharingUnsplitAmountQueryV3(transactionId);
            log.info("查询剩余待分金额V3成功：{}", result);
            return result;

        } catch (Exception e) {
            log.error("查询剩余待分金额V3失败：{}", e.getMessage(), e);
            throw e;
        }
    }

    // ==================== 分账完结 ====================

    /**
     * 分账完结 (传统API)
     *
     * @param transactionId 微信订单号
     * @param outOrderNo 商户分账单号
     * @param description 完结描述
     * @return 完结结果
     * @throws Exception 完结异常
     */
    public ProfitSharingResult profitSharingFinish(@NotBlank(message = "微信订单号不能为空") String transactionId,
                                                   @NotBlank(message = "商户分账单号不能为空") String outOrderNo,
                                                   String description) throws Exception {
        log.info("开始分账完结，子商户：{}，订单号：{}，分账单号：{}", subMchId, transactionId, outOrderNo);

        try {
            ProfitSharingService profitSharingService = wxPayService.getProfitSharingService();
            
            ProfitSharingUnfreezeRequest request = new ProfitSharingUnfreezeRequest();
            request.setSubMchId(subMchId);
            request.setSubAppId(subAppId);
            request.setTransactionId(transactionId);
            request.setOutOrderNo(outOrderNo);
            request.setDescription(description != null ? description : "分账完结");

            ProfitSharingResult result = profitSharingService.profitSharingFinish(request);
            log.info("分账完结成功：{}", result);
            return result;

        } catch (Exception e) {
            log.error("分账完结失败：{}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 解冻剩余资金API (V3 API)
     *
     * @param transactionId 微信订单号
     * @param outOrderNo 商户分账单号
     * @param description 解冻描述
     * @return 解冻结果
     * @throws Exception 解冻异常
     */
    public ProfitSharingUnfreezeV3Result profitSharingUnfreeze(@NotBlank(message = "微信订单号不能为空") String transactionId,
                                                               @NotBlank(message = "商户分账单号不能为空") String outOrderNo,
                                                               String description) throws Exception {
        log.info("开始解冻剩余资金V3，子商户：{}，订单号：{}，分账单号：{}", subMchId, transactionId, outOrderNo);

        try {
            ProfitSharingService profitSharingService = wxPayService.getProfitSharingService();
            
            ProfitSharingUnfreezeV3Request request = new ProfitSharingUnfreezeV3Request();
            request.setTransactionId(transactionId);
            request.setOutOrderNo(outOrderNo);
            request.setDescription(description != null ? description : "解冻剩余资金");

            ProfitSharingUnfreezeV3Result result = profitSharingService.profitSharingUnfreeze(request);
            log.info("解冻剩余资金V3成功：{}", result);
            return result;

        } catch (Exception e) {
            log.error("解冻剩余资金V3失败：{}", e.getMessage(), e);
            throw e;
        }
    }

    // ==================== 分账回退 ====================

    /**
     * 分账回退 (传统API)
     *
     * @param outOrderNo 原分账单号
     * @param outReturnNo 回退单号
     * @param returnMchId 回退方商户号
     * @param amount 回退金额
     * @param description 回退描述
     * @return 回退结果
     * @throws Exception 回退异常
     */
    public ProfitSharingReturnResult profitSharingReturn(@NotBlank(message = "原分账单号不能为空") String outOrderNo,
                                                         @NotBlank(message = "回退单号不能为空") String outReturnNo,
                                                         @NotBlank(message = "回退方商户号不能为空") String returnMchId,
                                                         Integer amount,
                                                         String description) throws Exception {
        log.info("开始分账回退，子商户：{}，原分账单号：{}，回退单号：{}，回退金额：{}分", subMchId, outOrderNo, outReturnNo, amount);

        try {
            ProfitSharingService profitSharingService = wxPayService.getProfitSharingService();
            
            ProfitSharingReturnRequest request = new ProfitSharingReturnRequest();
            request.setSubMchId(subMchId);
            request.setSubAppId(subAppId);
            request.setOutOrderNo(outOrderNo);
            request.setOutReturnNo(outReturnNo);
            request.setDescription(description != null ? description : "分账回退");

            ProfitSharingReturnResult result = profitSharingService.profitSharingReturn(request);
            log.info("分账回退成功：{}", result);
            return result;

        } catch (Exception e) {
            log.error("分账回退失败：{}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 请求分账回退API (V3 API)
     *
     * @param outOrderNo 原分账单号
     * @param outReturnNo 回退单号
     * @param returnMchid 回退方商户号
     * @param amount 回退金额
     * @param description 回退描述
     * @return 回退结果
     * @throws Exception 回退异常
     */
    public ProfitSharingReturnV3Result profitSharingReturnV3(@NotBlank(message = "原分账单号不能为空") String outOrderNo,
                                                             @NotBlank(message = "回退单号不能为空") String outReturnNo,
                                                             @NotBlank(message = "回退方商户号不能为空") String returnMchid,
                                                             Integer amount,
                                                             String description) throws Exception {
        log.info("开始分账回退V3，子商户：{}，原分账单号：{}，回退单号：{}，回退金额：{}分", subMchId, outOrderNo, outReturnNo, amount);

        try {
            ProfitSharingService profitSharingService = wxPayService.getProfitSharingService();
            
            ProfitSharingReturnV3Request request = new ProfitSharingReturnV3Request();
            request.setOutOrderNo(outOrderNo);
            request.setOutReturnNo(outReturnNo);
            request.setReturnMchid(returnMchid);
            request.setAmount(Long.valueOf(amount));
            request.setDescription(description != null ? description : "分账回退");

            ProfitSharingReturnV3Result result = profitSharingService.profitSharingReturnV3(request);
            log.info("分账回退V3成功：{}", result);
            return result;

        } catch (Exception e) {
            log.error("分账回退V3失败：{}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 查询分账回退结果 (传统API)
     *
     * @param outOrderNo 原分账单号
     * @param outReturnNo 回退单号
     * @return 查询结果
     * @throws Exception 查询异常
     */
    public ProfitSharingReturnResult profitSharingReturnQuery(@NotBlank(message = "原分账单号不能为空") String outOrderNo,
                                                              @NotBlank(message = "回退单号不能为空") String outReturnNo) throws Exception {
        log.info("开始查询分账回退结果，子商户：{}，原分账单号：{}，回退单号：{}", subMchId, outOrderNo, outReturnNo);

        try {
            ProfitSharingService profitSharingService = wxPayService.getProfitSharingService();
            
            ProfitSharingReturnQueryRequest request = new ProfitSharingReturnQueryRequest();
            request.setSubMchId(subMchId);
            request.setOutOrderNo(outOrderNo);
            request.setOutReturnNo(outReturnNo);

            ProfitSharingReturnResult result = profitSharingService.profitSharingReturnQuery(request);
            log.info("查询分账回退结果成功：{}", result);
            return result;

        } catch (Exception e) {
            log.error("查询分账回退结果失败：{}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 查询分账回退结果API (V3商户平台)
     *
     * @param outOrderNo 原分账单号
     * @param outReturnNo 回退单号
     * @return 查询结果
     * @throws Exception 查询异常
     */
    public ProfitSharingReturnV3Result profitSharingReturnQueryV3(@NotBlank(message = "原分账单号不能为空") String outOrderNo,
                                                                  @NotBlank(message = "回退单号不能为空") String outReturnNo) throws Exception {
        log.info("开始查询分账回退结果V3，原分账单号：{}，回退单号：{}", outOrderNo, outReturnNo);

        try {
            ProfitSharingService profitSharingService = wxPayService.getProfitSharingService();
            
            ProfitSharingReturnV3Result result = profitSharingService.profitSharingReturnQueryV3(outOrderNo, outReturnNo);
            log.info("查询分账回退结果V3成功：{}", result);
            return result;

        } catch (Exception e) {
            log.error("查询分账回退结果V3失败：{}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 查询分账回退结果API (V3服务商平台)
     *
     * @param outOrderNo 原分账单号
     * @param outReturnNo 回退单号
     * @param subMchId 子商户号
     * @return 查询结果
     * @throws Exception 查询异常
     */
    public ProfitSharingReturnV3Result profitSharingReturnQueryV3ForPartner(@NotBlank(message = "原分账单号不能为空") String outOrderNo,
                                                                            @NotBlank(message = "回退单号不能为空") String outReturnNo,
                                                                            @NotBlank(message = "子商户号不能为空") String subMchId) throws Exception {
        log.info("开始查询分账回退结果V3服务商，原分账单号：{}，回退单号：{}，子商户：{}", outOrderNo, outReturnNo, subMchId);

        try {
            ProfitSharingService profitSharingService = wxPayService.getProfitSharingService();
            
            ProfitSharingReturnV3Result result = profitSharingService.profitSharingReturnQueryV3(outOrderNo, outReturnNo, subMchId);
            log.info("查询分账回退结果V3服务商成功：{}", result);
            return result;

        } catch (Exception e) {
            log.error("查询分账回退结果V3服务商失败：{}", e.getMessage(), e);
            throw e;
        }
    }

    // ==================== 分账比例查询 ====================

    /**
     * 查询最大分账比例 (传统API)
     *
     * @param targetSubMchId 目标子商户号
     * @return 查询结果
     * @throws Exception 查询异常
     */
    public ProfitSharingMerchantRatioQueryResult profitSharingMerchantRatioQuery(@NotBlank(message = "子商户号不能为空") String targetSubMchId) throws Exception {
        log.info("开始查询最大分账比例，子商户：{}", targetSubMchId);

        try {
            ProfitSharingService profitSharingService = wxPayService.getProfitSharingService();
            
            ProfitSharingMerchantRatioQueryRequest request = new ProfitSharingMerchantRatioQueryRequest();
            request.setSubMchId(targetSubMchId);

            ProfitSharingMerchantRatioQueryResult result = profitSharingService.profitSharingMerchantRatioQuery(request);
            log.info("查询最大分账比例成功：{}", result);
            return result;

        } catch (Exception e) {
            log.error("查询最大分账比例失败：{}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 查询最大分账比例 (V3 API)
     *
     * @param targetSubMchId 目标子商户号
     * @return 查询结果
     * @throws Exception 查询异常
     */
    public ProfitSharingMerchantRatioQueryV3Result profitSharingMerchantRatioQueryV3(@NotBlank(message = "子商户号不能为空") String targetSubMchId) throws Exception {
        log.info("开始查询最大分账比例V3，子商户：{}", targetSubMchId);

        try {
            ProfitSharingService profitSharingService = wxPayService.getProfitSharingService();
            
            ProfitSharingMerchantRatioQueryV3Result result = profitSharingService.profitSharingMerchantRatioQueryV3(targetSubMchId);
            log.info("查询最大分账比例V3成功：{}", result);
            return result;

        } catch (Exception e) {
            log.error("查询最大分账比例V3失败：{}", e.getMessage(), e);
            throw e;
        }
    }

    // ==================== 分账账单 ====================

    /**
     * 申请分账账单 (V3 API)
     *
     * @param billDate 账单日期 (格式：YYYY-MM-DD)
     * @param tarType 压缩类型
     * @return 账单结果
     * @throws Exception 申请异常
     */
    public ProfitSharingBillV3Result profitSharingBill(@NotBlank(message = "账单日期不能为空") String billDate,
                                                       String tarType) throws Exception {
        log.info("开始申请分账账单，子商户：{}，账单日期：{}", subMchId, billDate);

        try {
            ProfitSharingService profitSharingService = wxPayService.getProfitSharingService();
            
            ProfitSharingBillV3Request request = new ProfitSharingBillV3Request();
            request.setBillDate(billDate);
            request.setTarType(tarType != null ? tarType : "GZIP");

            ProfitSharingBillV3Result result = profitSharingService.profitSharingBill(request);
            log.info("申请分账账单成功：{}", result);
            return result;

        } catch (Exception e) {
            log.error("申请分账账单失败：{}", e.getMessage(), e);
            throw e;
        }
    }

    // ==================== 分账通知处理 ====================

    /**
     * 解析分账动账通知
     *
     * @param notifyData 通知数据
     * @param header 签名头
     * @return 通知结果
     * @throws Exception 解析异常
     */
    public ProfitSharingNotifyV3Result parseProfitSharingNotifyResult(@NotBlank(message = "通知数据不能为空") String notifyData,
                                                                      SignatureHeader header) throws Exception {
        log.info("开始解析分账动账通知");

        try {
            ProfitSharingService profitSharingService = wxPayService.getProfitSharingService();
            
            ProfitSharingNotifyV3Result result = profitSharingService.parseProfitSharingNotifyResult(notifyData, header);
            log.info("解析分账动账通知成功：{}", result);
            return result;

        } catch (Exception e) {
            log.error("解析分账动账通知失败：{}", e.getMessage(), e);
            throw e;
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 生成商户分账单号
     *
     * @return 分账单号
     */
    private String generateOutOrderNo() {
        return "PS" + System.currentTimeMillis();
    }

    /**
     * 掩码处理账号信息（保护隐私）
     *
     * @param account 原始账号
     * @return 掩码后的账号
     */
    private String maskAccount(String account) {
        if (account == null || account.length() <= 4) {
            return account;
        }
        return account.substring(0, 2) + "****" + account.substring(account.length() - 2);
    }

    /**
     * 创建微信支付服务实例 (4.7.0版本配置)
     *
     * @return 配置完成的WxPayService实例
     */
    private WxPayService createWxPayService() {
        WxPayService service = new WxPayServiceImpl();
        WxPayConfig config = new WxPayConfig();

        // 基础配置
        config.setMchId(mchId);
        config.setApiV3Key(apiV3Key);

        // 4.7.0版本推荐的证书配置方式
        if (privateKeyPath != null && new File(privateKeyPath).exists()) {
            config.setPrivateKeyPath(privateKeyPath);
        } else {
            log.warn("私钥文件不存在：{}", privateKeyPath);
        }

        if (privateCertPath != null && new File(privateCertPath).exists()) {
            config.setPrivateCertPath(privateCertPath);
        } else {
            log.warn("证书文件不存在：{}", privateCertPath);
        }

        // 4.7.0版本支持证书序列号配置
        if (certSerialNo != null && !certSerialNo.trim().isEmpty()) {
            config.setCertSerialNo(certSerialNo);
        }

        config.setUseSandboxEnv(false);  // 生产环境

        service.setConfig(config);

        log.debug("微信支付服务配置完成，商户号：{}", mchId);
        return service;
    }

    /**
     * 获取微信支付服务配置（与示例代码保持一致）
     *
     * @return 配置完成的WxPayService实例
     */
    WxPayService getWxPayServiceConfig() {
        WxPayService wxPayService = new WxPayServiceImpl();
        WxPayConfig wxPayConfig = new WxPayConfig();
        wxPayConfig.setApiV3Key(apiV3Key);
        wxPayConfig.setMchId(mchId);
        wxPayConfig.setPrivateKeyPath(privateKeyPath);
        wxPayConfig.setPrivateCertPath(privateCertPath);
        wxPayService.setConfig(wxPayConfig);
        return wxPayService;
    }

    /**
     * 测试分账服务连接
     *
     * @return 连接状态
     */
    public boolean testConnection() {
        try {
            WxPayService service = getWxPayServiceConfig();
            log.info("分账服务连接测试成功");
            return service != null;
        } catch (Exception e) {
            log.error("分账服务连接测试失败：{}", e.getMessage(), e);
            return false;
        }
    }
}