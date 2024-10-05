package me.flyray.bsin.infrastructure.biz;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.blockchain.enums.ChainEnv;
import me.flyray.bsin.blockchain.enums.ChainType;
import me.flyray.bsin.constants.ResponseCode;
import me.flyray.bsin.domain.entity.CustomerBase;
import me.flyray.bsin.domain.entity.Merchant;
import me.flyray.bsin.exception.BusinessException;
import me.flyray.bsin.facade.service.AccountService;
import me.flyray.bsin.facade.service.CustomerService;
import me.flyray.bsin.facade.service.MerchantService;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author bolei
 * @date 2023/8/9 13:51
 * @desc
 */
@Component
@Slf4j
public class CustomerInfoBiz {

  @Value("${bsin.jiujiu.aesKey}")
  private String aesKey;

  @DubboReference(version = "${dubbo.provider.version}")
  private MerchantService merchantService;

  @DubboReference(version = "${dubbo.provider.version}")
  private CustomerService customerService;



  /**
   * 获取客户信息
   * TODO: 返回 object 非 Map
   * */
  public Map<String, Object> getCustomerBase(String customerNo, String chainType) {
    // 1.查找客户信息
    Map<String, Object> reqCustomerBase = new HashMap();
    reqCustomerBase.put("serialNo", customerNo);
    CustomerBase customerBase = customerService.getDetail(reqCustomerBase);
    if (customerBase == null) {
      throw new BusinessException(ResponseCode.CUSTOMER_ERROR);
    }

    // 3.判断私钥是否设置和正确(此处应该为商户私钥，数字资产由商户发行)
    SymmetricCrypto aes = new SymmetricCrypto(SymmetricAlgorithm.AES, aesKey.getBytes());
    String privateKey = null;
    String walletAddress = null;
    if (chainType == null || ChainType.CONFLUX.getCode().equals(chainType)) {
      if (StringUtils.isBlank(customerBase.getWalletPrivateKey())) {
        throw new BusinessException(ResponseCode.CUSTOMER_WALLET_PRIVATEKEY_ERROR);
      }
      privateKey =
          aes.decryptStr(customerBase.getWalletPrivateKey(), CharsetUtil.CHARSET_UTF_8);
      if (privateKey.length() != 64) {
        throw new BusinessException(ResponseCode.CUSTOMER_WALLET_PRIVATEKEY_ERROR);
      }
      walletAddress = customerBase.getEvmWalletAddress();
    } else {
      if (StringUtils.isBlank(customerBase.getWalletPrivateKey())) {
        throw new BusinessException(ResponseCode.CUSTOMER_WALLET_PRIVATEKEY_ERROR);
      }
      privateKey =
          aes.decryptStr(customerBase.getWalletPrivateKey(), CharsetUtil.CHARSET_UTF_8);
      if (privateKey.length() != 64) {
        throw new BusinessException(ResponseCode.CUSTOMER_WALLET_PRIVATEKEY_ERROR);
      }
      walletAddress = customerBase.getEvmWalletAddress();
    }
    Map<String, Object> customerBaseMap = new HashMap<>();
    customerBaseMap.put("privateKey", privateKey);
    customerBaseMap.put("walletAddress", walletAddress);

    return customerBaseMap;
  }


}
