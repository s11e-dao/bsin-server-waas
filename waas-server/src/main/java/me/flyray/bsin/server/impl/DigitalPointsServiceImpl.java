package me.flyray.bsin.server.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.constants.ResponseCode;
import me.flyray.bsin.context.BsinServiceContext;
import me.flyray.bsin.domain.entity.ContractProtocol;
import me.flyray.bsin.domain.entity.DigitalAssetsCollection;
import me.flyray.bsin.domain.entity.TokenParam;
import me.flyray.bsin.domain.enums.AssetsCollectionType;
import me.flyray.bsin.exception.BusinessException;
import me.flyray.bsin.facade.request.DigitalAssetsIssueReqDTO;
import me.flyray.bsin.facade.service.DigitalAssetsCollectionService;
import me.flyray.bsin.facade.engine.DigitalPointsServiceEngine;
import me.flyray.bsin.infrastructure.biz.*;
import me.flyray.bsin.infrastructure.mapper.ContractProtocolMapper;
import me.flyray.bsin.infrastructure.mapper.DigitalAssetsCollectionMapper;
import me.flyray.bsin.infrastructure.mapper.TokenParamMapper;
import me.flyray.bsin.security.contex.LoginInfoContextHelper;
import me.flyray.bsin.security.domain.LoginUser;
import me.flyray.bsin.utils.BsinSnowflake;
import org.apache.commons.collections4.MapUtils;
import org.apache.shenyu.client.apache.dubbo.annotation.ShenyuDubboService;
import org.apache.shenyu.client.apidocs.annotations.ApiDoc;
import org.apache.shenyu.client.apidocs.annotations.ApiModule;
import org.apache.shenyu.client.dubbo.common.annotation.ShenyuDubboClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

/**
 * @author bolei
 * @date 2023/7/3 15:49
 * @desc
 */

@Slf4j
@ShenyuDubboService(path = "/digitalPoints", timeout = 6000)
@ApiModule(value = "digitalPoints")
public class DigitalPointsServiceImpl implements DigitalPointsServiceEngine {

  @Autowired private ContractProtocolMapper contractProtocolMapper;
  @Autowired private DigitalAssetsBiz digitalAssetsBiz;
  @Autowired private DigitalAssetsCollectionMapper digitalAssetsCollectionMapper;
  @Autowired private TokenParamMapper tokenParamMapper;
  @Autowired private DigitalAssetsItemBiz digitalAssetsItemBiz;
  @Autowired private MerchantInfoBiz merchantInfoBiz;
  @Autowired private CrmAccountBiz crmAccountBiz;
  @Autowired private DigitalAssetsCollectionService digitalAssetsCollectionService;

  @Value("${bsin.jiujiu.aesKey}")
  private String aesKey;

