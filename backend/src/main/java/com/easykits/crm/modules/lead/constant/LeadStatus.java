package com.easykits.crm.modules.lead.constant;

import java.util.Arrays;

/**
 * 线索状态机：新建(0) → 跟进中(1) → 已验证(2)，不合格(3) 可从新建/跟进中进入。
 * 状态流转只能前进；进入「已验证」只能通过转化接口（{@code POST /leads/{id}/convert}）。
 */
public enum LeadStatus {

    NEW(0, "新建"),
    FOLLOWING(1, "跟进中"),
    VERIFIED(2, "已验证"),
    UNQUALIFIED(3, "不合格");

    private final int code;
    private final String label;

    LeadStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static LeadStatus of(Integer code) {
        if (code == null) {
            return NEW;
        }
        return Arrays.stream(values())
                .filter(s -> s.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("非法的线索状态: " + code));
    }

    /**
     * 手工状态流转校验（不含转化）：只能前进。
     * 允许：0→1、0→3、1→3；1→2 与 0→2 必须走转化接口。
     */
    public boolean canManualTransitionTo(LeadStatus target) {
        if (target == VERIFIED) {
            return false;
        }
        return switch (this) {
            case NEW -> target == FOLLOWING || target == UNQUALIFIED;
            case FOLLOWING -> target == UNQUALIFIED;
            default -> false;
        };
    }

    /**
     * 是否允许转化：新建 / 跟进中可转化；已验证不可重复转化，不合格不可转化。
     */
    public boolean canConvert() {
        return this == NEW || this == FOLLOWING;
    }
}
