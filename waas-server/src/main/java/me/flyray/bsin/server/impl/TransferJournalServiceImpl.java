package me.flyray.bsin.server.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.context.BsinServiceContext;
import me.flyray.bsin.domain.entity.WaasTransferJournal;
import me.flyray.bsin.facade.service.TransferJournalService;
import me.flyray.bsin.infrastructure.mapper.WaasTransferJournalMapper;
import me.flyray.bsin.mybatis.utils.Pagination;
import me.flyray.bsin.security.contex.LoginInfoContextHelper;
import me.flyray.bsin.security.domain.LoginUser;
import org.apache.commons.collections4.MapUtils;
import org.apache.shenyu.client.apache.dubbo.annotation.ShenyuDubboService;
import org.apache.shenyu.client.apidocs.annotations.ApiDoc;
import org.apache.shenyu.client.apidocs.annotations.ApiModule;
import org.apache.shenyu.client.dubbo.common.annotation.ShenyuDubboClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author bolei
 * @date 2023/8/13
 * @desc
 */

@Slf4j
@ShenyuDubboService(path = "/transferJournal", timeout = 6000)
@ApiModule(value = "transferJournal")
@Service
public class TransferJournalServiceImpl implements TransferJournalService {

  @Autowired private WaasTransferJournalMapper waasTransferJournalMapper;

  @ShenyuDubboClient("/getPageList")
  @ApiDoc(desc = "getPageList")
  @Override
  public IPage<?> getPageList(Map<String, Object> requestMap) {
    LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
    WaasTransferJournal waasTransferJournal =
        BsinServiceContext.getReqBodyDto(WaasTransferJournal.class, requestMap);
    Object paginationObj =  requestMap.get("pagination");
    Pagination pagination = new Pagination();
    BeanUtil.copyProperties(paginationObj,pagination);
    Page<WaasTransferJournal> page = new Page<>(pagination.getPageNum(), pagination.getPageSize());
    LambdaUpdateWrapper<WaasTransferJournal> warapper = new LambdaUpdateWrapper<>();
    warapper.orderByDesc(WaasTransferJournal::getCreateTime);
    warapper.eq(WaasTransferJournal::getTenantId, loginUser.getTenantId());
    warapper.eq(WaasTransferJournal::getMerchantNo, loginUser.getMerchantNo());
    warapper.eq(
        ObjectUtil.isNotNull(waasTransferJournal.getTokenId()),
        WaasTransferJournal::getTokenId,
        waasTransferJournal.getTokenId());
    warapper.eq(
        ObjectUtil.isNotNull(waasTransferJournal.getTxHash()),
        WaasTransferJournal::getTxHash,
        waasTransferJournal.getTxHash());
    warapper.eq(
        ObjectUtil.isNotNull(waasTransferJournal.getDigitalAssetsCollectionNo()),
        WaasTransferJournal::getDigitalAssetsCollectionNo,
        waasTransferJournal.getDigitalAssetsCollectionNo());
    warapper.eq(
        ObjectUtil.isNotNull(waasTransferJournal.getAssetsType()),
        WaasTransferJournal::getAssetsType,
        waasTransferJournal.getAssetsType());
    IPage<WaasTransferJournal> pageList = waasTransferJournalMapper.selectPage(page, warapper);
    return pageList;
  }

  @ShenyuDubboClient("/getDetail")
  @ApiDoc(desc = "getDetail")
  @Override
  public WaasTransferJournal getDetail(Map<String, Object> requestMap) {
    String serialNo = MapUtils.getString(requestMap, "serialNo");
    WaasTransferJournal waasTransferJournal = waasTransferJournalMapper.selectById(serialNo);
    return waasTransferJournal;
  }

}