  /**
   * 注：租户平台通过直属商户发行积分，解决积分商城和功能共用问题
   * 开通商户的积分账户，商户可以选择激励的数字资产，开通对应数字积分账户，通过币种关联数字资产中心
   * 1.获取发行商信息
   * 2.账户余额判断
   * 3.根据protocolCode和chainType 获取 contractProtocol
   * 4.部署合约
   * 5.账户扣费
   * 6.写入资产信息
   * 7.插入token基础信息
   * @param requestMap
   * @return
   * @throws Exception
   */
  @ShenyuDubboClient("/issue")
  @ApiDoc(desc = "issue")
  @Override
  @Transactional
  public void issue(Map<String, Object> requestMap) throws Exception {
    log.info("DigitalPointsService issue 请求参数:{}", JSON.toJSONString(requestMap));
    // 发行商
    LoginUser loginUser = LoginInfoContextHelper.getLoginUser();

    String merchantNo = loginUser.getMerchantNo();
    if (merchantNo == null) {
      throw new BusinessException(ResponseCode.MERCHANT_NO_IS_NULL);
    }

    String tenantId = loginUser.getTenantId();
    DigitalAssetsIssueReqDTO digitalAssetsIssueReqDTO =
        BsinServiceContext.getReqBodyDto(DigitalAssetsIssueReqDTO.class, requestMap);

    String chainEnv = digitalAssetsIssueReqDTO.getChainEnv();
    String chainType = digitalAssetsIssueReqDTO.getChainType();
    // 合约是否被赞数
    String sponsorFlag = digitalAssetsIssueReqDTO.getSponsorFlag();

    String contractProtocolNo = MapUtils.getString(requestMap, "contractProtocolNo");
    String protocolCode = MapUtils.getString(requestMap, "protocolCode");
//    if (protocolCode == null) {
//      throw new BusinessException("100000", "protocolCode为空！！！");
//    }

    // 1.获取发行商信息
    // Map issuerBase = merchantInfoBiz.getMerchantInfo(merchantNo, chainType);

//    digitalAssetsIssueReqDTO.setPrivateKey((String) issuerBase.get("privateKey"));
//    digitalAssetsIssueReqDTO.setOwnerAddress((String) issuerBase.get("walletAddress"));

    // 2.发行商账户余额判断
    // crmAccountBiz.checkAccountBalance(issuerBase, chainType, chainEnv);

    // 3.根据protocolCode和chainType 获取 contractProtocol
    ContractProtocol contractProtocol =
        digitalAssetsItemBiz.getContractProtocol(
            digitalAssetsIssueReqDTO, protocolCode, contractProtocolNo);
    // 4.部署合约
//    ContractTransactionResp contractTransactionResp =
//        digitalAssetsBiz.deployContract(digitalAssetsIssueReqDTO);
//    log.info("DigitalAssetsService issue 相应结果:{}", JSON.toJSONString(contractTransactionResp));
    // 5.账户扣费
    // crmAccountBiz.accountOut(issuerBase, chainEnv);

    // 6.写入资产信息
    DigitalAssetsCollection digitalAssetsColletion = new DigitalAssetsCollection();
    BeanUtil.copyProperties(digitalAssetsIssueReqDTO, digitalAssetsColletion);
    digitalAssetsColletion.setMerchantNo(merchantNo);
    digitalAssetsColletion.setSerialNo(BsinSnowflake.getId());
    digitalAssetsColletion.setContractAddress("contractTransactionResp.getContractAddress()");
    digitalAssetsColletion.setSponsorFlag(sponsorFlag);
    digitalAssetsColletion.setCreateBy(merchantNo);
    digitalAssetsColletion.setContractProtocolNo(contractProtocol.getSerialNo());
    digitalAssetsColletion.setName((String) requestMap.get("name"));

    if (digitalAssetsIssueReqDTO.getDecimals() != null) {
      digitalAssetsColletion.setDecimals(digitalAssetsIssueReqDTO.getDecimals());
    } else {
      digitalAssetsColletion.setDecimals(0);
    }

    BigDecimal melo =
        new BigDecimal(Math.pow(10, digitalAssetsColletion.getDecimals().doubleValue()));
    // 初始供应量
    if (digitalAssetsIssueReqDTO.getInitialSupply() != null) {
      digitalAssetsColletion.setInitialSupply(
          digitalAssetsIssueReqDTO.getInitialSupply().multiply(melo));
    } else {
      digitalAssetsColletion.setInitialSupply(new BigDecimal("0"));
    }
    // 总供应量
    digitalAssetsColletion.setTotalSupply(
        new BigDecimal((String) requestMap.get("totalSupply")).multiply(melo));

    // 库存
    BigDecimal inventory =
        digitalAssetsColletion.getTotalSupply().subtract(digitalAssetsColletion.getInitialSupply());
    digitalAssetsColletion.setInventory(inventory);

    digitalAssetsColletion.setTenantId(tenantId);
    digitalAssetsColletion.setChainEnv(chainEnv);
    digitalAssetsColletion.setChainType(contractProtocol.getChainType());
    digitalAssetsColletion.setCollectionType(digitalAssetsIssueReqDTO.getAssetsCollectionType());
    digitalAssetsCollectionMapper.insert(digitalAssetsColletion);

    // 7.插入token基础信息
    TokenParam tokenParam = new TokenParam();
    tokenParam.setSerialNo(BsinSnowflake.getId());
    tokenParam.setTenantId(loginUser.getTenantId());
    tokenParam.setMerchantNo(merchantNo);
    tokenParam.setDigitalAssetsCollectionNo(digitalAssetsColletion.getSerialNo());
    tokenParam.setTotalSupply(digitalAssetsColletion.getTotalSupply());
    tokenParam.setDecimals(digitalAssetsColletion.getDecimals());
    tokenParam.setCirculation(digitalAssetsColletion.getInitialSupply());
    tokenParam.setName(digitalAssetsColletion.getName());
    tokenParam.setSymbol(digitalAssetsColletion.getSymbol());
    tokenParamMapper.insert(tokenParam);

  }

  @ShenyuDubboClient("/getDetailByMerchantNo")
  @ApiDoc(desc = "getDetailByMerchantNo")
  @Override
  public DigitalAssetsCollection getDetailByMerchantNo(Map<String, Object> requestMap) {
    String merchantNo = MapUtils.getString(requestMap, "merchantNo");
    DigitalAssetsCollection digitalAssetsCollection =
        digitalAssetsCollectionMapper.selectOne(
            new LambdaQueryWrapper<DigitalAssetsCollection>()
                .eq(DigitalAssetsCollection::getMerchantNo, merchantNo)
                .eq(
                    DigitalAssetsCollection::getCollectionType,
                    AssetsCollectionType.DIGITAL_POINT.getCode()));
    return digitalAssetsCollection;
  }

  /**
   * 在链上mint积分
   * @param requestMap
   * @throws Exception
   */
  @ShenyuDubboClient("/mint")
  @ApiDoc(desc = "mint")
  @Override
  public void mint(Map<String, Object> requestMap) throws Exception {
    digitalAssetsCollectionService.mint(requestMap);
  }

  /**
   * 在链上mint积分
   * @param requestMap
   * @throws Exception
   */
  @ShenyuDubboClient("/release")
  @ApiDoc(desc = "release")
  @Override
  public void release(Map<String, Object> requestMap) throws Exception {

    // 计算每个人应该得到多少积分，根据结果为每个人进行铸造

    digitalAssetsCollectionService.mint(requestMap);
  }

  /**
   * 根据劳动价值计算应该激励的数字积分
   * 1、计算劳动价值所有铸造量：根据实际贡献价值，计算联合曲线上的应得价值
   * 2、基于应得价值去铸铸造链上数字积分
   * @throws Exception
   */
  @ShenyuDubboClient("/calculateValueOfLabor")
  @ApiDoc(desc = "calculateValueOfLabor")
  @Override
  public BigDecimal calculateValueOfLabor(Integer bondingCurveValue) throws Exception {
    return BigDecimal.ZERO;
  }

}
