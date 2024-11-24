package me.flyray.bsin.server.listen;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import me.flyray.bsin.domain.entity.*;
import me.flyray.bsin.domain.enums.TransactionStatus;
import me.flyray.bsin.domain.request.TransactionDTO;
import me.flyray.bsin.enums.TransactionType;
import me.flyray.bsin.infrastructure.biz.TransactionBiz;
import me.flyray.bsin.infrastructure.biz.WalletAccountBiz;
import me.flyray.bsin.infrastructure.mapper.*;
import me.flyray.bsin.redis.provider.BsinCacheProvider;
import me.flyray.bsin.utils.BsinSnowflake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.websocket.WebSocketService;
import org.web3j.utils.Numeric;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 链上数据监听
 */
@Service
public class ChainTransactionListen {

    @Value("${bsin.app-chain.eth.chain-listen-url-wss}")
    private String ethChainListenUrlWss;
    @Value("${bsin.app-chain.eth.chain-listen-url-http}")
    private String ethChainListenUrlHttp;
    @Value("${bsin.app-chain.bsc.chain-listen-url-wss}")
    private String bscChainListenUrlWss;
    @Value("${bsin.app-chain.bsc.chain-listen-url-http}")
    private String bscChainListenUrlHttp;

    private static final Logger log = LoggerFactory.getLogger(ChainTransactionListen.class);

    @Resource
    ThreadPoolTaskExecutor taskExecutor;
    @Autowired
    private WaasTransactionMapper transactionMapper;
    @Autowired
    private WalletAccountMapper walletAccountMapper;
    @Autowired
    private WalletMapper walletMapper;
    @Autowired
    private ChainCoinMapper chainCoinMapper;
    @Autowired
    private ContractMethodMapper contractMethodMapper;
    @Autowired
    private TransactionBiz transferBiz;
    @Autowired
    private WalletAccountBiz walletAccountBiz;

