package com.ensah.qoe.Models;
import com.ensah.qoe.Models.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private Connection connection;

    public UserDAO(Connection connection) {
        this.connection = connection;
    }

    // Récupérer tous les utilisateurs
    public List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM utilisateurs ORDER BY ID_USER";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                User user = new User();
                user.setIdUser(rs.getInt("ID_USER"));
                user.setUsername(rs.getString("USERNAME"));
                user.setPassword(rs.getString("PASSWORD"));
                user.setEmail(rs.getString("EMAIL"));
                user.setRole(rs.getString("ROLE"));
                user.setDateCreation(rs.getString("DATE_CREATION"));
                user.setResetToken(rs.getString("RESET_TOKEN"));

                users.add(user);
            }
        }
        return users;
    }

    // Ajouter un nouvel utilisateur
    public boolean addUser(User user) throws SQLException {
        String query = "INSERT INTO utilisateurs (USERNAME, PASSWORD, EMAIL, ROLE, DATE_CREATION) VALUES (?, ?, ?, ?, NOW())";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getRole());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    // Mettre à jour un utilisateur
    public boolean updateUser(User user) throws SQLException {
        String query = "UPDATE utilisateurs SET USERNAME = ?, EMAIL = ?, ROLE = ? WHERE ID_USER = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getRole());
            pstmt.setInt(4, user.getIdUser());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    // Supprimer un utilisateur
    public boolean deleteUser(int userId) throws SQLException {
        String query = "DELETE FROM utilisateurs WHERE ID_USER = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, userId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    // Rechercher des utilisateurs
    public List<User> searchUsers(String keyword) throws SQLException {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM utilisateurs WHERE USERNAME LIKE ? OR EMAIL LIKE ? OR ROLE LIKE ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            String searchPattern = "%" + keyword + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    User user = new User();
                    user.setIdUser(rs.getInt("ID_USER"));
                    user.setUsername(rs.getString("USERNAME"));
                    user.setPassword(rs.getString("PASSWORD"));
                    user.setEmail(rs.getString("EMAIL"));
                    user.setRole(rs.getString("ROLE"));
                    user.setDateCreation(rs.getString("DATE_CREATION"));
                    user.setResetToken(rs.getString("RESET_TOKEN"));

                    users.add(user);
                }
            }
        }
        return users;
    }

    // Vérifier si un username existe déjà
    public boolean usernameExists(String username) throws SQLException {
        String query = "SELECT COUNT(*) FROM utilisateurs WHERE USERNAME = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    // Vérifier si un email existe déjà
    public boolean emailExists(String email) throws SQLException {
        String query = "SELECT COUNT(*) FROM utilisateurs WHERE EMAIL = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, email);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }
}