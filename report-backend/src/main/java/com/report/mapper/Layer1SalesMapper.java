package com.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.entity.dwd.Layer1Sales;
import com.report.entity.dws.Layer2Summary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface Layer1SalesMapper extends BaseMapper<Layer1Sales> {

    List<Layer2Summary> aggregateByProduct(@Param("partitionDate") Date partitionDate);
}
