package me.flyray.bsin.infrastructure.biz;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import me.flyray.bsin.domain.entity.PayChannelConfig;
import me.flyray.bsin.domain.entity.ProfitSharingConfig;
import me.flyray.bsin.domain.entity.Transaction;
import me.flyray.bsin.domain.entity.WalletAccount;
import me.flyray.bsin.enums.TransactionType;
import me.flyray.bsin.exception.BusinessException;
import me.flyray.bsin.infrastructure.mapper.PayChannelConfigMapper;
import me.flyray.bsin.infrastructure.mapper.ProfitSharingConfigMapper;
import me.flyray.bsin.infrastructure.mapper.TransactionMapper;
import me.flyray.bsin.infrastructure.mapper.WalletAccountMapper;
import me.flyray.bsin.infrastructure.utils.OkHttpUtils;
import me.flyray.bsin.mq.enums.MqEventCode;
import me.flyray.bsin.mq.producer.RocketMQProducer;
import me.flyray.bsin.security.domain.LoginUser;
import me.flyray.bsin.utils.BsinSnowflake;
import me.flyray.bsin.utils.StringUtils;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Hash;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.Sign;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;

import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

@Service
public class TransactionBiz {

    private static final Logger log = LoggerFactory.getLogger(TransactionBiz.class);
    
    // 常量定义
    private static final String PRODUCT_PROFIT_SHARING_TYPE = "2";
    
    @Autowired
    private WalletAccountMapper walletAccountMapper;
    @Autowired
    private TransactionMapper transactionMapper;
    @Autowired
    private ProfitSharingConfigMapper profitSharingConfigMapper;
    @Autowired
    private PayChannelConfigMapper payChannelConfigMapper;

    @Value("${bsin.app-chain.gateway-url}")
    private String appChainGatewayUrl;

    @Value("${bsin.app-chain.get-gas.address}")
    private String getGasAddress;

    @Value("${bsin.app-chain.get-gas.amount}")
    private String getGasAmount;
    @Autowired
    private RocketMQProducer rocketMQProducer;
    @Value("${rocketmq.consumer.topic}")
    private String topic;



    // 节点
    private static final String HTTP_URL = "https://go.getblock.io/dc197e59d9b34e0c9428f2f13df66d6e";

    // 连接以太坊节点
    Web3j web3 = Web3j.build(new HttpService(HTTP_URL));

