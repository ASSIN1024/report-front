package com.report.trigger;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.report.entity.TriggerStateRecord;
import com.report.mapper.TriggerStateRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

@Slf4j
@Service
public class DatabaseTriggerStateManager implements TriggerStateManager {

    @Autowired
    private TriggerStateRecordMapper triggerStateRecordMapper;

    @Value("${spring.application.name:report-backend}")
    private String applicationName;

    private String instanceId;

    public DatabaseTriggerStateManager() {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            String pid = java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
            this.instanceId = hostname + "-" + pid;
        } catch (UnknownHostException e) {
            this.instanceId = "unknown-" + System.currentTimeMillis();
        }
    }

    @Override
    @Transactional
    public TriggerState getOrCreate(String triggerCode) {
        LambdaQueryWrapper<TriggerStateRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TriggerStateRecord::getTriggerCode, triggerCode);

        TriggerStateRecord record = triggerStateRecordMapper.selectOne(wrapper);

        if (record == null) {
            record = new TriggerStateRecord();
            record.setTriggerCode(triggerCode);
            record.setRetryCount(0);
            record.setTriggered(false);
            record.setInstanceId(instanceId);
            record.setCreateTime(new Date());
            record.setUpdateTime(new Date());
            record.setVersion(0);
            try {
                triggerStateRecordMapper.insert(record);
            } catch (Exception e) {
                log.warn("并发插入触发器状态记录，尝试重新查询: {}", triggerCode);
                record = triggerStateRecordMapper.selectOne(wrapper);
                if (record == null) {
                    throw new RuntimeException("无法创建触发器状态记录: " + triggerCode);
                }
            }
        }

        return convertToState(record);
    }

    @Override
    @Transactional
    public void reset(String triggerCode) {
        int updated = triggerStateRecordMapper.resetTriggered(triggerCode);
        log.debug("重置触发器状态: triggerCode={}, updated={}", triggerCode, updated);
    }

    @Override
    @Transactional
    public void incrementRetryCount(String triggerCode) {
        TriggerStateRecord record = getRecordByCode(triggerCode);
        if (record != null) {
            record.setRetryCount(record.getRetryCount() + 1);
            record.setLastCheckTime(new Date());
            record.setUpdateTime(new Date());
            triggerStateRecordMapper.updateById(record);
        }
    }

    @Override
    @Transactional
    public void setTriggered(String triggerCode, boolean triggered) {
        TriggerStateRecord record = getRecordByCode(triggerCode);
        if (record != null) {
            int updated = triggerStateRecordMapper.updateTriggeredWithVersion(
                triggerCode, triggered, instanceId, record.getVersion()
            );
            if (updated == 0) {
                log.warn("触发器状态更新失败(版本冲突)，可能被其他实例修改: triggerCode={}", triggerCode);
            }
        }
    }

    private TriggerStateRecord getRecordByCode(String triggerCode) {
        LambdaQueryWrapper<TriggerStateRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TriggerStateRecord::getTriggerCode, triggerCode);
        return triggerStateRecordMapper.selectOne(wrapper);
    }

    private TriggerState convertToState(TriggerStateRecord record) {
        TriggerState state = new TriggerState();
        state.setTriggerCode(record.getTriggerCode());
        state.setRetryCount(record.getRetryCount());
        state.setLastCheckTime(record.getLastCheckTime());
        state.setTriggered(record.getTriggered());
        return state;
    }
}
