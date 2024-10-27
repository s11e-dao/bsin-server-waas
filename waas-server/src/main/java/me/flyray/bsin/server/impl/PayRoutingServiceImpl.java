package me.flyray.bsin.server.impl;

import com.github.binarywang.wxpay.bean.order.WxPayMpOrderResult;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderRequest;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.api.WxMpService;
import me.flyray.bsin.constants.ResponseCode;
import me.flyray.bsin.domain.entity.BizRoleApp;
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

import java.util.Map;

/**
 * 第三方支付路由处理
 */

@Slf4j
@ShenyuDubboService(path = "/payRouting", timeout = 6000)
@ApiModule(value = "payRouting")
@Service
public class PayRoutingServiceImpl implements PayRoutingService {

    @Value("${wx.pay.callbackUrl}")
    private String wxCallbackUrl;
    @Autowired
    BsinWxPayServiceUtil bsinWxPayServiceUtil;
    /**
     * 根据支付方式判断处理
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
        String openId = MapUtils.getString(requestMap, "openId");
        if ("wx".equals(payWay)) {
            Double deciPrice = Double.parseDouble(amount) * 100;
            Integer totalFee = deciPrice.intValue() * Integer.parseInt(quantity);
            String purchaseNo = "pn-" + BsinSnowflake.getId();
            WxPayMpOrderResult payResult = new WxPayMpOrderResult();
            WxPayUnifiedOrderRequest wxPayRequest = new WxPayUnifiedOrderRequest();
            wxPayRequest.setAppid("wx581d0c32a8c78");
            wxPayRequest.setMchId("1516165741");
            wxPayRequest.setBody("飞雷充值");
            //wxPayRequest.setDetail((String) map.get("detail"));
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

            // 火源支付，暂未开放
        }
        return null;
    }
}
