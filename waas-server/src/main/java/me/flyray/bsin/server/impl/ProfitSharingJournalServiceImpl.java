package me.flyray.bsin.server.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.constants.ResponseCode;
import me.flyray.bsin.context.BsinServiceContext;
import me.flyray.bsin.domain.entity.ProfitSharingJournal;
import me.flyray.bsin.exception.BusinessException;
import me.flyray.bsin.facade.service.ProfitSharingJournalService;
import me.flyray.bsin.infrastructure.mapper.ProfitSharingJournalMapper;
import me.flyray.bsin.mybatis.utils.Pagination;
import me.flyray.bsin.security.contex.LoginInfoContextHelper;
import me.flyray.bsin.security.domain.LoginUser;
import me.flyray.bsin.utils.BsinSnowflake;
import org.apache.commons.lang3.StringUtils;
import org.apache.shenyu.client.apache.dubbo.annotation.ShenyuDubboService;
import org.apache.shenyu.client.apidocs.annotations.ApiDoc;
import org.apache.shenyu.client.apidocs.annotations.ApiModule;
import org.apache.shenyu.client.dubbo.common.annotation.ShenyuDubboClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static me.flyray.bsin.constants.ResponseCode.SERIAL_NO_NOT_EXIST;

@Slf4j
@ShenyuDubboService(path = "/profitSharingJournal", timeout = 6000)
@ApiModule(value = "profitSharingJournal")
@Service
public class ProfitSharingJournalServiceImpl implements ProfitSharingJournalService {

    @Autowired
    private ProfitSharingJournalMapper profitSharingJournalMapper;

    @ApiDoc(desc = "add")
    @ShenyuDubboClient("/add")
    @Override
    public ProfitSharingJournal add(Map<String, Object> requestMap) {
        log.debug("请求ProfitSharingJournalService.add,参数:{}", requestMap);
        LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
        ProfitSharingJournal profitSharingJournal = BsinServiceContext.getReqBodyDto(ProfitSharingJournal.class, requestMap);
        
        // 设置租户ID
        profitSharingJournal.setTenantId(loginUser.getTenantId());
        
        // 生成流水号
        profitSharingJournal.setSerialNo(BsinSnowflake.getId());
        
        // 设置创建时间
        profitSharingJournal.setCreatedTime(new Date());
        
        // 设置默认状态
        if (StringUtils.isBlank(profitSharingJournal.getStatus())) {
            profitSharingJournal.setStatus("1"); // 1-正常
        }
        
        profitSharingJournalMapper.insert(profitSharingJournal);
        return profitSharingJournal;
    }

    @ApiDoc(desc = "delete")
    @ShenyuDubboClient("/delete")
    @Override
    public void delete(Map<String, Object> requestMap) {
        log.debug("请求ProfitSharingJournalService.delete,参数:{}", requestMap);
        String serialNo = (String) requestMap.get("serialNo");
        if (StringUtils.isBlank(serialNo)) {
            throw new BusinessException(SERIAL_NO_NOT_EXIST);
        }
        
        if (profitSharingJournalMapper.deleteById(serialNo) == 0) {
            throw new BusinessException(SERIAL_NO_NOT_EXIST);
        }
    }

    @ApiDoc(desc = "edit")
    @ShenyuDubboClient("/edit")
    @Override
    public ProfitSharingJournal edit(Map<String, Object> requestMap) {
        log.debug("请求ProfitSharingJournalService.edit,参数:{}", requestMap);
        LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
        ProfitSharingJournal profitSharingJournal = BsinServiceContext.getReqBodyDto(ProfitSharingJournal.class, requestMap);
        
        // 设置租户ID
        profitSharingJournal.setTenantId(loginUser.getTenantId());
        
        // 验证记录是否存在
        ProfitSharingJournal existingJournal = profitSharingJournalMapper.selectById(profitSharingJournal.getSerialNo());
        if (existingJournal == null) {
            throw new BusinessException(SERIAL_NO_NOT_EXIST);
        }
        
        if (profitSharingJournalMapper.updateById(profitSharingJournal) == 0) {
            throw new BusinessException(SERIAL_NO_NOT_EXIST);
        }
        
        return profitSharingJournal;
    }

