package me.flyray.bsin.facade.engine;

import java.util.Map;

/**
 * 数据贡献价值计算分配引擎
 */

public interface DataValueAllocationEngine {

    /**
     * 根据数据分类和级别进行价值计算和价值分配
     */
    void excute(Map<String, Object> requestMap) throws Exception;

}
