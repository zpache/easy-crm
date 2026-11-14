import { requestClient } from '#/api/request';

export namespace TaskApi {
  /** 待办任务 */
  export interface Task {
    [key: string]: any;
    bizId?: null | number;
    bizType?: null | string;
    bizTypeName?: string;
    content?: string;
    createTime?: string;
    dueTime: string;
    expired?: boolean;
    id: number;
    ownerId?: null | number;
    ownerName?: string;
    source?: string;
    status: 0 | 1 | 2;
    statusName?: string;
    title: string;
    updateTime?: string;
  }

  /** 创建 / 编辑表单 */
  export interface TaskPayload {
    content?: string;
    dueTime: string;
    title: string;
  }

  /** 分页结果 */
  export interface PageResult<T> {
    pageNum: number;
    pageSize: number;
    pages: number;
    records: T[];
    total: number;
  }
}

/** 任务状态字典（2=已过期为动态计算） */
export const TASK_STATUSES = [
  { label: '待办', value: 0 },
  { label: '已完成', value: 1 },
  { label: '已过期', value: 2 },
] as const;

/** 任务来源标签 */
export function sourceLabel(source?: string) {
  return source === 'FOLLOW' ? '跟进生成' : '手动创建';
}

/**
 * 任务分页列表
 */
async function getTaskList(params: Recordable<any>) {
  const result = await requestClient.get<TaskApi.PageResult<any>>(
    '/tasks',
    { params },
  );
  return { items: result.records, total: result.total };
}

/**
 * 我的待办（未完成且临近 3 天内或已过期）
 */
async function getMyTodo(params: Recordable<any>) {
  const result = await requestClient.get<TaskApi.PageResult<any>>(
    '/tasks/todo',
    { params },
  );
  return { items: result.records, total: result.total };
}

async function createTask(data: TaskApi.TaskPayload) {
  return requestClient.post<{ id: number }>('/tasks', data);
}

async function updateTask(id: number, data: TaskApi.TaskPayload) {
  return requestClient.put(`/tasks/${id}`, data);
}

async function completeTask(id: number) {
  return requestClient.put(`/tasks/${id}/complete`);
}

export {
  completeTask,
  createTask,
  getMyTodo,
  getTaskList,
  updateTask,
};
