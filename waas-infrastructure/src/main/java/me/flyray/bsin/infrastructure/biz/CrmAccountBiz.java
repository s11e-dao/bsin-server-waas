package me.flyray.bsin.infrastructure.biz;

import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.blockchain.enums.ChainEnv;
import me.flyray.bsin.facade.service.AccountService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class CrmAccountBiz {

    @DubboReference(version = "${dubbo.provider.version}")
    private AccountService accountService;

    /**
     * 检验客户余额
     * */
    public boolean checkAccountBalance(Map customerBase, String chainType, String chainEnv) {
        if (!ChainEnv.TEST.getCode().equals(chainEnv)) {
            Map reqMap = new HashMap();
            reqMap.put("customerNo", (String) customerBase.get("customerNo"));
            // TODO 调用crm中心冻结账户余额
            accountService.freeze(reqMap);
        }
        // TODO 判断用户账户余额是否充足
        else {

        }
        return true;
    }

    /** 扣费 */
    public boolean accountOut(Map customerBase, String chainEnv) {
        // TODO 判断网络：如果是测试网不需要扣费，解冻并扣费
        if (!ChainEnv.TEST.getCode().equals(chainEnv)) {
            // 扣除用户余额
            Map reqMap = new HashMap();
            reqMap.put("customerNo", (String) customerBase.get("customerNo"));
            reqMap.putAll(reqMap);
            // TODO 生成扣费订单，调用crm中心扣费
        }
        return true;
    }

}
