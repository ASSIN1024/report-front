package com.report.common.constant;

public class ErrorCode {

    private ErrorCode() {}

    public static final int SUCCESS = 200;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int INTERNAL_ERROR = 500;

    public static final int FTP_CONNECT_ERROR = 1001;
    public static final int FILE_PARSE_ERROR = 1002;
    public static final int DATA_VALIDATE_ERROR = 1003;
    public static final int DB_OPERATE_ERROR = 1004;
}
