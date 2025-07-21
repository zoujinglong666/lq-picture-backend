package com.zjl.lqpicturebackend.model.enums;

public enum UserRoleEnum {

    /**
     * 角色枚举
     */
    USER("用户", "user"),

    ADMIN("管理员", "admin");

    private final String value;
    private final String text;


    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return text;
    }

    public static UserRoleEnum fromValue(String value) {
        for (UserRoleEnum b : UserRoleEnum.values()) {
            if (b.getValue().equals(value)) {
                return b;
            }
        }
        return null;
    }

    public static UserRoleEnum fromText(String text) {
        for (UserRoleEnum b : UserRoleEnum.values()) {
            if (b.getText().equals(text)) {
                return b;
            }
        }
        return null;
    }


    }
