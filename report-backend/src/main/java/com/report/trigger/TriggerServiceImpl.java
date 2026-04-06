package com.report.trigger;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.report.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class TriggerServiceImpl implements TriggerService {

    @Autowired
    private TriggerConfigMapper triggerConfigMapper;

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
    public int checkDataExists(TriggerConfig config, LocalDate partitionDate) {
        String sql = String.format(
            "SELECT COUNT(*) FROM %s WHERE %s = '%s'",
            config.getSourceTable(),
            config.getPartitionColumn(),
            partitionDate
        );
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    @Override
    public void updateLastTriggerTime(String triggerCode) {
        TriggerConfig config = getByCode(triggerCode);
        if (config != null) {
            config.setLastTriggerTime(java.time.LocalDateTime.now());
            triggerConfigMapper.updateById(config);
        }
    }
}