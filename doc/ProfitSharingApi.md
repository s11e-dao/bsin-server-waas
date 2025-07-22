# 微信分账API接口文档

## 概述

本文档描述了微信分账功能的HTTP API接口，用于后台管理UI的分账操作、调试测试和系统集成。

## 项目结构

```
bsin-server-apps/bsin-server-waas/
├── waas-facade/src/main/java/me/flyray/bsin/facade/service/
│   └── ProfitSharingApiService.java          # 接口定义
└── waas-server/src/main/java/me/flyray/bsin/server/impl/
    └── ProfitSharingApiServiceImpl.java      # 接口实现
```

## 基础信息

- **基础路径**: `/wx/profitShare`
- **请求方式**: POST
- **数据格式**: JSON
- **字符编码**: UTF-8
- **接口定义**: `me.flyray.bsin.facade.service.ProfitSharingApiService`
- **接口实现**: `me.flyray.bsin.server.impl.ProfitSharingApiServiceImpl`

## 通用响应格式

### 成功响应
```json
{
    "code": 0,
    "message": "操作成功",
    "data": {
        // 具体数据
    }
}
```

### 错误响应
```json
{
    "code": 1,
    "message": "错误信息"
}
```

## 接口列表

### 1. 请求分账

**接口地址**: `/wx/profitShare/request`

**功能描述**: 向微信支付平台发起分账请求

**请求参数**:
```json
{
    "transactionNo": "交易单号"
}
```

**响应示例**:
```json
{
    "code": 0,
    "message": "分账请求成功",
    "data": {
        "success": true,
        "message": "分账成功",
        "orderId": "分账单号",
        "state": "SUCCESS",
        "amount": 100.00
    }
}
```

### 2. 查询分账结果

**接口地址**: `/wx/profitShare/query`

**功能描述**: 查询分账请求的处理结果

**请求参数**:
```json
{
    "transactionNo": "交易单号"
}
```

**响应示例**:
```json
{
    "code": 0,
    "message": "查询成功",
    "data": {
        "success": true,
        "message": "SUCCESS",
        "orderId": "分账单号",
        "state": "SUCCESS",
        "amount": 100.00
    }
}
```

### 3. 请求分账回退

**接口地址**: `/wx/profitShare/return`

**功能描述**: 请求分账回退，将已分账资金退回

**请求参数**:
```json
{
    "orderId": "微信分账单号",
    "outReturnNo": "商户回退单号",
    "returnMchid": "回退商户号",
    "amount": 100.00,
    "description": "回退原因"
}
```

**响应示例**:
```json
{
    "code": 0,
    "message": "分账回退请求成功"
}
```

### 4. 查询分账回退结果

**接口地址**: `/wx/profitShare/returnQuery`

**功能描述**: 查询分账回退请求的处理结果

**请求参数**:
```json
{
    "orderId": "微信分账单号",
    "outReturnNo": "商户回退单号"
}
```

**响应示例**:
```json
{
    "code": 0,
    "message": "查询成功",
    "data": {
        "orderId": "微信分账单号",
        "outReturnNo": "商户回退单号",
        "status": "SUCCESS"
    }
}
```

### 5. 解冻剩余资金

**接口地址**: `/wx/profitShare/unfreeze`

**功能描述**: 解冻订单中剩余未分账资金

**请求参数**:
```json
{
    "transactionId": "微信支付订单号",
    "outOrderNo": "商户订单号",
    "description": "解冻原因"
}
```

**响应示例**:
```json
{
    "code": 0,
    "message": "解冻剩余资金成功"
}
```

### 6. 查询剩余待分金额

**接口地址**: `/wx/profitShare/remaining`

**功能描述**: 查询订单中剩余可分账金额

**请求参数**:
```json
{
    "transactionId": "微信支付订单号"
}
```

**响应示例**:
```json
{
    "code": 0,
    "message": "查询成功",
    "data": {
        "transactionId": "微信支付订单号",
        "remainingAmount": "100.00",
        "currency": "CNY"
    }
}
```

### 7. 添加分账接收方

**接口地址**: `/wx/profitShare/addReceiver`

**功能描述**: 添加分账接收方信息

**请求参数**:
```json
{
    "receiverId": "接收方账号",
    "receiverName": "接收方姓名",
    "receiverType": "MERCHANT_ID",
    "relationType": "STORE_OWNER",
    "customRelation": "自定义分账关系"
}
```

**响应示例**:
```json
{
    "code": 0,
    "message": "添加分账接收方成功"
}
```

