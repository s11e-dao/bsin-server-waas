package me.flyray.bsin.server.controller;

import com.alibaba.fastjson.JSONObject;
import com.github.binarywang.wxpay.bean.notify.WxPayNotifyResponse;
import com.github.binarywang.wxpay.bean.notify.WxPayOrderNotifyResult;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.service.WxService;
import me.flyray.bsin.domain.entity.BizRoleApp;
import me.flyray.bsin.domain.entity.Member;
import me.flyray.bsin.enums.AppType;
import me.flyray.bsin.exception.BusinessException;
import me.flyray.bsin.facade.service.CustomerService;
import me.flyray.bsin.facade.service.MemberService;
import me.flyray.bsin.facade.service.UniflyOrderService;
import me.flyray.bsin.payment.BsinWxPayServiceUtil;
import me.flyray.bsin.thirdauth.wx.utils.WxRedisConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.shenyu.client.apidocs.annotations.ApiDoc;
import org.apache.shenyu.client.apidocs.annotations.ApiModule;
import org.apache.shenyu.client.springmvc.annotation.ShenyuSpringMvcClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import static me.flyray.bsin.constants.ResponseCode.PAYMENT_STATUS_ERROR;

@RestController
@RequestMapping("/payCallback")
@ShenyuSpringMvcClient("/payCallback")
@ApiModule(value = "payCallback")
@Slf4j
public class PayCallbackController {

  @DubboReference(version = "${dubbo.provider.version}")
  private UniflyOrderService uniflyOrderService;

  @Value("${bsin.oms.aesKey}")
  private String aesKey;

  @Value("${wx.mp.redisConfig.host}")
  private String wxRedisHost;

  @Value("${wx.mp.redisConfig.port}")
  private Integer wxRedisPort;

  @Value("${wx.mp.redisConfig.password}")
  private String wxRedisPassword;

  private static WxRedisConfig wxRedisConfig;

  @Autowired
  BsinWxPayServiceUtil bsinWxPayServiceUtil;

  @DubboReference(version = "${dubbo.provider.version}")
  private MemberService memberService;

  /**
   * 1、解析回调包文
   * 2、调用订单完成方法统一处理
   * @param body
   * @return
   * @throws Exception
   */
  @PostMapping("/wxpay")
  @ShenyuSpringMvcClient("/wechat")
  @ApiDoc(desc = "wechat")
  public Object wxpay(@RequestBody String body) throws Exception {
    WxPayOrderNotifyResult result = null;
    try {
      // 解析回调包文
      BizRoleApp bizRoleApp = new BizRoleApp();
      WxPayService wxPayService = getWxService(bizRoleApp);
      result = wxPayService.parseOrderNotifyResult(body);
      log.info("处理腾讯支付平台的订单支付");
      log.info(JSONObject.toJSONString(result));
      // 处理微信支付成功回调
      Map<String, Object> requestMap = new HashMap<>();
      requestMap.put("resultCode", result.getResultCode());
      requestMap.put("tradeNo", result.getOutTradeNo());
      requestMap.put("cashFee", result.getCashFee());
      requestMap.put("payId", result.getTransactionId());
      // 调用订单完成方法统一处理
      uniflyOrderService.completePay(requestMap);
    } catch (Exception e) {
      return WxPayNotifyResponse.fail("支付失败");
    }
    return WxPayNotifyResponse.success("success");
  }

  /**
   * Save order dto.
   *
   * @return the order dto
   */
  @PostMapping("/wechat")
  @ShenyuSpringMvcClient("/wechat")
  @ApiDoc(desc = "wechat")
  public Map save(@RequestBody final String body) {
    Map<String, Object> respMap = new HashMap<>();
    WxPayOrderNotifyResult result = null;
    try {
      // TODO: 微信支付走 crm 服务？？？？
      //      result = wxPayService.parseOrderNotifyResult(body);
      log.info("处理腾讯支付平台的订单支付");
      log.info(JSONObject.toJSONString(result));
      // 处理微信支付成功回调
      Map<String, Object> requestMap = new HashMap<>();
      requestMap.put("resultCode", result.getResultCode());
      requestMap.put("tradeNo", result.getOutTradeNo());
      requestMap.put("cashFee", result.getCashFee());
      requestMap.put("payId", result.getTransactionId());
      Member member = memberService.openMember(requestMap);
    }
    //    catch (WxPayException we) {
    //      log.error("[微信解析回调请求] 异常", we);
    //      //      return WxPayNotifyResponse.fail(we.getMessage());
    //      throw new BusinessException(PAYMENT_WECHAT_PARSE_CALLBACK_ERROR);
    //
    //    }
    catch (Exception e) {
      //      return WxPayNotifyResponse.fail("支付失败");
      throw new BusinessException(PAYMENT_STATUS_ERROR);
    }
    respMap.put("status", "1");
    return respMap;
  }

  private WxPayService getWxService(BizRoleApp merchantWxApp) {
    WxPayService wxPayService = null;
    if (StringUtils.equals(merchantWxApp.getAppType(), AppType.WX_PAY.getType())) {
      log.info("微信支付应用");
      WxPayConfig config = new WxPayConfig();
      config.setAppId(merchantWxApp.getAppId());
      //      SymmetricCrypto aes = new SymmetricCrypto(SymmetricAlgorithm.AES, aesKey.getBytes());
      //      config.setSecret(aes.decryptStr(merchantWxApp.getAppSecret(),
      // CharsetUtil.CHARSET_UTF_8));
      if (wxRedisConfig == null) {
        wxRedisConfig = new WxRedisConfig();
        wxRedisConfig.setHost(wxRedisHost);
        wxRedisConfig.setPort(wxRedisPort);
        wxRedisConfig.setPassword(wxRedisPassword);
      }
      wxPayService = (WxPayService) bsinWxPayServiceUtil.getWxPayService(config);
    } else {
    }
    return wxPayService;
  }
}
