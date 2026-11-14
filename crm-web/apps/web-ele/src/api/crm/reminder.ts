import { requestClient } from '#/api/request';

export namespace ReminderApi {
  /** 提醒记录（按当前用户 ownerId 过滤后返回） */
  export interface Reminder {
    [key: string]: any;
    bizId: number;
    bizType: string;
    content: string;
    createTime?: string;
    id: number;
    levelName?: string;
    ownerId: number;
    readFlag: number;
    remindLevel: number;
    remindTime?: string;
  }

  export interface PageResult<T> {
    pageNum: number;
    pageSize: number;
    pages: number;
    records: T[];
    total: number;
  }

  /** vxe-grid 需要 items + total，后端原始结构是 records + total，这里做一层转换 */
  export interface PagedResult<T> {
    items: T[];
    total: number;
  }

  export interface Query {
    onlyUnread?: boolean;
    pageNum?: number;
    pageSize?: number;
  }
}

/**
 * 提醒分页（按当前用户 ownerId 过滤）
 */
async function getReminders(
  params: ReminderApi.Query,
): Promise<ReminderApi.PagedResult<ReminderApi.Reminder>> {
  const result = await requestClient.get<
    ReminderApi.PageResult<ReminderApi.Reminder>
  >('/reminders', { params });
  return { items: result.records, total: result.total };
}

/** 当前用户未读条数（铃铛角标） */
async function getUnreadCount() {
  return requestClient.get<{ count: number }>('/reminders/unread-count');
}

/** 标记单条已读 */
async function readReminder(id: number) {
  return requestClient.put<void>(`/reminders/${id}/read`);
}

/** 全部标记已读，返回本次置为已读的条数 */
async function readAllReminders() {
  return requestClient.put<number>('/reminders/read-all');
}

export {
  getReminders,
  getUnreadCount,
  readAllReminders,
  readReminder,
};
