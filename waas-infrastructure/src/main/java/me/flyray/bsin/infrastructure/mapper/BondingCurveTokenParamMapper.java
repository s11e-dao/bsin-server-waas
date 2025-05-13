package me.flyray.bsin.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.flyray.bsin.domain.entity.BondingCurveTokenParam;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
* @author bolei
* @description 针对表【crm_bonding_curve_token_param】的数据库操作Mapper
* @createDate 2023-08-06 13:47:52
* @Entity me.flyray.bsin.infrastructure.domain.BondingCurveTokenParam
*/

@Repository
@Mapper
public interface BondingCurveTokenParamMapper extends BaseMapper<BondingCurveTokenParam> {

}




