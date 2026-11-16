<script lang="ts" setup>
import type { ProductApi } from '#/api/crm/product';

import { ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import {
  ElDescriptions,
  ElDescriptionsItem,
  ElTable,
  ElTableColumn,
} from 'element-plus';

import { getQuoteDetail } from '#/api/crm/product';

const quote = ref<null | ProductApi.Quote>(null);

const [Drawer, drawerApi] = useVbenDrawer({
  onOpenChange(isOpen) {
    if (!isOpen) {
      return;
    }
    const data = drawerApi.getData<ProductApi.Quote>();
    quote.value = data ?? null;
    if (data?.id) {
      // 列表行不带明细，打开时拉详情
      getQuoteDetail(data.id).then((detail) => {
        quote.value = detail;
      });
    }
  },
});
</script>

<template>
  <Drawer title="报价单详情" class="w-[720px]">
    <ElDescriptions v-if="quote" :column="2" border size="small">
      <ElDescriptionsItem label="报价单编号">
        {{ quote.quoteNo }}
      </ElDescriptionsItem>
      <ElDescriptionsItem label="客户">
        {{ quote.customerName ?? quote.customerId }}
      </ElDescriptionsItem>
      <ElDescriptionsItem label="状态">
        {{ quote.statusName }}
      </ElDescriptionsItem>
      <ElDescriptionsItem label="总金额">
        {{ quote.totalAmount }}
      </ElDescriptionsItem>
      <ElDescriptionsItem label="有效期至">
        {{ quote.validUntil ?? '-' }}
      </ElDescriptionsItem>
      <ElDescriptionsItem label="关联商机">
        {{ quote.opportunityId ?? '-' }}
      </ElDescriptionsItem>
      <ElDescriptionsItem label="备注" :span="2">
        {{ quote.remark ?? '-' }}
      </ElDescriptionsItem>
    </ElDescriptions>

    <div class="mb-2 mt-4 text-sm font-medium">报价明细</div>
    <ElTable :data="quote?.items ?? []" border size="small">
      <ElTableColumn prop="productName" label="产品名称" min-width="150" />
      <ElTableColumn prop="unitPrice" label="单价" width="110" />
      <ElTableColumn prop="quantity" label="数量" width="90" />
      <ElTableColumn prop="discount" label="折扣" width="90" />
      <ElTableColumn prop="subtotal" label="小计" width="110" />
    </ElTable>
  </Drawer>
</template>
