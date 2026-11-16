package cn.easykits.crm.mapper;

import cn.easykits.crm.entity.Campaign;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 营销活动 Mapper。
 * <p>活动效果聚合（线索数 / 转化数 / 商机金额 / 赢单金额）全部在 SQL 侧 GROUP BY 完成，
 * 不把线索、商机明细拉回内存；参数一律 #{} 绑定。
 * <p>crm_lead / crm_opportunity 已登记行级数据权限，非「全部」范围用户看到的活动效果
 * 只统计其可见线索，口径与列表页一致。
 */
@Mapper
public interface CampaignMapper extends BaseMapper<Campaign> {

    /** 线索数与转化数（按活动分组）。 */
    @Select("""
            <script>
            SELECT l.campaign_id AS campaignId,
                   COUNT(*) AS leadCount,
                   SUM(CASE WHEN l.converted_customer_id IS NOT NULL THEN 1 ELSE 0 END) AS convertedCount
            FROM crm_lead l
            WHERE l.deleted = 0
              AND l.campaign_id IN
              <foreach collection="campaignIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            GROUP BY l.campaign_id
            </script>
            """)
    List<Map<String, Object>> leadStats(@Param("campaignIds") List<Long> campaignIds);

    /** 商机金额与赢单金额（按活动分组；赢单以阶段配置 is_won=1 为准）。 */
    @Select("""
            <script>
            SELECT l.campaign_id AS campaignId,
                   COALESCE(SUM(o.amount), 0) AS pipelineAmount,
                   COALESCE(SUM(CASE WHEN s.is_won = 1 THEN o.actual_amount ELSE 0 END), 0) AS wonAmount
            FROM crm_lead l
            JOIN crm_opportunity o ON o.lead_id = l.id AND o.deleted = 0
            LEFT JOIN crm_opportunity_stage s ON s.stage_code = o.stage_code AND s.deleted = 0
            WHERE l.deleted = 0
              AND l.campaign_id IN
              <foreach collection="campaignIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            GROUP BY l.campaign_id
            </script>
            """)
    List<Map<String, Object>> opportunityStats(@Param("campaignIds") List<Long> campaignIds);

    /** 下拉选项：只返回未删除活动的 id / name / status。 */
    @Select("""
            SELECT id, code, name, status
            FROM crm_campaign
            WHERE deleted = 0
            ORDER BY status ASC, id DESC
            """)
    List<Map<String, Object>> options();
}
