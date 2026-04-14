package com.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.entity.ods.OsdCompanyDeposit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface OsdCompanyDepositMapper extends BaseMapper<OsdCompanyDeposit> {
    @Select("SELECT * FROM osd_company_deposit WHERE pt_dt = #{ptDt}")
    List<OsdCompanyDeposit> selectByPtDt(java.sql.Date ptDt);
}