### 8. 删除分账接收方

**接口地址**: `/wx/profitShare/deleteReceiver`

**功能描述**: 删除已添加的分账接收方

**请求参数**:
```json
{
    "receiverId": "接收方账号"
}
```

**响应示例**:
```json
{
    "code": 0,
    "message": "删除分账接收方成功"
}
```

### 9. 申请分账账单

**接口地址**: `/wx/profitShare/applyBill`

**功能描述**: 申请分账账单文件

**请求参数**:
```json
{
    "billDate": "2024-01-01",
    "tarType": "GZIP"
}
```

**响应示例**:
```json
{
    "code": 0,
    "message": "申请分账账单成功",
    "data": {
        "billDate": "2024-01-01",
        "tarType": "GZIP",
        "downloadUrl": "https://example.com/bill.zip",
        "hashValue": "sha256_hash_value"
    }
}
```

### 10. 下载账单

**接口地址**: `/wx/profitShare/downloadBill`

**功能描述**: 下载分账账单文件

**请求参数**:
```json
{
    "billDate": "2024-01-01",
    "tarType": "GZIP"
}
```

**响应示例**:
```json
{
    "code": 0,
    "message": "下载账单成功",
    "data": {
        "billDate": "2024-01-01",
        "tarType": "GZIP",
        "fileContent": "base64_encoded_file_content",
        "fileName": "profit_share_bill_2024-01-01.zip"
    }
}
```

## 参数说明

### 接收方类型 (receiverType)
- `MERCHANT_ID`: 商户号
- `PERSONAL_OPENID`: 个人openid

### 分账关系 (relationType)
- `STORE_OWNER`: 店主
- `STAFF`: 员工
- `STORE_OWNER`: 店主
- `PARTNER`: 合作伙伴
- `HEADQUARTER`: 品牌方
- `BRAND`: 品牌方
- `DISTRIBUTOR`: 分销商
- `USER`: 用户
- `SUPPLIER`: 供应商
- `CUSTOM`: 自定义

### 压缩类型 (tarType)
- `GZIP`: GZIP压缩
- `LZMA`: LZMA压缩

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 0 | 成功 |
| 1 | 失败 |

## 使用示例

### JavaScript示例

```javascript
// 请求分账
async function requestProfitShare(transactionNo) {
    const response = await fetch('/wx/profitShare/request', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            transactionNo: transactionNo
        })
    });
    
    const result = await response.json();
    return result;
}

// 查询分账结果
async function queryProfitShareResult(transactionNo) {
    const response = await fetch('/wx/profitShare/query', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            transactionNo: transactionNo
        })
    });
    
    const result = await response.json();
    return result;
}

// 添加分账接收方
async function addProfitShareReceiver(receiverInfo) {
    const response = await fetch('/wx/profitShare/addReceiver', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(receiverInfo)
    });
    
    const result = await response.json();
    return result;
}
```

### cURL示例

```bash
# 请求分账
curl -X POST http://localhost:8080/wx/profitShare/request \
  -H "Content-Type: application/json" \
  -d '{"transactionNo": "123456789"}'

# 查询分账结果
curl -X POST http://localhost:8080/wx/profitShare/query \
  -H "Content-Type: application/json" \
  -d '{"transactionNo": "123456789"}'

# 添加分账接收方
curl -X POST http://localhost:8080/wx/profitShare/addReceiver \
  -H "Content-Type: application/json" \
  -d '{
    "receiverId": "1900000109",
    "receiverName": "张三",
    "receiverType": "MERCHANT_ID",
    "relationType": "STORE_OWNER"
  }'
```

## 注意事项

1. **参数验证**: 所有接口都会验证必要参数，缺少参数会返回错误响应
2. **异常处理**: 接口内部异常会被捕获并返回友好的错误信息
3. **日志记录**: 所有请求和响应都会被记录到日志中
4. **幂等性**: 分账相关操作需要保证幂等性，避免重复操作
5. **安全性**: 建议在生产环境中添加适当的认证和授权机制

## 测试建议

1. **单元测试**: 为每个接口编写单元测试
2. **集成测试**: 测试与微信支付API的集成
3. **性能测试**: 测试接口的响应时间和并发处理能力
4. **异常测试**: 测试各种异常情况的处理

## 后续优化

1. **缓存机制**: 对查询结果进行缓存
2. **异步处理**: 将耗时操作改为异步处理
3. **监控告警**: 添加接口调用监控和异常告警
4. **限流控制**: 添加接口调用频率限制 