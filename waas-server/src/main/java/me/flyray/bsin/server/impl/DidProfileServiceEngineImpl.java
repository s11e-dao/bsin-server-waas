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
    public void create(Map<String, Object> requestMap) {

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
        // 如果是个人需要验证实名信息
        if(CustomerType.PERSONAL.getCode().equals(didType)){
            // 调用第三方进行实名认证

        }else if (CustomerType.ENTERPRISE.getCode().equals(didType)){
            // 如果是商户需要验证营业执照和法人信息

        }else {
            throw new BusinessException("11","类型暂不支持");
        }
        Map<String, String> didProfile = trustedDataSpaceClient.createDidProfile(requestMap);
        DidProfile customerProfile = new DidProfile();
        customerProfile.setDid(didProfile.get("did"));
        customerProfile.setName(MapUtils.getString(requestMap, "name"));
        customerProfile.setDidDoc(didProfile.get("didDocument"));
        customerProfile.setDidkeyDataJson(didProfile.get("keyDataJson"));
        customerProfile.setDescription("DID profile for " + MapUtils.getString(requestMap, "name"));
        // 保存可信数据空间用户身份信息
        customerProfileMapper.insert(customerProfile);

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
        String data = MapUtils.getString(requestMap, "data");

        // 从数据库查询DID档案信息
        LambdaQueryWrapper<DidProfile> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DidProfile::getDid, did);
        DidProfile didProfile = customerProfileMapper.selectOne(queryWrapper);

        String keyDataJson = didProfile.getDidkeyDataJson();

        // 从keyDataJson解析出密钥信息
        Map<String, String> keyData = DefaultTrustedDataSpaceConnector.parseKeyDataFromJson(keyDataJson);

        // 重建KeyPair对象
        String privateKeyBase58 = keyData.get("privateKeyBase58");
        String publicKeyBase58 = keyData.get("publicKeyBase58");

        PrivateKey privateKey = DIDGeneratorUtil.decodePrivateKeyFromBase58(privateKeyBase58);
        PublicKey publicKey = DIDGeneratorUtil.decodePublicKeyFromBase58(publicKeyBase58);
        KeyPair keyPair = new KeyPair(publicKey, privateKey);

        // 对数据进行签名
        byte[] signature = DIDGeneratorUtil.signData(data.getBytes("UTF-8"), keyPair.getPrivate());
        String signatureBase64 = java.util.Base64.getEncoder().encodeToString(signature);

        // 返回签名结果
        result.put("did", did);
        result.put("data", data);
        result.put("sign", signatureBase64);
        result.put("publicKey", publicKeyBase58);

        log.info("DID签名成功，DID: {}, 数据长度: {}", did, data.length());
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
        String data = MapUtils.getString(requestMap, "data");
        String sign = MapUtils.getString(requestMap, "sign");

        // 从数据库查询DID档案信息
        LambdaQueryWrapper<DidProfile> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DidProfile::getDid, did);
        DidProfile didProfile = customerProfileMapper.selectOne(queryWrapper);
        String keyDataJson = didProfile.getDidkeyDataJson();


        // 从keyDataJson解析出公钥（验证签名只需要公钥）
        String publicKeyBase58 = DefaultTrustedDataSpaceConnector.parsePublicKeyBase58FromJson(keyDataJson);
        // 重建公钥对象
        PublicKey publicKey = DIDGeneratorUtil.decodePublicKeyFromBase58(publicKeyBase58);
        // 解码签名数据
        byte[] signatureBytes = java.util.Base64.getDecoder().decode(sign);
        // 验证签名
        boolean isValid = DIDGeneratorUtil.verifySignature(
                data.getBytes("UTF-8"),
                signatureBytes,
                publicKey
        );

        // 返回验证结果
        result.put("did", did);
        result.put("data", data);
        result.put("sign", sign);
        result.put("publicKey", publicKeyBase58);
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
        LambdaUpdateWrapper<DidProfile> warapper = new LambdaUpdateWrapper<>();
        warapper.orderByDesc(DidProfile::getDid);
        DidProfile customerProfile =customerProfileMapper.selectOne(warapper);
        return customerProfile;
    }

}
