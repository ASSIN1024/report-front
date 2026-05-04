package com.report.test;

import com.report.common.enums.ProcessStep;
import com.report.entity.ProcessMonitorLog;
import com.report.ftp.BuiltInFtpConfig;
import com.report.ftp.BuiltInFtpConfigMapper;
import com.report.service.MonitorService;
import com.report.service.FtpDirectoryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class FtpUploadE2eTest {

    private static final String TEST_REPORT_CODE = "TEST_E2E_001";
    private static final String TEST_SCAN_PATH = "/upload/test";
    private static final String TEST_FILE_NAME = "test120260429.xlsx";

    @Autowired
    private BuiltInFtpConfigMapper builtInFtpConfigMapper;

    @Autowired
    private FtpDirectoryService ftpDirectoryService;

    @Autowired
    private MonitorService monitorService;

    private BuiltInFtpConfig ftpConfig;

    @Before
    public void setUp() {
        ftpConfig = builtInFtpConfigMapper.getConfig();
        assertNotNull("FTP配置不应为空", ftpConfig);
        assertTrue("FTP服务应已启用", ftpConfig.getEnabled());
    }

    @Test
    public void testFtpDirectoryCreation() {
        log.info("[E2E] ========== 测试FTP目录创建 ==========");

        Long monitorId = ftpDirectoryService.getOrCreateMonitorId(TEST_REPORT_CODE, TEST_SCAN_PATH);
        assertNotNull("监控ID不应为空", monitorId);

        ProcessMonitorLog monitorLog = monitorService.getMonitorHistory(TEST_REPORT_CODE).stream()
                .filter(log -> monitorId.equals(log.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull("监控日志不应为空", monitorLog);
        assertEquals("步骤应为DIR_CREATED", ProcessStep.DIR_CREATED.name(), monitorLog.getStep());
        assertEquals("状态应为SUCCESS", ProcessMonitorLog.STATUS_SUCCESS, monitorLog.getStatus());

        assertTrue("目录应该存在", ftpDirectoryService.directoryExists(TEST_SCAN_PATH));

        log.info("[E2E] FTP目录创建测试通过");
    }

    @Test
    public void testFtpFileUpload() throws IOException {
        log.info("[E2E] ========== 测试FTP文件上传 ==========");

        File testFile = new File("src/main/resources/test120260429.xlsx");
        if (!testFile.exists()) {
            log.warn("[E2E] 测试文件不存在，跳过上传测试: {}", testFile.getAbsolutePath());
            return;
        }

        Long monitorId = monitorService.startMonitor(TEST_REPORT_CODE, TEST_FILE_NAME, ProcessStep.FILE_UPLOADING.name());

        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect("localhost", ftpConfig.getPort());
            ftpClient.login(ftpConfig.getUsername(), ftpConfig.getPassword());
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);

            String targetPath = TEST_SCAN_PATH + "/" + TEST_FILE_NAME;
            try (FileInputStream fis = new FileInputStream(testFile)) {
                boolean uploaded = ftpClient.storeFile(targetPath, fis);
                assertTrue("文件上传应成功", uploaded);
                log.info("[E2E] 文件上传成功: {}", targetPath);
            }

            monitorService.updateMonitor(monitorId, ProcessStep.FILE_UPLOADED.name(),
                    ProcessMonitorLog.STATUS_SUCCESS, "文件上传成功: " + targetPath);

            boolean fileExists = ftpDirectoryService.directoryExists(TEST_SCAN_PATH + "/" + TEST_FILE_NAME);
            log.info("[E2E] 文件在FTP目录中是否存在: {}", fileExists);

        } finally {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
            monitorService.endMonitor(monitorId, ProcessMonitorLog.STATUS_SUCCESS, "FTP操作完成");
        }

        log.info("[E2E] FTP文件上传测试通过");
    }

    @Test
    public void testEndToEndFlow() throws IOException {
        log.info("[E2E] ========== 端到端流程测试 ==========");

        String scanPath = "/upload/e2e_test";
        String fileName = "e2e_test_file.txt";

        Long dirMonitorId = ftpDirectoryService.getOrCreateMonitorId(TEST_REPORT_CODE, scanPath);
        log.info("[E2E] 目录创建监控ID: {}", dirMonitorId);
        assertTrue("目录应创建成功", ftpDirectoryService.directoryExists(scanPath));

        Long uploadMonitorId = monitorService.startMonitor(TEST_REPORT_CODE, fileName, ProcessStep.FILE_UPLOADING.name());

        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect("localhost", ftpConfig.getPort());
            ftpClient.login(ftpConfig.getUsername(), ftpConfig.getPassword());
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);

            File tempFile = new File("/tmp/" + fileName);
            tempFile.createNewFile();

            try (FileInputStream fis = new FileInputStream(tempFile)) {
                String targetPath = scanPath + "/" + fileName;
                boolean uploaded = ftpClient.storeFile(targetPath, fis);
                assertTrue("文件上传应成功", uploaded);
                log.info("[E2E] 文件上传成功: {}", targetPath);
            }

            monitorService.updateMonitor(uploadMonitorId, ProcessStep.FILE_UPLOADED.name(),
                    ProcessMonitorLog.STATUS_SUCCESS, "文件上传成功");

        } finally {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
            monitorService.endMonitor(uploadMonitorId, ProcessMonitorLog.STATUS_SUCCESS, "上传流程完成");
        }

        List<ProcessMonitorLog> history = monitorService.getMonitorHistory(TEST_REPORT_CODE);
        log.info("[E2E] 监控历史记录数: {}", history.size());
        history.forEach(m ->
                log.info("[E2E] 监控记录 - step: {}, status: {}, message: {}",
                        m.getStep(), m.getStatus(), m.getMessage())
        );

        assertFalse("应有监控记录", history.isEmpty());

        log.info("[E2E] 端到端流程测试通过");
    }
}