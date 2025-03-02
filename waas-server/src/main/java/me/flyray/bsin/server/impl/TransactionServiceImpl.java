package me.flyray.bsin.server.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.binarywang.wxpay.bean.order.WxPayMpOrderResult;
import com.github.binarywang.wxpay.bean.profitsharing.request.ProfitSharingRequest;
import com.github.binarywang.wxpay.bean.request.WxPayRefundRequest;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderRequest;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderV3Request;
import com.github.binarywang.wxpay.bean.result.WxPayRefundResult;
import com.github.binarywang.wxpay.bean.result.enums.TradeTypeEnum;
import com.github.binarywang.wxpay.bean.transfer.TransferBatchesRequest;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.constant.WxPayConstants;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.ProfitSharingService;
import com.github.binarywang.wxpay.service.TransferService;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.constants.ResponseCode;
import me.flyray.bsin.context.BsinServiceContext;
import me.flyray.bsin.domain.entity.*;
import me.flyray.bsin.domain.enums.PayWayEnum;
import me.flyray.bsin.domain.enums.TransactionStatus;
import me.flyray.bsin.domain.request.TransactionDTO;
import me.flyray.bsin.domain.request.TransactionRequest;
import me.flyray.bsin.enums.TransactionType;
import me.flyray.bsin.exception.BusinessException;
import me.flyray.bsin.facade.service.BizRoleAppService;
import me.flyray.bsin.facade.service.TransactionService;
import me.flyray.bsin.infrastructure.mapper.*;
import me.flyray.bsin.mybatis.utils.Pagination;
import me.flyray.bsin.payment.BsinWxPayServiceUtil;
import me.flyray.bsin.server.listen.ChainTransactionListen;
import me.flyray.bsin.infrastructure.biz.TransactionBiz;
import me.flyray.bsin.security.contex.LoginInfoContextHelper;
import me.flyray.bsin.security.domain.LoginUser;
import me.flyray.bsin.utils.BsinSnowflake;
import me.flyray.bsin.utils.StringUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.shenyu.client.apache.dubbo.annotation.ShenyuDubboService;
import org.apache.shenyu.client.apidocs.annotations.ApiDoc;
import org.apache.shenyu.client.apidocs.annotations.ApiModule;
import org.apache.shenyu.client.dubbo.common.annotation.ShenyuDubboClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

import static me.flyray.bsin.constants.ResponseCode.NOT_SUPPORTED_PAY_WAY;
import static me.flyray.bsin.utils.Utils.ObjectToMapConverter;

/**
* @author Admin
* @description 针对表【crm_transaction(交易记录;)】的数据库操作Service实现
* @createDate 2024-04-24 20:36:00
*/


@Slf4j
@ApiModule(value = "transaction")
@ShenyuDubboService(value = "/transaction" ,timeout = 5000)
@Transactional(rollbackFor = Exception.class)
public class TransactionServiceImpl  implements TransactionService {

    @Value("${wx.pay.callbackUrl}")
    private String wxCallbackUrl;

    @Autowired
    private BsinWxPayServiceUtil bsinWxPayServiceUtil;
    @Autowired private PayChannelConfigMapper payChannelConfigMapper;
    @Autowired private TransactionJournalMapper transactionJournalMapper;

    @DubboReference(version = "${dubbo.provider.version}")
    private BizRoleAppService bizRoleAppService;

    @Autowired
    private TransactionMapper transactionMapper;
    @Autowired
    private TransactionAuditMapper transactionAuditMapper;
    @Autowired
    private ChainCoinMapper chainCoinMapper;
    @Autowired
    private WalletAccountMapper walletAccountMapper;
    @Autowired
    private ChainTransactionListen transactionBiz;
    @Autowired
    private TransactionBiz transferBiz;

