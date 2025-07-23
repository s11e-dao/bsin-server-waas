package me.flyray.bsin.server.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.binarywang.wxpay.bean.order.WxPayMpOrderResult;
import com.github.binarywang.wxpay.bean.request.WxPayRefundRequest;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderRequest;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderV3Request;
import com.github.binarywang.wxpay.bean.result.WxPayRefundResult;
import com.github.binarywang.wxpay.bean.result.enums.TradeTypeEnum;
import com.github.binarywang.wxpay.bean.transfer.TransferBatchesRequest;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.constant.WxPayConstants;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.TransferService;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.constants.ResponseCode;
import me.flyray.bsin.context.BsinServiceContext;
import me.flyray.bsin.domain.entity.*;
import me.flyray.bsin.domain.enums.PayChannelInterfaceEnum;
import me.flyray.bsin.domain.enums.TransactionStatus;
import me.flyray.bsin.domain.request.TransactionDTO;
import me.flyray.bsin.domain.request.TransactionRequest;
import me.flyray.bsin.domain.response.PayChannelConfigParamDTO;
import me.flyray.bsin.enums.TransactionType;
import me.flyray.bsin.exception.BusinessException;
import me.flyray.bsin.facade.engine.RevenueShareServiceEngine;
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
@ShenyuDubboService(value = "/transaction", timeout = 5000)
@Transactional(rollbackFor = Exception.class)
public class TransactionServiceImpl implements TransactionService {

  // 常量定义
  private static final String PRODUCT_PROFIT_SHARING_TYPE = "2";
  private static final String WX_PAY_API_V3 = "V3";
  private static final String PAY_RESULT_SUCCESS = "success";
  private static final String DEFAULT_IP = "127.0.0.1";
  private static final int PRICE_MULTIPLY_FACTOR = 100;

  @Value("${wx.pay.callbackUrl}")
  private String wxCallbackUrl;

  @Autowired private BsinWxPayServiceUtil bsinWxPayServiceUtil;
  @Autowired private TransactionJournalMapper transactionJournalMapper;
  @Autowired private TransactionMapper transactionMapper;
  @Autowired private TransactionAuditMapper transactionAuditMapper;
  @Autowired private ChainCoinMapper chainCoinMapper;
  @Autowired private WalletAccountMapper walletAccountMapper;
  @Autowired private ChainTransactionListen chainTransactionListen;
  @Autowired private TransactionBiz transactionBiz;
  @Autowired private RevenueShareServiceEngine revenueShareServiceEngine;

