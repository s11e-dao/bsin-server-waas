package me.flyray.bsin.domain.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

@Data
public class ProfitSharingJournal implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 分账记录ID
     */
    private String recordId;

    /**
     * 服务商ID:
     */
    private String isvId;

    /**
     * 收款配置类型 1-服务商 2-商户 3-店铺
     */
    private Integer infoType;

    /**
     * 服务商号（服务商支付参数）或应用AppId（商户支付参数）
     */
    private String infoId;

    /**
     * 支付接口代码
     */
    private String ifCode;

    /**
     * 类型: 1-普通商户, 2-特约商户(服务商模式)
     */
    private Integer merchantType;

    /**
     * 系统支付订单号
     */
    private String payOrderId;

    /**
     * 支付订单渠道支付订单号
     */
    private String payOrderChannelOrderNo;

    /**
     * 订单金额,单位元
     */
    private BigDecimal payOrderAmount;

    /**
     * 订单实际分账金额, 单位：元（订单金额 - 商户手续费 - 已退款金额）
     */
    private BigDecimal payOrderDivisionAmount;

    /**
     * 系统分账批次号
     */
    private String batchOrderId;

    /**
     * 上游分账批次号
     */
    private String channelBatchOrderId;

    /**
     * 状态: 0-待分账 1-分账成功（明确成功）, 2-分账失败（明确失败）, 3-分账已受理（上游受理）
     */
    private Integer state;

    /**
     * 上游返回数据包
     */
    private String channelRespResult;

    /**
     * 账号快照》 分账接收者ID
     */
    private String receiverId;

    /**
     * 账号快照》 分账接收账号类型: 0-个人 1-商户
     */
    private Integer accType;

    /**
     * 账号快照》 分账接收账号
     */
    private String accNo;

    /**
     * 账号快照》 分账接收账号名称
     */
    private String accName;

    /**
     * 账号快照》 分账关系类型（参考微信）, 如： SERVICE_PROVIDER 服务商等
     */
    private String relationType;

    /**
     * 账号快照》 当选择自定义时，需要录入该字段。 否则为对应的名称
     */
    private String relationTypeName;

    /**
     * 计算该接收方的分账金额,单位元
     */
    private BigDecimal calDivisionAmount;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;

    /**
     * 向下游回调状态, 0-未发送,  1-已发送
     */
    private Integer notifyState;
}
