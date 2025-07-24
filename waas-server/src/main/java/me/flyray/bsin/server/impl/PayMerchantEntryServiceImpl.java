package me.flyray.bsin.server.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.constant.WxPayConstants;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;

import com.github.binarywang.wxpay.bean.applyment.WxPayApplyment4SubCreateRequest;
import com.github.binarywang.wxpay.bean.applyment.WxPayApplymentCreateResult;
import com.github.binarywang.wxpay.bean.bank.BankBranchesResult;
import com.github.binarywang.wxpay.bean.bank.BankInfo;
import com.github.binarywang.wxpay.bean.bank.BankingResult;
import com.github.binarywang.wxpay.bean.media.ImageUploadResult;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.Applyment4SubService;
import com.github.binarywang.wxpay.service.impl.Applyment4SubServiceImpl;

import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.domain.entity.MerchantPayEntry;
import me.flyray.bsin.domain.enums.PayChannelInterfaceEnum;
import me.flyray.bsin.exception.BusinessException;
import me.flyray.bsin.facade.service.PayMerchantEntryService;
import me.flyray.bsin.payment.BsinWxPayServiceUtil;
import me.flyray.bsin.security.contex.LoginInfoContextHelper;
import me.flyray.bsin.security.domain.LoginUser;
import me.flyray.bsin.utils.BsinSnowflake;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shenyu.client.apache.dubbo.annotation.ShenyuDubboService;
import org.apache.shenyu.client.apidocs.annotations.ApiDoc;
import org.apache.shenyu.client.apidocs.annotations.ApiModule;
import org.apache.shenyu.client.dubbo.common.annotation.ShenyuDubboClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import com.github.binarywang.wxpay.bean.applyment.ApplymentStateQueryResult;

/**
 * 支付商户进件
 */

@Slf4j
@ShenyuDubboService(path = "/payMerchantEntry", timeout = 6000)
@ApiModule(value = "payMerchantEntry")
@Service
public class PayMerchantEntryServiceImpl implements PayMerchantEntryService {

    @Autowired
    private BsinWxPayServiceUtil bsinWxPayServiceUtil;

