package com.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.entity.dwd.DwdCleanTestFlow;
import com.report.entity.dws.DwdTestFlowAgg;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DwdCleanTestFlowMapper extends BaseMapper<DwdCleanTestFlow> {

    List<DwdTestFlowAgg> aggregateByName(@Param("partitionDate") LocalDate partitionDate);
}
