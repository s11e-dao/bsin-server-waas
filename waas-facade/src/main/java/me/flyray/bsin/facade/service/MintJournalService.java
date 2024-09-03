package me.flyray.bsin.facade.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import me.flyray.bsin.domain.entity.MintJournal;

import java.util.Map;

/**
 * @author bolei
 * @date 2023/8/13
 * @desc
 */

public interface MintJournalService {

    /**
     * 分页查询
     */
    public IPage<?> getPageList(Map<String, Object> requestMap);


    /**
     * 查询详情
     */
    public MintJournal getDetail(Map<String, Object> requestMap);
    
}
