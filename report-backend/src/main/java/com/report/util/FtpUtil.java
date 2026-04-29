package com.report.util;

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

    public static FTPClient connect(String host, int port, String username, String password) throws IOException {
        FTPClient ftpClient = new FTPClient();
        ftpClient.setConnectTimeout(10000);
        ftpClient.setDefaultTimeout(10000);
        ftpClient.setDataTimeout(10000);
        ftpClient.connect(host, port);
        ftpClient.login(username, password);
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

    public static List<String> listFiles(FTPClient ftpClient, String path, String pattern) throws IOException {
        List<String> files = new ArrayList<>();
        FTPFile[] ftpFiles = ftpClient.listFiles(path);
        for (FTPFile file : ftpFiles) {
            if (file.isFile()) {
                String fileName = file.getName();
                if (pattern != null && !fileName.matches(pattern.replace("*", ".*"))) {
                    continue;
                }
                String filePath = path.endsWith("/") ? path + fileName : path + "/" + fileName;
                files.add(filePath);
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

    public static boolean makeDirectory(FTPClient ftpClient, String path) throws IOException {
        if (path == null || path.isEmpty() || path.equals("/")) {
            return true;
        }
        String[] dirs = path.split("/");
        String currentPath = "";
        for (String dir : dirs) {
            if (dir.isEmpty()) {
                continue;
            }
            currentPath += "/" + dir;
            if (!ftpClient.changeWorkingDirectory(currentPath)) {
                if (!ftpClient.makeDirectory(currentPath)) {
                    log.warn("Failed to create directory: {}", currentPath);
                    return false;
                }
                log.info("Created FTP directory: {}", currentPath);
            }
        }
        return true;
    }
}
