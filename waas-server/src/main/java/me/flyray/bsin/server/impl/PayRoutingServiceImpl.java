package me.flyray.bsin.server.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.binarywang.wxpay.bean.order.WxPayMpOrderResult;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderRequest;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.constants.ResponseCode;
import me.flyray.bsin.domain.entity.Account;
import me.flyray.bsin.domain.entity.Transaction;
import me.flyray.bsin.domain.entity.TransferJournal;
import me.flyray.bsin.domain.enums.PayWayEnum;
import me.flyray.bsin.domain.enums.TransactionStatus;
import me.flyray.bsin.domain.enums.TransactionType;
import me.flyray.bsin.exception.BusinessException;
import me.flyray.bsin.facade.service.PayRoutingService;
import me.flyray.bsin.infrastructure.mapper.TransactionMapper;
import me.flyray.bsin.infrastructure.mapper.TransferJournalMapper;
import me.flyray.bsin.payment.BsinWxPayServiceUtil;
import me.flyray.bsin.security.contex.LoginInfoContextHelper;
import me.flyray.bsin.security.domain.LoginUser;
import me.flyray.bsin.utils.BsinSnowflake;
import org.apache.commons.collections4.MapUtils;
import org.apache.shenyu.client.apache.dubbo.annotation.ShenyuDubboService;
import org.apache.shenyu.client.apidocs.annotations.ApiDoc;
import org.apache.shenyu.client.apidocs.annotations.ApiModule;
import org.apache.shenyu.client.dubbo.common.annotation.ShenyuDubboClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/** 第三方支付路由处理 */
@Slf4j
@ShenyuDubboService(path = "/payRouting", timeout = 6000)
@ApiModule(value = "payRouting")
@Service
public class PayRoutingServiceImpl implements PayRoutingService {

  @Value("${wx.pay.callbackUrl}")
  private String wxCallbackUrl;

  @Autowired BsinWxPayServiceUtil bsinWxPayServiceUtil;
  @Autowired private TransactionMapper transactionMapper;

  /**
   * 根据支付方式判断处理
   *
   * @param requestMap
   * @return
   */
  @ApiDoc(desc = "pay")
  @ShenyuDubboClient("/pay")
  @Override
  public Map<String, Object> pay(Map<String, Object> requestMap) {
    LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
    String payWay = MapUtils.getString(requestMap, "payWay");
    String payAmount = MapUtils.getString(requestMap, "payAmount");
    String appId = MapUtils.getString(requestMap, "appId");
    String orderNo = MapUtils.getString(requestMap, "orderNo");
    String tenantId = MapUtils.getString(requestMap, "tenantId");
    String merchantNo = MapUtils.getString(requestMap, "merchantNo");
    String customerNo = MapUtils.getString(requestMap, "customerNo");
    String mark = MapUtils.getString(requestMap, "mark");
    if (payWay.isEmpty() || payAmount.isEmpty()) {
      throw new BusinessException(ResponseCode.PARAM_ERROR);
    }

    // 1.创建交易订单
    Transaction transaction =
        transactionMapper.selectOne(
            new LambdaQueryWrapper<Transaction>().eq(Transaction::getOutSerialNo, orderNo));
    // 订单已支付成功,直接返回
    if (transaction != null
        && TransactionStatus.SUCCESS.getCode().equals(transaction.getTransactionStatus())) {
      requestMap.put("payResult", "success");
      return requestMap;
    } else if (transaction == null) {
      transaction.setSerialNo(BsinSnowflake.getId());
      transaction.setOutSerialNo(orderNo);
      transaction.setTransactionType(TransactionType.PAY.getCode());
      transaction.setComment(mark);
      transaction.setTxAmount(new BigDecimal(payAmount));
      //    transaction.setFromAddress(transactionRequest.getFromAddress());
      //    transaction.setToAddress(transactionRequest.getToAddress());
      //    transaction.setBizRoleType(user.getBizRoleType());
      //    transaction.setBizRoleTypeNo(user.getBizRoleTypeNo());
      transaction.setTenantId(tenantId);
      transaction.setCreateTime(new Date());
      transaction.setCreateBy(customerNo);
      transactionMapper.insert(transaction);
    }
    // 2、创建支付流水

    // 3、支付
    if (PayWayEnum.WX_MP.getCode().equals(payWay)) {
      String openId = MapUtils.getString(requestMap, "openId");
      Double deciPrice = Double.parseDouble(payAmount) * 100;
      WxPayMpOrderResult payResult = new WxPayMpOrderResult();
      WxPayUnifiedOrderRequest wxPayRequest = new WxPayUnifiedOrderRequest();
      // TODO: 支付配置应用,appId 从商户应用配置的支付应用中获取
      wxPayRequest.setAppid(appId);
      wxPayRequest.setMchId("1516165741");
      wxPayRequest.setBody("飞雷充值");
      // wxPayRequest.setDetail((String) map.get("detail"));

      wxPayRequest.setOutTradeNo(transaction.getSerialNo());
      wxPayRequest.setTotalFee(deciPrice.intValue());
      wxPayRequest.setSpbillCreateIp("127.0.0.1");
      //! 微信收到后的回调地址，会自动回调该地址：
      wxPayRequest.setNotifyUrl(wxCallbackUrl);
      wxPayRequest.setTradeType("JSAPI");
      wxPayRequest.setOpenid(openId);
      //      log.info("传递的参数{}", wxPayRequest);
      // 添加支付流水
      try {
        WxPayConfig wxPayConfig = new WxPayConfig();
        WxPayService wxPayService = bsinWxPayServiceUtil.getWxPayService(wxPayConfig);
        payResult = wxPayService.createOrder(wxPayRequest);
        requestMap.put("PayResult", payResult);

      } catch (WxPayException e) {
        e.printStackTrace();
        //        log.info("支付异常{}", e);
        throw new BusinessException(ResponseCode.FAIL);
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
    }
    return requestMap;
  }
}
