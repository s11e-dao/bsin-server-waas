package me.flyray.bsin.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ValueAllocationType {

    /***
     * 交易类型 transaction_type
     */
    TRANSACTION_TYPE("1", "交易分配"),

    /***
     * 数据类型 data_type
     */
    DATA_TYPE("2", "数据价值分配");

    private String code;

    private String desc;

    ValueAllocationType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /** Json 枚举序列化 */
    @JsonCreator
    public static ValueAllocationType getInstanceById(String id) {
        if (id == null) {
            return null;
        }
        for (ValueAllocationType status : values()) {
            if (id.equals(status.getCode())) {
                return status;
            }
        }
        return null;
    }

}
