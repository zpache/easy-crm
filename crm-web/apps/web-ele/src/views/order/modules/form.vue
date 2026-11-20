<script lang="ts" setup>
import type { OrderApi } from '#/api/crm/order';

import { computed, onMounted, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import {
  ElButton,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElTable,
  ElTableColumn,
} from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import {
  createOrder,
  getOrderDetail,
  updateOrder,
} from '#/api/crm/order';

import { loadCustomerOptions, useFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | OrderApi.Order>(null);
const items = ref<OrderApi.OrderItem[]>([]);

const getTitle = computed(() =>
  formData.value?.id ? '编辑订单' : '新增订单',
);

/** 明细汇总 = Σ 单价*数量-折扣，保存前必须与总金额一致 */
const detailSum = computed(() =>
  items.value.reduce(
    (sum, item) =>
      sum +
      (Number(item.unitPrice) || 0) * (Number(item.quantity) || 0) -
      (Number(item.discount) || 0),
    0,
  ),
);

function newItem(): OrderApi.OrderItem {
  return { productName: '', unitPrice: 0, quantity: 1, discount: 0 };
}

function addItem() {
  items.value = [...items.value, newItem()];
}

function removeItem(index: number) {
  items.value = items.value.filter((_, i) => i !== index);
}

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: useFormSchema(),
  showDefaultActions: false,
});

function applyValues(data: OrderApi.Order) {
  formApi.setValues({
    contractId: data.contractId,
    quoteId: (data as any).quoteId ?? undefined,
    customerId: data.customerId,
    deliveryDate: data.deliveryDate,
    opportunityId: data.opportunityId,
    orderNo: data.orderNo,
    remark: data.remark,
    shipDate: data.shipDate,
    totalAmount: data.totalAmount,
  });
}

const [Drawer, drawerApi] = useVbenDrawer({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    if (items.value.length === 0) {
      ElMessage.error('请至少添加一条订单明细');
      return;
    }
    if (items.value.some((i) => !i.productName?.trim())) {
      ElMessage.error('明细产品名称不能为空');
      return;
    }
    const values = await formApi.getValues();
    if (Math.abs(detailSum.value - Number(values.totalAmount)) > 0.000_001) {
      ElMessage.error(
        `明细汇总(${detailSum.value.toFixed(2)}) 与总金额(${Number(values.totalAmount).toFixed(2)}) 不一致，拒绝保存`,
      );
      return;
    }
    drawerApi.lock();
    const payload: OrderApi.OrderPayload = {
      contractId: values.contractId ?? undefined,
      quoteId: values.quoteId ?? undefined,
      customerId: values.customerId,
      deliveryDate: values.deliveryDate || undefined,
      items: items.value.map((i) => ({
        discount: Number(i.discount) || 0,
        productId: i.productId ?? undefined,
        productName: i.productName,
        quantity: Number(i.quantity),
        remark: i.remark || undefined,
        unitPrice: Number(i.unitPrice),
      })),
      opportunityId: values.opportunityId ?? undefined,
      orderNo: values.orderNo || undefined,
      remark: values.remark || undefined,
      shipDate: values.shipDate || undefined,
      totalAmount: Number(values.totalAmount),
    };
    try {
      if (formData.value?.id) {
        await updateOrder(formData.value.id, payload);
      } else {
        await createOrder(payload);
      }
      emit('success');
      drawerApi.close();
    } finally {
      drawerApi.lock(false);
    }
  },
  onOpenChange(isOpen) {
    if (isOpen) {
      const data = drawerApi.getData<OrderApi.Order>();
      formData.value = data && data.id ? data : null;
      formApi.resetForm();
      if (formData.value?.id) {
        applyValues(formData.value);
        // 列表行不含明细，编辑时拉详情回填 items
        getOrderDetail(formData.value.id).then((detail) => {
          items.value = (detail.items ?? []).map((i) => ({ ...i }));
          if (items.value.length === 0) {
            items.value = [newItem()];
          }
        });
      } else {
        items.value = [newItem()];
      }
    }
  },
});

onMounted(async () => {
  await loadCustomerOptions();
});
</script>

<template>
  <Drawer :title="getTitle" class="w-[720px]">
    <Form />
    <div class="mb-2 mt-4 flex items-center justify-between">
      <span class="text-sm font-medium">订单明细</span>
      <ElButton size="small" type="primary" plain @click="addItem">
        添加明细
      </ElButton>
    </div>
    <ElTable :data="items" border size="small">
      <ElTableColumn label="产品名称" min-width="150">
        <template #default="{ row }">
          <ElInput v-model="row.productName" placeholder="必填" />
        </template>
      </ElTableColumn>
      <ElTableColumn label="单价" width="120">
        <template #default="{ row }">
          <ElInputNumber
            v-model="row.unitPrice"
            :min="0"
            :precision="2"
            controls-position="right"
            size="small"
          />
        </template>
      </ElTableColumn>
      <ElTableColumn label="数量" width="110">
        <template #default="{ row }">
          <ElInputNumber
            v-model="row.quantity"
            :min="0"
            :precision="2"
            controls-position="right"
            size="small"
          />
        </template>
      </ElTableColumn>
      <ElTableColumn label="折扣" width="110">
        <template #default="{ row }">
          <ElInputNumber
            v-model="row.discount"
            :min="0"
            :precision="2"
            controls-position="right"
            size="small"
          />
        </template>
      </ElTableColumn>
      <ElTableColumn label="小计" width="110">
        <template #default="{ row }">
          {{
            (
              (Number(row.unitPrice) || 0) * (Number(row.quantity) || 0) -
              (Number(row.discount) || 0)
            ).toFixed(2)
          }}
        </template>
      </ElTableColumn>
      <ElTableColumn label="操作" width="80">
        <template #default="{ $index }">
          <ElButton link type="danger" @click="removeItem($index)">删除</ElButton>
        </template>
      </ElTableColumn>
    </ElTable>
    <div class="mt-2 text-right text-sm">
      明细汇总：<b>{{ detailSum.toFixed(2) }}</b>
      <span class="ml-2 text-gray-500">（须与上方「总金额」一致）</span>
    </div>
  </Drawer>
</template>
