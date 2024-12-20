package me.flyray.bsin.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.flyray.bsin.domain.entity.TransactionJournal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
* @author bolei
* @description 针对表【waas_transaction_journal】的数据库操作Mapper
* @createDate 2023-06-27 17:10:27
* @Entity generator.domain.
*/

@Repository
@Mapper
public interface TransactionJournalMapper extends BaseMapper<TransactionJournal> {

    boolean updateTransferStatus(@Param("transactionNo") String serialNo,@Param("status") String status);


}




