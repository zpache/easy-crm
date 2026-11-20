<script lang="ts" setup>
import type { ContractApi } from '#/api/crm/contract';

import { computed, onMounted, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { createContract, updateContract } from '#/api/crm/contract';

import { loadCustomerOptions, loadOpportunityOptions, useFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | ContractApi.Contract>(null);

const getTitle = computed(() =>
  formData.value?.id ? '编辑合同' : '新增合同',
);

const [Form, formApi] = useVbenForm({
  handleValuesChange: async (values: Record<string, any>) => {
    // 客户变更 → 级联加载该客户的商机候选
    await loadOpportunityOptions(values.customerId);
  },
  layout: 'vertical',
  schema: useFormSchema(opportunityOptions),
  showDefaultActions: false,
});

const [Drawer, drawerApi] = useVbenDrawer({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    const values = await formApi.getValues();
    drawerApi.lock();
    const payload: ContractApi.ContractPayload = {
      amount: values.amount,
      contractNo: values.contractNo || undefined,
      customerId: values.customerId,
      opportunityId: values.opportunityId ?? undefined,
      remark: values.remark || undefined,
      signDate: values.signDate || undefined,
      status: values.status ?? 0,
    };
    try {
      if (formData.value?.id) {
        await updateContract(formData.value.id, payload);
      } else {
        await createContract(payload);
      }
      emit('success');
      drawerApi.close();
    } finally {
      drawerApi.lock(false);
    }
  },
  onOpenChange(isOpen) {
    if (isOpen) {
      const data = drawerApi.getData<ContractApi.Contract>();
      formData.value = data && data.id ? data : null;
      formApi.resetForm();
      if (formData.value?.id) {
        formApi.setValues({
          amount: formData.value.amount,
          contractNo: formData.value.contractNo,
          customerId: formData.value.customerId,
          opportunityId: formData.value.opportunityId,
          remark: formData.value.remark,
          signDate: formData.value.signDate,
          status: formData.value.status,
        });
      }
    }
  },
});

onMounted(async () => {
  await loadCustomerOptions();
});
</script>

<template>
  <Drawer :title="getTitle" class="w-[520px]">
    <Form />
  </Drawer>
</template>
