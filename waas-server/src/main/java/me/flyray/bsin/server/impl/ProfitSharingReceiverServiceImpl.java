package me.flyray.bsin.server.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import me.flyray.bsin.constants.ResponseCode;
import me.flyray.bsin.context.BsinServiceContext;
import me.flyray.bsin.domain.entity.ProfitSharingReceiver;
import me.flyray.bsin.exception.BusinessException;
import me.flyray.bsin.facade.service.ProfitSharingReceiverService;
import me.flyray.bsin.infrastructure.mapper.ProfitSharingReceiverMapper;
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
@ShenyuDubboService(path = "/profitSharingReceiverService", timeout = 6000)
@ApiModule(value = "profitSharingReceiverService")
@Service
public class ProfitSharingReceiverServiceImpl implements ProfitSharingReceiverService {

    @Autowired
    private ProfitSharingReceiverMapper profitSharingReceiverMapper;

    @ApiDoc(desc = "add")
    @ShenyuDubboClient("/add")
    @Override
    public ProfitSharingReceiver add(Map<String, Object> requestMap) {
        log.debug("请求ProfitSharingReceiverService.add,参数:{}", requestMap);
        LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
        ProfitSharingReceiver profitSharingReceiver = BsinServiceContext.getReqBodyDto(ProfitSharingReceiver.class, requestMap);
        
        // 设置租户ID
        profitSharingReceiver.setTenantId(loginUser.getTenantId());
        
        // 生成流水号
        profitSharingReceiver.setSerialNo(BsinSnowflake.getId());
        
        // 设置创建时间
        profitSharingReceiver.setCreateTime(new Date());
        
        // 设置默认状态
        if (StringUtils.isBlank(profitSharingReceiver.getStatus())) {
            profitSharingReceiver.setStatus("1"); // 1-正常分账
        }
        
        // 验证接收方ID是否已存在
        LambdaQueryWrapper<ProfitSharingReceiver> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProfitSharingReceiver::getTenantId, loginUser.getTenantId());
        wrapper.eq(ProfitSharingReceiver::getReceiverId, profitSharingReceiver.getReceiverId());
        wrapper.eq(ProfitSharingReceiver::getPayChannelCode, profitSharingReceiver.getPayChannelCode());
        
        if (profitSharingReceiverMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("该接收方已存在");
        }
        
