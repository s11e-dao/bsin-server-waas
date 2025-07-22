package me.flyray.bsin.server.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.binarywang.wxpay.bean.notify.WxPayNotifyResponse;
import com.github.binarywang.wxpay.bean.notify.WxPayOrderNotifyResult;
import com.github.binarywang.wxpay.bean.profitsharing.request.ProfitSharingReceiverRequest;
import com.github.binarywang.wxpay.bean.profitsharing.request.ProfitSharingRequest;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.constant.WxPayConstants;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.ProfitSharingService;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.domain.entity.MerchantConfig;
import me.flyray.bsin.domain.entity.ProfitSharingConfig;
import me.flyray.bsin.domain.entity.Transaction;
import me.flyray.bsin.domain.enums.TransactionStatus;
import me.flyray.bsin.dubbo.invoke.BsinServiceInvoke;
import me.flyray.bsin.exception.BusinessException;
import me.flyray.bsin.facade.engine.RevenueShareServiceEngine;
import me.flyray.bsin.facade.service.MerchantConfigService;
import me.flyray.bsin.facade.service.MerchantPayService;
import me.flyray.bsin.infrastructure.mapper.TransactionJournalMapper;
import me.flyray.bsin.infrastructure.mapper.TransactionMapper;
import me.flyray.bsin.payment.BsinWxPayServiceUtil;
import me.flyray.bsin.server.impl.RevenueShareServiceEngineImpl;
import me.flyray.bsin.thirdauth.wx.utils.WxRedisConfig;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.shenyu.client.apidocs.annotations.ApiDoc;
import org.apache.shenyu.client.apidocs.annotations.ApiModule;
import org.apache.shenyu.client.springmvc.annotation.ShenyuSpringMvcClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/payCallback")
@ShenyuSpringMvcClient("/payCallback/**")
@ApiModule(value = "payCallback")
@Slf4j
public class PayCallbackController {

  @Autowired private BsinServiceInvoke bsinServiceInvoke;

  // 常量定义
  private static final String ORDER_TYPE = "1";
  private static final String PRODUCT_TYPE = "2";

  @Value("${bsin.oms.aesKey}")
  private String aesKey;

  @Value("${wx.mp.redisConfig.host}")
  private String wxRedisHost;

  @Value("${wx.mp.redisConfig.port}")
  private Integer wxRedisPort;

  @Value("${wx.mp.redisConfig.password}")
  private String wxRedisPassword;

  private static WxRedisConfig wxRedisConfig;

  // 依赖注入
  @Autowired
  private MerchantPayService merchantPayService;
  @Autowired
  BsinWxPayServiceUtil bsinWxPayServiceUtil;
  @Autowired
  private TransactionMapper transactionMapper;
  @Autowired
  private TransactionJournalMapper waasTransactionJournalMapper;
  private MerchantConfigService merchantConfigService;

