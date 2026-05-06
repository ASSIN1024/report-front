package com.report.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "job")
public class JobProperties {

    private FtpScanConfig ftpScan = new FtpScanConfig();
    private BatchPackagingConfig batchPackaging = new BatchPackagingConfig();

    @Data
    public static class FtpScanConfig {
        private boolean enabled = true;
        private int intervalMinutes = 5;
    }

    @Data
    public static class BatchPackagingConfig {
        private boolean enabled = true;
        private int intervalMinutes = 1;
        private long maxPackageSize = 209715200L; // 单包大小上限(字节),默认200MB
    }
}
