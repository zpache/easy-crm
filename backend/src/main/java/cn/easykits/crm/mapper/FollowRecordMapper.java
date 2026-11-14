package cn.easykits.crm.mapper;

import cn.easykits.crm.entity.FollowRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.easykits.crm.modules.follow.vo.TimelineItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FollowRecordMapper extends BaseMapper<FollowRecord> {

    /**
     * 时间线聚合：跟进记录 + 待办任务 + 商机阶段变更（仅 bizType=opportunity 时纳入）
     * 一条 UNION ALL 查询完成（含分页），全局按时间倒序，避免多表分页合并错位。
     * 行级数据权限：拦截器为 f（crm_follow_record）与 t（crm_task）注入 owner 条件；
     * stage_log 子查询 JOIN crm_opportunity（已注册），同步注入商机 owner 条件，修复越权（审查报告 #4）。
     */
    @Select("""
            <script>
            SELECT * FROM (
                SELECT 'follow'     AS source,
                       f.id         AS id,
                       f.follow_type AS typeCode,
                       NULL         AS fromStage,
                       NULL         AS toStage,
                       f.content    AS content,
                       f.create_time AS time,
                       f.owner_id   AS operatorId
                FROM crm_follow_record f
                WHERE f.deleted = 0 AND f.biz_type = #{bizType} AND f.biz_id = #{bizId}
                UNION ALL
                SELECT 'task'       AS source,
                       t.id         AS id,
                       t.status     AS typeCode,
                       NULL         AS fromStage,
                       NULL         AS toStage,
                       t.title      AS content,
                       t.due_time   AS time,
                       t.owner_id   AS operatorId
                FROM crm_task t
                WHERE t.deleted = 0 AND t.biz_type = #{bizType} AND t.biz_id = #{bizId}
                <if test="bizType == 'opportunity'">
                UNION ALL
                SELECT 'stage_log'  AS source,
                       l.id         AS id,
                       l.to_stage   AS typeCode,
                       l.from_stage AS fromStage,
                       l.to_stage   AS toStage,
                       l.remark     AS content,
                       l.change_time AS time,
                       l.operator_id AS operatorId
                FROM crm_opportunity_stage_log l
                JOIN crm_opportunity o ON l.opportunity_id = o.id AND o.deleted = 0
                WHERE l.opportunity_id = #{bizId}
                </if>
            ) timeline
            ORDER BY time DESC
            LIMIT #{offset}, #{size}
            </script>
            """)
    List<TimelineItemVO> timeline(@Param("bizType") String bizType,
                                  @Param("bizId") Long bizId,
                                  @Param("offset") long offset,
                                  @Param("size") long size);

    /**
     * 时间线总数（同一 UNION 的计数查询，保证分页 total 正确）。
     */
    @Select("""
            <script>
            SELECT COUNT(*) FROM (
                SELECT f.id
                FROM crm_follow_record f
                WHERE f.deleted = 0 AND f.biz_type = #{bizType} AND f.biz_id = #{bizId}
                UNION ALL
                SELECT t.id
                FROM crm_task t
                WHERE t.deleted = 0 AND t.biz_type = #{bizType} AND t.biz_id = #{bizId}
                <if test="bizType == 'opportunity'">
                UNION ALL
                SELECT l.id
                FROM crm_opportunity_stage_log l
                JOIN crm_opportunity o ON l.opportunity_id = o.id AND o.deleted = 0
                WHERE l.opportunity_id = #{bizId}
                </if>
            ) timeline
            </script>
            """)
    long timelineCount(@Param("bizType") String bizType, @Param("bizId") Long bizId);
}
