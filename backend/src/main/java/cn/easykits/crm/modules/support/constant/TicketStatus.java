package cn.easykits.crm.modules.support.constant;

import java.util.Arrays;

/**
 * 工单状态机：
 * <pre>
 *   PENDING 待受理 → ASSIGNED 已派单 → PROCESSING 处理中 → WAITING_CONFIRM 待客户确认
 *        → COMPLETED 已完成 / CLOSED 已关闭
 * </pre>
 * 终态（COMPLETED / CLOSED）不可再变更；关闭（CLOSED）可从任意非终态直接进入。
 * 状态流转合法性在 Service 层强制校验，不依赖前端。
 */
public enum TicketStatus {

    PENDING("PENDING", "待受理"),
    ASSIGNED("ASSIGNED", "已派单"),
    PROCESSING("PROCESSING", "处理中"),
    WAITING_CONFIRM("WAITING_CONFIRM", "待客户确认"),
    COMPLETED("COMPLETED", "已完成"),
    CLOSED("CLOSED", "已关闭");

    private final String code;
    private final String label;

    TicketStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static TicketStatus of(String code) {
        return Arrays.stream(values())
                .filter(s -> s.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("非法的工单状态: " + code));
    }

    public static boolean isValid(String code) {
        return code != null && Arrays.stream(values()).anyMatch(s -> s.code.equals(code));
    }

    public static String labelOf(String code) {
        return isValid(code) ? of(code).label : code;
    }

    /** 终态：已完成 / 已关闭，不可再变更。 */
    public boolean isTerminal() {
        return this == COMPLETED || this == CLOSED;
    }

    /**
     * 合法状态转移校验（终态不可再变）。
     * 允许：
     * PENDING → ASSIGNED / CLOSED；
     * ASSIGNED → PROCESSING / CLOSED；
     * PROCESSING → WAITING_CONFIRM / CLOSED；
     * WAITING_CONFIRM → COMPLETED / PROCESSING（客户驳回重新处理）/ CLOSED；
     * COMPLETED / CLOSED → 无。
     */
    public boolean canTransitionTo(TicketStatus target) {
        if (target == null || isTerminal()) {
            return false;
        }
        return switch (this) {
            case PENDING -> target == ASSIGNED || target == CLOSED;
            case ASSIGNED -> target == PROCESSING || target == CLOSED;
            case PROCESSING -> target == WAITING_CONFIRM || target == CLOSED;
            case WAITING_CONFIRM -> target == COMPLETED || target == PROCESSING || target == CLOSED;
            default -> false;
        };
    }

    /** 非终态状态集合（供 SLA 超时扫描排除终态工单）。 */
    public static String[] nonTerminalCodes() {
        return Arrays.stream(values())
                .filter(s -> !s.isTerminal())
                .map(TicketStatus::code)
                .toArray(String[]::new);
    }
}
