package me.flyray.bsin.facade.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import me.flyray.bsin.domain.entity.BondingCurveTokenJournal;
import me.flyray.bsin.domain.entity.BondingCurveTokenParam;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

/**
 * @author bolei
 * @createDate 2023-07-19 21:12:25
 */
public interface BondingCurveTokenService {


  /**
   * 计算一系列现金流在给定的联合收益率曲线下的现值。
   * 基于劳动价值贡献和数据价值贡献计算联合曲线值
   */
  public double calculateCurveValue(Map<String, Object> yieldCurve);

  /** 新增 */
  public BondingCurveTokenParam add(Map<String, Object> requestMap);

  /** 删除 */
  public void delete(Map<String, Object> requestMap);

  /** 修改 */
  public Map<String, Object> edit(Map<String, Object> requestMap);

  /** 查询曲线列表 */
  public List<?> getList(Map<String, Object> requestMap);


  /** 查询商戶BC曲线---唯一的 */
  public BondingCurveTokenParam getMerchantCurve(Map<String, Object> requestMap);

  /** 分页查询曲线详情 */
  public IPage<?> getPageList(Map<String, Object> requestMap);

  /** 查询曲线详情 */
  public BondingCurveTokenParam getDetail(Map<String, Object> requestMap);

  /**
   * 根据劳动价值铸造原力(成长值):
   * 捕获劳动价值的联合曲线积分若绑定了商户发行的数字积分，mint时需要查询tokenParam配置参数，按照配置参数进行数字积分释放铸造
   *
   * @param: laborValue 劳动价值(法币标的)
   * @return: 原力值
   */
  public Map<String, Object> mint(Map<String, Object> requestMap) throws UnsupportedEncodingException;

  /**
   * 赎回
   * @param: 原力积分数量
   * @return: laborValue
   */
  public Map<String, Object> redeem(Map<String, Object> requestMap) throws UnsupportedEncodingException;

  /**
   * 获取联合曲线数据
   *
   * @param:
   * @return:
   */
  public IPage<?> getJournalPageList(Map<String, Object> requestMap);

  /**
   * 获取联合曲线流水详情
   *
   * @param:
   * @return:
   */
  public BondingCurveTokenJournal getTransactionDetail(Map<String, Object> requestMap);

  /**
   * 获取联合曲线数据--曲线展示
   *
   * @param:
   * @return:
   */
  public List<?> getJournalList(Map<String, Object> requestMap);

  /**
   * 获取联合曲线未来数据--曲线展示
   *
   * @param:
   * @return:
   */
  public List<?> getTrendList(Map<String, Object> requestMap);

}
