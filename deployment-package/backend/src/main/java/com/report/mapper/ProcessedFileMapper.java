package com.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.entity.ProcessedFile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 已处理文件Mapper接口
 */
@Mapper
public interface ProcessedFileMapper extends BaseMapper<ProcessedFile> {
}
