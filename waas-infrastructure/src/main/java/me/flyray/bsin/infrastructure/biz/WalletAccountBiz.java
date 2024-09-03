package me.flyray.bsin.infrastructure.biz;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import me.flyray.bsin.domain.entity.ChainCoin;
import me.flyray.bsin.domain.entity.Platform;
import me.flyray.bsin.domain.entity.Wallet;
import me.flyray.bsin.domain.entity.WalletAccount;
import me.flyray.bsin.exception.BusinessException;
import me.flyray.bsin.infrastructure.mapper.ChainCoinMapper;
import me.flyray.bsin.infrastructure.mapper.WalletAccountMapper;
import me.flyray.bsin.infrastructure.mapper.WalletMapper;
import me.flyray.bsin.infrastructure.utils.OkHttpUtils;
import me.flyray.bsin.mq.producer.RocketMQProducer;
import me.flyray.bsin.utils.BsinSnowflake;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class WalletAccountBiz {

    private static final Logger log = LoggerFactory.getLogger(WalletAccountBiz.class);
    @Autowired
    private WalletAccountMapper walletAccountMapper;
    @Autowired
    private ChainCoinMapper chainCoinMapper;
    @Autowired
    private WalletMapper walletMapper;
    @Autowired
    private RocketMQProducer rocketMQProducer;
    @Value("${rocketmq.consumer.topic}")
    private String topic;
    @Value("${bsin.app-chain.gateway-url}")
    private String appChainGatewayUrl;

    /**
     * 创建钱包账户
     *  1、创建链上钱包并返回地址
     *  2、创建钱包地址
     *  3、创建钱包账户
     * @param wallet
     * @param chainCoinNo
     */
    public WalletAccount createWalletAccount(Wallet wallet, String chainCoinNo) {
        log.info("开始创建钱包账户，wallet:{},chainCoinNo:{}",wallet,chainCoinNo);
        ChainCoin chainCoin = chainCoinMapper.selectById(chainCoinNo);
        if(chainCoin == null || chainCoin.getStatus() == 0){
            throw new BusinessException("chain coin not exist or off shelves");
        }
        // 1、创建链上钱包
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("walletName", wallet.getWalletName());
        jsonObject.put("walletType", "mpc");
        jsonObject.put("chainType", "ERC20");
        jsonObject.put("threshold", 2);
        jsonObject.put("partiyNum", 2);
        List mpcClients = new ArrayList();
        mpcClients.add(0);
        mpcClients.add(1);
        jsonObject.put("mpcClients", mpcClients);
        jsonObject.put("sync", false);
        jsonObject.put("timeout", 1000);
        JSONObject data = OkHttpUtils.httpPost(appChainGatewayUrl + "/api/v1/mpc/keygen", jsonObject);
        String requisitionId = (String)data.get("requisitionId");
        log.info("MCP网络返回请求ID：{}", requisitionId);
        // 2、创建钱包账户
        String walletAccountNo = BsinSnowflake.getId();
        WalletAccount walletAccount = new WalletAccount();
        walletAccount.setSerialNo(walletAccountNo);
        walletAccount.setChainCoinNo(chainCoinNo);
        walletAccount.setStatus(1);  // 账户状态 1、正常
        walletAccount.setWalletNo(wallet.getSerialNo());
        walletAccount.setBalance(BigDecimal.ZERO);
        wallet.setBizRoleTypeNo(wallet.getBizRoleTypeNo());
        wallet.setBizRoleType(wallet.getBizRoleType());
        walletAccount.setTenantId(wallet.getTenantId());
        walletAccount.setCreateTime(new Date());
        walletAccountMapper.insert(walletAccount);
        log.info("结束创建钱包账户，wallet:{}, chainCoinNo:{}", wallet,chainCoinNo);

        // TODO 请求消息队列，添加一条延时队列
        JSONObject mQMsgReq = new JSONObject();
        mQMsgReq.put("requisitionId", requisitionId);
        mQMsgReq.put("eventCode", "createMpcWallet");
        mQMsgReq.put("walletAccountNo", walletAccountNo);
        SendCallback callback = new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                System.out.println("createMpcWallet 成功");
            }
            @Override
            public void onException(Throwable throwable) {
                System.out.println("createMpcWallet 失败");
            }
        };
        // 延时消息等级分为18个：1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
        rocketMQProducer.sendDelay(topic,mQMsgReq.toString(), callback,7);
        return walletAccount;
    }

    /**
     * 获取平台归集账户
     * TODO 查询平台配置的钱包处理平台归集钱包
     */
    public WalletAccount getGatherAccount(String tenantId, String chainCoinNo) throws Exception {
        QueryWrapper<Platform> platformQueryWrapper = new QueryWrapper<>();
        platformQueryWrapper.eq("tenant_id", tenantId);

        QueryWrapper<Wallet> walletQueryWrapper = new QueryWrapper<>();
        walletQueryWrapper.eq("wallet_tag", "GATHER");
        walletQueryWrapper.eq("biz_role_type", 1);  // 平台
        walletQueryWrapper.eq("biz_role_type_no", "1");
        walletQueryWrapper.eq("type", 1); // 默认钱包
        walletQueryWrapper.eq("tenant_id", '1');
        walletQueryWrapper.eq("status", 1);  // 状态正常

        Wallet wallet = walletMapper.selectOne(walletQueryWrapper);

        QueryWrapper<WalletAccount> queryWrapper = new QueryWrapper();
        queryWrapper.eq("chain_coin_no", chainCoinNo);
        queryWrapper.eq("wallet_no", wallet.getSerialNo());
        queryWrapper.eq("tenant_id",tenantId);
        WalletAccount walletAccount = walletAccountMapper.selectOne(queryWrapper);
        return walletAccount;
    };

    /**
     * 1、查询MPC网络钱包地址
     * 2、更新用户的钱包地址
     */
    public void getAppChainWalletAddress(JSONObject mQMsg) {
        log.info("mq 消息：{}", mQMsg.toString());
        // 查询MPC网络
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("walletType", "mpc");
        jsonObject.put("chainType", "ERC20");
        jsonObject.put("threshold", 2);
        jsonObject.put("partiyNum", 2);
        jsonObject.put("timeout", 1000);
        JSONObject result = OkHttpUtils.httpGet(appChainGatewayUrl + "/api/v1/mpc/requisition/"+ mQMsg.get("requisitionId"));
        log.info("MPC 信息：{}", result.toString());
        String address = (String)result.get("address");
        String pubKey = (String)result.get("pubKey");
        if(StringUtils.isNotEmpty(address)){
            //  更新钱包地址
            WalletAccount walletAccount = new WalletAccount();
            walletAccount.setAddress(address);
            walletAccount.setPubKey(pubKey);
            walletAccount.setSerialNo((String) mQMsg.get("walletAccountNo"));
            walletAccountMapper.updateById(walletAccount);
        }

    }
}