    @Override
    public Map<String, Object> pay(Map<String, Object> requestMap) {
        LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
        String payWay = MapUtils.getString(requestMap, "payWay");
        String payAmount = MapUtils.getDouble(requestMap, "payAmount").toString();
        String orderNo = MapUtils.getString(requestMap, "orderNo");
        String tenantId = MapUtils.getString(requestMap, "tenantId");
        if (tenantId == null) {
            tenantId = loginUser.getTenantId();
        }
        String merchantNo = MapUtils.getString(requestMap, "merchantNo");
        if (merchantNo == null) {
            merchantNo = loginUser.getMerchantNo();
        }
        String customerNo = MapUtils.getString(requestMap, "customerNo");
        if (customerNo == null) {
            customerNo = loginUser.getCustomerNo();
        }
        String notifyUrl = MapUtils.getString(requestMap, "notifyUrl");
        // 小程序ID
        String appId = MapUtils.getString(requestMap, "appId");
        // 商戶应用ID
        String bizRoleAppId = MapUtils.getString(requestMap, "appId");
        String remark = MapUtils.getString(requestMap, "remark");
        if (payWay.isEmpty() || payAmount.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR);
        }

        // 1.创建交易订单
        Transaction waasTransaction =
                transactionMapper.selectOne(
                        new LambdaQueryWrapper<Transaction>().eq(Transaction::getOutSerialNo, orderNo));
        // 订单已支付成功,直接返回
        if (waasTransaction != null
                && TransactionStatus.SUCCESS.getCode().equals(waasTransaction.getTransactionStatus())) {
            requestMap.put("payResult", "success");
            return requestMap;
        } else if (waasTransaction == null) {
            waasTransaction = new Transaction();
            waasTransaction.setSerialNo(BsinSnowflake.getId());
            waasTransaction.setOutSerialNo(orderNo);
            waasTransaction.setTransactionType(TransactionType.PAY.getCode());
            waasTransaction.setComment(remark);
            waasTransaction.setTxAmount(new BigDecimal(payAmount));
            waasTransaction.setFromAddress(customerNo);
            waasTransaction.setToAddress(merchantNo);
            waasTransaction.setBizRoleType(loginUser.getBizRoleType());
            waasTransaction.setBizRoleTypeNo(loginUser.getBizRoleTypeNo());
            waasTransaction.setTenantId(tenantId);
            waasTransaction.setCreateTime(new Date());
            waasTransaction.setFromAddress(customerNo);
            waasTransaction.setCreateBy(customerNo);
            transactionMapper.insert(waasTransaction);
        }
        // 2、创建支付转账流水
        TransactionJournal waasTransactionJournal =
                transactionJournalMapper.selectOne(
                        new LambdaQueryWrapper<TransactionJournal>()
                                .eq(TransactionJournal::getTransactionNo, waasTransaction.getSerialNo()));
        if (waasTransactionJournal == null) {
            waasTransactionJournal = new TransactionJournal();
            waasTransactionJournal.setTransactionNo(waasTransaction.getSerialNo());
            waasTransactionJournal.setPayAmount(new BigDecimal(payAmount));
            waasTransactionJournal.setSerialNo(BsinSnowflake.getId());
            waasTransactionJournal.setStatus(TransactionStatus.PENDING.getCode());
            transactionJournalMapper.insert(waasTransactionJournal);
        }

