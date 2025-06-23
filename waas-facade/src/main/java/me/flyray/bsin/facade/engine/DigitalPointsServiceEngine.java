package me.flyray.bsin.facade.engine;

import me.flyray.bsin.domain.entity.DigitalAssetsCollection;

import java.math.BigDecimal;
import java.util.Map;

/**
 * @author bolei
 * @date 2023/7/3 15:46
 * @desc
 */

public interface DigitalPointsServiceEngine {

    /**
     * 发行
     * 1、开通数字积分相关账户
     * 2、部署智能合约
     */
    void issue(Map<String, Object> requestMap) throws Exception;

    /**
     * 查询商户数字积分信息
     */
    DigitalAssetsCollection getDetailByMerchantNo(Map<String, Object> requestMap);

    /**
     * 铸造数字积分
     */
    void mint(Map<String, Object> requestMap) throws Exception;

    /**
     * 给每个用户释放获得的数字积分
     */
    void release(Map<String, Object> requestMap) throws Exception;

    /**
     * 根据联合曲线价值，计算出劳动价值应该获得多少的积分分配
     * 计算应该产出多少数字积分
     * Calculate the value of labor
     */
    BigDecimal calculateValueOfLabor(Integer bondingCurveValue) throws Exception;


}
