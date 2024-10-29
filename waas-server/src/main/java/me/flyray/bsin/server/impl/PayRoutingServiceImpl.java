package me.flyray.bsin.server.impl;

import com.github.binarywang.wxpay.bean.order.WxPayMpOrderResult;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderRequest;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.constants.ResponseCode;
import me.flyray.bsin.domain.entity.Account;
import me.flyray.bsin.domain.entity.TokenParam;
import me.flyray.bsin.domain.entity.UniflyOrder;
import me.flyray.bsin.domain.enums.AccountCategory;
import me.flyray.bsin.domain.enums.CcyType;
import me.flyray.bsin.domain.enums.PayWay;
import me.flyray.bsin.exception.BusinessException;
import me.flyray.bsin.facade.service.PayRoutingService;
import me.flyray.bsin.payment.BsinWxPayServiceUtil;
import me.flyray.bsin.utils.BsinSnowflake;
import org.apache.commons.collections4.MapUtils;
import org.apache.shenyu.client.apache.dubbo.annotation.ShenyuDubboService;
import org.apache.shenyu.client.apidocs.annotations.ApiDoc;
import org.apache.shenyu.client.apidocs.annotations.ApiModule;
import org.apache.shenyu.client.dubbo.common.annotation.ShenyuDubboClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
    String payWay = MapUtils.getString(requestMap, "payWay");
    String amount = MapUtils.getString(requestMap, "amount");
    String quantity = MapUtils.getString(requestMap, "quantity");
    if (PayWay.WX.getCode().equals(payWay)) {
      String openId = MapUtils.getString(requestMap, "openId");
      Double deciPrice = Double.parseDouble(amount) * 100;
      Integer totalFee = deciPrice.intValue() * Integer.parseInt(quantity);
      String purchaseNo = "pn-" + BsinSnowflake.getId();
      WxPayMpOrderResult payResult = new WxPayMpOrderResult();
      WxPayUnifiedOrderRequest wxPayRequest = new WxPayUnifiedOrderRequest();
      wxPayRequest.setAppid("wx581d0c32a8c78");
      wxPayRequest.setMchId("1516165741");
      wxPayRequest.setBody("飞雷充值");
      // wxPayRequest.setDetail((String) map.get("detail"));
      wxPayRequest.setOutTradeNo(purchaseNo);
      wxPayRequest.setTotalFee(totalFee);
      wxPayRequest.setSpbillCreateIp("127.0.0.1");
      wxPayRequest.setNotifyUrl(wxCallbackUrl);
      wxPayRequest.setTradeType("JSAPI");
      wxPayRequest.setOpenid(openId);
      log.info("传递的参数{}", wxPayRequest);
      // 添加支付流水

      try {
        WxPayConfig wxPayConfig = new WxPayConfig();
        WxPayService wxPayService = bsinWxPayServiceUtil.getWxPayService(wxPayConfig);
        payResult = wxPayService.createOrder(wxPayRequest);
      } catch (WxPayException e) {
        e.printStackTrace();
        log.info("支付异常{}", e);
        throw new BusinessException(ResponseCode.FAIL);
      }
      // 火源支付
    } else if (PayWay.FIRE_DIAMOND.getCode().equals(payWay)) {
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
    // 品牌积分
    else if (PayWay.BRAND_POINT.getCode().equals(payWay)) {
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
    return null;
  }
}
