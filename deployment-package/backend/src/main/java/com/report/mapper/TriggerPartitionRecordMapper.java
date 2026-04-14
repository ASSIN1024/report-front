package com.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.entity.TriggerPartitionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

@Mapper
public interface TriggerPartitionRecordMapper extends BaseMapper<TriggerPartitionRecord> {

    int markTriggering(@Param("triggerCode") String triggerCode,
                       @Param("partitionDate") Date partitionDate,
                       @Param("instanceId") String instanceId);

    int markTriggered(@Param("triggerCode") String triggerCode,
                      @Param("partitionDate") Date partitionDate,
                      @Param("pipelineTaskId") Long pipelineTaskId,
                      @Param("instanceId") String instanceId);

    TriggerPartitionRecord selectByTriggerAndDate(@Param("triggerCode") String triggerCode,
                                                 @Param("partitionDate") Date partitionDate);
}
