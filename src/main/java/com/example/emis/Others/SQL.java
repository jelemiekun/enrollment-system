package com.example.emis.Others;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

import static com.example.emis.Enums.SQLRolesEnum.*;
import static com.example.emis.Others.Alerts.*;

public class SQL {
    private static final String key = "yrGrat07BQbk1OISHESH9mHjp8oCPmGx";
    private static HikariConfig config = new HikariConfig();
    private static HikariDataSource dataSource;
    private static String url = "jdbc:mysql://localhost:3306/enrollment_system";
    private static String user = "root";
    private static String password = "admin";

    public static void SQLCreateConnection() {
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.addDataSourceProperty( "cachePrepStmts" , "true" );
        config.addDataSourceProperty( "prepStmtCacheSize" , "250" );
        config.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );
        config.setMaximumPoolSize(20);
        config.setConnectionTimeout(30000); // 30 seconds
        config.setIdleTimeout(600000); // 10 minutes
        config.setMaxLifetime(1800000); // 30 minutes
        dataSource = new HikariDataSource(config);
    }

    public static String encryptString(String input) {
        // encrypted password for admin = NGtfAJPLaJACyajHXvBM6A==
        try {
            SecretKey secretKey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(input.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            alertUnexpectedError();
            return null;
        }
    }

    public static String decryptString(String encryptedInput) {
        try {
            SecretKey secretKey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedInput));
            return new String(decryptedBytes);
        } catch (Exception e) {
            alertUnexpectedError();
            return null;
        }
    }


    public static boolean SQLLogin(String email, String password) {
        String query = "SELECT password FROM login_register WHERE email = ?";

        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, email);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    String decryptPassword = decryptString(resultSet.getString("password"));
                    return password.equals(decryptPassword);
                }
            }
        } catch (SQLException e) {
            alertNoConnection();
        }
        return false;
    }

    public static String SQLWhatRole(String email) {
        String query = "SELECT role FROM login_register WHERE email = ?";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, email);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next())
                    return resultSet.getString("role");
            }
        } catch (SQLException e) {
            alertNoConnection();
            throw new RuntimeException(e);
        }
        return null;
    }

    public static boolean SQLIsEmailDuplicate(String email) {
        String query = "SELECT email FROM login_register where EMAIL = ?";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, email);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            alertNoConnection();
            throw new RuntimeException(e);
        }
    }

    public static boolean SQLRegisterStudent(String email, String password) {
        boolean studentInRegisterSuccess = SQLRegisterStudentInLoginRegister(email, password);
        boolean studentInStudentSuccess = SQLRegisterStudentInStudent(email, password);

        return studentInRegisterSuccess && studentInStudentSuccess;
    }

    private static boolean SQLRegisterStudentInLoginRegister(String email, String password) {
        String query = "INSERT INTO login_register(email, password, role) values (?, ?, ?);";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, email);
            preparedStatement.setString( 2, encryptString(password));
            preparedStatement.setString(3, STUDENT_ROLE_ENUM.getRole());

            preparedStatement.executeUpdate();
            return true;
        } catch (SQLException e) {
            alertNoConnection();
            throw new RuntimeException(e);
        }
    }

    private static boolean SQLRegisterStudentInStudent(String email, String password) {
        String query = "INSERT INTO student(email, password) values (?, ?);";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, email);
            preparedStatement.setString(2, password);

            preparedStatement.executeUpdate();
            return true;
        } catch (SQLException e) {
            alertNoConnection();
            throw new RuntimeException(e);
        }
    }
}
