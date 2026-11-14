package cn.easykits.crm.mapper;

import cn.easykits.crm.entity.Satisfaction;
import cn.easykits.crm.modules.support.vo.ScoreCountVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SatisfactionMapper extends BaseMapper<Satisfaction> {

    /**
     * 评分分布聚合（1-5 分各多少条），供满意度统计页使用。
     * 自定义 SQL 参数一律用 #{}；行级数据权限由拦截器按 crm_satisfaction 登记表注入。
     * 总数 / 平均分在 Service 层由分布结果汇总，避免第二条聚合 SQL。
     */
    @Select("""
            <script>
            SELECT score AS score, COUNT(*) AS cnt
            FROM crm_satisfaction
            WHERE deleted = 0
            <if test="startDate != null"> AND evaluate_time &gt;= #{startDate}</if>
            <if test="endDate != null"> AND evaluate_time &lt;= #{endDate}</if>
            GROUP BY score
            ORDER BY score
            </script>
            """)
    List<ScoreCountVO> selectScoreDistribution(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);
}
