package cn.easykits.crm.modules.system.vo;

/**
 * 字典项 VO（读接口对外统一结构，含内置 fallback）。
 */
public record DictItemVO(
        Long id,
        String dictType,
        String label,
        String value,
        Integer sortNo,
        Integer status
) {
}
