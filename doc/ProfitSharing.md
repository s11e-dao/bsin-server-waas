# 分账架构设计说明

## 概述

本分账架构设计支持多支付渠道的分账功能，采用策略模式实现，具有良好的扩展性和维护性。

## 架构组件

### 1. 核心接口

#### ProfitSharingStrategy
- **位置**: `me.flyray.bsin.facade.service.ProfitSharingStrategy`
- **作用**: 分账策略抽象接口，定义所有分账操作的标准方法
- **主要方法**:
  - `executeProfitSharing()`: 执行分账
  - `addProfitSharingReceiver()`: 添加分账接收方
  - `queryProfitSharingResult()`: 查询分账结果
  - `returnProfitSharing()`: 分账回退
  - `unfreezeRemainingFunds()`: 解冻剩余资金

### 2. 策略实现

#### WxProfitSharingStrategyImpl
- **位置**: `me.flyray.bsin.server.impl.WxProfitSharingStrategyImpl`
- **作用**: 微信支付分账策略实现
- **特点**: 使用WxJava库调用微信分账API

#### AliProfitSharingStrategyImpl
- **位置**: `me.flyray.bsin.server.impl.AliProfitSharingStrategyImpl`
- **作用**: 支付宝分账策略实现
- **特点**: 调用支付宝分账API

### 3. 策略工厂

#### ProfitSharingStrategyFactory
- **位置**: `me.flyray.bsin.server.service.ProfitSharingStrategyFactory`
- **作用**: 管理不同支付渠道的分账策略
- **功能**:
  - 策略注册
  - 策略获取
  - 支持渠道查询

### 4. 统一服务

#### ProfitSharingService
- **位置**: `me.flyray.bsin.server.service.ProfitSharingService`
- **作用**: 分账功能的统一入口
- **特点**: 根据支付渠道类型自动选择对应的分账策略

## 使用流程

### 1. 支付回调处理

```java
// 在PayCallbackController中
@PostMapping("/wxpay/{mchId}")
public Object wxpay(@RequestBody String body, @PathVariable String mchId) {
    // 1. 解析回调结果
    // 2. 更新交易状态
    // 3. 执行分账
    if ("SUCCESS".equals(result.getResultCode())) {
        executePaymentAllocationSafely(transaction);
    }
    // 4. 处理业务逻辑
}
```

### 2. 分账执行

```java
// 在PayCallbackController中
private PaymentAllocationResult executePaymentAllocationSafely(Transaction transaction) {
    // 检查是否需要分账
    if (!shouldExecuteProfitSharing(transaction)) {
        return new PaymentAllocationResult(false, null);
    }
    
    // 执行分账
    var result = profitSharingService.executeProfitSharing(transaction, "wxPay");
    return new PaymentAllocationResult(result.isSuccess(), result.getMessage());
}
```

## 扩展新支付渠道

### 1. 创建策略实现

```java
@Service("newPayProfitSharingStrategy")
public class NewPayProfitSharingStrategyImpl implements ProfitSharingStrategy {
    @Override
    public ProfitSharingResult executeProfitSharing(Transaction transaction) {
        // 实现新支付渠道的分账逻辑
    }
    
    @Override
    public String getPayChannelType() {
        return "newPay";
    }
    
    // 实现其他方法...
}
```

### 2. 注册策略

```java
// 在ProfitSharingStrategyFactory中
@Autowired
public ProfitSharingStrategyFactory(ProfitSharingStrategy wxProfitSharingStrategy,
                                   ProfitSharingStrategy aliProfitSharingStrategy,
                                   ProfitSharingStrategy newPayProfitSharingStrategy) {
    strategyMap.put("wxPay", wxProfitSharingStrategy);
    strategyMap.put("aliPay", aliProfitSharingStrategy);
    strategyMap.put("newPay", newPayProfitSharingStrategy);
}
```

### 3. 添加回调接口

```java
// 在PayCallbackController中
@PostMapping("/newpay/{mchId}")
public Object newpay(@RequestBody String body, @PathVariable String mchId) {
    // 实现新支付渠道的回调处理
    // 调用分账服务
    profitSharingService.executeProfitSharing(transaction, "newPay");
}
```

## 数据模型

### 1. 分账流水 (ProfitSharingJournal)
- 记录每次分账操作的详细信息
- 包含交易号、接收方、分账金额、状态等

### 2. 分账接收方 (ProfitSharingReceiver)
- 存储分账接收方信息
- 支持多支付渠道

### 3. 商户配置 (MerchantConfig)
- 存储商户的分账配置
- 包含分账比例等参数

## 配置说明

### 1. 分账类型
- **订单分账**: 使用预设的分账金额
- **商品分账**: 根据交易金额和分账比例计算

### 2. 分账条件
- 交易金额大于0
- 商户配置了分账参数
- 存在分账接收方

## 注意事项

1. **异常处理**: 分账失败不影响主交易流程
2. **幂等性**: 分账操作需要保证幂等性
3. **事务管理**: 分账操作使用独立事务
4. **日志记录**: 详细记录分账操作的日志
5. **监控告警**: 分账失败需要监控告警

## 后续优化

1. **异步处理**: 将分账操作改为异步处理
2. **重试机制**: 添加分账失败的重试机制
3. **监控面板**: 提供分账操作的监控面板
4. **配置管理**: 提供分账配置的管理界面 