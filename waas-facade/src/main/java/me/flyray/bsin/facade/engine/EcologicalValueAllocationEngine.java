package me.flyray.bsin.facade.engine;

import java.util.Map;

/**
 * 生态价值分配引擎：交易价值分配和数据价值分配
 * 统一换算成劳动价值，基于劳动价值铸造曲线价值，基于曲线价值获得数字积分（贡献分红凭证）
 * +----------------------------------+
 * |          生态价值分配引擎         |
 * |  +----------------------------+  |
 * |  | 交易价值分配               |  |
 * |  +----------------------------+  |
 * |  +----------------------------+  |
 * |  | 数据价值分配               |  |
 * |  +----------------------------+  |
 * +--------------+-------------------+
 *                |
 *                v
 * +----------------------------------+
 * | 统一换算成劳动价值               |
 * +----------------------------------+
 *                |
 *                v
 * +----------------------------------+
 * | 基于劳动价值铸造曲线价值         |
 * +----------------------------------+
 *                |
 *                v
 * +----------------------------------+
 * | 基于曲线价值获得数字积分         |
 * | （贡献分红凭证）                 |
 * +----------------------------------+
 */

public interface EcologicalValueAllocationEngine {

    /**
     * 生态价值分配引擎定义，不同的租户有不同的价值分配模型
     * @param requestMap
     * @throws Exception
     */
    void excute(Map<String, Object> requestMap) throws Exception;

}