  /**
   * 创建交易订单
   *
   * @param requestMap 创建交易请求参数
   * @return 交易创建结果
   */
  @Override
  public Map<String, Object> create(Map<String, Object> requestMap) {
    log.info("开始创建交易订单");

    // 获取登录用户信息
    LoginUser loginUser = LoginInfoContextHelper.getLoginUser();

    // 提取请求参数
    String payWay = MapUtils.getString(requestMap, "payWay");
    Double payAmountDouble = MapUtils.getDouble(requestMap, "payAmount");
    String outOrderNo = MapUtils.getString(requestMap, "outOrderNo");

    // 参数验证
    if (StringUtils.isEmpty(payWay) || payAmountDouble == null || StringUtils.isEmpty(outOrderNo)) {
      log.warn("创建交易失败：必填参数缺失");
      throw new BusinessException(ResponseCode.PARAM_ERROR);
    }

    String payAmount = payAmountDouble.toString();

    // 设置默认值
    String tenantId =
        StringUtils.isEmpty(MapUtils.getString(requestMap, "tenantId"))
            ? loginUser.getTenantId()
            : MapUtils.getString(requestMap, "tenantId");
    String merchantNo =
        StringUtils.isEmpty(MapUtils.getString(requestMap, "merchantNo"))
            ? loginUser.getMerchantNo()
            : MapUtils.getString(requestMap, "merchantNo");
    String customerNo =
        StringUtils.isEmpty(MapUtils.getString(requestMap, "customerNo"))
            ? loginUser.getCustomerNo()
            : MapUtils.getString(requestMap, "customerNo");

    String notifyUrl = MapUtils.getString(requestMap, "notifyUrl");
    String appId = MapUtils.getString(requestMap, "appId");
    String bizRoleAppId = MapUtils.getString(requestMap, "appId");
    String remark = MapUtils.getString(requestMap, "remark");

    // 1. 创建或获取交易订单（包含分账配置处理）
    String profitSharingType = MapUtils.getString(requestMap, "profitSharingType");
    String profitSharingAmountStr = MapUtils.getString(requestMap, "profitSharingAmount");

    Transaction waasTransaction =
        transactionBiz.createOrGetTransactionWithProfitSharing(
            outOrderNo,
            payAmount,
            loginUser,
            tenantId,
            merchantNo,
            customerNo,
            remark,
            profitSharingType,
            profitSharingAmountStr);

    // 如果订单已支付成功，直接返回
    if (waasTransaction != null
        && TransactionStatus.SUCCESS.getCode().equals(waasTransaction.getTransactionStatus())) {
      log.info("订单已支付成功，直接返回，outOrderNo: {}", outOrderNo);
      requestMap.put("payResult", PAY_RESULT_SUCCESS);
      return requestMap;
    }
    // 2. 创建支付流水记录
    // 查询是否已存在支付流水，避免重复创建
    TransactionJournal waasTransactionJournal =
        transactionJournalMapper.selectOne(
            new LambdaQueryWrapper<TransactionJournal>()
                .eq(TransactionJournal::getTransactionNo, waasTransaction.getSerialNo()));

    if (waasTransactionJournal == null) {
      // 创建新的支付流水记录
      waasTransactionJournal = new TransactionJournal();
      waasTransactionJournal.setTransactionNo(waasTransaction.getSerialNo());
      waasTransactionJournal.setPayAmount(new BigDecimal(payAmount));
      waasTransactionJournal.setSerialNo(BsinSnowflake.getId());
      waasTransactionJournal.setStatus(TransactionStatus.PENDING.getCode());
      transactionJournalMapper.insert(waasTransactionJournal);
      log.info("支付流水记录创建成功，journalNo: {}", waasTransactionJournal.getSerialNo());
    }

    WxPayMpOrderResult wxPayMpOrderResult = new WxPayMpOrderResult();

    // 3. 支付处理
    if (PayChannelInterfaceEnum.WXPAY.getCode().equals(payWay)) {
      log.info("开始处理微信支付，outOrderNo: {}", outOrderNo);

      // 验证微信支付必填参数
      String openId = MapUtils.getString(requestMap, "openId");
      if (StringUtils.isEmpty(openId)) {
        log.error("微信支付失败：openId不能为空");
        throw new BusinessException(ResponseCode.OPEN_ID_NOT_EXISTS);
      }

      // 计算支付金额（转换为分）
      Integer deciPrice = (int) (Double.parseDouble(payAmount) * PRICE_MULTIPLY_FACTOR);

      // 获取支付渠道配置参数
      String configParamsStr = null;
      PayChannelConfig payChannelConfig =
          transactionBiz.getPayChannelConfigParamsString(bizRoleAppId);
      if (payChannelConfig == null) {
        log.error("支付渠道配置参数不存在");
        throw new BusinessException("支付渠道配置参数不存在");
      }
      if (payChannelConfig.getIsNormalMerchantMode()) {
        log.debug("普通商户模式");
        configParamsStr = payChannelConfig.getNormalMerchantParams();
      } else if (payChannelConfig.getIsIsvSubMerchantMode()) {
        log.debug("服务商子商户模式");
        configParamsStr = payChannelConfig.getIsvParams();
      } else {
        log.debug("特殊商户模式");
        configParamsStr = payChannelConfig.getSpecialMerchantParams();
      }
      JSONObject payChannelConfigParams = JSONObject.parseObject(configParamsStr);

      // 提取微信支付配置参数
      String apiVersion = payChannelConfigParams.getString("apiVersion");
      String key = payChannelConfigParams.getString("key");
      String keyPath = payChannelConfigParams.getString("keyPath");
      appId = payChannelConfigParams.getString("appId");
      String mchId = payChannelConfigParams.getString("mchId");
      notifyUrl = payChannelConfigParams.getString("notifyUrl");

      // 构建微信支付配置对象
      WxPayConfig wxPayConfig = new WxPayConfig();
      wxPayConfig.setAppId(appId);
      wxPayConfig.setMchId(mchId);
      wxPayConfig.setMchKey(key);
      wxPayConfig.setSignType(WxPayConstants.SignType.MD5);
      wxPayConfig.setApiV3Key(payChannelConfigParams.getString("apiV3Key"));
      wxPayConfig.setNotifyUrl(notifyUrl);
      wxPayConfig.setKeyPath(keyPath);
      wxPayConfig.setPrivateKeyPath(keyPath);
      wxPayConfig.setUseSandboxEnv(false);

      // 创建微信支付服务实例
      WxPayService wxPayService = bsinWxPayServiceUtil.getWxPayService(wxPayConfig);

      try {
        if (WX_PAY_API_V3.equals(apiVersion)) {
          // 使用微信支付 API V3 创建订单
          WxPayUnifiedOrderV3Request wxPayUnifiedOrderV3Request = new WxPayUnifiedOrderV3Request();
          wxPayMpOrderResult =
              wxPayService.createOrderV3(TradeTypeEnum.APP, wxPayUnifiedOrderV3Request);
          log.info("微信支付V3统一下单成功，请求参数: {}", wxPayUnifiedOrderV3Request);
          return ObjectToMapConverter(wxPayMpOrderResult);
        } else {
          // 使用微信支付 API V2 创建订单
          WxPayUnifiedOrderRequest wxPayRequest = new WxPayUnifiedOrderRequest();
          wxPayRequest.setAppid(appId);
          wxPayRequest.setMchId(mchId);
          wxPayRequest.setBody(remark); // 商品描述
          wxPayRequest.setDetail(MapUtils.getString(requestMap, "detail")); // 商品详情
          wxPayRequest.setOutTradeNo(outOrderNo); // 商户订单号
          wxPayRequest.setTotalFee(deciPrice); // 支付金额（分）
          wxPayRequest.setSpbillCreateIp(DEFAULT_IP); // 用户IP
          wxPayRequest.setNotifyUrl(notifyUrl); // 异步通知地址
          wxPayRequest.setTradeType(WxPayConstants.TradeType.JSAPI); // 小程序支付
          wxPayRequest.setOpenid(openId); // 用户OpenID

          wxPayMpOrderResult = wxPayService.createOrder(wxPayRequest);
          log.info("微信支付V2统一下单成功，请求参数: {}", wxPayRequest);
          return ObjectToMapConverter(wxPayMpOrderResult);
        }
      } catch (WxPayException e) {
        log.error("微信支付创建订单失败，outOrderNo: {}, 错误信息: {}", outOrderNo, e.getMessage(), e);
        throw new BusinessException("100000", "微信支付创建订单失败：" + e.getMessage());
      }
    } else if (PayChannelInterfaceEnum.FIRE_DIAMOND.getCode().equals(payWay)) {
      // 火源支付 - 基于平台火钻进行支付
      log.info("开始处理火源支付，outOrderNo: {}", outOrderNo);
      // TODO: 实现火源支付逻辑 - 包括余额验证、扣减等
      log.warn("火源支付功能尚未实现");

    } else if (PayChannelInterfaceEnum.BRAND_POINT.getCode().equals(payWay)) {
      // 品牌积分支付 - 使用商户品牌积分进行支付
      log.info("开始处理品牌积分支付，outOrderNo: {}", outOrderNo);
      // TODO: 实现品牌积分支付逻辑 - 包括积分余额验证、扣减等
      log.warn("品牌积分支付功能尚未实现");

    } else {
      // 不支持的支付方式
      log.error("不支持的支付方式：{}", payWay);
      throw new BusinessException(NOT_SUPPORTED_PAY_WAY);
    }

    log.info("交易订单创建完成，outOrderNo: {}", outOrderNo);
    return requestMap;
  }