    /**
     * 代币转出交易
     * 1、构建未签名交易
     * 2、序列化交易生成交易 Hash
     * 3、调用 API 创建 MPC Sign 签名任务
     * 4、交易任务审批和签名
     * 5、构建签名交易
     * 6、广播交易
     * fromAddress 发起地址
     * toAddress  接受地址
     * contractAddress 合约地址
     * amount 交易金额
     * decimals 位数
     * gasValue gasPrice
     */
    @Async("taskExecutor")
    public String tokenTransfer(String fromAddress, String toAddress, String contractAddress, BigInteger amount, BigInteger decimals) throws Exception {
        // 1、构建未签名交易
        // 获取发起地址的nonce、chainId
        EthGetTransactionCount ethGetTransactionCount = web3.ethGetTransactionCount(fromAddress, DefaultBlockParameterName.LATEST).send();
        BigInteger nonce = ethGetTransactionCount.getTransactionCount();
        EthChainId ethChainId = web3.ethChainId().send();

        // 代币数量
        BigDecimal tokenValue = new BigDecimal(amount).multiply(new BigDecimal(Math.pow(10, decimals.longValue())));
        // 创建Function对象
        Function function = new Function(
                "transfer",
                Arrays.asList(new Address(toAddress), new Uint256(tokenValue.toBigInteger())), // 接受地址、携带代币数量
                Arrays.asList(new TypeReference<Type>() {
                }));
        String data = FunctionEncoder.encode(function);

        // 预估gas费
        org.web3j.protocol.core.methods.request.Transaction transaction = 
            org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(fromAddress, contractAddress, data);
        EthEstimateGas gasLimit = web3.ethEstimateGas(transaction).send();
        if(gasLimit.hasError()){
            throw new Exception(String.format("error estimate gas:%s-%s", gasLimit.getError().getCode(), gasLimit.getError().getMessage()));
        }
        // 设定最大优先费 maxfepergas
        BigInteger maxPriorityFeePerGas = Convert.toWei("60", Convert.Unit.GWEI).toBigInteger();
        // 获取最新区块
        EthBlock.Block latestBlock = web3.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send().getBlock();

        // 建议将basefepergas值设置为最新数据块basefepergas值的2倍
        // maxfepergas不能小于basefepergas + maxpriority fepergas
        BigDecimal maxFeePerGas = new BigDecimal(latestBlock.getBaseFeePerGas())
                .multiply(new BigDecimal("2"))
                .add(new BigDecimal(maxPriorityFeePerGas));

        // 加油--ETH转出交易
        String ethAddress ="0xf4f762dcf0e1ad96a9e98c321ef52fb1174c9325";
        BigInteger amountGas = gasLimit.getAmountUsed().multiply(maxFeePerGas.toBigInteger());
        log.info("加油gas费为："+amountGas);
        String ethTxHash = ethTransfer(ethAddress, fromAddress, amountGas);

        // 使线程休眠 5 秒钟
        Thread.sleep(60000); // 5000 毫秒 = 5 秒钟

        // 创建原始交易
        RawTransaction rawTransaction = RawTransaction.createTransaction(
                ethChainId.getChainId().longValue(),
                nonce,
                gasLimit.getAmountUsed(),
//                BigInteger.valueOf(21000), // Gas limit
                contractAddress,
                Convert.toWei("0", Convert.Unit.ETHER).toBigInteger(),
                data,
                maxPriorityFeePerGas,
                maxFeePerGas.toBigInteger());

        // 2、序列化交易生成交易 Hash
        // 对交易进行编码并计算哈希值
        byte[] encodedRawTransaction = TransactionEncoder.encode(rawTransaction);
        String unsignedHash = Numeric.toHexString(Hash.sha3(encodedRawTransaction));

        // 3、签名交易
        String signedTransaction = signRawTransaction(rawTransaction, unsignedHash, fromAddress);

        // 4、广播交易
        EthSendTransaction ethSendTransaction = web3.ethSendRawTransaction(signedTransaction).send();
        String transactionHash = ethSendTransaction.getTransactionHash();
        if(transactionHash == null){
            log.info("转出合约代币，token_error:{}",ethSendTransaction.getError().getMessage());
        }
        System.out.println("TOKEN_transactionHash:"+transactionHash);
        return transactionHash;
    }

    /**
     * ETH转出交易
     * fromAddress  发起地址
     * toAddress    接受地址
     * amount       eth数量
     * gasLimit     最大gas费
     * gasPrice     gas费价格
     */
    public String ethTransfer(String fromAddress, String toAddress, BigInteger amount) throws Exception {
        log.info("开始ETH转出交易，fromAddress:{}，toAddress:{},amount:{}", fromAddress, toAddress, amount);
        // 获取发起地址的nonce、chainId
        EthGetTransactionCount ethGetTransactionCount = web3.ethGetTransactionCount(
                fromAddress, DefaultBlockParameterName.LATEST).sendAsync().get();
        BigInteger nonce = ethGetTransactionCount.getTransactionCount();
        EthChainId ethChainId = web3.ethChainId().send();

        // 设置 maxPriorityFeePerGas 和 maxFeePerGas
        // 设定最大优先费 maxfepergas
        BigInteger maxPriorityFeePerGas = Convert.toWei("60", Convert.Unit.GWEI).toBigInteger();
        // 获取最新区块
        EthBlock.Block latestBlock = web3.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send().getBlock();

        // 建议将basefepergas值设置为最新数据块basefepergas值的2倍
        // maxfepergas不能小于basefepergas + maxpriority fepergas
        BigDecimal maxFeePerGas = new BigDecimal(latestBlock.getBaseFeePerGas())
                .multiply(new BigDecimal("2"))
                .add(new BigDecimal(maxPriorityFeePerGas));

        RawTransaction rawTransaction = RawTransaction.createEtherTransaction(
                ethChainId.getChainId().longValue(),
                nonce,
                BigInteger.valueOf(21000), // Gas limit
                toAddress,
                amount , // amount
                maxPriorityFeePerGas,
                maxFeePerGas.toBigInteger()
        );

        // 2、序列化交易生成交易 Hash
        byte[] encodedRawTransaction = TransactionEncoder.encode(rawTransaction);
        String unsignedHash = Numeric.toHexString(Hash.sha3(encodedRawTransaction));

        // 3、签名交易
        String signedTransaction = signRawTransaction(rawTransaction, unsignedHash, fromAddress);

        // 4、广播交易
        EthSendTransaction ethSendTransaction = web3.ethSendRawTransaction(signedTransaction).send();
        String transactionHash = ethSendTransaction.getTransactionHash();
        if(transactionHash == null){
            log.info("转出ETH错误，eth_error:{}",ethSendTransaction.getError().getMessage());
        }
        log.info("ETH_transactionHash: " + transactionHash);
        return transactionHash;
    }