    @Autowired
    public ChainTransactionListen(ThreadPoolTaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    /**
     * 启动链上交易监听
     */
    @PostConstruct
    public void executeAllMonitor() {
        QueryWrapper<ChainCoin> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.isNotNull("contract_address");
        queryWrapper.notIn("coin","ETH");
        List<ChainCoin> chainCoins = chainCoinMapper.selectList(queryWrapper);
        chainCoins.forEach(m -> {
            try {
                contractAddressMonitor(m.getContractAddress());
                // contractAddressMonitor("0xf543ee44170d417cbe70e0ed49927a433f62bff5");
            } catch (Exception e) {
                e.printStackTrace();
                log.info("智能合约监听失败，智能合约：{}", m.getContractAddress());
                // throw new BusinessException("智能合约监听失败");
            }
        });
    }

    /**
     * 支持单个和多个事件，同时可以根据事件的indexed参数进行过滤
     */
    @Async("taskExecutor")
    public void contractAddressMonitor(String contractAddress) throws Exception {
        log.info("开始监听智能合约：{}", contractAddress);
        WebSocketService ws = new WebSocketService(bscChainListenUrlWss, true);
        ws.connect();
        Web3j web3jWs = Web3j.build(ws);

        // 设置过滤条件 100指的是监听最新的100个块
        BigInteger blockNumber = web3jWs.ethBlockNumber().send().getBlockNumber()
                .subtract(new BigInteger("100"));
        EthFilter ethFilter = new EthFilter(DefaultBlockParameter.valueOf(blockNumber),
                DefaultBlockParameterName.LATEST, contractAddress);

        // 监听的合约对应的币种
        QueryWrapper<ChainCoin> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.eq("contract_address", contractAddress);
        ChainCoin chainCoin = chainCoinMapper.selectOne(queryWrapper);

        List<ContractMethod> contractMethods = contractMethodMapper.selectList(new QueryWrapper<>());
        Map<String, String> methodMap = new HashMap<>();
        Map<String, String> notMethodMap = new HashMap<>();
        for (ContractMethod contractMethod : contractMethods) {
            if(contractMethod.getType() == 1){
                // 合约方法
                methodMap.put(contractMethod.getMethodId(), contractMethod.getMethodName());
            }else if(contractMethod.getType() == 2){
                // 非合约方法
                notMethodMap.put(contractMethod.getMethodId(), contractMethod.getMethodName());
            }
        }

        Web3j web3j = Web3j.build(new HttpService(bscChainListenUrlHttp));

        // 查看智能合约币种状态是否正常
        QueryWrapper<ChainCoin> chainCoinQueryWrapper = new QueryWrapper<>();
        chainCoinQueryWrapper.eq("contract_address", contractAddress);
        chainCoinQueryWrapper.eq("status", 1);  // 上架
        List<ChainCoin> chainCoins = chainCoinMapper.selectList(chainCoinQueryWrapper);
        if (chainCoins == null || chainCoins.size() == 0) {
            taskExecutor.shutdown();
        }

        // 监听链上活动数据
        web3jWs.ethLogFlowable(ethFilter).subscribe(ethLog -> {
            System.out.println(ethLog);
            log.info("链上日志信息: {}", ethLog);
            String txHash = ethLog.getTransactionHash();         // 交易hash
            log.info("交易hash: {}", txHash);

            // 判断该交易是否已经处理
            QueryWrapper<WaasTransaction> query = Wrappers.query();
            query.eq("tx_hash",txHash);
            List<WaasTransaction> transactionList = transactionMapper.selectList(query);
            if(!transactionList.isEmpty()){
                return;
            }

            // 对交易进行加锁
            String value = BsinCacheProvider.get("waas",txHash);
            if (value != null) {
                return;
            }
            BsinCacheProvider.put("waas",txHash, String.valueOf(ethLog.getBlockNumber()));
            try{
                // 1、通过hash获取交易信息 包含logs
                EthGetTransactionReceipt ethGetTransactionReceipt = web3j.ethGetTransactionReceipt(txHash).sendAsync().get();
                TransactionReceipt transactionReceipt = ethGetTransactionReceipt.getTransactionReceipt().orElseThrow(RuntimeException::new);

                // 2、通过hash获取链上交易
                Optional<Transaction> transactions = web3j.ethGetTransactionByHash(txHash).send().getTransaction();
                if (transactions.isPresent()) {
                    Transaction transaction = transactions.orElseThrow(RuntimeException::new);
                    String from = transaction.getFrom();
                    String to = transaction.getTo();
                    // 3、获取合约方法
                    String contractMethod = null;
                    Integer methodInvokeWay = 1;
                    String input = transaction.getInput();

                    // 检查是否是原生代币交易（transfer 方法调用）
                    if (input.startsWith("0xa9059cbb")) {
                        // 第一个参数是接收地址（to 地址）
                        to = "0x"+Numeric.cleanHexPrefix(input.substring(34, 74));
                        log.info("ERC-20 Token Receiver Address:  {}", to);
                    } else {
                        log.info("This is not an ERC-20 token transfer transaction.");
                    }

                    BigInteger tokenTransferAmount = getTokenTransferAmount(input);
                    // 根据input开头判断是否是gnosis safe合约的方法, 是则找到具体执行的方法名称和参数
                    if (!input.equals("0X")) {
                        String methodId = input.substring(2, 10);
                        if (notMethodMap.containsKey(methodId)) {
                            log.info("inputData:"+input);
                            methodInvokeWay = 2;      // 合约调用
                            String subInput = input.substring(10);
                            System.out.println("length:"+subInput.length());
                            List<String> subInputList = new ArrayList<>();
                            for (int i = 0; i < subInput.length();) {
                                String substring = subInput.substring(i, i + 63);
                                subInputList.add(substring);
                                i+=64;
                            }
                            for(String sub:subInputList){
                                methodId = sub.substring(0,8);
                                if (methodMap.containsKey(methodId)) {
                                    break;
                                }
                            }
                        }
                        if (methodMap.containsKey(methodId)) {
                            contractMethod = methodMap.get(methodId);
                        } else {
                            contractMethod = methodId;
                        }
                    }

                    // 交易记录
                    TransactionDTO transactionDTO = new TransactionDTO();
                    transactionDTO.setTxHash(transaction.getHash());    // 交易hash
                    transactionDTO.setContractAddress(contractAddress);    // 合约地址
                    transactionDTO.setContractMethod(contractMethod);      // 合约方法
                    transactionDTO.setMethodInvokeWay(methodInvokeWay);     // 合约调用方式
                    if (transactionReceipt.getStatus().equals("0x1")) {
                        transactionDTO.setTransactionStatus(TransactionStatus.SUCCESS.getCode());         //‘0x1’ 事务成功
                    } else if (transactionReceipt.getStatus().equals("0x0")) {
                        transactionDTO.setTransactionStatus(TransactionStatus.FAIL.getCode());         // 0x0’ 事务失败
                    }
                    transactionDTO.setFromAddress(from);  // 交易发起者地址
                    // 交易数量
                    BigDecimal txAmount = new BigDecimal(tokenTransferAmount);

                    transactionDTO.setGasFee(new BigDecimal(transactionReceipt.getCumulativeGasUsed())); // 当前交易执行后累计花费的gas总值
                    transactionDTO.setToAddress(to);    // 交易接受者地址
                    transactionDTO.setCompletedTime(LocalDateTime.now().toString());
                    transactionDTO.setCreateTime(new Date());

                    QueryWrapper<WalletAccount> toWalletAccountQueryWrapper = new QueryWrapper();
                    toWalletAccountQueryWrapper.eq("address", to);
                    WalletAccount toWalletAccount = walletAccountMapper.selectOne(toWalletAccountQueryWrapper);

                    // 转入地址在系统生成的地址中则是转入交易
                    if (toWalletAccount != null) {
                        handleTransferIn(from, to, tokenTransferAmount, transactionDTO, chainCoin.getSerialNo(), toWalletAccount, chainCoin.getCoinDecimal());
                    }

                    QueryWrapper<WalletAccount> formWalletAccountQueryWrapper = new QueryWrapper();
                    formWalletAccountQueryWrapper.eq("address", from);
                    WalletAccount fromWalletAccount = walletAccountMapper.selectOne(formWalletAccountQueryWrapper);

                    //  转出地址在系统生成的地址中则是转出交易
                    if (fromWalletAccount != null) {
                        handleTransferOut(from, to, tokenTransferAmount, transactionDTO, chainCoin.getSerialNo(), fromWalletAccount);
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                // TODO 处理失败的hash进行记录
            }finally {
                // 对交易解锁
                BsinCacheProvider.evict("waas",txHash);
            }
        });
    }

    private void handleTransferIn(String from, String to, BigInteger tokenTransferAmount, TransactionDTO transactionDTO, String chainCoinNo, WalletAccount walletAccount, BigInteger decimals) throws Exception {
        log.info("监听到平台用户钱包账户，to地址：{}",to);
        BigDecimal txAmount = new BigDecimal(tokenTransferAmount);
        Wallet wallet = walletMapper.selectById(walletAccount.getWalletNo());
        if (wallet != null) {
            // 查询归集账户地址
            WalletAccount gatherAccount = walletAccountBiz.getGatherAccount(wallet.getTenantId(), chainCoinNo);
            if(gatherAccount != null && gatherAccount.getAddress().equals(to)){
                transactionDTO.setTransactionType(TransactionType.TRANSFER.getCode());     // 交易类型：3、资金归集
            }else {
                transactionDTO.setTransactionType(TransactionType.RECHARGE.getCode());     // 交易类型：1、转入
            }
            transactionDTO.setTxAmount(txAmount);     // 交易金额
            transactionDTO.setSerialNo(BsinSnowflake.getId());
            transactionDTO.setBizRoleType(wallet.getBizRoleType());
            transactionDTO.setBizRoleTypeNo(wallet.getBizRoleTypeNo());
            transactionDTO.setTenantId(walletAccount.getTenantId());
            transactionMapper.insert(transactionDTO);
            log.info("生成交易记录成功，to地址：{}", to);
            // 代币数量>0,需要资金归集
            if(wallet != null && tokenTransferAmount.compareTo(BigInteger.ZERO) > 0){
                // 入账
                log.info("合约代币数量 > 0,开始进行入账，账户余额：{}",walletAccount.getBalance());
                BigDecimal balance = walletAccount.getBalance().add(txAmount);
                walletAccount.setBalance(balance);
                walletAccountMapper.updateById(walletAccount);
                log.info("入账结束，当前账户余额为：{}", balance);

                // 资金归集-查询钱包标识为DEPOSIT
                if( wallet.getWalletTag().equals("DEPOSIT")){
                    if(gatherAccount != null && !gatherAccount.equals(to)){
                        log.info("归集账户资金开始,账户地址：{}", to);
                        // TODO 资金归集处理 String fromAddress, String toAddress, String contractAddress, BigInteger amount, BigInteger decimals
                        transferBiz.cashConcentrationProcess(from, to, transactionDTO.getContractAddress(), tokenTransferAmount, decimals);
                        // transferBiz.tokenTransfer(to, gatherAccount.getAddress(), contractAddress, tokenTransferAmount, BigInteger.valueOf(0));
                        log.info("归集账户资金结束");
                    }
                }
            }
        }
    }

    private void handleTransferOut(String from, String to, BigInteger tokenTransferAmount, TransactionDTO transactionDTO, String chainCoinNo , WalletAccount walletAccount) throws Exception {
        log.info("监听到平台用户钱包账户，from地址：{}",from);
        BigDecimal txAmount = new BigDecimal(tokenTransferAmount);
        Wallet wallet = walletMapper.selectById(walletAccount.getWalletNo());
        if (wallet != null && tokenTransferAmount.compareTo(BigInteger.ZERO)>0) {
            // 查询归集账户地址
            WalletAccount gatherAccount = walletAccountBiz.getGatherAccount(wallet.getTenantId(), chainCoinNo);
            if(gatherAccount != null&& gatherAccount.getAddress().equals(to)){
                return;
            }else {
                transactionDTO.setTransactionType(TransactionType.WITHDRAW.getCode());     // 交易类型：2、转出
            }
            transactionDTO.setTxAmount(txAmount.multiply(new BigDecimal("-1")));
            transactionDTO.setSerialNo(BsinSnowflake.getId());
            transactionDTO.setBizRoleType(wallet.getBizRoleType());
            transactionDTO.setBizRoleTypeNo(wallet.getBizRoleTypeNo());
            transactionDTO.setTenantId(walletAccount.getTenantId());
            transactionMapper.insert(transactionDTO);
            log.info("生成交易记录成功，from地址：{}",from);
        }
    }

    // 解析 ERC-20 代币转移函数调用数据，获取代币数量
    private static BigInteger getTokenTransferAmount(String inputData) {
        // ERC-20 代币转移函数的transfer 方法签名是 0xa9059cbb，transferFrom 方法签名是 0x23b872dd
        String transferMethodSignature = "0xa9059cbb";
        String transferFromMethodSignature = "0x23b872dd";
        if (inputData.startsWith(transferMethodSignature) || inputData.startsWith(transferFromMethodSignature)) {
            // 代币数量参数在数据的第三个 32 字节的位置开始，每个参数占据 32 字节
            String amountHex = inputData.substring(inputData.length() - 64);
            return Numeric.toBigInt(amountHex);
        } else {
            return BigInteger.ZERO;
        }
    }

    /**
     * 添加交易，并出/入账
     * @param transactionDTO
     * @return
     * @throws Exception
     */
    public Wallet addTransaction(TransactionDTO transactionDTO) throws Exception{
        QueryWrapper<WalletAccount> queryWrapper = new QueryWrapper();
        queryWrapper.eq("address", transactionDTO.getAddress());
        WalletAccount walletAccount = walletAccountMapper.selectOne(queryWrapper);
        if (walletAccount != null) {
            Wallet wallet = walletMapper.selectById(walletAccount.getWalletNo());
            if (wallet != null) {

            }
            return wallet;
        }
        return null;
    }


    /*
   资金归集分为两种情况：一种归集币种是链主币，一种归集币种是链代币。
   使用背景：
   1. 客户需要将钱包线所有地址的余额（或余额的一部分）归集到一个地址中。
   2. 资金归集会消耗手续费，按需使用该功能。
   具体实现逻辑如下：
   1. 校验地址toAddr是否有效。校验失败，直接退出，归集失败。
   2. 获取归集手续费币种。
   3. 汇总钱包中所有地址对应币种余额，校验余额是否大于待归集金额。如果余额小于待归集金额，直接退出，归集失败。
   4. 将钱包中每个地址余额转账到目标地址（如果地址是目标地址，则跳过不转账），直到转账金额=待归集金额为止。
   注意事项：
   1. 确保feeFromAddress属于apikey对应的custody钱包。
   2. 调用资金归集之前，如果不确定交易手续费币种，可以调用estimateFee查询。确保feeFromAddress有足够的手续费支持资金归集。
    */
    public Boolean fundCollection(String coin, BigInteger fromAddr, BigInteger amount,  String toAddr){

        return true;
    }


    /**
     * 转出交易
     */
    public void transferOut() {
        // 支付gas费
//        transferOutETH();
        // 转出代币
//        transferOutToken();
    }

    /**
     * 代币转出交易
     * toAddress  接受地址
     * contractAddress 合约地址
     * amount 交易金额
     * gasPrice gas费价格
     * gasLimit 最多gas费
     */
    public String tokenTransfer(String fromAddress, String toAddress, String contractAddress, BigInteger amount) throws Exception {
//        Web3j web3j = Web3j.build(new HttpService("节点"));
//        Credentials credentials = Credentials.create("密钥");
//
//        String fromAddress = credentials.getAddress();
//
//        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
//                fromAddress, DefaultBlockParameterName.LATEST).sendAsync().get();
//        BigInteger nonce = ethGetTransactionCount.getTransactionCount();
//
//        // 创建Function对象
//        Function function = new Function(
//                "transfer",             // 合约的方法名
//                Arrays.asList(new Address(toAddress), new Uint256(amount)), // 参数列表，发送1个代币
//                Collections.emptyList()); // transfer函数不返回任何值，所以返回类型是空的
//
//        String encodedFunction = FunctionEncoder.encode(function);
//
////        BigInteger gasPrice = Convert.toWei("20", Convert.Unit.GWEI).toBigInteger(); // 20 Gwei
////        BigInteger gasLimit = BigInteger.valueOf(300000L); // 300,000 gas
//        // 创建RawTransaction
//        RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, gasPrice,
//                gasLimit, contractAddress, encodedFunction);
//        // 签名交易
//        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
//        // 将签名后的交易编码为十六进制字符串
//        String hexValue = Numeric.toHexString(signedMessage);
//        // 发送原始交易
//        EthSendTransaction ethSendTransaction;
//        try {
//            ethSendTransaction = web3j.ethSendRawTransaction(hexValue).sendAsync().get();
//        } catch (InterruptedException | ExecutionException e) {
//            // 处理异常
//            e.printStackTrace();
//            throw new BusinessException("SYSTEM_ERROR");
//        }
//        String transactionHash = ethSendTransaction.getTransactionHash();
//        System.out.println(transactionHash);
//        return transactionHash;
        return null;
    }

    private class RedisClientUtil {
    }
}
