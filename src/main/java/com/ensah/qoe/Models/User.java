package com.ensah.qoe.Models;

public class User {
    private int idUser;
    private String username;
    private String password;
    private String email;
    private String role;
    private String dateCreation;
    private String resetToken;

    // Constructeur par défaut
    public User() {
        this.idUser = 0;
        this.username = "";
        this.password = "";
        this.email = "";
        this.role = "";
        this.dateCreation = "";
        this.resetToken = null;
    }

    // Constructeur avec tous les champs
    public User(int idUser, String username, String password, String email,
                String role, String dateCreation, String resetToken) {
        this.idUser = idUser;
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
        this.dateCreation = dateCreation;
        this.resetToken = resetToken;
    }

    // Getters et Setters
    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }

    // Méthodes de compatibilité (pour les autres contrôleurs)
    public int getId() { return idUser; }
    public void setId(int id) { this.idUser = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getDateCreation() { return dateCreation; }
    public void setDateCreation(String dateCreation) { this.dateCreation = dateCreation; }

    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }

    @Override
    public String toString() {
        return "User{" +
                "idUser=" + idUser +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", dateCreation='" + dateCreation + '\'' +
                '}';
    }
}