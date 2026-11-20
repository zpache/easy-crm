import { ref } from 'vue';

import { requestClient } from '#/api/request';

/** 字典项（/dict-data/{dictType} 返回） */
export interface DictOption {
  label: string;
  value: string;
}

/**
 * 字典下拉选项（模块级缓存，同类型只请求一次）。
 *
 * <p>取值策略：优先字典接口数据，接口失败或为空时回退调用方传入的硬编码常量
 * （常量类后续整体退役由字典表接管）。
 *
 * <p>value 归一化：纯数字字符串转为 Number，与表单 Integer 字段直接匹配。
 */
export function useDictOptions(
  dictType: string,
  fallback: Array<{ label: string; value: any }>,
) {
  const options = ref<Array<{ label: string; value: any }>>(
    fallback.map((o) => ({ ...o })),
  );
  const loaded = ref(false);

  async function load() {
    if (loaded.value) {
      return;
    }
    loaded.value = true;
    try {
      const data = await requestClient.get<Array<DictOption>>(
        `/dict-data/${dictType}`,
      );
      if (data && data.length > 0) {
        options.value = data
          .filter((d) => d.status === undefined || d.status === 1)
          .map((d) => ({
            label: d.label,
            value: /^\d+$/.test(d.value) ? Number(d.value) : d.value,
          }));
      }
    } catch {
      // 字典接口失败：静默保留 fallback 常量
      loaded.value = false;
    }
  }

  return { options, load };
}
