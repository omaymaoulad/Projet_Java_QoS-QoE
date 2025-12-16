package com.ensah.qoe.Controller;

import com.ensah.qoe.Models.User;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class AdminProfileController implements Initializable {

    // Référence au contrôleur principal
    private MainAdminLayoutController mainController;

    // Données de l'utilisateur
    private User currentUser;

    // Composants du profil
    @FXML
    private Label profileInitial;
    @FXML
    private Label profileNameLabel;
    @FXML
    private Label profileRoleLabel;
    @FXML
    private Button changePhotoBtn;

    // Champs du formulaire
    @FXML
    private TextField fullNameField;
    @FXML
    private TextField usernameField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField phoneField;

    // Champs de sécurité
    @FXML
    private PasswordField currentPasswordField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;

    // Informations du compte
    @FXML
    private Label memberSinceLabel;
    @FXML
    private Label lastLoginLabel;
    @FXML
    private Label activeSessionsLabel;

    // Boutons d'action
    @FXML
    private Button cancelBtn;
    @FXML
    private Button saveBtn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupEventHandlers();
        setupValidation();

        // Si currentUser est déjà défini, charger les données
        if (currentUser != null) {
            loadProfileData();
        }
    }

    // Méthode pour définir l'utilisateur courant
    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (currentUser != null) {
            loadProfileData();
        }
    }

    // Méthode pour définir le contrôleur principal
    public void setMainController(MainAdminLayoutController mainController) {
        this.mainController = mainController;
        if (mainController != null && mainController.getCurrentUser() != null) {
            this.currentUser = mainController.getCurrentUser();
            loadProfileData();
        }
    }

    private void setupEventHandlers() {
        // Bouton changer photo
        changePhotoBtn.setOnAction(event -> handleChangePhoto());

        // Bouton annuler
        cancelBtn.setOnAction(event -> handleCancel());

        // Bouton sauvegarder
        saveBtn.setOnAction(event -> handleSaveProfile());

        // Validation en temps réel
        setupRealTimeValidation();
    }

    private void loadProfileData() {
        System.out.println("🔍 === DEBUG loadProfileData ===");
        System.out.println("currentUser is null? " + (currentUser == null));

        if (currentUser != null) {
            System.out.println("✅ Loading profile for user: " + currentUser.getUsername());
            System.out.println("   - ID: " + currentUser.getIdUser());
            System.out.println("   - Username: " + currentUser.getUsername());
            System.out.println("   - Email: " + currentUser.getEmail());
            System.out.println("   - Date création: " + currentUser.getDateCreation());

            // Initials du profil
            String initials = getInitials(currentUser.getUsername());
            System.out.println("   - Setting initials: " + initials);
            profileInitial.setText(initials);

            // Informations personnelles
            profileNameLabel.setText(currentUser.getUsername());
            profileRoleLabel.setText("Administrateur");

            // Formulaire
            fullNameField.setText(currentUser.getUsername());
            usernameField.setText(currentUser.getUsername());
            emailField.setText(currentUser.getEmail());

            // Informations du compte
            if (currentUser.getDateCreation() != null && !currentUser.getDateCreation().isEmpty()) {
                String formattedDate = formatDateString(currentUser.getDateCreation());
                System.out.println("   - Formatted date: " + formattedDate);
                memberSinceLabel.setText(formattedDate);
            } else {
                System.out.println("⚠️ Date de création non disponible");
                memberSinceLabel.setText("Non spécifié");
            }

            // Dernière connexion
            String lastLogin = "Aujourd'hui, " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            lastLoginLabel.setText(lastLogin);

            // Sessions actives
            int activeSessions = 1;
            activeSessionsLabel.setText(activeSessions + " appareil");

            System.out.println("✅ Profile data loaded successfully");

        } else {
            System.err.println("❌ currentUser is NULL - using default values");
            // Valeurs par défaut
            profileInitial.setText("A");
            profileNameLabel.setText("Administrator");
            profileRoleLabel.setText("Administrateur Système");
            fullNameField.setText("Administrator");
            usernameField.setText("admin");
            emailField.setText("admin@qoe-system.com");
            memberSinceLabel.setText("12 Janvier 2024");
            lastLoginLabel.setText("Aujourd'hui, 14:30");
            activeSessionsLabel.setText("1 appareil");
        }
    }
