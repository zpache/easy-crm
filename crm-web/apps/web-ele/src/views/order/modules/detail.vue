<script lang="ts" setup>
import type { OrderApi } from '#/api/crm/order';

import { ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { ElDescriptions, ElDescriptionsItem, ElTable, ElTableColumn } from 'element-plus';

const order = ref<OrderApi.Order | null>(null);

const [Drawer, drawerApi] = useVbenDrawer({
  onOpenChange(isOpen) {
    if (isOpen) {
      order.value = drawerApi.getData<OrderApi.Order>() ?? null;
    }
  },
});
</script>

<template>
  <Drawer title="订单详情" class="w-[720px]">
    <ElDescriptions v-if="order" :column="2" border size="small">
      <ElDescriptionsItem label="订单编号">
        {{ order.orderNo }}
      </ElDescriptionsItem>
      <ElDescriptionsItem label="客户">
        {{ order.customerName ?? order.customerId }}
      </ElDescriptionsItem>
      <ElDescriptionsItem label="状态">
        {{ order.statusName }}
      </ElDescriptionsItem>
      <ElDescriptionsItem label="总金额">
        {{ order.totalAmount }}
      </ElDescriptionsItem>
      <ElDescriptionsItem label="发货日期">
        {{ order.shipDate ?? '-' }}
      </ElDescriptionsItem>
      <ElDescriptionsItem label="交付日期">
        {{ order.deliveryDate ?? '-' }}
      </ElDescriptionsItem>
      <ElDescriptionsItem label="备注" :span="2">
        {{ order.remark ?? '-' }}
      </ElDescriptionsItem>
    </ElDescriptions>

    <div class="mb-2 mt-4 text-sm font-medium">订单明细</div>
    <ElTable :data="order?.items ?? []" border size="small">
      <ElTableColumn prop="productName" label="产品名称" min-width="150" />
      <ElTableColumn prop="unitPrice" label="单价" width="110" />
      <ElTableColumn prop="quantity" label="数量" width="90" />
      <ElTableColumn prop="discount" label="折扣" width="90" />
      <ElTableColumn prop="subtotal" label="小计" width="110" />
    </ElTable>
  </Drawer>
</template>
