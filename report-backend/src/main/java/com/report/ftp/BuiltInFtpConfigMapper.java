package com.report.ftp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BuiltInFtpConfigMapper extends BaseMapper<BuiltInFtpConfig> {

    BuiltInFtpConfig getConfig();
}