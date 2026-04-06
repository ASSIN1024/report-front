package com.report.trigger;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service("triggerServiceImpl")
public class TriggerServiceImpl implements ITriggerService {

    @Autowired
    private TriggerConfigMapper triggerConfigMapper;

    @Autowired
    private TriggerExecutionLogMapper triggerExecutionLogMapper;

    @Autowired
    private TriggerStateManager stateManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<TriggerConfig> getAllEnabled() {
        return triggerConfigMapper.selectList(
            new LambdaQueryWrapper<TriggerConfig>()
                .eq(TriggerConfig::getStatus, "ENABLED")
        );
    }

    @Override
    public TriggerConfig getByCode(String triggerCode) {
        return triggerConfigMapper.selectOne(
            new LambdaQueryWrapper<TriggerConfig>()
                .eq(TriggerConfig::getTriggerCode, triggerCode)
        );
    }

    @Override
    public int checkDataExists(TriggerConfig config, Date partitionDate) {
        java.sql.Date sqlDate = new java.sql.Date(partitionDate.getTime());
        String sql = String.format(
            "SELECT COUNT(*) FROM %s WHERE %s = '%s'",
            config.getSourceTable(),
            config.getPartitionColumn(),
            new java.sql.Date(partitionDate.getTime())
        );
        log.info("检查数据存在: {}", sql);
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    @Override
    public void updateLastTriggerTime(String triggerCode) {
        TriggerConfig config = getByCode(triggerCode);
        if (config != null) {
            config.setLastTriggerTime(new Date());
            triggerConfigMapper.updateById(config);
        }
    }

    @Override
    public TriggerRealtimeState getRealtimeState(String triggerCode) {
        TriggerConfig config = getByCode(triggerCode);
        if (config == null) {
            return null;
        }

        TriggerState state = stateManager.getOrCreate(triggerCode);
        TriggerRealtimeState realtimeState = new TriggerRealtimeState();
        realtimeState.setTriggerCode(config.getTriggerCode());
        realtimeState.setTriggerName(config.getTriggerName());
        realtimeState.setPipelineCode(config.getPipelineCode());
        realtimeState.setPollIntervalSeconds(config.getPollIntervalSeconds());
        realtimeState.setMaxRetries(config.getMaxRetries());
        realtimeState.setRetryCount(state.getRetryCount());
        realtimeState.setLastCheckTime(state.getLastCheckTime());
        realtimeState.setLastTriggerTime(config.getLastTriggerTime());

        if (state.isTriggered()) {
            realtimeState.setStatus("TRIGGERED");
        } else if (state.getRetryCount() >= config.getMaxRetries()) {
            realtimeState.setStatus("SKIPPED");
        } else {
            realtimeState.setStatus("WAITING");
        }

        return realtimeState;
    }

    @Override
    public List<TriggerRealtimeState> getRealtimeStates() {
        List<TriggerConfig> configs = getAllEnabled();
        return configs.stream()
            .map(config -> getRealtimeState(config.getTriggerCode()))
            .collect(Collectors.toList());
    }

    @Override
    public void logTriggerExecution(TriggerExecutionLog log) {
        triggerExecutionLogMapper.insert(log);
    }

    @Override
    public List<TriggerExecutionLog> getExecutionHistory(String triggerCode, LocalDate partitionDate) {
        LambdaQueryWrapper<TriggerExecutionLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TriggerExecutionLog::getTriggerCode, triggerCode);
        if (partitionDate != null) {
            wrapper.eq(TriggerExecutionLog::getPartitionDate, partitionDate);
        }
        wrapper.orderByDesc(TriggerExecutionLog::getExecutionTime);
        return triggerExecutionLogMapper.selectList(wrapper);
    }
}
