package me.flyray.bsin.domain.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DistributorValueAllocationDTO {

    /**
     * 参与者角色类型
     */
    private String roleType;

    /**
     * 参与者角色编号
     */
    private String roleNo;


    /**
     * 参与者角色贡献价值
     */
    private BigDecimal laborValue;

}
