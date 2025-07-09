package me.flyray.bsin.facade.engine;

import me.flyray.bsin.domain.entity.DidProfile;

import java.util.Map;

public interface DidProfileServiceEngine {

    /**
     * 可信身份注册
     * 身份类型： 设备、客户、商户
     */
    DidProfile create(Map<String, Object> requestMap);


    /**
     * 可信身份签名数据
     * @param requestMap 请求参数，包含：did（DID标识）、data（要签名的数据）
     * @return 返回包含签名结果的Map
     */
    Map<String, Object> signData(Map<String, Object> requestMap) throws Exception;

    /**
     * 可信身份验证签名数据
     * @param requestMap 请求参数，包含：did（DID标识）、data（原始数据）、signature（Base64编码的签名）
     * @return 返回包含验证结果的Map
     */
    Map<String, Object> verifySign(Map<String, Object> requestMap) throws Exception;

    /**
     * 可信身份查询
     * 根据不同的类型路由到不同的身份信息
     * @param requestMap
     */
    public DidProfile getDetail(Map<String, Object> requestMap);

}
