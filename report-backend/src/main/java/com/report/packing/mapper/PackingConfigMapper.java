package com.report.packing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.packing.entity.PackingConfig;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository("packingConfigMapper")
public interface PackingConfigMapper extends BaseMapper<PackingConfig> {
}