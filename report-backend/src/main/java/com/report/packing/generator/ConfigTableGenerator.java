package com.report.packing.generator;

import java.io.File;
import java.util.List;

public interface ConfigTableGenerator {
    File generate(List<Long> processedFileIds, String batchNo);
}