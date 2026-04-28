package com.report.service;

public interface ExcelTransformService {
    TransformResult transform(String filePath, Long reportConfigId);
}