    /**
     * 签名交易
     * @param rawTransaction
     * @param unsignedHash
     * @param address
     * @return
     * @throws Exception
     */
    public String signRawTransaction(RawTransaction rawTransaction,String unsignedHash,String address) throws Exception {
        log.info("开始签名交易，address:{}",address);
        QueryWrapper<WalletAccount> queryWrapper = new QueryWrapper();
        queryWrapper.eq("address", address);
        WalletAccount walletAccount = walletAccountMapper.selectOne(queryWrapper);

        // 3、调用 API 创建 MPC Sign 签名任务
        String pubkey = walletAccount.getPubKey();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("message", unsignedHash);
        JSONObject jsonData = OkHttpUtils.httpPost(appChainGatewayUrl+ "/api/v1/mpc/sign/" + pubkey, jsonObject);
        String sig = (String) jsonData.get("signature");

        // 5、构建签名交易
        // Split sig into R, S, V
        String sigR = sig.substring(0, 64);
        String sigS = sig.substring(64, 128);
        String sigV = sig.substring(128);

        Integer v = Integer.parseInt(sigV, 16) + 27;

        // 创建签名。SignatureData对象
        Sign.SignatureData signatureData = new Sign.SignatureData(v.byteValue(),
                Numeric.hexStringToByteArray(sigR),
                Numeric.hexStringToByteArray(sigS));
        // 这里的 unsignedTransaction 是获取到签名结果后重新构建的，构建方法与上文中的 "构建未签名交易" 完全一致，
        // 需要注意的是，两次构建过程中使用的所有数据必须保持完全一致。
        byte[] signedMessage = TransactionEncoder.encode(rawTransaction, signatureData);
        String signedTransaction = Numeric.toHexString(signedMessage);
        log.info("签名交易完成，address:{}",address);
        return signedTransaction;
    }

    /**
     * 查询
     * @param contractAddress 代币合约地址
     * @param holderAddress 代币持有者地址
     * @return
     * @throws Exception
     */
    public BigInteger getTokenBalance (String contractAddress,String holderAddress) throws Exception {
        // 构造查询代币余额的函数
        Function function = new Function(
                "balanceOf",
                Arrays.asList(new org.web3j.abi.datatypes.Address(holderAddress)),
                Arrays.asList(new TypeReference<Uint256>() {})
        );
        String encodedFunction = FunctionEncoder.encode(function);

        // 发送以太坊调用请求
        EthCall ethCall = web3.ethCall(org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                null, contractAddress, encodedFunction), DefaultBlockParameterName.LATEST).send();

        // 解析调用结果
        List<Type> results = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
        BigInteger balance = (BigInteger) results.get(0).getValue();

