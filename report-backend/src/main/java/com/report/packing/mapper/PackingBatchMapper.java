package com.report.packing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.packing.entity.PackingBatch;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository("packingBatchMapper")
public interface PackingBatchMapper extends BaseMapper<PackingBatch> {
}