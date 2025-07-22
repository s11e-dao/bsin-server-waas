package me.flyray.bsin.server.impl;

import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.facade.service.ProfitSharingReceiverService;
import org.apache.shenyu.client.apache.dubbo.annotation.ShenyuDubboService;
import org.apache.shenyu.client.apidocs.annotations.ApiModule;

@Slf4j
@ShenyuDubboService(path = "/profitSharingReceiverService", timeout = 6000)
@ApiModule(value = "profitSharingReceiverService")
public class ProfitSharingReceiverServiceImpl implements ProfitSharingReceiverService {

}