        WxPayMpOrderResult wxPayMpOrderResult = new WxPayMpOrderResult();
        // 3、支付
        if (PayWayEnum.WXPAY.getCode().equals(payWay)) {
            String openId = MapUtils.getString(requestMap, "openId");
            if (StringUtils.isEmpty(openId)) {
                throw new BusinessException(ResponseCode.OPEN_ID_NOT_EXISTS);
            }
            Double deciPrice = Double.parseDouble(payAmount) * 100;
            // 支付配置应用: 从商户应用配置的支付应用中获取
            LambdaQueryWrapper<PayChannelConfig> warapper = new LambdaQueryWrapper<>();
            warapper.eq(PayChannelConfig::getBizRoleAppId, bizRoleAppId);
            warapper.orderByDesc(PayChannelConfig::getCreateTime);
            PayChannelConfig payChannelConfig = payChannelConfigMapper.selectOne(warapper);
            if (payChannelConfig == null) {
                throw new BusinessException(ResponseCode.PAY_CHANNEL_CONFIG_NOT_EXIST);
            }
            JSONObject payChannelConfigParams = JSONObject.parseObject(payChannelConfig.getParams());

            String apiVersion = payChannelConfigParams.getString("apiVersion");
            String key = payChannelConfigParams.getString("key");
            String keyPath = payChannelConfigParams.getString("keyPath");
            appId = payChannelConfigParams.getString("appId");
            String mchId = payChannelConfigParams.getString("mchId");
            notifyUrl = payChannelConfigParams.getString("notifyUrl");

            WxPayConfig wxPayConfig = new WxPayConfig();
            wxPayConfig.setAppId(appId);
            wxPayConfig.setMchId(mchId);
            wxPayConfig.setMchKey(key);
            wxPayConfig.setSignType(WxPayConstants.SignType.MD5);
            String apiV3Key = payChannelConfigParams.getString("apiV3Key");
            wxPayConfig.setApiV3Key(apiV3Key);
            wxPayConfig.setNotifyUrl(notifyUrl);
            wxPayConfig.setKeyPath(keyPath);
            wxPayConfig.setPrivateKeyPath(keyPath);
            //      wxPayConfig.setCertSerialNo(certSerialNo);
            //      wxPayConfig.setPrivateKeyContent(
            //          payChannelConfigParams.getString("privateKey").getBytes(StandardCharsets.UTF_8));
            //      wxPayConfig.setPrivateCertString(payChannelConfigParams.getString("privateCert"));
            wxPayConfig.setUseSandboxEnv(false);
            WxPayService wxPayService = bsinWxPayServiceUtil.getWxPayService(wxPayConfig);
            try {
                if ("V3".equals(apiVersion)) {
                    // 统一下单 V3
                    WxPayUnifiedOrderV3Request wxPayUnifiedOrderV3Request = new WxPayUnifiedOrderV3Request();
                    wxPayMpOrderResult =
                            wxPayService.createOrderV3(TradeTypeEnum.APP, wxPayUnifiedOrderV3Request);
                    log.info("传递的参数{}", wxPayUnifiedOrderV3Request);
                    return ObjectToMapConverter(wxPayMpOrderResult);
                } else {
                    // 统一下单 V2
                    WxPayUnifiedOrderRequest wxPayRequest = new WxPayUnifiedOrderRequest();
                    wxPayRequest.setAppid(appId);
                    wxPayRequest.setMchId(mchId);
                    // 订单备注
                    wxPayRequest.setBody(remark);
                    wxPayRequest.setDetail(MapUtils.getString(requestMap, "detail"));
                    wxPayRequest.setOutTradeNo(orderNo);
                    wxPayRequest.setTotalFee(deciPrice.intValue());
                    wxPayRequest.setSpbillCreateIp("127.0.0.1");
                    // ! 微信收到后的回调地址，会自动回调该地址： ？？ 是否需要配置在app
                    //      wxPayRequest.setNotifyUrl(wxCallbackUrl);
//                    notifyUrl = payChannelConfigParams.getString("notifyUrl");
                    wxPayRequest.setNotifyUrl(notifyUrl);
                    // 小程序支付统一下单接口：
                    wxPayRequest.setTradeType(WxPayConstants.TradeType.JSAPI);
                    wxPayRequest.setOpenid(openId);
                    wxPayMpOrderResult = wxPayService.createOrder(wxPayRequest);
                    log.info("传递的参数{}", wxPayRequest);
                    return ObjectToMapConverter(wxPayMpOrderResult);
                }
            } catch (WxPayException e) {
                e.printStackTrace();
                //        log.info("支付异常{}", e);
                throw new BusinessException("100000", "微信支付创建订单失败：" + e.getMessage());
            }
            // 火源支付
        } else if (PayWayEnum.FIRE_DIAMOND.getCode().equals(payWay)) {
            //      // 查询订单金额
            //      UniflyOrder uniflyOrder = orderMapper.selectById(orderNo);
            //      // 查询用户余额进行扣除
            //      Map reqMap = new HashMap();
            //      reqMap.put("customerNo", customerNo);
            //      reqMap.put("ccy", CcyType.CNY.getCode());
            //      reqMap.put("category", AccountCategory.BALANCE.getCode());
            //      // TODO 需要后端根据兑换积分数量计算
            //      reqMap.put("amount", uniflyOrder.getPayAmount());
            //      reqMap.put("decimals", "2");
            //      customerAccountService.outAccount(reqMap);
            //      // 更新订单状态
            //      uniflyOrder.setPayTime(new Date());
            //      uniflyOrder.setPayStatus("20");
            //      orderMapper.updateById(uniflyOrder);
        }
        // 品牌积分支付
        else if (PayWayEnum.BRAND_POINT.getCode().equals(payWay)) {
            //      // 查询订单金额
            //      UniflyOrder uniflyOrder = orderMapper.selectById(orderNo);
            //      // 查询商户的品牌积分币种和小数位数
            //      Map<String, Object> tokenReq = new HashMap();
            //      tokenReq.put("merchantNo", uniflyOrder.getMerchantNo());
            //      TokenParam tokenParamMap = tokenParamService.getDetailByMerchantNo(tokenReq);
            //      Account accountDetail = null;
            //      String ccy = tokenParamMap.getSymbol();
            //      Integer decimals = (Integer) tokenParamMap.getDecimals();
            //
            //      Map reqMap = new HashMap();
            //      reqMap.put("customerNo", customerNo);
            //      reqMap.put("ccy", ccy);
            //      reqMap.put("category", AccountCategory.BALANCE.getCode());
            //      // TODO 需要后端根据兑换积分数量计算
            //      reqMap.put("amount", uniflyOrder.getPayAmount());
            //      reqMap.put("decimals", decimals);
            //      customerAccountService.outAccount(reqMap);
            //      // 更新订单状态
            //      uniflyOrder.setPayTime(new Date());
            //      uniflyOrder.setPayStatus("20");
            //      orderMapper.updateById(uniflyOrder);
        } else {
            throw new BusinessException(NOT_SUPPORTED_PAY_WAY);
        }
        return requestMap;
    }

    @Override
    public Transaction recharge(Map<String, Object> requestMap) {
        return null;
    }

    /**
     * 处理各种类型转账交易
     * @param requestMap
     * @return
     */
    @Override
    public Transaction transfer(Map<String, Object> requestMap) {
        WxPayConfig wxPayConfig = new WxPayConfig();

        TransferService wxTransferService = bsinWxPayServiceUtil.getWxTransferService(wxPayConfig);

        TransferBatchesRequest transferBatchesRequest = new TransferBatchesRequest();

        try {
            wxTransferService.transferBatches(transferBatchesRequest);
        } catch (WxPayException e) {
            e.printStackTrace();
            //        log.info("支付异常{}", e);
            throw new BusinessException("100000", "微信转账创建订单失败：" + e.getMessage());
        }
        return null;
    }

    @Override
    public Transaction withdraw(Map<String, Object> requestMap) {
        return null;
    }

    @ApiDoc(desc = "withdrawApply")
    @ShenyuDubboClient("/withdrawApply")
    @Override
    public Transaction withdrawApply(Map<String, Object> requestMap) {
        LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
        Transaction transaction = BsinServiceContext.getReqBodyDto(Transaction.class, requestMap);
        if(TransactionType.getInstanceById(transaction.getTransactionType()) == null){
            throw new BusinessException("999","交易类型不存在！");
        }
        transaction.setTenantId(loginUser.getTenantId());
        transaction.setFromAddress(loginUser.getBizRoleTypeNo());
        transaction.setAuditStatus("1");
        transactionMapper.insert(transaction);
        return transaction;
    }

    @ApiDoc(desc = "withdrawAudit")
    @ShenyuDubboClient("/withdrawAudit")
    @Override
    public void withdrawAudit(Map<String, Object> requestMap) {
        Transaction transaction = BsinServiceContext.getReqBodyDto(Transaction.class, requestMap);
        transactionMapper.updateById(transaction);
    }

    @Override
    public Transaction refund(Map<String, Object> requestMap) {
        String bizRoleAppId = MapUtils.getString(requestMap, "appId");
        // 支付配置应用: 从商户应用配置的支付应用中获取
        LambdaQueryWrapper<PayChannelConfig> warapper = new LambdaQueryWrapper<>();
        warapper.eq(PayChannelConfig::getBizRoleAppId, bizRoleAppId);
        warapper.orderByDesc(PayChannelConfig::getCreateTime);
        PayChannelConfig payChannelConfig = payChannelConfigMapper.selectOne(warapper);
        if (payChannelConfig == null) {
            throw new BusinessException(ResponseCode.PAY_CHANNEL_CONFIG_NOT_EXIST);
        }
        JSONObject payChannelConfigParams = JSONObject.parseObject(payChannelConfig.getParams());

        String apiVersion = payChannelConfigParams.getString("apiVersion");
        String key = payChannelConfigParams.getString("key");
        String keyPath = payChannelConfigParams.getString("keyPath");
        String appId = payChannelConfigParams.getString("appId");
        String mchId = payChannelConfigParams.getString("mchId");

        WxPayConfig wxPayConfig = new WxPayConfig();
        wxPayConfig.setAppId(appId);
        wxPayConfig.setMchId(mchId);
        wxPayConfig.setMchKey(key);
        wxPayConfig.setSignType(WxPayConstants.SignType.MD5);

        WxPayService wxPayService = bsinWxPayServiceUtil.getWxPayService(wxPayConfig);
        WxPayRefundRequest request = new WxPayRefundRequest();
        try {
            WxPayRefundResult wxPayRefundResult = wxPayService.refund(request);
        }catch (WxPayException e) {
            e.printStackTrace();
            //        log.info("支付异常{}", e);
            throw new BusinessException("100000", "微信分账创建订单失败：" + e.getMessage());
        }

        return null;
    }

    @Override
    public Transaction settlement(Map<String, Object> requestMap) {
        return null;
    }

    @Override
    public Transaction income(Map<String, Object> requestMap) {
        return null;
    }

    @Override
    public Transaction redeem(Map<String, Object> requestMap) {
        return null;
    }

    @Override
    @ShenyuDubboClient("/create")
    @ApiDoc(desc = "create")
    public void create(TransactionRequest transactionRequest) {
        log.debug("请求TransactionService.createTransaction,参数:{}", transactionRequest);
        LoginUser user = LoginInfoContextHelper.getLoginUser();
        try{
            // 查询币种是否支持
            QueryWrapper<ChainCoin> chainCoinQueryWrapper = new QueryWrapper<>();
            chainCoinQueryWrapper.eq("chain_coin_key", transactionRequest.getChainCoinKey());
            chainCoinQueryWrapper.eq("status", 1);  // 已上架
            ChainCoin chainCoin = chainCoinMapper.selectOne(chainCoinQueryWrapper);
            if(chainCoin == null){
                throw new BusinessException("CHAIN_COIN_NOT_EXIST");
            }
            // 查询用户账户状态是否正常
            QueryWrapper<WalletAccount> walletAccountQueryWrapper = new QueryWrapper<>();
            walletAccountQueryWrapper.eq("address",transactionRequest.getToAddress());
            walletAccountQueryWrapper.eq("chain_coin_no", chainCoin.getSerialNo());
            walletAccountQueryWrapper.eq("tenant_id", transactionRequest.getTenantId());
            WalletAccount walletAccounts = walletAccountMapper.selectOne(walletAccountQueryWrapper);
            if(walletAccounts == null){}

            // 创建交易记录
            String serialNo = BsinSnowflake.getId(); // 雪花算法
            Transaction transaction = new Transaction();
            transaction.setSerialNo(serialNo);
            transaction.setOutSerialNo(transactionRequest.getOutSerialNo());
            transaction.setTransactionType(TransactionType.TRANSFER.getCode());       // 交易类型 2、转出
            transaction.setComment(transactionRequest.getComment());
            transaction.setFromAddress(transactionRequest.getFromAddress());
            transaction.setTxAmount(new BigDecimal(transactionRequest.getTxAmount()));
            transaction.setToAddress(transactionRequest.getToAddress());
            transaction.setBizRoleType(user.getBizRoleType());
            transaction.setBizRoleTypeNo(user.getBizRoleTypeNo());
            transaction.setTenantId(transactionRequest.getTenantId());
            transaction.setCreateTime(new Date());
            transaction.setCreateBy(user.getUserId());
            transactionMapper.insert(transaction);

            // TODO 调用风控方法

            // 风控审核通过，则执行转出动作
            if(true){
                transactionBiz.transferOut();
            }else {
                // 风控拦截交易，进入人工审核
                TransactionAudit transactionAudit = new TransactionAudit();
                transactionAudit.setAuditYpe(1);      // 1、交易转出审核
                transactionAudit.setAuditStatus(1);     // 1、待审核状态
                transactionAudit.setAuditLevel(1);      // 根据风控判断风险等级，暂默认为 1、低级风险
                transactionAudit.setTransactionNo(serialNo);
                transactionAudit.setCreateTime(new Date());
                transactionAudit.setCreateBy(user.getUserId());
                transactionAuditMapper.insert(transactionAudit);
            }
        }catch (BusinessException be){
            throw be;
        }catch (Exception e){
            e.printStackTrace();
            throw new BusinessException("SYSTEM_ERROR");
        }
    }

    /**
     * @param transactionDTO
     * fromAddress
     * toAddress
     * txAmount
     * chainCoinNo
     */
    @Override
    @ShenyuDubboClient("/transferOut")
    @ApiDoc(desc = "transferOut")
    public void transferOut(TransactionDTO transactionDTO) throws Exception {
        log.debug("请求TransactionService.transferOut,参数:{}", transactionDTO);
        String fromAddress = transactionDTO.getFromAddress();
        String toAddress = transactionDTO.getToAddress();
        String contractAddress = transactionDTO.getContractAddress();
        BigDecimal txAmount = transactionDTO.getTxAmount();

        // 查询合约币种
        QueryWrapper<ChainCoin> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.eq("contract_address", contractAddress);
//        queryWrapper.eq("tenant_id",tenant_id);
        ChainCoin chainCoin = chainCoinMapper.selectOne(queryWrapper);

        // 查询转出账户
        QueryWrapper<WalletAccount> walletAccountQueryWrapper = new QueryWrapper();
        walletAccountQueryWrapper.eq("address", fromAddress);
        WalletAccount walletAccount = walletAccountMapper.selectOne(walletAccountQueryWrapper);
        if(walletAccount == null){
            throw new BusinessException("非平台钱包账户地址");
        }
        log.info("开始提现交易，账户余额为：{}",walletAccount.getBalance());
        transferBiz.tokenTransfer(fromAddress,toAddress,contractAddress,txAmount.toBigInteger(), chainCoin.getCoinDecimal());
        BigDecimal balance = walletAccount.getBalance().subtract(txAmount);
        log.info("提现交易结束，账户余额为：{}",balance);
        walletAccount.setBalance(balance);
        walletAccountMapper.updateById(walletAccount);
    }

    @Override
    @ShenyuDubboClient("/getPageList")
    @ApiDoc(desc = "getPageList")
    public Page<Transaction> getPageList(TransactionDTO transactionDTO) {
        log.debug("请求TransactionService.pageList,参数:{}", transactionDTO);
        LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
        Pagination pagination =  transactionDTO.getPagination();
        Page<Transaction> page = new Page<>(pagination.getPageNum(), pagination.getPageSize());
        LambdaQueryWrapper<Transaction> warapper = new LambdaQueryWrapper<>();
        warapper.orderByDesc(Transaction::getCreateTime);
        warapper.eq(Transaction::getTenantId, loginUser.getTenantId());
        warapper.eq(ObjectUtils.isNotEmpty(transactionDTO.getTransactionType()), Transaction::getTransactionType, transactionDTO.getTransactionType());
        return transactionMapper.selectPage(page,warapper);
    }

    @ShenyuDubboClient("/getDetail")
    @ApiDoc(desc = "getDetail")
    @Override
    public Transaction getDetail(Map<String, Object> requestMap) {
        String serialNo = MapUtils.getString(requestMap, "serialNo");
        Transaction transaction = transactionMapper.selectById(serialNo);
        return transaction;
    }

    /**
     * 支付成功后进行分账
     * @param requestMap
     * @return
     */
    @Override
    public Map<String, Object> profitsharing(Map<String, Object> requestMap){

        String bizRoleAppId = MapUtils.getString(requestMap, "appId");
        // 支付配置应用: 从商户应用配置的支付应用中获取
        LambdaQueryWrapper<PayChannelConfig> warapper = new LambdaQueryWrapper<>();
        warapper.eq(PayChannelConfig::getBizRoleAppId, bizRoleAppId);
        warapper.orderByDesc(PayChannelConfig::getCreateTime);
        PayChannelConfig payChannelConfig = payChannelConfigMapper.selectOne(warapper);
        if (payChannelConfig == null) {
            throw new BusinessException(ResponseCode.PAY_CHANNEL_CONFIG_NOT_EXIST);
        }
        JSONObject payChannelConfigParams = JSONObject.parseObject(payChannelConfig.getParams());

        String apiVersion = payChannelConfigParams.getString("apiVersion");
        String key = payChannelConfigParams.getString("key");
        String keyPath = payChannelConfigParams.getString("keyPath");
        String appId = payChannelConfigParams.getString("appId");
        String mchId = payChannelConfigParams.getString("mchId");

        WxPayConfig wxPayConfig = new WxPayConfig();
        wxPayConfig.setAppId(appId);
        wxPayConfig.setMchId(mchId);
        wxPayConfig.setMchKey(key);
        wxPayConfig.setSignType(WxPayConstants.SignType.MD5);

        ProfitSharingService wxProfitSharingService = bsinWxPayServiceUtil.getProfitSharingService(wxPayConfig);
        ProfitSharingRequest profitSharingRequest = new ProfitSharingRequest();
        try {
            wxProfitSharingService.multiProfitSharing(profitSharingRequest);
        }catch (WxPayException e) {
            e.printStackTrace();
            //        log.info("支付异常{}", e);
            throw new BusinessException("100000", "微信分账创建订单失败：" + e.getMessage());
        }
        return null;
    }

    /**
     * 根据支付方式判断处理
     *
     * @param requestMap
     * @return
     */
    @ApiDoc(desc = "queryOrder")
    @ShenyuDubboClient("/queryOrder")
    @Override
    @Transactional
    public Map<String, Object> queryOrder(Map<String, Object> requestMap) {
        //    WxPayConfig wxPayConfig = new WxPayConfig();
        //    wxPayConfig.setAppId(appId);
        //    wxPayConfig.setMchId(mchId);
        //    wxPayConfig.setMchKey(key);
        //    String apiV3Key = payChannelConfigParams.getString("apiV3Key");
        //    wxPayConfig.setApiV3Key(apiV3Key);
        //    wxPayConfig.setNotifyUrl(notifyUrl);
        //    wxPayConfig.setKeyPath(keyPath);
        //    wxPayConfig.setUseSandboxEnv(false);
        //    String outTradeNo = MapUtils.getString(requestMap, "outTradeNo");
        //    WxPayService wxPayService = bsinWxPayServiceUtil.getWxPayService(wxPayConfig);
        //    WxPayOrderQueryResult wxPayOrderQueryResult = wxPayService.queryOrder(null, outTradeNo);
        //    log.info(wxPayOrderQueryResult.toString());
        //    requestMap.put("orderResult", wxPayOrderQueryResult);
        return requestMap;
    }

}




