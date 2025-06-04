package me.flyray.bsin.server.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.binarywang.wxpay.bean.notify.WxPayNotifyResponse;
import com.github.binarywang.wxpay.bean.notify.WxPayOrderNotifyResult;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.domain.entity.Transaction;
import me.flyray.bsin.domain.enums.TransactionStatus;
import me.flyray.bsin.dubbo.invoke.BsinServiceInvoke;
import me.flyray.bsin.facade.engine.RevenueShareServiceEngine;
import me.flyray.bsin.infrastructure.mapper.TransactionJournalMapper;
import me.flyray.bsin.infrastructure.mapper.TransactionMapper;
import me.flyray.bsin.payment.BsinWxPayServiceUtil;
import me.flyray.bsin.thirdauth.wx.utils.WxRedisConfig;
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
@ShenyuSpringMvcClient("/payCallback/**")
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

  @Autowired
  BsinWxPayServiceUtil bsinWxPayServiceUtil;
  @Autowired
  private TransactionMapper transactionMapper;
  @Autowired
  private TransactionJournalMapper waasTransactionJournalMapper;
  @Autowired
  private RevenueShareServiceEngine revenueShareServiceEngine;

//  @DubboReference(version = "${dubbo.provider.version}")
//  private MemberService memberService;

  /**
   * 1、解析回调包文
   * 2、调用订单完成方法统一处理
   * 参考：https://pay.weixin.qq.com/wiki/doc/api/jsapi.php?chapter=9_7
   *
   * @param body
   * @return
   * @throws Exception
   */
  @PostMapping("/wxpay/{mchId}")
  @ApiDoc(desc = "wxpay")
  public Object wxpay(
      @RequestBody(required = false) String body, @PathVariable("mchId") String mchId)
      throws Exception {
    WxPayOrderNotifyResult result = null;
    try {
//      // 解析回调包文
//      BizRoleApp bizRoleApp = new BizRoleApp();
//      bizRoleApp.setAppChannel(AppChannel.WX_PAY.getType());
//      bizRoleApp.setMchId(mchId);
      WxPayService wxPayService = null ; //getWxService(null);
      result = wxPayService.parseOrderNotifyResult(body);
      log.info("处理腾讯支付平台的订单支付");
      log.info(JSONObject.toJSONString(result));
      // 处理微信支付成功回调
      Map<String, Object> requestMap = new HashMap<>();
      requestMap.put("resultCode", result.getResultCode());
      // getOutTradeNo 是创建订单时候的订单号
      requestMap.put("orderNo", result.getOutTradeNo());
      requestMap.put("cashFee", result.getCashFee());
      requestMap.put("payId", result.getTransactionId());
      // 根据 WaasTransactionNo 查询交易订单并更新交易状态
      Transaction waasTransaction =
              transactionMapper.selectOne(
                      new LambdaQueryWrapper<Transaction>().eq(Transaction::getOutSerialNo, result.getOutTradeNo()));
      if (waasTransaction == null) {
        return WxPayNotifyResponse.fail("未找到交易订单");
      }
      // 更新交易流水
      if ("SUCCESS".equals(result.getResultCode())) {
        waasTransaction.setTransactionStatus(TransactionStatus.SUCCESS.getCode());
        waasTransactionJournalMapper.updateTransferStatus(waasTransaction.getSerialNo(), TransactionStatus.SUCCESS.getCode());
      } else {
        waasTransaction.setTransactionStatus(TransactionStatus.FAIL.getCode());
        waasTransactionJournalMapper.updateTransferStatus(waasTransaction.getSerialNo(), TransactionStatus.FAIL.getCode());
      }
      transactionMapper.updateById(waasTransaction);

      // 分佣分账引擎
      revenueShareServiceEngine.excute(requestMap);

      // 异步调用（泛化调用解耦）订单完成方法统一处理： 根据订单类型后续处理
      bsinServiceInvoke.genericInvoke("UniflyOrderService", "completePay", "dev", requestMap);
    } catch (Exception e) {
      System.out.println(e.getCause());
      return WxPayNotifyResponse.fail("支付失败");
    }
    return WxPayNotifyResponse.success("success");
  }

//  private WxPayService getWxService(BizRoleApp merchantWxApp) {
//    WxPayService wxPayService = null;
//    if (StringUtils.equals(merchantWxApp.getAppChannel(), AppChannel.WX_PAY.getType())) {
//      log.info("微信支付应用");
//      WxPayConfig config = new WxPayConfig();
////      config.setMchId(merchantWxApp.getMchId());
//      //      SymmetricCrypto aes = new SymmetricCrypto(SymmetricAlgorithm.AES, aesKey.getBytes());
//      //      config.setSecret(aes.decryptStr(merchantWxApp.getAppSecret(),
//      // CharsetUtil.CHARSET_UTF_8));
//      if (wxRedisConfig == null) {
//        wxRedisConfig = new WxRedisConfig();
//        wxRedisConfig.setHost(wxRedisHost);
//        wxRedisConfig.setPort(wxRedisPort);
//        wxRedisConfig.setPassword(wxRedisPassword);
//      }
//      wxPayService = (WxPayService) bsinWxPayServiceUtil.getWxPayService(config);
//    } else {
//    }
//    return wxPayService;
//  }

}
