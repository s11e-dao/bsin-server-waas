package me.flyray.bsin.facade.service;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.Map;

public interface ProfitSharingConfigService {

    public IPage<?> getPageList(Map<String, Object> requestMap);

}
