package me.flyray.bsin.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.flyray.bsin.domain.entity.PayChannelInterface;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
* @author rednet
* @description 针对表【waas_pay_channel_interface(支付渠道具体接口定义表)】的数据库操作Mapper
* @createDate 2024-10-26 10:15:56
* @Entity generator.domain.PayChannelInterface
*/
@Repository
@Mapper
public interface PayChannelInterfaceMapper extends BaseMapper<PayChannelInterface> {

}




