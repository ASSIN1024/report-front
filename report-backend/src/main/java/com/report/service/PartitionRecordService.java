package com.report.service;

import java.util.Date;

public interface PartitionRecordService {

    boolean isPartitionTriggered(String triggerCode, Date partitionDate);

    boolean markPartitionTriggering(String triggerCode, Date partitionDate);

    void markPartitionTriggered(String triggerCode, Date partitionDate, Long pipelineTaskId);

    Object getPartitionRecord(String triggerCode, Date partitionDate);
}
