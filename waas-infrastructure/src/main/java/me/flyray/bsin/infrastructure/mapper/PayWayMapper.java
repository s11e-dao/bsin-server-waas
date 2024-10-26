package me.flyray.bsin.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.flyray.bsin.domain.entity.PayWay;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
* @author rednet
* @description 针对表【waas_pay_way(支付渠道表)】的数据库操作Mapper
* @createDate 2024-10-26 10:16:00
* @Entity generator.domain.PayWay
*/
@Repository
@Mapper
public interface PayWayMapper extends BaseMapper<PayWay> {

}




