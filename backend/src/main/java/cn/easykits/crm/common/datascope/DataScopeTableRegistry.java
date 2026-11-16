package cn.easykits.crm.common.datascope;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据权限表注册表：登记哪些业务表参与行级数据权限过滤。
 *
 * <p><b>后续接入新表的方式</b>（TODO 随业务模块落地逐张登记）：
 * <ol>
 *   <li>确认表上有 owner_id / dept_id 列（至少其一），列名不同则在
 *       {@link DataScopeTable} 里指定；</li>
 *   <li>在本类构造器里调用 {@link #register}，或改成从
 *       {@code application.yml}（easy-crm.datascope.tables）读取实现动态开关；</li>
 *   <li>无需改动 {@link DataScopeHandler}，拦截器按注册表自动生效。</li>
 * </ol>
 *
 * <p>不登记的表不做任何过滤——系统级表（sys_user、sys_role、sys_dept 等）
 * 由 RBAC 的功能权限控制可见性，不走行级数据权限。
 */
@Component
public class DataScopeTableRegistry {

    private final Map<String, DataScopeTable> tables = new HashMap<>();

    public DataScopeTableRegistry() {
        // 行级数据权限已接入的表（接入新表在此登记，见类注释）
        register(DataScopeTable.of("crm_lead"));     // 线索表：owner_id + dept_id
        register(DataScopeTable.of("crm_customer")); // 客户表：owner_id + dept_id
        register(DataScopeTable.of("crm_opportunity")); // 商机表：owner_id + dept_id（预测聚合 SQL 同样会被注入别名 o 的条件）
        register(DataScopeTable.of("crm_follow_record")); // 跟进记录：owner_id + dept_id
        register(new DataScopeTable("crm_task", "owner_id", null)); // 待办任务：仅 owner_id（跟进记录生成的任务同理）

        // 交易闭环：报价 / 合同 / 订单 / 销售目标
        register(DataScopeTable.of("crm_quote"));
        register(DataScopeTable.of("crm_contract"));
        register(DataScopeTable.of("crm_order"));
        register(DataScopeTable.of("crm_sales_target"));

        // 客户服务：工单 / 满意度
        register(DataScopeTable.of("crm_service_ticket"));
        register(DataScopeTable.of("crm_satisfaction"));

        // 营销域：活动按负责人 / 部门可见
        register(DataScopeTable.of("crm_campaign"));

        // 联系人可见性随客户（crm_contact 无 owner 列，不注册）
        // 阶段配置（crm_opportunity_stage）与提醒（crm_reminder）为全局配置/私有提醒，不注册
        // TODO（轮次 4 跟进记录表建立后）：
        // register(new DataScopeTable("crm_follow_log", "owner_id", null)); // 跟进记录：仅 owner_id
    }

    public void register(DataScopeTable table) {
        tables.put(table.tableName(), table);
    }

    /**
     * 按表名查配置；未登记返回 null（拦截器将跳过该表）。
     */
    public DataScopeTable get(String tableName) {
        return tableName == null ? null : tables.get(tableName.toLowerCase());
    }

    public Map<String, DataScopeTable> all() {
        return Collections.unmodifiableMap(tables);
    }
}
