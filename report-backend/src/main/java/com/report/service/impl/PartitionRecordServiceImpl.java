package com.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.report.entity.TriggerPartitionRecord;
import com.report.mapper.TriggerPartitionRecordMapper;
import com.report.service.PartitionRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

@Slf4j
@Service
public class PartitionRecordServiceImpl implements PartitionRecordService {

    @Autowired
    private TriggerPartitionRecordMapper partitionRecordMapper;

    private String instanceId;

    public PartitionRecordServiceImpl() {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            String pid = java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
            this.instanceId = hostname + "-" + pid;
        } catch (UnknownHostException e) {
            this.instanceId = "unknown-" + System.currentTimeMillis();
        }
    }

    @Override
    public boolean isPartitionTriggered(String triggerCode, Date partitionDate) {
        TriggerPartitionRecord record = partitionRecordMapper.selectByTriggerAndDate(triggerCode, partitionDate);
        if (record == null) {
            return false;
        }

        if ("TRIGGERED".equals(record.getStatus())) {
            log.debug("[{}] 分区 {} 已触发(TRIGGERED)，跳过", triggerCode, partitionDate);
            return true;
        }

        if ("TRIGGERING".equals(record.getStatus())) {
            if (instanceId.equals(record.getInstanceId())) {
                log.debug("[{}] 分区 {} 正在触发(TRIGGERING)且属于本实例，继续", triggerCode, partitionDate);
                return false;
            } else {
                log.debug("[{}] 分区 {} 正在触发(TRIGGERING)且属于其他实例，跳过", triggerCode, partitionDate);
                return true;
            }
        }

        return false;
    }

    @Override
    @Transactional
    public boolean markPartitionTriggering(String triggerCode, Date partitionDate) {
        TriggerPartitionRecord existing = partitionRecordMapper.selectByTriggerAndDate(triggerCode, partitionDate);

        if (existing == null) {
            TriggerPartitionRecord record = new TriggerPartitionRecord();
            record.setTriggerCode(triggerCode);
            record.setPartitionDate(partitionDate);
            record.setStatus("TRIGGERING");
            record.setTriggered(false);
            record.setInstanceId(instanceId);
            record.setCreateTime(new Date());
            record.setUpdateTime(new Date());
            record.setVersion(0);

            try {
                partitionRecordMapper.insert(record);
                log.debug("[{}] 分区 {} 标记为 TRIGGERING", triggerCode, partitionDate);
                return true;
            } catch (Exception e) {
                log.debug("[{}] 分区 {} 并发插入失败: {}", triggerCode, partitionDate, e.getMessage());
                return false;
            }
        }

        if ("TRIGGERED".equals(existing.getStatus())) {
            log.debug("[{}] 分区 {} 已触发(TRIGGERED)，跳过", triggerCode, partitionDate);
            return false;
        }

        if ("TRIGGERING".equals(existing.getStatus())) {
            if (instanceId.equals(existing.getInstanceId())) {
                log.debug("[{}] 分区 {} 已被本实例标记为TRIGGERING，继续", triggerCode, partitionDate);
                return true;
            }
            log.debug("[{}] 分区 {} 已被其他实例标记为TRIGGERING，跳过", triggerCode, partitionDate);
            return false;
        }

        existing.setStatus("TRIGGERING");
        existing.setInstanceId(instanceId);
        existing.setUpdateTime(new Date());
        int updated = partitionRecordMapper.updateById(existing);
        if (updated > 0) {
            log.debug("[{}] 分区 {} 更新为 TRIGGERING", triggerCode, partitionDate);
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public void markPartitionTriggered(String triggerCode, Date partitionDate, Long pipelineTaskId) {
        TriggerPartitionRecord record = partitionRecordMapper.selectByTriggerAndDate(triggerCode, partitionDate);
        if (record != null) {
            record.setTriggered(true);
            record.setStatus("TRIGGERED");
            record.setPipelineTaskId(pipelineTaskId);
            record.setTriggerTime(new Date());
            record.setInstanceId(instanceId);
            record.setUpdateTime(new Date());
            partitionRecordMapper.updateById(record);
            log.info("[{}] 分区 {} 标记为 TRIGGERED", triggerCode, partitionDate);
        }
    }

    @Override
    public Object getPartitionRecord(String triggerCode, Date partitionDate) {
        return partitionRecordMapper.selectByTriggerAndDate(triggerCode, partitionDate);
    }
}
