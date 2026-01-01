package com.ncwu.iotdevice.service.impl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class VulnerableCode {
    public void getUserData(String userId) {
        try {
            // 1. 硬编码凭据 (Hardcoded Credentials)
            // CodeQL 会识别出在代码中直接写死密码的行为
            String password = "super_secret_password_123";
            
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "admin", password);
            Statement statement = conn.createStatement();

            // 2. SQL 注入漏洞 (SQL Injection)
            // 直接拼接字符串而不是使用 PreparedStatement，
            // 攻击者可以通过 userId 参数 (如: '1' OR '1'='1') 获取所有用户数据
            String query = "SELECT * FROM users WHERE id = '" + userId + "'";
            ResultSet rs = statement.executeQuery(query);

            while (rs.next()) {
                System.out.println("User: " + rs.getString("name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}