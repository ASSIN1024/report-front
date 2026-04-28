package com.report.packing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.packing.entity.AlertRecord;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository("packingAlertRecordMapper")
public interface AlertRecordMapper extends BaseMapper<AlertRecord> {
}