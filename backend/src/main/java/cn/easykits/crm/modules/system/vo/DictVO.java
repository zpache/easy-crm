package cn.easykits.crm.modules.system.vo;

import java.time.LocalDateTime;

/**
 * 字典类型 VO。
 */
public record DictVO(
        Long id,
        String dictType,
        String name,
        String remark,
        Integer builtIn,
        Integer status,
        Long itemCount,
        LocalDateTime updateTime
) {
}
