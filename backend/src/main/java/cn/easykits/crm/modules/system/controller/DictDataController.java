package cn.easykits.crm.modules.system.controller;

import cn.easykits.crm.common.api.Result;
import cn.easykits.crm.modules.system.service.DictService;
import cn.easykits.crm.modules.system.vo.DictItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 字典数据读取（前端下拉 / Excel 校验共用）。登录即可读，不做按钮级权限。
 */
@Tag(name = "系统-字典数据")
@RestController
@RequestMapping("/dict-data")
@RequiredArgsConstructor
public class DictDataController {

    private final DictService dictService;

    @Operation(summary = "按字典类型取生效字典项（带内置默认值回退）")
    @GetMapping("/{dictType}")
    public Result<List<DictItemVO>> items(@PathVariable String dictType) {
        return Result.ok(dictService.itemsOfType(dictType));
    }
}