  @Override
  @ShenyuDubboClient("/pay")
  @ApiDoc(desc = "pay")
  public void pay(TransactionRequest transactionRequest) {
    log.debug("请求TransactionService.pay,参数:{}", transactionRequest);
    LoginUser user = LoginInfoContextHelper.getLoginUser();
    try {
      // 查询币种是否支持
      QueryWrapper<ChainCoin> chainCoinQueryWrapper = new QueryWrapper<>();
      chainCoinQueryWrapper.eq("chain_coin_key", transactionRequest.getChainCoinKey());
      chainCoinQueryWrapper.eq("status", 1); // 已上架
      ChainCoin chainCoin = chainCoinMapper.selectOne(chainCoinQueryWrapper);
      if (chainCoin == null) {
        throw new BusinessException("CHAIN_COIN_NOT_EXIST");
      }
      // 查询用户账户状态是否正常
      QueryWrapper<WalletAccount> walletAccountQueryWrapper = new QueryWrapper<>();
      walletAccountQueryWrapper.eq("address", transactionRequest.getToAddress());
      walletAccountQueryWrapper.eq("chain_coin_no", chainCoin.getSerialNo());
      walletAccountQueryWrapper.eq("tenant_id", transactionRequest.getTenantId());
      WalletAccount walletAccounts = walletAccountMapper.selectOne(walletAccountQueryWrapper);
      if (walletAccounts == null) {}

      // 创建交易记录
      String serialNo = BsinSnowflake.getId(); // 雪花算法
      Transaction transaction = new Transaction();
      transaction.setSerialNo(serialNo);
      transaction.setOutSerialNo(transactionRequest.getOutSerialNo());
      transaction.setTransactionType(TransactionType.TRANSFER.getCode()); // 交易类型 2、转出
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
      if (true) {
        chainTransactionListen.transferOut();
      } else {
        // 风控拦截交易，进入人工审核
        TransactionAudit transactionAudit = new TransactionAudit();
        transactionAudit.setAuditYpe(1); // 1、交易转出审核
        transactionAudit.setAuditStatus(1); // 1、待审核状态
        transactionAudit.setAuditLevel(1); // 根据风控判断风险等级，暂默认为 1、低级风险
        transactionAudit.setTransactionNo(serialNo);
        transactionAudit.setCreateTime(new Date());
        transactionAudit.setCreateBy(user.getUserId());
        transactionAuditMapper.insert(transactionAudit);
      }
    } catch (BusinessException be) {
      throw be;
    } catch (Exception e) {
      e.printStackTrace();
      throw new BusinessException("SYSTEM_ERROR");
    }
  }

