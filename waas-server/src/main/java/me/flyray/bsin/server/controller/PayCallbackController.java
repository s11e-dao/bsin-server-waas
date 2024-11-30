package me.flyray.bsin.server.controller;

import com.alibaba.fastjson.JSONObject;
import com.github.binarywang.wxpay.bean.notify.WxPayNotifyResponse;
import com.github.binarywang.wxpay.bean.notify.WxPayOrderNotifyResult;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.domain.entity.BizRoleApp;
import me.flyray.bsin.domain.entity.WaasTransaction;
import me.flyray.bsin.domain.enums.TransactionStatus;
import me.flyray.bsin.dubbo.invoke.BsinServiceInvoke;
import me.flyray.bsin.enums.AppType;
import me.flyray.bsin.facade.service.MemberService;
import me.flyray.bsin.infrastructure.mapper.WaasTransactionMapper;
import me.flyray.bsin.payment.BsinWxPayServiceUtil;
import me.flyray.bsin.thirdauth.wx.utils.WxRedisConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.shenyu.client.apidocs.annotations.ApiDoc;
import org.apache.shenyu.client.apidocs.annotations.ApiModule;
import org.apache.shenyu.client.springmvc.annotation.ShenyuSpringMvcClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/payCallback")
@ShenyuSpringMvcClient("/payCallback")
@ApiModule(value = "payCallback")
@Slf4j
public class PayCallbackController {

  @Autowired private BsinServiceInvoke bsinServiceInvoke;

  @Value("${bsin.oms.aesKey}")
  private String aesKey;

  @Value("${wx.mp.redisConfig.host}")
  private String wxRedisHost;

  @Value("${wx.mp.redisConfig.port}")
  private Integer wxRedisPort;

  @Value("${wx.mp.redisConfig.password}")
  private String wxRedisPassword;

  private static WxRedisConfig wxRedisConfig;

  @Autowired BsinWxPayServiceUtil bsinWxPayServiceUtil;
  @Autowired private WaasTransactionMapper transactionMapper;

  @DubboReference(version = "${dubbo.provider.version}")
  private MemberService memberService;

  /**
   * 1、解析回调包文
   *
   * <p>2、调用订单完成方法统一处理
   *
   * @param body
   * @return
   * @throws Exception
   */
  @PostMapping("/wxpay/{appId}")
  @ShenyuSpringMvcClient("/wxpay/{appId}")
  @ApiDoc(desc = "wxpay")
  public Object wxpay(@RequestBody(required = false) String body, @PathVariable("appId") String appId) throws Exception {
    WxPayOrderNotifyResult result = null;
    try {
      // 解析回调包文
      BizRoleApp bizRoleApp = new BizRoleApp();
      bizRoleApp.setAppType(AppType.WX_PAY.getType());
      // TODO: 获取appId ??????????????????
      bizRoleApp.setAppId(appId);
      WxPayService wxPayService = getWxService(bizRoleApp);
      result = wxPayService.parseOrderNotifyResult(body);
      log.info("处理腾讯支付平台的订单支付");
      log.info(JSONObject.toJSONString(result));
      // 处理微信支付成功回调
      Map<String, Object> requestMap = new HashMap<>();
      requestMap.put("resultCode", result.getResultCode());
      // getOutTradeNo 是 创建订单时候的订单号
      requestMap.put("transactionNo", result.getOutTradeNo());
      requestMap.put("cashFee", result.getCashFee());
      requestMap.put("payId", result.getTransactionId());
      // 根据 WaasTransactionNo 查询交易订单并更新交易状态
      WaasTransaction transaction = transactionMapper.selectById(result.getOutTradeNo());
      if ("SUCCESS".equals(result.getResultCode())) {
        transaction.setTransactionStatus(TransactionStatus.SUCCESS.getCode());
      } else {
        transaction.setTransactionStatus(TransactionStatus.FAIL.getCode());
      }
      transactionMapper.updateById(transaction);
      // 异步调用（泛化调用解耦）订单完成方法统一处理： 根据订单类型后续处理
      bsinServiceInvoke.genericInvoke("UniflyOrderService", "completePay", "dev", requestMap);
    } catch (Exception e) {
      return WxPayNotifyResponse.fail("支付失败");
    }
    return WxPayNotifyResponse.success("success");
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
