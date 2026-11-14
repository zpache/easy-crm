<script lang="ts" setup>
import type { CustomerApi } from '#/api/crm/customer';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { ElDescriptions, ElDescriptionsItem, ElTable } from 'element-plus';

import { getCustomer360 } from '#/api/crm/customer';

const customer = ref<null | CustomerApi.Customer>(null);
const contacts = ref<Array<Recordable<any>>>([]);
const opportunities = ref<Array<Recordable<any>>>([]);
const followRecords = ref<Array<Recordable<any>>>([]);

const getTitle = computed(() =>
  customer.value ? `客户 360 视图 - ${customer.value.name}` : '客户 360 视图',
);

const [Drawer, drawerApi] = useVbenDrawer({
  async onOpenChange(isOpen) {
    if (!isOpen) {
      return;
    }
    const data = drawerApi.getData<CustomerApi.Customer>();
    const view = await getCustomer360(data.id);
    customer.value = view.customer;
    contacts.value = view.contacts;
    opportunities.value = view.opportunities;
    followRecords.value = view.followRecords;
  },
});
</script>

<template>
  <Drawer :title="getTitle" class="w-[720px]">
    <template v-if="customer">
      <div class="mb-3 text-sm font-medium">基本信息</div>
      <ElDescriptions :column="3" border size="small">
        <ElDescriptionsItem label="客户名称">{{ customer.name }}</ElDescriptionsItem>
        <ElDescriptionsItem label="信用代码">{{ customer.creditCode || '-' }}</ElDescriptionsItem>
        <ElDescriptionsItem label="状态">{{ customer.statusName }}</ElDescriptionsItem>
        <ElDescriptionsItem label="行业">{{ customer.industryName || '-' }}</ElDescriptionsItem>
        <ElDescriptionsItem label="规模">{{ customer.scaleName || '-' }}</ElDescriptionsItem>
        <ElDescriptionsItem label="级别">{{ customer.levelName || '-' }}</ElDescriptionsItem>
        <ElDescriptionsItem label="区域">{{ customer.regionName || '-' }}</ElDescriptionsItem>
        <ElDescriptionsItem label="负责人">{{ customer.ownerName || '公海' }}</ElDescriptionsItem>
        <ElDescriptionsItem label="部门">{{ customer.deptName || '-' }}</ElDescriptionsItem>
        <ElDescriptionsItem label="最近跟进">{{ customer.lastFollowTime || '-' }}</ElDescriptionsItem>
        <ElDescriptionsItem label="创建时间">{{ customer.createTime || '-' }}</ElDescriptionsItem>
        <ElDescriptionsItem label="备注">{{ customer.remark || '-' }}</ElDescriptionsItem>
      </ElDescriptions>

      <div class="mb-2 mt-4 text-sm font-medium">联系人（{{ contacts.length }}）</div>
      <ElTable :data="contacts" size="small" border>
        <ElTable.TableColumn prop="name" label="姓名" width="120" />
        <ElTable.TableColumn prop="phone" label="手机号" width="130" />
        <ElTable.TableColumn prop="email" label="邮箱" min-width="140" />
        <ElTable.TableColumn prop="position" label="职位" width="110" />
        <ElTable.TableColumn label="主联系人" width="90" align="center">
          <template #default="{ row }">
            <span v-if="row.isPrimary" class="text-primary font-medium">是</span>
            <span v-else>否</span>
          </template>
        </ElTable.TableColumn>
      </ElTable>

      <div class="mb-2 mt-4 text-sm font-medium">商机（{{ opportunities.length }}）</div>
      <ElTable :data="opportunities" size="small" border empty-text="暂无商机">
        <ElTable.TableColumn
          label="商机名称"
          min-width="160"
          prop="name"
          show-overflow-tooltip
        />
        <ElTable.TableColumn label="阶段" prop="stageName" width="110" />
        <ElTable.TableColumn label="预计金额" width="120">
          <template #default="{ row }">
            {{ row.amount == null ? '-' : Number(row.amount).toLocaleString() }}
          </template>
        </ElTable.TableColumn>
        <ElTable.TableColumn label="赢单率%" prop="winRate" width="90" />
        <ElTable.TableColumn label="负责人" prop="ownerName" width="110" />
        <ElTable.TableColumn label="预计成交" prop="expectedCloseDate" width="120" />
        <ElTable.TableColumn label="创建时间" prop="createTime" width="160" />
      </ElTable>

      <div class="mb-2 mt-4 text-sm font-medium">跟进记录（{{ followRecords.length }}）</div>
      <ElTable :data="followRecords" size="small" border empty-text="暂无跟进记录">
        <ElTable.TableColumn label="跟进类型" prop="followTypeName" width="100" />
        <ElTable.TableColumn
          label="跟进内容"
          min-width="220"
          prop="content"
          show-overflow-tooltip
        />
        <ElTable.TableColumn label="跟进人" prop="ownerName" width="110" />
        <ElTable.TableColumn label="下次跟进" prop="nextFollowTime" width="160" />
        <ElTable.TableColumn label="跟进时间" prop="createTime" width="160" />
      </ElTable>
    </template>
  </Drawer>
</template>
