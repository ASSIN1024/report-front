package com.report.util;

import com.report.entity.FtpConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FtpUtil {

    private FtpUtil() {}

    public static FTPClient connect(FtpConfig config) throws IOException {
        FTPClient ftpClient = new FTPClient();
        ftpClient.setConnectTimeout(10000);
        ftpClient.setDefaultTimeout(10000);
        ftpClient.setDataTimeout(10000);
        ftpClient.connect(config.getHost(), config.getPort());
        ftpClient.login(config.getUsername(), config.getPassword());
        ftpClient.enterLocalPassiveMode();
        ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
        return ftpClient;
    }

    public static void disconnect(FTPClient ftpClient) {
        if (ftpClient != null && ftpClient.isConnected()) {
            try {
                ftpClient.logout();
                ftpClient.disconnect();
            } catch (IOException e) {
                log.error("FTP断开连接失败: {}", e.getMessage(), e);
            }
        }
    }

    public static boolean testConnection(FtpConfig config) {
        FTPClient ftpClient = null;
        try {
            ftpClient = connect(config);
            return ftpClient.isConnected();
        } catch (IOException e) {
            log.error("FTP连接测试失败: {}", e.getMessage(), e);
            return false;
        } finally {
            disconnect(ftpClient);
        }
    }

    public static List<String> listFiles(FtpConfig config, String path, String pattern) throws IOException {
        FTPClient ftpClient = null;
        List<String> files = new ArrayList<>();
        try {
            ftpClient = connect(config);
            String scanPath = path != null ? path : config.getScanPath();
            FTPFile[] ftpFiles = ftpClient.listFiles(scanPath);
            for (FTPFile file : ftpFiles) {
                if (file.isFile()) {
                    String fileName = file.getName();
                    if (pattern != null && !fileName.matches(pattern.replace("*", ".*"))) {
                        continue;
                    }
                    files.add(scanPath + "/" + fileName);
                }
            }
            return files;
        } finally {
            disconnect(ftpClient);
        }
    }

    public static byte[] downloadFile(FtpConfig config, String remotePath) throws IOException {
        FTPClient ftpClient = null;
        InputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;
        try {
            ftpClient = connect(config);
            inputStream = ftpClient.retrieveFileStream(remotePath);
            if (inputStream == null) {
                throw new IOException("文件不存在: " + remotePath);
            }
            outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            disconnect(ftpClient);
        }
    }

    public static boolean deleteFile(FtpConfig config, String remotePath) throws IOException {
        FTPClient ftpClient = null;
        try {
            ftpClient = connect(config);
            return ftpClient.deleteFile(remotePath);
        } finally {
            disconnect(ftpClient);
        }
    }

    public static boolean renameFile(FtpConfig config, String from, String to) throws IOException {
        FTPClient ftpClient = null;
        try {
            ftpClient = connect(config);
            return ftpClient.rename(from, to);
        } finally {
            disconnect(ftpClient);
        }
    }

    public static List<String> listFiles(FTPClient ftpClient, String path, String pattern) throws IOException {
        List<String> files = new ArrayList<>();
        FTPFile[] ftpFiles = ftpClient.listFiles(path);
        for (FTPFile file : ftpFiles) {
            if (file.isFile()) {
                String fileName = file.getName();
                if (pattern != null && !fileName.matches(pattern.replace("*", ".*"))) {
                    continue;
                }
                files.add(path + "/" + fileName);
            }
        }
        return files;
    }

    public static byte[] downloadFile(FTPClient ftpClient, String remotePath) throws IOException {
        InputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;
        try {
            inputStream = ftpClient.retrieveFileStream(remotePath);
            if (inputStream == null) {
                throw new IOException("文件不存在: " + remotePath);
            }
            outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            ftpClient.completePendingCommand();
        }
    }
}
