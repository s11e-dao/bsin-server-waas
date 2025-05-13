package me.flyray.bsin.facade.engine;

import java.util.Map;

public interface DidProfileServiceEngine {

    /**
     * 可信身份注册
     */
    void create(Map<String, Object> requestMap);


    /**
     * 可信身份查询
     * @param requestMap
     */
    public void getDetail(Map<String, Object> requestMap);

}
