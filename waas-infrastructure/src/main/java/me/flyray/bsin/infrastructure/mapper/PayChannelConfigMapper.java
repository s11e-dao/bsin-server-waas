package me.flyray.bsin.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.flyray.bsin.domain.entity.PayChannelConfig;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
* @author rednet
* @description 针对表【waas_pay_channel_config(应用支付接口参数配置表)】的数据库操作Mapper
* @createDate 2024-10-26 10:15:50
* @Entity generator.domain.PayChannelConfig
*/
@Repository
@Mapper
public interface PayChannelConfigMapper extends BaseMapper<PayChannelConfig> {

}