        profitSharingReceiverMapper.insert(profitSharingReceiver);
        return profitSharingReceiver;
    }

    @ApiDoc(desc = "delete")
    @ShenyuDubboClient("/delete")
    @Override
    public void delete(Map<String, Object> requestMap) {
        log.debug("请求ProfitSharingReceiverService.delete,参数:{}", requestMap);
        String serialNo = (String) requestMap.get("serialNo");
        if (StringUtils.isBlank(serialNo)) {
            throw new BusinessException(SERIAL_NO_NOT_EXIST);
        }
        
        if (profitSharingReceiverMapper.deleteById(serialNo) == 0) {
            throw new BusinessException(SERIAL_NO_NOT_EXIST);
        }
    }

    @ApiDoc(desc = "edit")
    @ShenyuDubboClient("/edit")
    @Override
    public ProfitSharingReceiver edit(Map<String, Object> requestMap) {
        log.debug("请求ProfitSharingReceiverService.edit,参数:{}", requestMap);
        LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
        ProfitSharingReceiver profitSharingReceiver = BsinServiceContext.getReqBodyDto(ProfitSharingReceiver.class, requestMap);
        
        // 设置租户ID
        profitSharingReceiver.setTenantId(loginUser.getTenantId());
        
        // 验证记录是否存在
        ProfitSharingReceiver existingReceiver = profitSharingReceiverMapper.selectById(profitSharingReceiver.getSerialNo());
        if (existingReceiver == null) {
            throw new BusinessException(SERIAL_NO_NOT_EXIST);
        }
        
        if (profitSharingReceiverMapper.updateById(profitSharingReceiver) == 0) {
            throw new BusinessException(SERIAL_NO_NOT_EXIST);
        }
        
        return profitSharingReceiver;
    }

    @ApiDoc(desc = "getDetail")
    @ShenyuDubboClient("/getDetail")
    @Override
    public ProfitSharingReceiver getDetail(Map<String, Object> requestMap) {
        log.debug("请求ProfitSharingReceiverService.getDetail,参数:{}", requestMap);
        String serialNo = (String) requestMap.get("serialNo");
        if (StringUtils.isBlank(serialNo)) {
            throw new BusinessException(SERIAL_NO_NOT_EXIST);
        }
        
        ProfitSharingReceiver profitSharingReceiver = profitSharingReceiverMapper.selectById(serialNo);
        if (profitSharingReceiver == null) {
            throw new BusinessException(SERIAL_NO_NOT_EXIST);
        }
        
        return profitSharingReceiver;
    }

    @ApiDoc(desc = "getPageList")
    @ShenyuDubboClient("/getPageList")
    @Override
    public IPage<?> getPageList(Map<String, Object> requestMap) {
        log.debug("请求ProfitSharingReceiverService.getPageList,参数:{}", requestMap);
        LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
        
        // 获取分页参数
        Object paginationObj = requestMap.get("pagination");
        Pagination pagination = new Pagination();
        BeanUtil.copyProperties(paginationObj, pagination);
        
        // 构建查询条件
        LambdaQueryWrapper<ProfitSharingReceiver> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProfitSharingReceiver::getTenantId, loginUser.getTenantId());
        
        // 添加查询条件
        String receiverName = (String) requestMap.get("receiverName");
        if (StringUtils.isNotBlank(receiverName)) {
            wrapper.like(ProfitSharingReceiver::getReceiverName, receiverName);
        }
        
        String receiverId = (String) requestMap.get("receiverId");
        if (StringUtils.isNotBlank(receiverId)) {
            wrapper.eq(ProfitSharingReceiver::getReceiverId, receiverId);
        }
        
        String type = (String) requestMap.get("type");
        if (StringUtils.isNotBlank(type)) {
            wrapper.eq(ProfitSharingReceiver::getType, type);
        }
        
        String status = (String) requestMap.get("status");
        if (StringUtils.isNotBlank(status)) {
            wrapper.eq(ProfitSharingReceiver::getStatus, status);
        }
        
        String payChannelCode = (String) requestMap.get("payChannelCode");
        if (StringUtils.isNotBlank(payChannelCode)) {
            wrapper.eq(ProfitSharingReceiver::getPayChannelCode, payChannelCode);
        }
        
        // 按创建时间倒序排列
        wrapper.orderByDesc(ProfitSharingReceiver::getCreateTime);
        
        Page<ProfitSharingReceiver> page = new Page<>(pagination.getPageNum(), pagination.getPageSize());
        IPage<ProfitSharingReceiver> pageList = profitSharingReceiverMapper.selectPage(page, wrapper);
        
        return pageList;
    }

    @ApiDoc(desc = "getByReceiverId")
    @ShenyuDubboClient("/getByReceiverId")
    @Override
    public ProfitSharingReceiver getByReceiverId(Map<String, Object> requestMap) {
        log.debug("请求ProfitSharingReceiverService.getByReceiverId,参数:{}", requestMap);
        LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
        String receiverId = (String) requestMap.get("receiverId");
        String payChannelCode = (String) requestMap.get("payChannelCode");
        
        if (StringUtils.isBlank(receiverId)) {
            throw new BusinessException("接收方ID不能为空");
        }
        
        LambdaQueryWrapper<ProfitSharingReceiver> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProfitSharingReceiver::getTenantId, loginUser.getTenantId());
        wrapper.eq(ProfitSharingReceiver::getReceiverId, receiverId);
        
        if (StringUtils.isNotBlank(payChannelCode)) {
            wrapper.eq(ProfitSharingReceiver::getPayChannelCode, payChannelCode);
        }
        
        return profitSharingReceiverMapper.selectOne(wrapper);
    }

    @ApiDoc(desc = "getListByMerchantNo")
    @ShenyuDubboClient("/getListByMerchantNo")
    @Override
    public List<ProfitSharingReceiver> getListByMerchantNo(Map<String, Object> requestMap) {
        log.debug("请求ProfitSharingReceiverService.getListByMerchantNo,参数:{}", requestMap);
        LoginUser loginUser = LoginInfoContextHelper.getLoginUser();
        String senderMerchantNo = (String) requestMap.get("senderMerchantNo");
        
        if (StringUtils.isBlank(senderMerchantNo)) {
            throw new BusinessException("商户号不能为空");
        }
        
        LambdaQueryWrapper<ProfitSharingReceiver> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProfitSharingReceiver::getTenantId, loginUser.getTenantId());
        wrapper.eq(ProfitSharingReceiver::getSenderMerchantNo, senderMerchantNo);
        wrapper.eq(ProfitSharingReceiver::getStatus, "1"); // 只查询正常状态
        wrapper.orderByDesc(ProfitSharingReceiver::getCreateTime);
        
        return profitSharingReceiverMapper.selectList(wrapper);
    }

    @ApiDoc(desc = "updateStatus")
    @ShenyuDubboClient("/updateStatus")
    @Override
    public ProfitSharingReceiver updateStatus(Map<String, Object> requestMap) {
        log.debug("请求ProfitSharingReceiverService.updateStatus,参数:{}", requestMap);
        String serialNo = (String) requestMap.get("serialNo");
        String status = (String) requestMap.get("status");
        
        if (StringUtils.isBlank(serialNo)) {
            throw new BusinessException(SERIAL_NO_NOT_EXIST);
        }
        
        if (StringUtils.isBlank(status)) {
            throw new BusinessException("状态不能为空");
        }
        
        ProfitSharingReceiver profitSharingReceiver = profitSharingReceiverMapper.selectById(serialNo);
        if (profitSharingReceiver == null) {
            throw new BusinessException(SERIAL_NO_NOT_EXIST);
        }
        
        profitSharingReceiver.setStatus(status);
        
        if (profitSharingReceiverMapper.updateById(profitSharingReceiver) == 0) {
            throw new BusinessException(SERIAL_NO_NOT_EXIST);
        }
        
        return profitSharingReceiver;
    }
}