  @Override
  public Transaction recharge(Map<String, Object> requestMap) {
    return null;
  }

  /**
   * 处理各种类型转账交易
   *
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

  /**
   * 基于链账户的交易
   *
   * @param requestMap
   * @return
   */
  @ApiDoc(desc = "withdrawApply")
  @ShenyuDubboClient("/withdrawApply")
  @Override
  public Transaction withdrawApply(Map<String, Object> requestMap) {
    LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
    Transaction transaction = BsinServiceContext.getReqBodyDto(Transaction.class, requestMap);
    if (TransactionType.getInstanceById(transaction.getTransactionType()) == null) {
      throw new BusinessException("999", "交易类型不存在！");
    }
    transaction.setTenantId(loginUser.getTenantId());
    transaction.setFromAddress(loginUser.getBizRoleTypeNo());
    transaction.setAuditStatus("1");
    transactionMapper.insert(transaction);
    return transaction;
  }

  /**
   * 基于链账户的交易
   *
   * @param requestMap
   */
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

    // 获取支付渠道配置参数
    // 获取支付渠道配置参数
    String configParamsStr = null;
    PayChannelConfig payChannelConfig =
            transactionBiz.getPayChannelConfigParamsString(bizRoleAppId);
    if (payChannelConfig == null) {
      log.error("支付渠道配置参数不存在");
      throw new BusinessException("支付渠道配置参数不存在");
    }
    if (payChannelConfig.getIsNormalMerchantMode()) {
      log.debug("普通商户模式");
      configParamsStr = payChannelConfig.getNormalMerchantParams();
    } else if (payChannelConfig.getIsIsvSubMerchantMode()) {
      log.debug("服务商子商户模式");
      configParamsStr = payChannelConfig.getIsvParams();
    } else {
      log.debug("特殊商户模式");
      configParamsStr = payChannelConfig.getSpecialMerchantParams();
    }
    JSONObject payChannelConfigParams = JSONObject.parseObject(configParamsStr);

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
    } catch (WxPayException e) {
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

  /**
   * 业务订单完成之后调用钱包模块进行分账分润 定时任务触发调用 * @return
   *
   * @throws Exception
   */
  @ApiDoc(desc = "profitSharingSettlement")
  @ShenyuDubboClient("/profitSharingSettlement")
  @Override
  public Transaction profitSharingSettlement(Map<String, Object> requestMap) throws Exception {
    String outSerialNo = MapUtils.getString(requestMap, "outSerialNo");
    LambdaQueryWrapper<Transaction> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(Transaction::getOutSerialNo, outSerialNo);
    // 根据订单号查询交易
    Transaction transaction = transactionMapper.selectOne(queryWrapper);
    // 生态价值分配引擎
    revenueShareServiceEngine.excute(transaction);
    return transaction;
  }

  @Override
  public Transaction income(Map<String, Object> requestMap) {
    return null;
  }

  @Override
  public Transaction redeem(Map<String, Object> requestMap) {
    return null;
  }

  /**
   * @param transactionDTO fromAddress toAddress txAmount chainCoinNo
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
    if (walletAccount == null) {
      throw new BusinessException("非平台钱包账户地址");
    }
    log.info("开始提现交易，账户余额为：{}", walletAccount.getBalance());
    transactionBiz.tokenTransfer(
        fromAddress,
        toAddress,
        contractAddress,
        txAmount.toBigInteger(),
        chainCoin.getCoinDecimal());
    BigDecimal balance = walletAccount.getBalance().subtract(txAmount);
    log.info("提现交易结束，账户余额为：{}", balance);
    walletAccount.setBalance(balance);
    walletAccountMapper.updateById(walletAccount);
  }

  @Override
  @ShenyuDubboClient("/getPageList")
  @ApiDoc(desc = "getPageList")
  public Page<Transaction> getPageList(TransactionDTO transactionDTO) {
    log.debug("请求TransactionService.pageList,参数:{}", transactionDTO);
    LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
    Pagination pagination = transactionDTO.getPagination();
    Page<Transaction> page = new Page<>(pagination.getPageNum(), pagination.getPageSize());
    LambdaQueryWrapper<Transaction> warapper = new LambdaQueryWrapper<>();
    warapper.orderByDesc(Transaction::getCreateTime);
    warapper.eq(Transaction::getTenantId, loginUser.getTenantId());
    warapper.eq(
        ObjectUtils.isNotEmpty(transactionDTO.getTransactionType()),
        Transaction::getTransactionType,
        transactionDTO.getTransactionType());
    return transactionMapper.selectPage(page, warapper);
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