//```
//
//        ### 6. Test complet
//
//    Après ces modifications, lors de la connexion, vous devriez voir dans la console :
//            ```
//            🔍 Executing query: username=salma
//✅ User found: User{idUser=1, username='salma', email='salma@email.com', role='admin', dateCreation='2024-01-15'}
//   - ID: 1
//            - Username: salma
//   - Email: salma@email.com
//   - Role: admin
//   - Date création: 2024-01-15
//            ✅ User passé au MainAdminLayoutController: salma
//✅ Profile loaded with user: salma
//🔍 === DEBUG loadProfileData ===
//    currentUser is null? false
//            ✅ Loading profile for user: salma
//   - ID: 1
//            - Username: salma
//   - Email: salma@email.com
//   - Date création: 2024-01-15
//            - Setting initials: S
//   - Formatted date: 15 janvier 2024
//            ✅ Profile data loaded successfully

    // Méthode pour récupérer la dernière connexion depuis la base
    private String getLastLoginFromDatabase() {
        // Si vous avez une table de logs, vous pouvez récupérer la dernière connexion ici
        // Pour l'instant, on retourne une valeur par défaut
        return "Aujourd'hui, " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    // Méthode pour compter les sessions actives
    private int getActiveSessionsCount() {
        // Si vous avez un système de gestion de sessions, implémentez cette méthode
        // Pour l'instant, on retourne 1 par défaut
        return 1;
    }

    private void setupValidation() {
        // Validation de l'email
        emailField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                validateEmail();
            }
        });

        // Validation du nom d'utilisateur (unicité)
        usernameField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal && !usernameField.getText().equals(currentUser.getUsername())) {
                checkUsernameAvailability();
            }
        });
    }

    private void setupRealTimeValidation() {
        saveBtn.disableProperty().bind(
                fullNameField.textProperty().isEmpty()
                        .or(usernameField.textProperty().isEmpty())
                        .or(emailField.textProperty().isEmpty())
        );
    }

    @FXML
    private void handleChangePhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(changePhotoBtn.getScene().getWindow());

        if (selectedFile != null) {
            try {
                Image image = new Image(selectedFile.toURI().toString());
                updateProfilePicture(image);
                profileInitial.setText("");
                showAlert("Succès", "Photo de profil mise à jour avec succès", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Erreur", "Impossible de charger l'image: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleSaveProfile() {
        if (!validateAllFields()) {
            return;
        }

        try {
            // Vérifier le mot de passe si nécessaire
            boolean passwordChanged = false;
            String newPassword = null;

            if (!currentPasswordField.getText().isEmpty() ||
                    !newPasswordField.getText().isEmpty()) {

                if (!validatePasswordChange()) {
                    return;
                }

                passwordChanged = true;
                newPassword = newPasswordField.getText();
            }

            // Mettre à jour l'utilisateur
            User updatedUser = new User();
            updatedUser.setIdUser(currentUser.getIdUser());
            updatedUser.setUsername(usernameField.getText());
            updatedUser.setEmail(emailField.getText());

            if (passwordChanged) {
                updatedUser.setPassword(newPassword);
            } else {
                updatedUser.setPassword(currentUser.getPassword());
            }

            updatedUser.setRole(currentUser.getRole());
            updatedUser.setDateCreation(currentUser.getDateCreation());

            // Sauvegarder dans la base de données
            boolean success = updateUserInDatabase(updatedUser);

            if (success) {
                this.currentUser = updatedUser;
                profileNameLabel.setText(updatedUser.getUsername());
                profileInitial.setText(getInitials(updatedUser.getUsername()));

                if (mainController != null) {
                  //  mainController.updateUserProfile(updatedUser);
                }

                currentPasswordField.clear();
                newPasswordField.clear();
                confirmPasswordField.clear();

                showAlert("Succès", "Profil mis à jour avec succès", Alert.AlertType.INFORMATION);
            } else {
                showAlert("Erreur", "Échec de la mise à jour du profil", Alert.AlertType.ERROR);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Une erreur est survenue: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleCancel() {
        loadProfileData();
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
        showAlert("Annulé", "Modifications annulées", Alert.AlertType.INFORMATION);
    }

    private boolean validateAllFields() {
        StringBuilder errors = new StringBuilder();

        if (fullNameField.getText().trim().isEmpty()) {
            errors.append("- Le nom complet est requis\n");
        }

        if (usernameField.getText().trim().isEmpty()) {
            errors.append("- Le nom d'utilisateur est requis\n");
        }

        if (!validateEmail()) {
            errors.append("- L'email n'est pas valide\n");
        }

        if (errors.length() > 0) {
            showAlert("Erreur de validation", errors.toString(), Alert.AlertType.ERROR);
            return false;
        }

        return true;
    }

    private boolean validateEmail() {
        String email = emailField.getText();
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";

        if (!email.matches(emailRegex)) {
            emailField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 8;");
            return false;
        } else {
            emailField.setStyle("");
            return true;
        }
    }

    // Vérifier si le nom d'utilisateur est disponible
    private void checkUsernameAvailability() {
        String newUsername = usernameField.getText().trim();

        // Si le nom d'utilisateur n'a pas changé, ne rien faire
        if (newUsername.equals(currentUser.getUsername())) {
            return;
        }

        try {
            Connection conn = com.ensah.qoe.Models.DBConnection.getConnection();
            String query = "SELECT COUNT(*) FROM UTILISATEURS WHERE username = ? AND id_user != ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, newUsername);
            pstmt.setInt(2, currentUser.getIdUser());

            ResultSet rs = pstmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                usernameField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 8;");
                showAlert("Erreur", "Ce nom d'utilisateur est déjà utilisé", Alert.AlertType.WARNING);
            } else {
                usernameField.setStyle("");
            }

            rs.close();
            pstmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean validatePasswordChange() {
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Vérifier le mot de passe actuel
        if (!verifyCurrentPassword(currentPassword)) {
            showAlert("Erreur", "Le mot de passe actuel est incorrect", Alert.AlertType.ERROR);
            currentPasswordField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 8;");
            return false;
        }

        // Vérifier que le nouveau mot de passe est différent
        if (currentPassword.equals(newPassword)) {
            showAlert("Erreur", "Le nouveau mot de passe doit être différent de l'actuel", Alert.AlertType.ERROR);
            newPasswordField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 8;");
            return false;
        }

        // Vérifier la confirmation
        if (!newPassword.equals(confirmPassword)) {
            showAlert("Erreur", "Les mots de passe ne correspondent pas", Alert.AlertType.ERROR);
            confirmPasswordField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 8;");
            return false;
        }

        // Vérifier la force du mot de passe
        if (newPassword.length() < 8) {
            showAlert("Erreur", "Le mot de passe doit contenir au moins 8 caractères", Alert.AlertType.ERROR);
            return false;
        }

        // Réinitialiser les styles si tout est bon
        currentPasswordField.setStyle("");
        newPasswordField.setStyle("");
        confirmPasswordField.setStyle("");

        return true;
    }

    // Vérifier le mot de passe actuel dans la base
    private boolean verifyCurrentPassword(String password) {
        try {
            Connection conn = com.ensah.qoe.Models.DBConnection.getConnection();

            String query = "SELECT password FROM UTILISATEURS WHERE id_user = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, currentUser.getIdUser());

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String storedPassword = rs.getString("password");
                // ⚠️ ATTENTION: Dans une application réelle, utilisez BCrypt ou autre hash
                // Pour l'instant, on compare en clair
                return storedPassword.equals(password);
            }

            rs.close();
            pstmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // Mettre à jour l'utilisateur dans la base
    private boolean updateUserInDatabase(User user) {
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = com.ensah.qoe.Models.DBConnection.getConnection();

            String query;
            if (user.getPassword() != null && !user.getPassword().equals(currentUser.getPassword())) {
                // Mise à jour avec mot de passe
                query = "UPDATE UTILISATEURS SET username = ?, email = ?, password = ? WHERE id_user = ?";
                pstmt = conn.prepareStatement(query);
                pstmt.setString(1, user.getUsername());
                pstmt.setString(2, user.getEmail());
                pstmt.setString(3, user.getPassword()); // ⚠️ Devrait être hashé
                pstmt.setInt(4, user.getIdUser());
            } else {
                // Mise à jour sans mot de passe
                query = "UPDATE UTILISATEURS SET username = ?, email = ? WHERE id_user = ?";
                pstmt = conn.prepareStatement(query);
                pstmt.setString(1, user.getUsername());
                pstmt.setString(2, user.getEmail());
                pstmt.setInt(3, user.getIdUser());
            }

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (pstmt != null) pstmt.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void updateProfilePicture(Image image) {
        // TODO: Implémenter la sauvegarde de l'image si nécessaire
        // Vous pourriez ajouter un champ 'profile_picture' dans la table UTILISATEURS
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "A";
        }

        String[] names = fullName.trim().split("\\s+");
        if (names.length >= 2) {
            return String.valueOf(names[0].charAt(0)) + names[names.length - 1].charAt(0);
        } else {
            return String.valueOf(fullName.charAt(0));
        }
    }

    private String formatDateString(String dateString) {
        try {
            if (dateString.contains("-")) {
                LocalDate date = LocalDate.parse(dateString);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");
                return date.format(formatter);
            } else {
                return dateString;
            }
        } catch (Exception e) {
            return dateString;
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}