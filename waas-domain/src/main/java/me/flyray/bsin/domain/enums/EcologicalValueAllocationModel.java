package me.flyray.bsin.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * 生态价值分配模型
 */
public enum EcologicalValueAllocationModel {

    CURVE_BASED("CURVE_BASED", "曲线价值分配模型"),
    PROPORTIONAL_DISTRIBUTION("PROPORTIONAL_DISTRIBUTION","等比例价值分配模型"),
    QUEUE_FREE_BILLING("QUEUE_FREE_BILLING","排队免单模型"),
    ;

    private String code;

    private String desc;

    EcologicalValueAllocationModel(String code, String desc) {
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
    public static EcologicalValueAllocationModel getInstanceById(String id) {
        if (id == null) {
            return null;
        }
        for (EcologicalValueAllocationModel status : values()) {
            if (id.equals(status.getCode())) {
                return status;
            }
        }
        return null;
    }

}
