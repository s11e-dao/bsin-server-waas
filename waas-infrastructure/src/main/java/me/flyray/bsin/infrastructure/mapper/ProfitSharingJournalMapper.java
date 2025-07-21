package me.flyray.bsin.infrastructure.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import me.flyray.bsin.domain.entity.ProfitSharingJournal;
import org.apache.ibatis.annotations.Param;

public interface ProfitSharingJournalMapper extends BaseMapper<ProfitSharingJournal> {

//  IPage<ProfitSharingJournal> distinctBatchOrderIdList(
//      IPage<?> page, @Param("ew") Wrapper<ProfitSharingJournal> wrapper);
}
