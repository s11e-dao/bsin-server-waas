package me.flyray.bsin.facade.engine;

import java.util.Map;

public interface DidProfileServiceEngine {

    /**
     * 可信身份注册
     * 身份类型： 设备、客户、商户
     */
    void create(Map<String, Object> requestMap);


    /**
     * 可信身份查询
     * 根据不同的类型路由到不同的身份信息
     * @param requestMap
     */
    public void getDetail(Map<String, Object> requestMap);

}
