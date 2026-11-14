import { requestClient } from '#/api/request';

/** 导出类型 */
export type CrmExportType =
  | 'contacts'
  | 'customers'
  | 'leads'
  | 'opportunities'
  | 'orders';

/** 导出中心：所有后端端点均为 POST（便于操作日志切面自动记录） */
export namespace CrmExportApi {
  export interface ExportParams {
    endDate?: string;
    keyword?: string;
    startDate?: string;
    status?: number;
  }
}

/**
 * 流式导出为 xlsx Blob。
 * requestClient.download 内部设置 responseType=blob / responseReturn=body，
 * 能正确绕过 JSON 响应拦截；通过 method:'POST' 对齐后端导出端点。
 */
async function exportCrm(type: CrmExportType, params: CrmExportApi.ExportParams = {}) {
  return requestClient.download<Blob>(`/exports/${type}`, {
    method: 'POST',
    params,
  });
}

/** 下载导入模板（仅表头） */
async function downloadCrmTemplate(type: CrmExportType = 'leads') {
  return requestClient.download<Blob>('/exports/template', {
    method: 'POST',
    params: { type },
  });
}

/** 浏览器端保存 Blob 文件 */
function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

/** 导出并直接触发下载 */
async function exportAndSave(type: CrmExportType, params: CrmExportApi.ExportParams = {}) {
  const blob = await exportCrm(type, params);
  const today = new Date().toISOString().slice(0, 10);
  downloadBlob(blob, `${type}_${today}.xlsx`);
}

export { downloadBlob, downloadCrmTemplate, exportAndSave, exportCrm };
