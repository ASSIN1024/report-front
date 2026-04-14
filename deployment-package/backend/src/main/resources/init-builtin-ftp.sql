-- 内置FTP配置初始化数据
INSERT INTO built_in_ftp_config (id, enabled, port, username, password, root_directory, max_connections, idle_timeout, passive_mode, passive_port_start, passive_port_end)
VALUES (1, 0, 2021, 'rpa_user', 'rpa_password', '/data/ftp-root', 10, 300, 1, 50000, 50100)
ON DUPLICATE KEY UPDATE id = id;