package me.flyray.bsin.facade.engine;

import java.util.Map;

/**
 * 数据贡献价值分配引擎
 */

public interface DataValueAllocationEngine {

    /**
     * 根据数据分类和级别进行激励发放
     */
    void excute(Map<String, Object> requestMap);

}
