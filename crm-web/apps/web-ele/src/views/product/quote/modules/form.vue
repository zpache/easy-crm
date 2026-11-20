<script lang="ts" setup>
import type { ProductApi } from '#/api/crm/product';

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
  loadProductOptions,
  productOptions,
} from '#/api/crm/product';
import {
  createQuote,
  getQuoteDetail,
  updateQuote,
} from '#/api/crm/product';

import { loadCustomerOptions, useFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | ProductApi.Quote>(null);
const items = ref<ProductApi.QuoteItem[]>([]);

const getTitle = computed(() =>
  formData.value?.id ? '编辑报价单' : '新增报价单',
);

/** 明细汇总 = Σ 单价*数量-折扣，保存前必须与总金额一致（后端同样强校验） */
const detailSum = computed(() =>
  items.value.reduce(
    (sum, item) =>
      sum +
      (Number(item.unitPrice) || 0) * (Number(item.quantity) || 0) -
      (Number(item.discount) || 0),
    0,
  ),
);

function newItem(): ProductApi.QuoteItem {
  return { productName: '', unitPrice: 0, quantity: 1, discount: 0 };
}

function addItem() {
  items.value = [...items.value, newItem()];
}

function removeItem(index: number) {
  items.value = items.value.filter((_, i) => i !== index);
}

/** 一键用明细汇总覆盖总金额，避免手工对不上 */
async function syncTotal() {
  await formApi.setValues({ totalAmount: Number(detailSum.value.toFixed(2)) });
}

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: useFormSchema(),
  showDefaultActions: false,
});

const [Drawer, drawerApi] = useVbenDrawer({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    if (items.value.length === 0) {
      ElMessage.error('请至少添加一条报价明细');
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
    const payload: ProductApi.QuotePayload = {
      customerId: Number(values.customerId),
      items: items.value.map((i) => ({
        discount: Number(i.discount) || 0,
        productId: i.productId ?? undefined,
        productName: i.productName,
        quantity: Number(i.quantity),
        remark: i.remark || undefined,
        unitPrice: Number(i.unitPrice),
      })),
      opportunityId: values.opportunityId ?? undefined,
      quoteNo: values.quoteNo || undefined,
      remark: values.remark || undefined,
      status: values.status ?? 0,
      totalAmount: Number(values.totalAmount),
      validUntil: values.validUntil || undefined,
    };
    try {
      if (formData.value?.id) {
        await updateQuote(formData.value.id, payload);
      } else {
        await createQuote(payload);
      }
      ElMessage.success('已保存');
      emit('success');
      drawerApi.close();
    } finally {
      drawerApi.lock(false);
    }
  },
  onOpenChange(isOpen) {
    if (isOpen) {
      const data = drawerApi.getData<ProductApi.Quote>();
      formData.value = data && data.id ? data : null;
      formApi.resetForm();
      if (formData.value?.id) {
        formApi.setValues({
          customerId: formData.value.customerId,
          opportunityId: formData.value.opportunityId,
          quoteNo: formData.value.quoteNo,
          remark: formData.value.remark,
          status: formData.value.status,
          totalAmount: formData.value.totalAmount,
          validUntil: formData.value.validUntil,
        });
        // 列表行不携带明细，编辑时拉详情回填
        getQuoteDetail(formData.value.id).then((detail) => {
          items.value = (detail.items ?? []).map((i) => ({ ...i }));
          if (items.value.length === 0) {
            items.value = [newItem()];
          }
        });
      } else {
        formApi.setValues({ status: 0 });
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
  <Drawer :title="getTitle" class="w-[780px]">
    <Form />
    <div class="mb-2 mt-4 flex items-center justify-between">
      <span class="text-sm font-medium">报价明细</span>
      <div>
        <ElButton size="small" plain type="primary" @click="syncTotal">
          用明细汇总回填总金额
        </ElButton>
        <ElButton size="small" type="primary" @click="addItem">
          添加明细
        </ElButton>
      </div>
    </div>
    <ElTable :data="items" border size="small">
      <ElTableColumn label="产品名称" min-width="150">
        <template #default="{ row }">
          <ElSelect
            v-model="row.productId"
            allow-create
            clearable
            filterable
            placeholder="选择商品或输入新品名"
            size="small"
            @change="(id: number) => onProductSelected($index, Number(id))"
          >
            <ElOption
              v-for="p in productOptions"
              :key="p.id"
              :label="p.name"
              :value="p.id"
            />
          </ElSelect>
          <ElInput
            v-if="!row.productId"
            v-model="row.productName"
            class="mt-1"
            placeholder="或输入产品名称"
            size="small"
          />
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
          <ElButton link type="danger" @click="removeItem($index)">
            删除
          </ElButton>
        </template>
      </ElTableColumn>
    </ElTable>
    <div class="mt-2 text-right text-sm">
      明细汇总：<b>{{ detailSum.toFixed(2) }}</b>
      <span class="ml-2 text-gray-500">（须与上方「总金额」一致）</span>
    </div>
  </Drawer>
</template>
