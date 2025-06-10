package me.flyray.bsin.server.impl;

import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.constant.WxPayConstants;
import com.github.binarywang.wxpay.service.ProfitSharingService;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.facade.engine.EcologicalValueAllocationEngine;
import me.flyray.bsin.facade.engine.RevenueShareServiceEngine;
import me.flyray.bsin.facade.service.MerchantPayService;
import me.flyray.bsin.payment.BsinWxPayServiceUtil;
import org.apache.shenyu.client.apache.dubbo.annotation.ShenyuDubboService;
import org.apache.shenyu.client.apidocs.annotations.ApiModule;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

@Slf4j
@ShenyuDubboService(path = "/revenueShare", timeout = 6000)
@ApiModule(value = "revenueShare")
public class RevenueShareServiceEngineImpl implements RevenueShareServiceEngine {


    @Autowired
    private EcologicalValueAllocationEngine ecologicalValueAllocationEngine;
    @Autowired
    private MerchantPayService merchantPayService;
    @Autowired
    private BsinWxPayServiceUtil bsinWxPayServiceUtil;

    /**
     * 执行分账分润
     * @param requestMap
     */
    @Override
    public void excute(Map<String, Object> requestMap) throws Exception {

        // 根据分账配置进行分账，逻辑实现
        merchantPayService.getProfitSharingConfig(requestMap);

        // 根据规则进行分账处理，调用第三服务
        WxPayConfig wxPayConfig = new WxPayConfig();
        wxPayConfig.setSignType(WxPayConstants.SignType.MD5);
        //      wxPayConfig.setCertSerialNo(certSerialNo);
        //      wxPayConfig.setPrivateKeyContent(
        //          payChannelConfigParams.getString("privateKey").getBytes(StandardCharsets.UTF_8));
        //      wxPayConfig.setPrivateCertString(payChannelConfigParams.getString("privateCert"));
        wxPayConfig.setUseSandboxEnv(false);
        ProfitSharingService profitSharingService = bsinWxPayServiceUtil.getProfitSharingService(wxPayConfig);

        // 生态贡献计算和价值分配
        ecologicalValueAllocationEngine.excute(requestMap);


    }

}