    @ApiDoc(desc = "getDetail")
    @ShenyuDubboClient("/getDetail")
    @Override
    public ProfitSharingJournal getDetail(Map<String, Object> requestMap) {
        log.debug("请求ProfitSharingJournalService.getDetail,参数:{}", requestMap);
        String serialNo = (String) requestMap.get("serialNo");
        if (StringUtils.isBlank(serialNo)) {
            throw new BusinessException(SERIAL_NO_NOT_EXIST);
        }
        
        ProfitSharingJournal profitSharingJournal = profitSharingJournalMapper.selectById(serialNo);
        if (profitSharingJournal == null) {
            throw new BusinessException(SERIAL_NO_NOT_EXIST);
        }
        
        return profitSharingJournal;
    }

    @ApiDoc(desc = "getPageList")
    @ShenyuDubboClient("/getPageList")
    @Override
    public IPage<?> getPageList(Map<String, Object> requestMap) {
        log.debug("请求ProfitSharingJournalService.getPageList,参数:{}", requestMap);
        LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
        
        // 获取分页参数
        Object paginationObj = requestMap.get("pagination");
        Pagination pagination = new Pagination();
        BeanUtil.copyProperties(paginationObj, pagination);
        
        // 构建查询条件
        LambdaQueryWrapper<ProfitSharingJournal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProfitSharingJournal::getTenantId, loginUser.getTenantId());
        
        // 添加查询条件
        String transactionNo = (String) requestMap.get("transactionNo");
        if (StringUtils.isNotBlank(transactionNo)) {
            wrapper.like(ProfitSharingJournal::getTransactionNo, transactionNo);
        }
        
        String bizRoleType = (String) requestMap.get("bizRoleType");
        if (StringUtils.isNotBlank(bizRoleType)) {
            wrapper.eq(ProfitSharingJournal::getBizRoleType, bizRoleType);
        }
        
        String status = (String) requestMap.get("status");
        if (StringUtils.isNotBlank(status)) {
            wrapper.eq(ProfitSharingJournal::getStatus, status);
        }
        
        String receiverId = (String) requestMap.get("receiverId");
        if (StringUtils.isNotBlank(receiverId)) {
            wrapper.eq(ProfitSharingJournal::getReceiverId, receiverId);
        }
        
        // 按创建时间倒序排列
        wrapper.orderByDesc(ProfitSharingJournal::getCreatedTime);
        
        Page<ProfitSharingJournal> page = new Page<>(pagination.getPageNum(), pagination.getPageSize());
        IPage<ProfitSharingJournal> pageList = profitSharingJournalMapper.selectPage(page, wrapper);
        
        return pageList;
    }

    @ApiDoc(desc = "getListByTransactionNo")
    @ShenyuDubboClient("/getListByTransactionNo")
    @Override
    public List<ProfitSharingJournal> getListByTransactionNo(Map<String, Object> requestMap) {
        log.debug("请求ProfitSharingJournalService.getListByTransactionNo,参数:{}", requestMap);
        LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
        String transactionNo = (String) requestMap.get("transactionNo");
        
        if (StringUtils.isBlank(transactionNo)) {
            throw new BusinessException("交易单号不能为空");
        }
        
        LambdaQueryWrapper<ProfitSharingJournal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProfitSharingJournal::getTenantId, loginUser.getTenantId());
        wrapper.eq(ProfitSharingJournal::getTransactionNo, transactionNo);
        wrapper.orderByDesc(ProfitSharingJournal::getCreatedTime);
        
        return profitSharingJournalMapper.selectList(wrapper);
    }
}
