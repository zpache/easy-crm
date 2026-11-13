package cn.easykits.crm.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import cn.easykits.crm.common.datascope.DataScopeHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件配置。
 *
 * <p>注意插件顺序（官方建议）：数据权限/多租户类在最前，分页插件必须最后添加，
 * 否则分页 count 语句不会被数据权限改写。
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(DataScopeHandler dataScopeHandler) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 1. 行级数据权限：按 DataScopeTableRegistry 登记的表，向 WHERE 注入 owner_id / dept_id 条件
        interceptor.addInnerInterceptor(new DataPermissionInterceptor(dataScopeHandler));
        // 2. 乐观锁：配合 BaseEntity 的 @Version 字段，updateById 时自动加 version 条件并自增
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        // 3. 分页插件（必须最后添加）
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(200L); // 单页最大条数，防止恶意大分页
        pagination.setOverflow(false);
        interceptor.addInnerInterceptor(pagination);
        // 4. 防全表更新/删除
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }
}
