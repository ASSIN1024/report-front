package com.report.trigger;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@Service("triggerServiceImpl")
public class TriggerServiceImpl implements ITriggerService {

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
}
