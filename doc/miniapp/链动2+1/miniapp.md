## 接入的小程序

| 小程序名称 |    小程序ID(appId)    |     ECS地址     |         域名         |         小程序秘钥(appSecret)         | 微信支付商户号(mchId) | 商户APIv2密钥(mchSecret)             | 商户APIv3密钥(apiV2Key)              | 服务商证书序列号(mchSerialNumber) | 服务商API私钥路径(privateKeyPath) | 通知回调地址(notifyUrl)       |  
|:-----:|:------------------:|:-------------:|:------------------:|:--------------------------------:|:---------------|:---------------------------------|:---------------------------------|:--------------------------|:---------------------------|:------------------------|        
| 分销商城  | wxfa99fb352d815a26 | 47.105.63.133 | gateway.iittii.com | ec29c82e7b8e8689e8e0f59ef84de1f4 | 1697497726     | Chenxiucai18853228353chenxiucai1 | Chenxiucai18853228353chenxiucai3 |                           |                            | https://域名/wxpay/wechat |  

### 配置参数

- 字段定义(interface)

~~~json
[
  {
    "name": "mchId",
    "desc": "微信支付商户号",
    "type": "text",
    "verify": "required"
  },
  {
    "name": "appId",
    "desc": "应用App ID",
    "type": "text",
    "verify": "required"
  },
  {
    "name": "appSecret",
    "desc": "应用AppSecret",
    "type": "text",
    "verify": "required",
    "star": "1"
  },
  {
    "name": "oauth2Url",
    "desc": "oauth2地址（置空将使用官方）",
    "type": "text"
  },
  {
    "name": "apiVersion",
    "desc": "微信支付API版本",
    "type": "radio",
    "values": "V2,V3",
    "titles": "V2,V3",
    "verify": "required"
  },
  {
    "name": "key",
    "desc": "APIv2密钥",
    "type": "textarea",
    "verify": "required",
    "star": "1"
  },
  {
    "name": "apiV3Key",
    "desc": "APIv3密钥（V3接口必填）",
    "type": "textarea",
    "verify": "",
    "star": "1"
  },
  {
    "name": "serialNo",
    "desc": "序列号（V3接口必填）",
    "type": "textarea",
    "verify": "",
    "star": "1"
  },
  {
    "name": "cert",
    "desc": "API证书(apiclient_cert.p12)",
    "type": "file",
    "verify": ""
  },
  {
    "name": "apiClientCert",
    "desc": "证书文件(apiclient_cert.pem) ",
    "type": "file",
    "verify": ""
  },
  {
    "name": "apiClientKey",
    "desc": "私钥文件(apiclient_key.pem)",
    "type": "file",
    "verify": ""
  }
]
~~~

- 配置(config)

~~~json
{
  "mchId": "1697497726",
  "appId": "wxfa99fb352d815a26",
  "appSecret": "ec29c82e7b8e8689e8e0f59ef84de1f4",
  "oauth2Url": "oauth2地址（置空将使用官方）",
  "apiVersion": "V2",
  "key": "Chenxiucai18853228353chenxiucai1",
  "apiV3Key": "Chenxiucai18853228353chenxiucai3",
  "serialNo": "序列号（V3接口必填）",
  "cert": "API证书(apiclient_cert.p12)",
  "apiClientCert": "证书文件(apiclient_cert.pem) ",
  "apiClientKey": "私钥文件(apiclient_key.pem)",
  "notifyUrl": "https://5p33041l87.vicp.fun/callback/wxPayCallback",
  "keyPath": "/home/leonard/ssd12/bsin-paas/bsin-paas-os/bsin-server-apps/bsin-server-waas/doc/miniapp/链动2+1/1697497726_20241106_cert/apiclient_key.pem"
}
~~~

- yaml配置(yaml)

~~~yaml
wx:
  pay:
    appId: wx74862e0dfcf69954
    mchId: 1558950191
    mchKey: 34345964330B66427E0D3D28826C4993C77E631F
    #    subAppId: #服务商模式下的子商户公众账号ID,普通模式请不要配置，请在配置文件中将对应项删除
    #    subMchId: #服务商模式下的子商户号，普通模式请不要配置，最好是请在配置文件中将对应项删除
    keyPath: apiclient_key.pem  # apiclient_cert.p12文件的绝对路径，或者如果放在项目中，请以classpath:开头指定
~~~

- 配置类WxPayConfiguration

~~~java

package com.github.binarywang.demo.wx.pay.config;

import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.service.WxPayService;
import com.github.binarywang.wxpay.service.impl.WxPayServiceImpl;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Binary Wang
 */
@Configuration
@ConditionalOnClass(WxPayService.class)
@EnableConfigurationProperties(WxPayProperties.class)
@AllArgsConstructor
public class WxPayConfiguration {
    private WxPayProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public WxPayService wxService() {
        WxPayConfig payConfig = new WxPayConfig();
        payConfig.setAppId(StringUtils.trimToNull(this.properties.getAppId()));
        payConfig.setMchId(StringUtils.trimToNull(this.properties.getMchId()));
        payConfig.setMchKey(StringUtils.trimToNull(this.properties.getMchKey()));
        payConfig.setSubAppId(StringUtils.trimToNull(this.properties.getSubAppId()));
        payConfig.setSubMchId(StringUtils.trimToNull(this.properties.getSubMchId()));
        payConfig.setKeyPath(StringUtils.trimToNull(this.properties.getKeyPath()));

        // 可以指定是否使用沙箱环境
        payConfig.setUseSandboxEnv(false);

        WxPayService wxPayService = new WxPayServiceImpl();
        wxPayService.setConfig(payConfig);
        return wxPayService;
    }

}
~~~