    /**
     * 支付渠道进件
     * 根据支付渠道请求不同的渠道商进件
     * @param requestMap
     * @return
     */
    @Override
    @ShenyuDubboClient("/apply")
    @ApiDoc(desc = "apply")
    public Map<String, Object> apply(Map<String, Object> requestMap) {
        log.info("开始处理支付渠道进件，请求参数：{}", requestMap);
        
        // 获取登录用户信息
        LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
        
        // 获取支付渠道参数
        String payChannel = MapUtils.getString(requestMap, "payChannel");
        if (StringUtils.isEmpty(payChannel)) {
            log.error("支付渠道参数不能为空");
            throw new BusinessException("PAY_CHANNEL_EMPTY", "支付渠道参数不能为空");
        }
        
        // 根据支付渠道枚举获取对应的处理逻辑
        PayChannelInterfaceEnum channelEnum = PayChannelInterfaceEnum.getInstanceById(payChannel);
        if (channelEnum == null) {
            log.error("不支持的支付渠道：{}", payChannel);
            throw new BusinessException("UNSUPPORTED_PAY_CHANNEL", "不支持的支付渠道：" + payChannel);
        }
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            switch (channelEnum) {
                case WXPAY:
                    result = handleWxPayEntry(requestMap, loginUser);
                    break;
                case ALIPAY:
                    result = handleAliPayEntry(requestMap, loginUser);
                    break;
                default:
                    log.error("未实现的支付渠道进件处理：{}", channelEnum.getDesc());
                    throw new BusinessException("UNIMPLEMENTED_PAY_CHANNEL", "未实现的支付渠道进件处理：" + channelEnum.getDesc());
            }
            
            log.info("支付渠道进件处理完成，渠道：{}，结果：{}", payChannel, result);
            return result;
            
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("支付渠道进件处理异常，渠道：{}，错误信息：{}", payChannel, e.getMessage(), e);
            throw new BusinessException("PAY_ENTRY_ERROR", "支付渠道进件处理异常：" + e.getMessage());
        }
    }
    
    /**
     * 处理微信支付进件
     */
    private Map<String, Object> handleWxPayEntry(Map<String, Object> requestMap, LoginUser loginUser) {
        log.info("开始处理微信支付进件");
        
        try {

            // 2. 获取微信支付配置
            WxPayConfig wxPayConfig = getWxPayConfig(requestMap);
            
            // 3. 创建微信支付服务实例
            WxPayService wxPayService = bsinWxPayServiceUtil.getWxPayService(wxPayConfig);
            
            // 4. 调用微信支付进件API
            String applymentId = simulateWxPayApplyment(requestMap, wxPayService);
            
            // 5. 返回进件结果（不保存数据库，由调用方保存）
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "微信支付进件申请已提交");
            result.put("applymentId", applymentId);
            result.put("status", "PENDING");
            result.put("channel", "wxPay");
            result.put("businessCode", MapUtils.getString(requestMap, "businessCode"));
            result.put("requestJson", JSONObject.toJSONString(requestMap));
            result.put("responseJson", JSONObject.toJSONString(Map.of("applymentId", applymentId, "status", "PENDING")));
            result.put("createTime", new Date());
            
            log.info("微信支付进件申请成功，申请单号：{}", applymentId);
            return result;
            
        } catch (Exception e) {
            log.error("微信支付进件处理异常：{}", e.getMessage(), e);
            throw new BusinessException("WX_PAY_ENTRY_ERROR", "微信支付进件处理异常：" + e.getMessage());
        }
    }
    
    /**
     * 获取微信支付配置
     */
    private WxPayConfig getWxPayConfig(Map<String, Object> requestMap) {
        WxPayConfig wxPayConfig = new WxPayConfig();
        wxPayConfig.setAppId("your_app_id");
        wxPayConfig.setMchId("your_mch_id");
        wxPayConfig.setMchKey("your_mch_key");
        wxPayConfig.setSignType(WxPayConstants.SignType.MD5);
        wxPayConfig.setUseSandboxEnv(false);
        
        return wxPayConfig;
    }
    
    /**
     * 微信支付进件申请
     * 使用服务商模式进行进件
     */
    private String simulateWxPayApplyment(Map<String, Object> requestMap, WxPayService wxPayService) {
        log.info("开始微信支付进件申请");
        
        try {
            // 获取当前用户信息
            String tenantId = LoginInfoContextHelper.getTenantId();
            String merchantId = LoginInfoContextHelper.getLoginUser().getMerchantNo();
            String storeId = LoginInfoContextHelper.getLoginUser().getStoreNo();
            
            // 生成业务申请编号
            String businessCode = generateBizBillNo(tenantId, merchantId, storeId);
            
            // 创建服务商进件服务
            Applyment4SubService applyment4SubService = new Applyment4SubServiceImpl(wxPayService);
            
            // 构建进件请求 - 使用JSON转换的方式
            WxPayApplyment4SubCreateRequest request = buildWxPayApplyment4SubRequest(requestMap, businessCode);
            
            // 调用微信支付进件API
            WxPayApplymentCreateResult result = applyment4SubService.createApply(request);
            
            String applymentId = result.getApplymentId();
            log.info("微信支付进件申请成功，申请单号：{}，业务编号：{}", applymentId, businessCode);
            
            return applymentId;
            
        } catch (WxPayException e) {
            log.error("微信支付进件申请失败：{}", e.getMessage(), e);
            throw new BusinessException("WX_PAY_APPLYMENT_ERROR", "微信支付进件申请失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("微信支付进件申请异常：{}", e.getMessage(), e);
            throw new BusinessException("WX_PAY_APPLYMENT_ERROR", "微信支付进件申请异常：" + e.getMessage());
        }
    }
    
    /**
     * 生成业务申请编号
     */
    private String generateBizBillNo(String tenantId, String merchantId, String storeId) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = String.valueOf((int)(Math.random() * 1000));
        return String.format("BIZ_%s_%s_%s_%s_%s", tenantId, merchantId, storeId, timestamp, random);
    }

    /**
     * 构建微信支付服务商进件请求
     * 使用JSON转换的方式，避免依赖特定的枚举类型
     */
    private WxPayApplyment4SubCreateRequest buildWxPayApplyment4SubRequest(Map<String, Object> requestMap, String businessCode) {
        // 构建完整的请求数据
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("businessCode", businessCode);
        
        // 复制所有必要的字段
        if (requestMap.containsKey("subjectInfo")) {
            requestData.put("subjectInfo", requestMap.get("subjectInfo"));
        }
        if (requestMap.containsKey("businessInfo")) {
            requestData.put("businessInfo", requestMap.get("businessInfo"));
        }
        if (requestMap.containsKey("settlementInfo")) {
            requestData.put("settlementInfo", requestMap.get("settlementInfo"));
        }
        if (requestMap.containsKey("bankAccountInfo")) {
            requestData.put("bankAccountInfo", requestMap.get("bankAccountInfo"));
        }
        if (requestMap.containsKey("contactInfo")) {
            requestData.put("contactInfo", requestMap.get("contactInfo"));
        }
        
        // 使用JSON转换创建请求对象
        String requestJson = JSONObject.toJSONString(requestData);
        log.info("微信支付进件请求JSON：{}", requestJson);
        
        // 通过JSON转换创建请求对象
        WxPayApplyment4SubCreateRequest request = JSONObject.parseObject(requestJson, WxPayApplyment4SubCreateRequest.class);
        
        log.info("微信支付服务商进件请求构建完成，业务编号：{}", businessCode);
        return request;
    }
    
    /**
     * 处理支付宝进件
     */
    private Map<String, Object> handleAliPayEntry(Map<String, Object> requestMap, LoginUser loginUser) {
        log.info("开始处理支付宝进件");
        
        try {
            // 1. 验证进件参数
            validateAliPayEntryParams(requestMap);
            
            // 2. 模拟支付宝进件申请
            String applymentId = "ALI_" + System.currentTimeMillis();
            
            // 3. 返回进件结果（不保存数据库，由调用方保存）
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "支付宝进件申请已提交");
            result.put("applymentId", applymentId);
            result.put("status", "PENDING");
            result.put("channel", "aliPay");
            result.put("businessCode", MapUtils.getString(requestMap, "businessCode"));
            result.put("requestJson", JSONObject.toJSONString(requestMap));
            result.put("responseJson", JSONObject.toJSONString(Map.of("applymentId", applymentId, "status", "PENDING")));
            result.put("createTime", new Date());
            
            log.info("支付宝进件申请成功，申请单号：{}", applymentId);
            return result;
            
        } catch (Exception e) {
            log.error("支付宝进件处理异常：{}", e.getMessage(), e);
            throw new BusinessException("ALI_PAY_ENTRY_ERROR", "支付宝进件处理异常：" + e.getMessage());
        }
    }
    
    /**
     * 验证支付宝进件参数
     */
    private void validateAliPayEntryParams(Map<String, Object> requestMap) {
        // 必填参数验证
        String businessCode = MapUtils.getString(requestMap, "businessCode");
        if (StringUtils.isEmpty(businessCode)) {
            throw new BusinessException("BUSINESS_CODE_EMPTY", "业务申请编号不能为空");
        }
        
        // 商户信息验证
        Map<String, Object> merchantInfo = (Map<String, Object>) requestMap.get("merchantInfo");
        if (merchantInfo == null) {
            throw new BusinessException("MERCHANT_INFO_EMPTY", "商户信息不能为空");
        }
    }
    
    /**
     * 进件状态查询
     */
    @Override
    @ShenyuDubboClient("/getApplyStatus")
    @ApiDoc(desc = "getApplyStatus")
    public Map<String, Object> getApplyStatus(Map<String, Object> requestMap) {
        log.info("开始查询进件状态，请求参数：{}", requestMap);
        
        // 获取登录用户信息
        LoginUser loginUser = LoginInfoContextHelper.getLoginUser();

        // 获取申请单号
        String applymentId = MapUtils.getString(requestMap, "applymentId");
        if (StringUtils.isEmpty(applymentId)) {
            throw new BusinessException("APPLYMENT_ID_EMPTY", "申请单号不能为空");
        }

        // 获取支付渠道
        String payChannel = MapUtils.getString(requestMap, "payChannel");
        if (StringUtils.isEmpty(payChannel)) {
            throw new BusinessException("PAY_CHANNEL_EMPTY", "支付渠道不能为空");
        }

        Map<String, Object> result = new HashMap<>();

        // 根据支付渠道查询状态
        if (PayChannelInterfaceEnum.WXPAY.getCode().equals(payChannel)) {
            result = queryWxPayStatus(applymentId);
        } else if (PayChannelInterfaceEnum.ALIPAY.getCode().equals(payChannel)) {
            result = queryAliPayStatus(applymentId);
        } else {
            throw new BusinessException("UNSUPPORTED_PAY_CHANNEL", "不支持的支付渠道：" + payChannel);
        }

        log.info("进件状态查询完成，申请单号：{}，状态：{}", applymentId, result.get("status"));
        return result;

    }
    
    /**
     * 查询微信支付进件状态
     */
    private Map<String, Object> queryWxPayStatus(String applymentId) {
        log.info("开始查询微信支付进件状态，申请单号：{}", applymentId);
        
        // 获取微信支付配置
        WxPayConfig wxPayConfig = getWxPayConfig(new HashMap<>());

        // 创建微信支付服务实例
        WxPayService wxPayService = bsinWxPayServiceUtil.getWxPayService(wxPayConfig);

        // 创建服务商进件服务
        Applyment4SubService applyment4SubService = new Applyment4SubServiceImpl(wxPayService);

        // 调用微信支付的状态查询API
        ApplymentStateQueryResult result = null;
        try {
            result = applyment4SubService.queryApplyStatusByApplymentId(applymentId);
        } catch (WxPayException e) {
            throw new RuntimeException(e);
        }

        // 构建返回结果
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("applymentId", applymentId);
        response.put("status", result.getApplymentState().name());
        response.put("message", getStatusMessage(result.getApplymentState().name()));
        response.put("updateTime", new Date());

        // TODO 根据结果回填特约商户支付信息


        log.info("微信支付进件状态查询成功，申请单号：{}，状态：{}", applymentId, result.getApplymentState());
        return response;

    }
    
    /**
     * 获取状态对应的中文描述
     */
    private String getStatusMessage(String status) {
        if (status == null) {
            return "未知状态";
        }
        switch (status) {
            case "SUBMITTED":
                return "已提交";
            case "AUDITING":
                return "审核中";
            case "APPROVED":
                return "审核通过";
            case "REJECTED":
                return "审核拒绝";
            case "CANCELED":
                return "已取消";
            default:
                return "未知状态";
        }
    }
    
    /**
     * 查询支付宝进件状态
     */
    private Map<String, Object> queryAliPayStatus(String applymentId) {
        log.info("查询支付宝进件状态，申请单号：{}", applymentId);
        
        // 这里应该调用支付宝的状态查询API
        // 暂时返回模拟状态
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("applymentId", applymentId);
        result.put("status", "PENDING"); // 可能的状态：PENDING, APPROVED, REJECTED
        result.put("message", "进件审核中");
        result.put("updateTime", new Date());
        
        return result;
    }

}