  /**
   * 1、解析回调结果
   * 2、验证更新交易状态
   * 3、调用oms模块处理业务订单
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
      // 1、解析回调结果
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

      // 2、验证更新交易状态
      Transaction transaction = updateTransactionStatus(result);

      // 3. 支付分账阶段（独立事务）
      executePaymentAllocationSafely(transaction);

      // 4、异步调用（泛化调用解耦）订单完成方法统一处理： 根据订单类型后续处理
      bsinServiceInvoke.genericInvoke("UniflyOrderService", "completePay", "dev", requestMap);
    } catch (Exception e) {
      System.out.println(e.getCause());
      return WxPayNotifyResponse.fail("支付失败");
    }
    return WxPayNotifyResponse.success("success");
  }

  /**
   * 安全执行支付分账，包含完整的异常处理
   * @param transaction 交易信息
   * @return PaymentAllocationResult 支付分账结果
   */
  private PayCallbackController.PaymentAllocationResult executePaymentAllocationSafely(Transaction transaction) {
    final String serialNo = transaction.getSerialNo();

    try {
      // 获得商户让利配置
      Map requestMap = new HashMap<>();
      requestMap.put("merchantNo", "merchantNo");
      MerchantConfig merchantConfig = merchantConfigService.getDetail(requestMap);

      if (merchantConfig != null) {
        executeProfitSharing(transaction, merchantConfig);
        log.info("支付分账执行成功，交易号：{}", serialNo);
        return new PayCallbackController.PaymentAllocationResult(true, null);
      } else {
        log.info("无分账配置，跳过支付分账，交易号：{}", serialNo);
        return new PayCallbackController.PaymentAllocationResult(false, null);
      }

    } catch (Exception e) {
      log.error("支付分账执行失败，交易号：{}", serialNo, e);
      return new PayCallbackController.PaymentAllocationResult(false, e.getMessage());
    }
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

  /**
   * 更新交易状态
   */
  @Transactional(rollbackFor = Exception.class)
  protected Transaction updateTransactionStatus(WxPayOrderNotifyResult result) {
    // 根据 WaasTransactionNo 查询交易订单并更新交易状态
    Transaction waasTransaction =
            transactionMapper.selectOne(
                    new LambdaQueryWrapper<Transaction>().eq(Transaction::getOutSerialNo, result.getOutTradeNo()));
    if (waasTransaction == null) {
      return null;
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
    return waasTransaction;
  }

  /**
   * 更新交易状态(分账完成后)
   */
  @Transactional(rollbackFor = Exception.class)
  protected void updateTransactionStatus(Transaction transaction) {
    // 分账完成后，更新交易状态为成功
    transaction.setTransactionStatus(TransactionStatus.SUCCESS.getCode());
    // 更新交易流水状态
    waasTransactionJournalMapper.updateTransferStatus(transaction.getSerialNo(), TransactionStatus.SUCCESS.getCode());
    // 更新交易记录
    transactionMapper.updateById(transaction);
    log.info("分账完成，交易状态已更新为成功，交易号：{}", transaction.getSerialNo());
  }

  /**
   * 执行支付分账
   * 包括计算分账金额、配置支付服务、执行分账请求、更新交易状态
   * 1、计算分账金额
   * 2、调用微信设置分账用户
   * 3、执行分账
   */
  private void executeProfitSharing(Transaction transaction, MerchantConfig merchantConfig) throws WxPayException {
    final String serialNo = transaction.getSerialNo();
    log.info("开始执行支付分账，交易号：{}", serialNo);

    // 计算分账金额
    BigDecimal profitSharingAmount = calculateProfitSharingAmount(transaction, merchantConfig);
    log.info("计算得出分账金额：{}，交易号：{}", profitSharingAmount, serialNo);

    // 执行分账流程
    ProfitSharingService profitSharingService = createProfitSharingService();
    // 绑定收框人接口
    addProfitSharingReceiver(profitSharingService);
    // 执行分账
    executeProfitSharingRequest(profitSharingService, transaction);
    updateTransactionStatus(transaction);

    log.info("支付分账执行完成，交易号：{}", serialNo);
  }

  /**
   * 计算分账金额
   * 订单类型：使用预设分账金额
   * 商品类型：交易金额 × 商户分润比例
   */
  private BigDecimal calculateProfitSharingAmount(Transaction transaction, MerchantConfig merchantConfig) {
    if (ORDER_TYPE.equals(transaction.getProfitSharingType())) {
      return transaction.getProfitSharingAmount();
    } else {
      return transaction.getTxAmount().multiply(merchantConfig.getProfitSharingRate());
    }
  }

  /**
   * 创建分账服务
   */
  private ProfitSharingService createProfitSharingService() throws WxPayException {
    WxPayConfig wxPayConfig = new WxPayConfig();
    wxPayConfig.setSignType(WxPayConstants.SignType.MD5);
    wxPayConfig.setUseSandboxEnv(false);
    return bsinWxPayServiceUtil.getProfitSharingService(wxPayConfig);
  }

  /**
   * 添加分账接收方
   */
  private void addProfitSharingReceiver(ProfitSharingService profitSharingService) throws WxPayException {
    // TODO 查询是否已经绑定过，绑定过则不需要绑定
    // 查询 waas_profit_sharing_receiver 表,存在支付商户账号，则认为已经绑定，直接查询出接收方信息，return
    ProfitSharingReceiverRequest receiverRequest = new ProfitSharingReceiverRequest();
    receiverRequest.setReceiver("");
    profitSharingService.addReceiver(receiverRequest);

  }

  /**
   * 执行分账请求
   */
  private void executeProfitSharingRequest(ProfitSharingService profitSharingService,
                                           Transaction transaction) throws WxPayException {
    ProfitSharingRequest profitSharingRequest = buildProfitSharingRequest(transaction);
    profitSharingService.multiProfitSharing(profitSharingRequest);
  }

  /**
   * 支付分账结果封装类
   */
  private static class PaymentAllocationResult {
    private final boolean success;
    private final String errorMessage;

    public PaymentAllocationResult(boolean success, String errorMessage) {
      this.success = success;
      this.errorMessage = errorMessage;
    }

    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
  }

  /**
   * 构建分账配置请求参数
   */
  private Map<String, Object> buildConfigRequestMap(Transaction transaction) {
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put("tenantId", transaction.getTenantId());
    requestMap.put("type", transaction.getProfitSharingType());
    return requestMap;
  }

  /**
   * 构建分账请求对象
   */
  private ProfitSharingRequest buildProfitSharingRequest(Transaction transaction) {
    ProfitSharingRequest request = new ProfitSharingRequest();
    // TODO: 根据实际业务需求设置分账请求参数
    return request;
  }

}
