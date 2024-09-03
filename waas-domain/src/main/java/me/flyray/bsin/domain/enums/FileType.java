package me.flyray.bsin.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * @author bolei
 * @date 2023/6/29 15:43
 * @desc 件类型： 1 图片  2 gif 3 视频 4 音频 5 json 6文件夹
 */
public enum FileType {

    /**
     * 图片
     */
    IMG("1", "图片"),
    /**
     * gif
     */
    GIF("2", "gif"),
    /**
     * 视频
     */
    VIDEO("3", "视频"),
    /**
     * 音频 audio
     */
    AUDIO("4", "音频"),
    /**
     * json
     */
    JSON("5", "json"),
    /**
     * 文件夹 folder
     */
    FOLDER("6", "文件夹");

    private String code;

    private String desc;

    FileType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * Json 枚举序列化
     */
    @JsonCreator
    public static FileType getInstanceById(String id) {
        if (id == null) {
            return null;
        }
        for (FileType status : values()) {
            if (id.equals(status.getCode())) {
                return status;
            }
        }
        return null;
    }

}