        // 打印余额
        System.out.println("Token Balance: " + balance);
        return balance;
    }

    /**
     * 用户充值资金归集
     * 1、给用户账户加油
     * 2、归集资金(队列处理)
     */
    public void cashConcentrationProcess(String fromAddress, String toAddress, String contractAddress, BigInteger amount, BigInteger decimals) throws Exception {
        // TODO 支付gas费用账户通过数据库配置 链原生TOKEN转账逻辑 ethTransfer String fromAddress, String toAddress, BigInteger amount

        // 1、给用户账户加油
        String txHash = ethTransfer(getGasAddress, toAddress, new BigInteger(getGasAmount));

        // 2、归集资金(队列处理) 调用延时队列，等加油成功之后做资金归集
        JSONObject mQMsgReq = new JSONObject();
        mQMsgReq.put("txHash", txHash);
        mQMsgReq.put("eventCode", MqEventCode.GET_GAS_NOTIFY.getCode());
        mQMsgReq.put("fromAddress", fromAddress);
        mQMsgReq.put("toAddress", MqEventCode.GET_GAS_NOTIFY.getCode());
        mQMsgReq.put("contractAddress", contractAddress);
        mQMsgReq.put("amount", amount);
        mQMsgReq.put("decimals", decimals);

        // 延时消息等级分为18个：1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
        SendCallback callback = new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                System.out.println("发送成功");
            }
            @Override
            public void onException(Throwable throwable) {
                System.out.println("发送失败");
            }
        };
        rocketMQProducer.sendDelay(topic, mQMsgReq.toString(), callback,4);
    }

    /**
     * gas 加油事件通知
     * 1、gas 加油确认
     * 2、进行资金归集
     * 3、放入队列等待确认
     */
    public void cashConcentration(JSONObject mQMsg) throws Exception {
        // 查询gas交易确认 通过hash获取链上交易
        String gasTxHash = (String) mQMsg.get("txHash");
        // 通过交易哈希查询交易收据
        TransactionReceipt transactionReceipt = web3.ethGetTransactionReceipt(gasTxHash).send().getTransactionReceipt().orElseThrow(() -> new RuntimeException("交易未找到"));

        String fromAddress = (String) mQMsg.get("");
        String toAddress = (String) mQMsg.get("");
        String contractAddress = (String) mQMsg.get("");
        BigInteger amount = (BigInteger) mQMsg.get("");
        BigInteger decimals = (BigInteger) mQMsg.get("");
        // 检查交易状态
        if (transactionReceipt.isStatusOK()) {
            // 交易成功进行资金归集 String fromAddress, String toAddress, String contractAddress, BigInteger amount, BigInteger decimals
            String txHash = tokenTransfer(fromAddress, toAddress, contractAddress, amount, decimals);

            // 放入队列等待确认
            JSONObject mQMsgReq = new JSONObject();
            mQMsgReq.put("txHash", txHash);
            mQMsgReq.put("eventCode", MqEventCode.CASH_CONCENTRATION_NOTIFY.getCode());
            // 延时消息等级分为18个：1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
            SendCallback callback = new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    System.out.println("发送成功");
                }
                @Override
                public void onException(Throwable throwable) {
                    System.out.println("发送失败");
                }
            };
            rocketMQProducer.sendDelay(topic, mQMsgReq.toString(), callback,4);

        } else {
            // 继续gas加油，继续仍给延时队列处理

        }

    }

    /**
     * 队列通知资金归集结果
     * 1、确认链上交易状态
     * 2、回调平台或是商户
     */
    public void cashConcentrationEventNotify(JSONObject mQMsg) throws IOException {
        // 查询链上交易，确认归集状态
// 查询gas交易确认 通过hash获取链上交易
        String gasTxHash = (String) mQMsg.get("txHash");
        // 通过交易哈希查询交易收据
        TransactionReceipt transactionReceipt = web3.ethGetTransactionReceipt(gasTxHash).send().getTransactionReceipt().orElseThrow(() -> new RuntimeException("交易未找到"));
        // 检查交易状态
        if (transactionReceipt.isStatusOK()) {
            // 通知平台或是商户

        }
    }

    /**
     * 创建或获取交易订单，并处理分账配置
     * 
     * @param outOrderNo 外部订单号
     * @param payAmount 支付金额
     * @param loginUser 登录用户信息
     * @param tenantId 租户ID
     * @param merchantNo 商户号
     * @param customerNo 客户号
     * @param remark 备注
     * @param profitSharingType 分账类型
     * @param profitSharingAmountStr 分账金额
     * @return 交易订单
     */
    public Transaction createOrGetTransactionWithProfitSharing(
            String outOrderNo, String payAmount, LoginUser loginUser, 
            String tenantId, String merchantNo, String customerNo, String remark,
            String profitSharingType, String profitSharingAmountStr) {
        
        log.info("开始创建或获取交易订单，outOrderNo: {}", outOrderNo);
        
        // 查询是否已存在交易订单
        Transaction waasTransaction = transactionMapper.selectOne(
            new LambdaQueryWrapper<Transaction>().eq(Transaction::getOutSerialNo, outOrderNo));
            
        // 如果交易不存在，创建新交易
        if (waasTransaction == null) {
            log.info("创建新交易订单，outOrderNo: {}", outOrderNo);
            
            waasTransaction = new Transaction();
            waasTransaction.setSerialNo(BsinSnowflake.getId());
            waasTransaction.setOutSerialNo(outOrderNo);
            waasTransaction.setTransactionType(TransactionType.PAY.getCode());
            waasTransaction.setComment(remark);
            waasTransaction.setTxAmount(new java.math.BigDecimal(payAmount));
            waasTransaction.setFromAddress(customerNo);
            waasTransaction.setToAddress(merchantNo);
            waasTransaction.setBizRoleType(loginUser.getBizRoleType());
            waasTransaction.setBizRoleTypeNo(loginUser.getBizRoleTypeNo());
            waasTransaction.setTenantId(tenantId);
            waasTransaction.setCreateTime(new java.util.Date());
            waasTransaction.setCreateBy(customerNo);

            // 处理分账配置
            boolean needProfitSharing = processProfitSharingConfig(
                waasTransaction, profitSharingType, profitSharingAmountStr, 
                merchantNo, tenantId, payAmount);
            
            waasTransaction.setProfitSharing(needProfitSharing);
            transactionMapper.insert(waasTransaction);
            log.info("交易订单创建成功，transactionNo: {}", waasTransaction.getSerialNo());
        }
        
        return waasTransaction;
    }

    /**
     * 处理分账配置逻辑
     * 
     * @param transaction 交易对象
     * @param profitSharingType 分账类型
     * @param profitSharingAmountStr 分账金额字符串
     * @param merchantNo 商户号
     * @param tenantId 租户ID
     * @param payAmount 支付金额
     * @return 是否需要分账
     */
    private boolean processProfitSharingConfig(Transaction transaction, String profitSharingType, 
            String profitSharingAmountStr, String merchantNo, String tenantId, String payAmount) {
        
        boolean needProfitSharing = false;
        
        if (PRODUCT_PROFIT_SHARING_TYPE.equals(profitSharingType)) {
            // 基于商品进行分账
            if (StringUtils.isNotEmpty(profitSharingAmountStr)) {
                needProfitSharing = true;
                transaction.setProfitSharingAmount(new java.math.BigDecimal(profitSharingAmountStr));
                transaction.setProfitSharingType(profitSharingType);
                log.debug("基于商品分账，分账金额: {}", profitSharingAmountStr);
            }
        } else {
            // 查询商户让利配置
            ProfitSharingConfig profitSharingConfig = profitSharingConfigMapper.selectOne(
                new LambdaQueryWrapper<ProfitSharingConfig>()
                    .eq(ProfitSharingConfig::getMerchantNo, merchantNo)
                    .eq(ProfitSharingConfig::getTenantId, tenantId));

            if (profitSharingConfig != null) {
                needProfitSharing = true;
                java.math.BigDecimal calculatedAmount = profitSharingConfig.getMerchantSharingRate()
                    .multiply(new java.math.BigDecimal(payAmount));
                transaction.setProfitSharingAmount(calculatedAmount);
                transaction.setProfitSharingType(profitSharingType);
                log.debug("基于商户让利配置分账，分账金额: {}", calculatedAmount);
            }
        }
        
        return needProfitSharing;
    }

    /**
     * 根据业务角色应用ID查询支付渠道配置
     * 
     * @param bizRoleAppId 业务角色应用ID
     * @return 支付渠道配置的参数JSON字符串
     */
    public String getPayChannelConfigParamsString(String bizRoleAppId) {
        log.info("查询支付渠道配置，bizRoleAppId: {}", bizRoleAppId);
        
        // 查询支付渠道配置
        LambdaQueryWrapper<PayChannelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PayChannelConfig::getBizRoleAppId, bizRoleAppId);
        wrapper.orderByDesc(PayChannelConfig::getCreateTime);
        PayChannelConfig payChannelConfig = payChannelConfigMapper.selectOne(wrapper);
        
        if (payChannelConfig == null) {
            log.error("支付渠道配置不存在，bizRoleAppId: {}", bizRoleAppId);
            throw new BusinessException("支付渠道配置不存在");
        }
        
        log.debug("支付渠道配置查询成功");
        return payChannelConfig.getParams();
    }
}
