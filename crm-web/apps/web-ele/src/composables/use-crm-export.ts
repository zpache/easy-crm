import { ref } from 'vue';

import { ElMessage } from 'element-plus';

import {
  exportAndSave,
  type CrmExportApi,
  type CrmExportType,
} from '#/api/crm/export';

/**
 * 列表页导出：按当前筛选条件导出 xlsx 并触发浏览器下载。
 * 统一处理 loading 与错误提示，避免每个页面重复 try/catch。
 */
export function useCrmExport(type: CrmExportType) {
  const exporting = ref(false);

  async function exportNow(params: CrmExportApi.ExportParams = {}) {
    if (exporting.value) {
      return;
    }
    exporting.value = true;
    try {
      await exportAndSave(type, params);
      ElMessage.success('导出已开始，请稍候查看下载文件');
    } catch {
      ElMessage.error('导出失败，请稍后重试');
    } finally {
      exporting.value = false;
    }
  }

  return { exportNow, exporting };
}
