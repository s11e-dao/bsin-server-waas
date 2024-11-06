## 接入的小程序

| 小程序名称 |      小程序ID(appId)      |           小程序秘钥(appSecret)           | 微信支付商户号(mchId) | 商户APIv2密钥(mchSecret)                        | 商户APIv3密钥(apiV2Key)                       | 服务商证书序列号(mchSerialNumber) | 服务商API私钥路径(privateKeyPath) | 通知回调地址(notifyUrl)       |  
|:-----:|:----------------------:|:------------------------------------:|:---------------|:--------------------------------------------|:------------------------------------------|:--------------------------|:---------------------------|:------------------------|        
| 分销商城  | wxfa99fb352d8545415a26 | cfb9abaf28c699bcd62b5d545498e8184ff9 | 1697495457726  | cChenxiucai18853228354353453che5354nxiucai1 | Chenxiuca5345i18853228353chen53453xiucai1 |                           |                            | https://域名/wxpay/wechat |  


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
  "mchId": "169749712334726",
  "appId": "wxfa99fb352d8143555a26",
  "appSecret": "cfb9abaf28c69943434bcd62b5d98e8184ff9",
  "oauth2Url": "oauth2地址（置空将使用官方）",
  "apiVersion": "V3",
  "key": "Chenxiucai1885322843434343353chenxiucai1",
  "apiV3Key": "Chenxiucai188532284w25466353chenxiucai1",
  "serialNo": "序列号（V3接口必填）",
  "cert": "API证书(apiclient_cert.p12)",
  "apiClientCert": "证书文件(apiclient_cert.pem) ",
  "apiClientKey": "私钥文件(apiclient_key.pem)"
}
~~~