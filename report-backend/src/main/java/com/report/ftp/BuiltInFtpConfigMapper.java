package com.report.ftp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BuiltInFtpConfigMapper extends BaseMapper<BuiltInFtpConfig> {

    @Select("SELECT * FROM built_in_ftp_config WHERE id = 1")
    BuiltInFtpConfig getConfig();
}