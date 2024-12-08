package me.flyray.bsin.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.flyray.bsin.domain.entity.MetadataFile;
import me.flyray.bsin.domain.entity.WaasTransactionJournal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;

/**
* @author bolei
* @description 针对表【waas_transaction_journal】的数据库操作Mapper
* @createDate 2023-06-27 17:10:27
* @Entity generator.domain.
*/

@Repository
@Mapper
public interface WaasTransactionJournalMapper extends BaseMapper<WaasTransactionJournal> {

    boolean updateTransferStatus(@Param("transactionNo") String serialNo,@Param("status") String status);


}




