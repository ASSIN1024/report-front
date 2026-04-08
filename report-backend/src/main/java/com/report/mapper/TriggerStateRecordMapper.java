package com.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.entity.TriggerStateRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TriggerStateRecordMapper extends BaseMapper<TriggerStateRecord> {

    int resetTriggered(@Param("triggerCode") String triggerCode);

    int updateTriggeredWithVersion(
        @Param("triggerCode") String triggerCode,
        @Param("triggered") Boolean triggered,
        @Param("instanceId") String instanceId,
        @Param("version") Integer version
    );
}
