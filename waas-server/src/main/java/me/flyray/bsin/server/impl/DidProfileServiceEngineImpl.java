package me.flyray.bsin.server.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.blockchain.tds.DefaultTrustedDataSpaceConnector;
import me.flyray.bsin.blockchain.tds.TrustedDataSpaceConnector;
import me.flyray.bsin.blockchain.utils.DIDGeneratorUtil;
import me.flyray.bsin.domain.entity.DidProfile;
import me.flyray.bsin.domain.enums.CustomerType;
import me.flyray.bsin.exception.BusinessException;
import me.flyray.bsin.facade.engine.DidProfileServiceEngine;
import me.flyray.bsin.infrastructure.mapper.DidProfileMapper;
import me.flyray.bsin.security.enums.BizRoleType;
import me.flyray.bsin.utils.BsinSnowflake;
import org.apache.commons.collections4.MapUtils;
import org.apache.shenyu.client.apache.dubbo.annotation.ShenyuDubboService;
import org.apache.shenyu.client.apidocs.annotations.ApiDoc;
import org.apache.shenyu.client.apidocs.annotations.ApiModule;
import org.apache.shenyu.client.dubbo.common.annotation.ShenyuDubboClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@ShenyuDubboService(path = "/didProfile", timeout = 6000)
@ApiModule(value = "didProfile")
public class DidProfileServiceEngineImpl implements DidProfileServiceEngine {

    // 固定的加密盐值配置（Base64编码）
    private static final String FIXED_ENCRYPTION_SALT = "bsin-paas-os-fixed-salt-2024-secure-key-base64-encoded==";

    @Value("${bsin.oss.ipfs.gateway}")
    private String tdsUrl;

    @Autowired
    private DidProfileMapper customerProfileMapper;

    /**
     * 基于客户实名认证创建可信身份
     * 调用可信数据空间创建可信身份
     * @param requestMap
     */
    @ShenyuDubboClient("/create")
    @ApiDoc(desc = "create")
    @Override
    public DidProfile create(Map<String, Object> requestMap) {

        TrustedDataSpaceConnector trustedDataSpaceClient = TrustedDataSpaceConnector.builder()
                .endpointUrl(tdsUrl)
                .apiKey("your-api-key-here")
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .maxRetries(5)
                .addCustomHeader("X-Tenant-ID", "tenant-123")
                .addCustomHeader("X-Client-Version", "1.0.0")
                .build();
        String didType = MapUtils.getString(requestMap, "didType");

        DidProfile didProfile = new DidProfile();
        // 如果是个人需要验证实名信息
        if(CustomerType.PERSONAL.getCode().equals(didType)){
            // 调用第三方进行实名认证
            didProfile.setBizRoleType(BizRoleType.CUSTOMER.getCode());
            // TODO  调用crm 生成一个客户信息
            didProfile.setBizRoleTypeNo(BsinSnowflake.getId());
        }else if (CustomerType.ENTERPRISE.getCode().equals(didType)){
            // 如果是商户需要验证营业执照和法人信息

        }else {
            throw new BusinessException("11","类型暂不支持");
        }
        requestMap.put("salt", FIXED_ENCRYPTION_SALT);
        Map<String, String> didProfileMap = trustedDataSpaceClient.createDidProfile(requestMap);

        didProfile.setDid(didProfileMap.get("did"));
        didProfile.setName(MapUtils.getString(requestMap, "name"));
        didProfile.setSymbol(didProfileMap.get("idNumber"));
        didProfile.setDidDoc(didProfileMap.get("didDocument"));
        didProfile.setDidKeyData(didProfileMap.get("didKeyData"));
        didProfile.setDescription("DID profile for " + MapUtils.getString(requestMap, "name"));
        // 保存可信数据空间用户身份信息
        customerProfileMapper.insert(didProfile);
        return didProfile;
    }

    /**
     * 通过DID信息对数据签名
     * @param requestMap 请求参数，包含：did（DID标识）、data（要签名的数据）
     * @return 返回包含签名结果的Map
     */
    @ShenyuDubboClient("/signData")
    @ApiDoc(desc = "signData")
    @Override
    public Map<String, Object> signData(Map<String, Object> requestMap) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        // 获取请求参数
        String did = MapUtils.getString(requestMap, "did");
        String signData = MapUtils.getString(requestMap, "signData");

        // 从数据库查询DID档案信息
        LambdaQueryWrapper<DidProfile> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DidProfile::getDid, did);
        DidProfile didProfile = customerProfileMapper.selectOne(queryWrapper);

        String keyDataJson = didProfile.getDidKeyData();

        // 从keyDataJson解析出密钥信息
        String signatureBase64 = DefaultTrustedDataSpaceConnector.signData(didProfile.getDid(), FIXED_ENCRYPTION_SALT, keyDataJson, signData);


        // 返回签名结果
        result.put("did", did);
        result.put("signData", signData);
        result.put("sign", signatureBase64);

        log.info("DID签名成功，DID: {}, 数据长度: {}", did, signData.length());
        return result;
    }

    /**
     * 通过DID信息对签名的数据验证签名
     * @param requestMap 请求参数，包含：did（DID标识）、data（原始数据）、signature（Base64编码的签名）
     * @return 返回包含验证结果的Map
     */
    @ShenyuDubboClient("/verifySign")
    @ApiDoc(desc = "verifySign")
    @Override
    public Map<String, Object> verifySign(Map<String, Object> requestMap) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        // 获取请求参数
        String did = MapUtils.getString(requestMap, "did");
        String signData = MapUtils.getString(requestMap, "signData");
        String sign = MapUtils.getString(requestMap, "sign");

        // 从数据库查询DID档案信息
        LambdaQueryWrapper<DidProfile> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DidProfile::getDid, did);
        DidProfile didProfile = customerProfileMapper.selectOne(queryWrapper);
        String keyDataJson = didProfile.getDidKeyData();

        // 从keyDataJson解析出公钥（验证签名只需要公钥）
        Boolean isValid = DefaultTrustedDataSpaceConnector.verifySign(didProfile.getDid(), FIXED_ENCRYPTION_SALT, keyDataJson, signData,  sign);

        // 返回验证结果
        result.put("did", did);
        result.put("signData", signData);
        result.put("sign", sign);
        result.put("valid", isValid);

        log.info("DID签名验证完成，DID: {}, 验证结果: {}", did, isValid);
        return result;
    }


    /**
     * 基于DID查询用户的身份详情
     * @param requestMap
     */
    @ShenyuDubboClient("/getDetail")
    @ApiDoc(desc = "getDetail")
    @Override
    public DidProfile getDetail(Map<String, Object> requestMap) {
        String did = MapUtils.getString(requestMap, "did");
        LambdaQueryWrapper<DidProfile> warapper = new LambdaQueryWrapper<>();
        warapper.orderByDesc(DidProfile::getCreateTime);
        warapper.eq(DidProfile::getDid, did);
        DidProfile customerProfile =customerProfileMapper.selectOne(warapper);
        return customerProfile;
    }

}
