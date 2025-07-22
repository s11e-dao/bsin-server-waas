package me.flyray.bsin.server.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.binarywang.wxpay.bean.notify.WxPayNotifyResponse;
import com.github.binarywang.wxpay.bean.notify.WxPayOrderNotifyResult;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.constant.WxPayConstants;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.domain.entity.MerchantConfig;
import me.flyray.bsin.domain.entity.Transaction;
import me.flyray.bsin.domain.enums.TransactionStatus;
import me.flyray.bsin.dubbo.invoke.BsinServiceInvoke;
import me.flyray.bsin.exception.BusinessException;
import me.flyray.bsin.facade.service.MerchantConfigService;
import me.flyray.bsin.facade.service.MerchantPayService;
import me.flyray.bsin.infrastructure.mapper.TransactionJournalMapper;
import me.flyray.bsin.infrastructure.mapper.TransactionMapper;
import me.flyray.bsin.payment.BsinWxPayServiceUtil;
import me.flyray.bsin.server.service.ProfitSharingService;
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
  private static final String WX_PAY_CHANNEL = "wxPay";

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
  @Autowired
  private MerchantConfigService merchantConfigService;
  @Autowired
  private ProfitSharingService profitSharingService;

  /**
   * 1、解析回调结果
   * 2、验证更新交易状态
   * 3、执行分账（如果配置了分账）
   * 4、调用oms模块处理业务订单
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
      WxPayService wxPayService = getWxService(mchId);
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
      if (transaction == null) {
        log.warn("未找到对应的交易记录，订单号：{}", result.getOutTradeNo());
        return WxPayNotifyResponse.fail("未找到交易记录");
      }

      // 3. 支付分账阶段（独立事务）
      if ("SUCCESS".equals(result.getResultCode())) {
        executePaymentAllocationSafely(transaction);
      }

      // 4、异步调用（泛化调用解耦）订单完成方法统一处理： 根据订单类型后续处理
      bsinServiceInvoke.genericInvoke("UniflyOrderService", "completePay", "dev", requestMap);
    } catch (Exception e) {
      log.error("微信支付回调处理失败", e);
      return WxPayNotifyResponse.fail("支付失败");
    }
    return WxPayNotifyResponse.success("success");
  }

  /**
   * 安全执行支付分账，包含完整的异常处理
   * @param transaction 交易信息
   * @return PaymentAllocationResult 支付分账结果
   */
  private PaymentAllocationResult executePaymentAllocationSafely(Transaction transaction) {
    final String serialNo = transaction.getSerialNo();

    try {
      // 检查是否需要分账
      if (!shouldExecuteProfitSharing(transaction)) {
        log.info("无需分账，跳过支付分账，交易号：{}", serialNo);
        return new PaymentAllocationResult(false, null);
      }

      // 执行分账
      var result = profitSharingService.executeProfitSharing(transaction, WX_PAY_CHANNEL);
      
      if (result.isSuccess()) {
        log.info("支付分账执行成功，交易号：{}", serialNo);
        return new PaymentAllocationResult(true, null);
      } else {
        log.warn("支付分账执行失败，交易号：{}，错误信息：{}", serialNo, result.getMessage());
        return new PaymentAllocationResult(false, result.getMessage());
      }

    } catch (Exception e) {
      log.error("支付分账执行失败，交易号：{}", serialNo, e);
      return new PaymentAllocationResult(false, e.getMessage());
    }
  }

  /**
   * 判断是否需要执行分账
   */
  private boolean shouldExecuteProfitSharing(Transaction transaction) {
    // 检查交易金额是否满足分账条件
    if (transaction.getTxAmount() == null || transaction.getTxAmount().compareTo(BigDecimal.ZERO) <= 0) {
      return false;
    }

    // 检查是否配置了分账
    MerchantConfig merchantConfig = getMerchantConfig(transaction);
    if (merchantConfig == null || merchantConfig.getProfitSharingRate() == null) {
      return false;
    }

    // 检查分账类型
    String profitSharingType = transaction.getProfitSharingType();
    if (ORDER_TYPE.equals(profitSharingType)) {
      // 订单类型：检查是否有预设分账金额
      return transaction.getProfitSharingAmount() != null && 
             transaction.getProfitSharingAmount().compareTo(BigDecimal.ZERO) > 0;
    } else if (PRODUCT_TYPE.equals(profitSharingType)) {
      // 商品类型：检查分账比例
      return merchantConfig.getProfitSharingRate().compareTo(BigDecimal.ZERO) > 0;
    }

    return false;
  }

  /**
   * 获取商户配置
   */
  private MerchantConfig getMerchantConfig(Transaction transaction) {
    try {
      Map<String, Object> requestMap = new HashMap<>();
      // 使用toAddress作为商户号，因为Transaction中toAddress存储的是商户号
      requestMap.put("merchantNo", transaction.getToAddress());
      requestMap.put("tenantId", transaction.getTenantId());
      return merchantConfigService.getDetail(requestMap);
    } catch (Exception e) {
      log.error("获取商户配置失败，商户号：{}", transaction.getToAddress(), e);
      return null;
    }
  }

  /**
   * 获取微信支付服务
   */
  private WxPayService getWxService(String mchId) {
    // TODO: 根据mchId获取对应的微信支付配置
    // 这里需要根据商户信息获取对应的微信支付配置
    WxPayConfig config = new WxPayConfig();
    config.setMchId(mchId);
    config.setSignType(WxPayConstants.SignType.MD5);
    config.setUseSandboxEnv(false);
    
    if (wxRedisConfig == null) {
      wxRedisConfig = new WxRedisConfig();
      wxRedisConfig.setHost(wxRedisHost);
      wxRedisConfig.setPort(wxRedisPort);
      wxRedisConfig.setPassword(wxRedisPassword);
    }
    
    return bsinWxPayServiceUtil.getWxPayService(config);
  }

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
   * 支付宝支付回调接口（预留）
   */
  @PostMapping("/alipay/{mchId}")
  @ApiDoc(desc = "alipay")
  public Object alipay(
      @RequestBody(required = false) String body, @PathVariable("mchId") String mchId)
      throws Exception {
    // TODO: 实现支付宝支付回调处理
    log.info("支付宝支付回调，商户号：{}", mchId);
    return "success";
  }

  /**
   * 银联支付回调接口（预留）
   */
  @PostMapping("/unionpay/{mchId}")
  @ApiDoc(desc = "unionpay")
  public Object unionpay(
      @RequestBody(required = false) String body, @PathVariable("mchId") String mchId)
      throws Exception {
    // TODO: 实现银联支付回调处理
    log.info("银联支付回调，商户号：{}", mchId);
    return "success";
  }
}
