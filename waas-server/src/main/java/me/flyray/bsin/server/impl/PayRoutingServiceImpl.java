package me.flyray.bsin.server.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.binarywang.wxpay.bean.order.WxPayMpOrderResult;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderRequest;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderV3Request;
import com.github.binarywang.wxpay.bean.result.enums.TradeTypeEnum;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.constant.WxPayConstants;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.constants.ResponseCode;
import me.flyray.bsin.domain.entity.*;
import me.flyray.bsin.domain.enums.PayWayEnum;
import me.flyray.bsin.domain.enums.TransactionStatus;
import me.flyray.bsin.enums.TransactionType;
import me.flyray.bsin.exception.BusinessException;
import me.flyray.bsin.facade.service.BizRoleAppService;
import me.flyray.bsin.facade.service.PayRoutingService;
import me.flyray.bsin.infrastructure.mapper.PayChannelConfigMapper;
import me.flyray.bsin.infrastructure.mapper.TransactionJournalMapper;
import me.flyray.bsin.infrastructure.mapper.TransactionMapper;
import me.flyray.bsin.payment.BsinWxPayServiceUtil;
import me.flyray.bsin.security.contex.LoginInfoContextHelper;
import me.flyray.bsin.security.domain.LoginUser;
import me.flyray.bsin.utils.BsinSnowflake;
import me.flyray.bsin.utils.StringUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.shenyu.client.apache.dubbo.annotation.ShenyuDubboService;
import org.apache.shenyu.client.apidocs.annotations.ApiDoc;
import org.apache.shenyu.client.apidocs.annotations.ApiModule;
import org.apache.shenyu.client.dubbo.common.annotation.ShenyuDubboClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

import static me.flyray.bsin.constants.ResponseCode.NOT_SUPPORTED_PAY_WAY;
import static me.flyray.bsin.utils.Utils.ObjectToMapConverter;

/** 第三方支付路由处理 */
@Slf4j
@ShenyuDubboService(path = "/payRouting", timeout = 6000)
@ApiModule(value = "payRouting")
@Service
public class PayRoutingServiceImpl implements PayRoutingService {

  @Value("${wx.pay.callbackUrl}")
  private String wxCallbackUrl;

  @Autowired BsinWxPayServiceUtil bsinWxPayServiceUtil;
  @Autowired private TransactionMapper waasTransactionMapper;
  @Autowired private PayChannelConfigMapper payChannelConfigMapper;
  @Autowired private TransactionJournalMapper waasTransactionJournalMapper;

  @DubboReference(version = "${dubbo.provider.version}")
  private BizRoleAppService bizRoleAppService;

  /**
   * 根据支付方式判断处理
   *
   * @param requestMap
   * @return
   */
  @ApiDoc(desc = "pay")
  @ShenyuDubboClient("/pay")
  @Override
  @Transactional
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
        waasTransactionMapper.selectOne(
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
      waasTransactionMapper.insert(waasTransaction);
    }
    // 2、创建支付转账流水
    TransactionJournal waasTransactionJournal =
        waasTransactionJournalMapper.selectOne(
            new LambdaQueryWrapper<TransactionJournal>()
                .eq(TransactionJournal::getTransactionNo, waasTransaction.getSerialNo()));
    if (waasTransactionJournal == null) {
      waasTransactionJournal = new TransactionJournal();
      waasTransactionJournal.setTransactionNo(waasTransaction.getSerialNo());
      waasTransactionJournal.setPayAmount(new BigDecimal(payAmount));
      waasTransactionJournal.setSerialNo(BsinSnowflake.getId());
      waasTransactionJournal.setStatus(TransactionStatus.PENDING.getCode());
      waasTransactionJournalMapper.insert(waasTransactionJournal);
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
          notifyUrl = payChannelConfigParams.getString("notifyUrl");
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
