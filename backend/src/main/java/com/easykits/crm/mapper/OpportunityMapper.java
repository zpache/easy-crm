package com.easykits.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easykits.crm.entity.Opportunity;
import com.easykits.crm.modules.opportunity.vo.ForecastVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface OpportunityMapper extends BaseMapper<Opportunity> {

    /**
     * 销售预测聚合：按阶段一条 SQL 完成（排除赢单/丢单终态），
     * 预测金额 = SUM(amount × 阶段 default_win_rate / 100)。
     * 行级数据权限拦截器会为 crm_opportunity（别名 o）自动注入 owner_id/dept_id 条件。
     */
    @Select("""
            <script>
            SELECT o.stage_code                                        AS stageCode,
                   s.stage_name                                        AS stageName,
                   COUNT(o.id)                                         AS cnt,
                   COALESCE(SUM(o.amount), 0)                          AS totalAmount,
                   COALESCE(SUM(o.amount * s.default_win_rate / 100), 0) AS forecastAmount
            FROM crm_opportunity o
            JOIN crm_opportunity_stage s ON o.stage_code = s.stage_code AND s.deleted = 0 AND s.enabled = 1
            WHERE o.deleted = 0 AND s.is_won = 0 AND s.is_lost = 0
              <if test="deptId != null"> AND o.dept_id = #{deptId}</if>
              <if test="startDate != null"> AND o.expected_close_date &gt;= #{startDate}</if>
              <if test="endDate != null"> AND o.expected_close_date &lt;= #{endDate}</if>
            GROUP BY o.stage_code, s.stage_name, s.sort_no, s.default_win_rate
            ORDER BY s.sort_no
            </script>
            """)
    List<ForecastVO.StageRow> forecastByStage(@Param("deptId") Long deptId,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);
}
