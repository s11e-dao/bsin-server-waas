package me.flyray.bsin.infrastructure.biz;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.blockchain.enums.ChainType;
import me.flyray.bsin.constants.ResponseCode;
import me.flyray.bsin.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class MerchantInfoBiz {

    @Value("${bsin.jiujiu.aesKey}")
    private String aesKey;

//    @DubboReference(version = "${dubbo.provider.version}")
//    private MerchantService merchantService;

//    public Merchant getMerchantBase(String merchantNo, String chainType) {
//        // 1.查找资产商户
//        Map<String, Object> reqMerchant = new HashMap();
//        reqMerchant.put("serialNo", merchantNo);
//        Merchant merchant = merchantService.getDetail(reqMerchant);
//        return merchant;
//    }

    /**
     * 获取DigitalAssetsItem所属商户的信息
     * TODO: 返回 object 非 Map
     * */
    public Map<String, Object> getMerchantInfo(String merchantNo, String chainType) {
        Map<String, Object> merchantDetail = null;
        // 1.查找资产商户
        Map<String, Object> reqMerchant = new HashMap();
        reqMerchant.put("serialNo", merchantNo);
//        Merchant merchant = merchantService.getDetail(reqMerchant);

        // 2.查找资产商户的管理员客户
        String merchanNo = null; // merchant.getSerialNo();

        // TODO 3. 根据商户查询商户的钱包信息
        merchantDetail = new HashMap<>();
        merchantDetail.put("walletPrivateKey","walletPrivateKey");
        merchantDetail.put("walletAddress","walletAddress");
        merchantDetail.put("evmWalletPrivateKey","evmWalletPrivateKey");
        merchantDetail.put("walletAddress","walletAddress");

        // 4.判断私钥是否设置和正确(此处应该为商户私钥，数字资产由商户发行)
        SymmetricCrypto aes = new SymmetricCrypto(SymmetricAlgorithm.AES, aesKey.getBytes());
        String privateKey = null;
        String walletAddress = null;
        if (chainType == null || ChainType.CONFLUX.getCode().equals(chainType)) {
            if (StringUtils.isBlank((String) merchantDetail.get("walletPrivateKey"))) {
                throw new BusinessException(ResponseCode.MERCHANT_WALLET_PRIVATEKEY_ERROR);
            }
            privateKey =
                    aes.decryptStr(
                            (String) merchantDetail.get("walletPrivateKey"), CharsetUtil.CHARSET_UTF_8);
            if (privateKey.length() != 64) {
                throw new BusinessException(ResponseCode.MERCHANT_WALLET_PRIVATEKEY_ERROR);
            }
            walletAddress = (String) merchantDetail.get("walletAddress");
        } else {
            if (StringUtils.isBlank((String) merchantDetail.get("evmWalletPrivateKey"))) {
                throw new BusinessException(ResponseCode.MERCHANT_WALLET_PRIVATEKEY_ERROR);
            }
            privateKey =
                    aes.decryptStr(
                            (String) merchantDetail.get("evmWalletPrivateKey"), CharsetUtil.CHARSET_UTF_8);
            if (privateKey.length() != 64) {
                throw new BusinessException(ResponseCode.MERCHANT_WALLET_PRIVATEKEY_ERROR);
            }
            walletAddress = (String) merchantDetail.get("evmWalletAddress");
        }
        merchantDetail.put("privateKey", privateKey);
        merchantDetail.put("walletAddress", walletAddress);
        return merchantDetail;
    }

    /**
     * 商户发行数字积分的钱包
     * @param customerNo
     * @param chainType
     * @return
     */
    public Map getMerchantIssueWallet(String customerNo, String chainType) {
        return null;
    }
}
