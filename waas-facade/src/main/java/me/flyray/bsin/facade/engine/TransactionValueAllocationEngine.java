package me.flyray.bsin.facade.engine;

import java.util.Map;

/**
 * 交易价值分配引擎
 */

public interface TransactionValueAllocationEngine {

    void excute(Map<String, Object> requestMap);

}
