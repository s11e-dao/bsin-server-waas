package me.flyray.bsin.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EcologicalValueAllocationType {

    EQUAL_DISTRIBUTION("EQUAL_DISTRIBUTION", "平均分配"),
    CONTRIBUTION_BASED("CONTRIBUTION_BASED","贡献度分配"),
    CARBON_CREDIT_BASED("CARBON_CREDIT_BASED","碳积分分配"),
    ECOSYSTEM_SERVICE_BASED("ECOSYSTEM_SERVICE_BASED","生态服务分配"),
    HYBRID("HYBRID","混合分配");

    private String code;

    private String desc;

    EcologicalValueAllocationType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    @JsonCreator
    public static EcologicalValueAllocationType getInstanceById(String id) {
        if (id == null) {
            return null;
        }
        for (EcologicalValueAllocationType status : values()) {
            if (id.equals(status.getCode())) {
                return status;
            }
        }
        return null;
    }

}
