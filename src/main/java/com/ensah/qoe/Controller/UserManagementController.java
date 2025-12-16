package com.ensah.qoe.Controller;

import com.ensah.qoe.Models.User;
import com.ensah.qoe.Models.UserDAO;
import com.ensah.qoe.Models.DBConnection; // Votre classe de connexion
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class UserManagementController implements Initializable {

    // Labels de statistiques
    @FXML private Label totalUsersLabel;
    @FXML private Label activeUsersLabel;
    @FXML private Label adminUsersLabel;
    @FXML private Label clientUsersLabel;

    // Champs de recherche et filtres
    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;

    // Table et colonnes
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Void> avatarColumn;
    @FXML private TableColumn<User, String> idColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> dateCreationColumn;
    @FXML private TableColumn<User, Void> actionsColumn;

    // Pagination
    @FXML private Label userCountLabel;
    @FXML private Label pageLabel;

    // Données
    private ObservableList<User> allUsers;
    private ObservableList<User> filteredUsers;
    private int currentPage = 1;
    private int itemsPerPage = 10;
    private int totalPages = 1;

    // Base de données
    private Connection connection;
    private UserDAO userDAO;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            // Initialiser la connexion à la base de données via votre classe
            initializeDatabaseConnection();

            // Configurer l'interface
            setupTableColumns();
            loadUsersFromDatabase();
            updateStatistics();
            setupFilters();
            setupPagination();

            System.out.println("UserManagementController initialized successfully");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to connect to database: " + e.getMessage(), Alert.AlertType.ERROR);

            // Initialiser les listes même en cas d'erreur
            allUsers = FXCollections.observableArrayList();
            filteredUsers = FXCollections.observableArrayList();
            setupPagination();
        }
    }

    private void initializeDatabaseConnection() throws SQLException {
        // Utiliser votre classe de connexion existante
        connection = DBConnection.getConnection();
        userDAO = new UserDAO(connection);
        System.out.println("Database connection established successfully");
    }

    private void setupTableColumns() {
        // Configuration des colonnes pour correspondre à votre table utilisateurs
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        dateCreationColumn.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));

        // Avatar column avec les initiales
        avatarColumn.setCellFactory(column -> new TableCell<User, Void>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());
                    Circle avatar = new Circle(20);
                    avatar.setFill(Color.web(getColorForUser(user.getUsername())));

                    Label initials = new Label(getInitials(user.getUsername()));
                    initials.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

                    javafx.scene.layout.StackPane stack = new javafx.scene.layout.StackPane(avatar, initials);
                    setGraphic(stack);
                }
            }
        });

        // Colonne Role avec badges colorés
        roleColumn.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(role);
                    if ("admin".equalsIgnoreCase(role)) {
                        setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #1e40af; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-weight: bold;");
                    } else if ("client".equalsIgnoreCase(role)) {
                        setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #374151; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Colonne Actions avec boutons
        actionsColumn.setCellFactory(column -> new TableCell<User, Void>() {
            private final Button editBtn = new Button("Edit ");
            private final Button deleteBtn = new Button("Delete");

            {
                editBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 4 8;");
                deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 4 8;");

                editBtn.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    editUser(user);
                });

                deleteBtn.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    deleteUser(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(5, editBtn, deleteBtn);
                    setGraphic(hbox);
                }
            }
        });
    }

    private void loadUsersFromDatabase() {
        try {
            if (userDAO == null) {
                System.err.println("UserDAO is not initialized");
                allUsers = FXCollections.observableArrayList();
            } else {
                List<User> usersList = userDAO.getAllUsers();
                allUsers = FXCollections.observableArrayList(usersList);
                System.out.println("Loaded " + usersList.size() + " users from database");
            }
        } catch (SQLException e) {
            System.err.println("Failed to load users: " + e.getMessage());
            e.printStackTrace();
            showAlert("Database Error", "Failed to load users: " + e.getMessage(), Alert.AlertType.ERROR);
            allUsers = FXCollections.observableArrayList();
        }

        filteredUsers = FXCollections.observableArrayList(allUsers);
        updatePaginationData();
    }

    private void updateStatistics() {
        if (allUsers == null) {
            allUsers = FXCollections.observableArrayList();
        }

        totalUsersLabel.setText(String.valueOf(allUsers.size()));

        long adminCount = allUsers.stream()
                .filter(u -> u.getRole() != null && "admin".equalsIgnoreCase(u.getRole()))
                .count();
        adminUsersLabel.setText(String.valueOf(adminCount));

        long clientCount = allUsers.stream()
                .filter(u -> u.getRole() != null && "client".equalsIgnoreCase(u.getRole()))
                .count();
        clientUsersLabel.setText(String.valueOf(clientCount));

        // Pour active users, on considère tous comme actifs
        activeUsersLabel.setText(String.valueOf(allUsers.size()));
    }

    private void setupFilters() {
        // Initialiser le ComboBox des rôles
        ObservableList<String> roles = FXCollections.observableArrayList("All Roles", "admin", "client");
        roleFilter.setItems(roles);
        roleFilter.setValue("All Roles");

        // Filtre de recherche
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });

        // Filtre de rôle
        roleFilter.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
    }

    private void applyFilters() {
        if (allUsers == null) {
            allUsers = FXCollections.observableArrayList();
        }

        String searchText = searchField.getText().toLowerCase();
        String selectedRole = roleFilter.getValue();

        filteredUsers = allUsers.stream()
                .filter(user -> {
                    // Filtre de recherche
                    boolean matchesSearch = searchText.isEmpty() ||
                            (user.getUsername() != null && user.getUsername().toLowerCase().contains(searchText)) ||
                            (user.getEmail() != null && user.getEmail().toLowerCase().contains(searchText)) ||
                            (user.getRole() != null && user.getRole().toLowerCase().contains(searchText));

                    // Filtre de rôle
                    boolean matchesRole = selectedRole == null ||
                            "All Roles".equals(selectedRole) ||
                            (user.getRole() != null && user.getRole().equalsIgnoreCase(selectedRole));

                    return matchesSearch && matchesRole;
                })
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        currentPage = 1;
        updatePaginationData();
    }

    private void setupPagination() {
        updatePaginationData();
    }

    private void updatePaginationData() {
        if (filteredUsers == null) {
            filteredUsers = FXCollections.observableArrayList();
        }

        totalPages = (int) Math.ceil((double) filteredUsers.size() / itemsPerPage);
        if (totalPages == 0) totalPages = 1;

        int fromIndex = (currentPage - 1) * itemsPerPage;
        int toIndex = Math.min(fromIndex + itemsPerPage, filteredUsers.size());

        ObservableList<User> pageUsers = FXCollections.observableArrayList(
                filteredUsers.subList(fromIndex, toIndex)
        );

        usersTable.setItems(pageUsers);

        userCountLabel.setText(String.format("Showing %d-%d of %d users",
                fromIndex + 1, toIndex, filteredUsers.size()));
        pageLabel.setText(String.format("Page %d of %d", currentPage, totalPages));
    }

    // ========== MÉTHODES @FXML (OBLIGATOIRES POUR LE FXML) ==========

    @FXML
    private void showAddUserDialog() {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Add New User");
        dialog.setHeaderText("Create a new user account");

        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 20;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        TextField emailField = new TextField();
        emailField.setPromptText("Email");

        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("admin", "client");
        roleCombo.setValue("client");

        content.getChildren().addAll(
                new Label("Username:"), usernameField,
                new Label("Password:"), passwordField,
                new Label("Email:"), emailField,
                new Label("Role:"), roleCombo
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                User user = new User();
                user.setUsername(usernameField.getText());
                user.setPassword(passwordField.getText());
                user.setEmail(emailField.getText());
                user.setRole(roleCombo.getValue());
                return user;
            }
            return null;
        });

        Optional<User> result = dialog.showAndWait();
        result.ifPresent(newUser -> {
            try {
                if (userDAO == null) {
                    showAlert("Error", "Database connection not available", Alert.AlertType.ERROR);
                    return;
                }

                // Vérifier si le username existe déjà
                if (userDAO.usernameExists(newUser.getUsername())) {
                    showAlert("Error", "Username already exists!", Alert.AlertType.ERROR);
                    return;
                }

                // Vérifier si l'email existe déjà
                if (userDAO.emailExists(newUser.getEmail())) {
                    showAlert("Error", "Email already exists!", Alert.AlertType.ERROR);
                    return;
                }

                if (userDAO.addUser(newUser)) {
                    // Recharger depuis la base de données
                    loadUsersFromDatabase();
                    updateStatistics();
                    showAlert("Success", "User added successfully!", Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Error", "Failed to add user", Alert.AlertType.ERROR);
                }
            } catch (SQLException e) {
                showAlert("Database Error", "Failed to add user: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void editUser(User user) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Edit User");

        // Style du dialog
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/css/dialog-style.css").toExternalForm());
        dialogPane.getStyleClass().add("modern-dialog");

        // Contenu principal
        VBox content = new VBox(20);
        content.setStyle("-fx-padding: 30; -fx-background-color: #f8f9fa;");

        // Header moderne
        VBox header = new VBox(8);
        header.setStyle("-fx-alignment: center;");

        Label titleLabel = new Label("Modifier l'utilisateur");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label subtitleLabel = new Label(user.getUsername());
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        Separator separator = new Separator();
        separator.setStyle("-fx-padding: 10 0 0 0;");

        header.getChildren().addAll(titleLabel, subtitleLabel, separator);

        // Grid pour les champs
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(20);
        grid.setStyle("-fx-padding: 20 0;");

        // Champ Username avec icône
        VBox usernameBox = createStyledField("👤", "Nom d'utilisateur");
        TextField usernameField = new TextField(user.getUsername());
        usernameField.setPromptText("Entrez le nom d'utilisateur");
        styleTextField(usernameField);
        usernameBox.getChildren().add(usernameField);

        // Champ Email avec icône
        VBox emailBox = createStyledField("✉", "Adresse email");
        TextField emailField = new TextField(user.getEmail());
        emailField.setPromptText("exemple@email.com");
        styleTextField(emailField);

        // Validation email en temps réel
        Label emailError = new Label();
        emailError.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
        emailError.setVisible(false);

        emailField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.isEmpty() && !newVal.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                emailField.setStyle(getTextFieldStyle() + "-fx-border-color: #e74c3c;");
                emailError.setText("⚠ Email invalide");
                emailError.setVisible(true);
            } else {
                emailField.setStyle(getTextFieldStyle());
                emailError.setVisible(false);
            }
        });

        emailBox.getChildren().addAll(emailField, emailError);

        // Champ Role avec style moderne
        VBox roleBox = createStyledField("🔑", "Rôle");
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("admin", "client");
        roleCombo.setValue(user.getRole() != null ? user.getRole() : "client");
        styleComboBox(roleCombo);
        roleBox.getChildren().add(roleCombo);

        // Ajouter au grid
        grid.add(usernameBox, 0, 0);
        grid.add(emailBox, 0, 1);
        grid.add(roleBox, 0, 2);

        // Info box
        HBox infoBox = new HBox(10);
        infoBox.setStyle(
                "-fx-background-color: #e8f4f8; " +
                        "-fx-padding: 15; " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-color: #3498db; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 8;"
        );

        Label infoIcon = new Label("ℹ");
        infoIcon.setStyle("-fx-font-size: 18px;");

        Label infoText = new Label("Les modifications seront sauvegardées immédiatement dans la base de données.");
        infoText.setStyle("-fx-text-fill: #2980b9; -fx-font-size: 12px;");
        infoText.setWrapText(true);

        infoBox.getChildren().addAll(infoIcon, infoText);

        content.getChildren().addAll(header, grid, infoBox);

        dialogPane.setContent(content);

        // Boutons personnalisés
        ButtonType saveButton = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogPane.getButtonTypes().addAll(saveButton, cancelButton);

        // Style des boutons
        Button saveBtn = (Button) dialogPane.lookupButton(saveButton);
        Button cancelBtn = (Button) dialogPane.lookupButton(cancelButton);

        saveBtn.setStyle(
                "-fx-background-color: #3498db; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 10 30; " +
                        "-fx-background-radius: 6; " +
                        "-fx-cursor: hand;"
        );

        saveBtn.setOnMouseEntered(e -> saveBtn.setStyle(
                saveBtn.getStyle() + "-fx-background-color: #2980b9;"
        ));

        cancelBtn.setStyle(
                "-fx-background-color: #ecf0f1; " +
                        "-fx-text-fill: #2c3e50; " +
                        "-fx-padding: 10 30; " +
                        "-fx-background-radius: 6; " +
                        "-fx-cursor: hand;"
        );

        // Validation avant sauvegarde
        saveBtn.addEventFilter(ActionEvent.ACTION, event -> {
            if (usernameField.getText().trim().isEmpty()) {
                showAlert("Validation", "Le nom d'utilisateur est requis", Alert.AlertType.WARNING);
                event.consume();
            } else if (emailField.getText().trim().isEmpty() || !emailField.getText().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                showAlert("Validation", "Une adresse email valide est requise", Alert.AlertType.WARNING);
                event.consume();
            }
        });

        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButton) {
                user.setUsername(usernameField.getText().trim());
                user.setEmail(emailField.getText().trim());
                user.setRole(roleCombo.getValue());
                return true;
            }
            return false;
        });

        dialog.showAndWait().ifPresent(updated -> {
            if (updated) {
                try {
                    if (userDAO == null) {
                        showAlert("Erreur", "Connexion à la base de données non disponible", Alert.AlertType.ERROR);
                        return;
                    }

                    if (userDAO.updateUser(user)) {
                        usersTable.refresh();
                        updateStatistics();
                        showSuccessNotification("Utilisateur mis à jour avec succès!");
                    } else {
                        showAlert("Erreur", "Échec de la mise à jour de l'utilisateur", Alert.AlertType.ERROR);
                    }
                } catch (SQLException e) {
                    showAlert("Erreur Base de Données", "Échec de la mise à jour: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }


    private void deleteUser(User user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete User");
        alert.setHeaderText("Delete " + user.getUsername() + "?");
        alert.setContentText("This action cannot be undone. Are you sure?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    if (userDAO == null) {
                        showAlert("Error", "Database connection not available", Alert.AlertType.ERROR);
                        return;
                    }

                    if (userDAO.deleteUser(user.getId())) {
                        allUsers.remove(user);
                        applyFilters();
                        updateStatistics();
                        showAlert("Success", "User deleted successfully!", Alert.AlertType.INFORMATION);
                    } else {
                        showAlert("Error", "Failed to delete user", Alert.AlertType.ERROR);
                    }
                } catch (SQLException e) {
                    showAlert("Database Error", "Failed to delete user: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    @FXML
    private void resetFilters() {
        searchField.clear();
        roleFilter.getSelectionModel().selectFirst();
        applyFilters();
    }

    @FXML
    private void exportUsers() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Users");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("users_export.csv");

        File file = fileChooser.showSaveDialog(usersTable.getScene().getWindow());
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("ID,Username,Email,Role,Date Creation\n");
                for (User user : filteredUsers) {
                    writer.write(String.format("%d,%s,%s,%s,%s\n",
                            user.getId(),
                            user.getUsername(),
                            user.getEmail(),
                            user.getRole(),
                            user.getDateCreation()));
                }
                showAlert("Export Complete", "Users exported successfully to:\n" + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Export Error", "Failed to export users: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    // ========== MÉTHODES DE PAGINATION ==========

    @FXML
    private void goToFirstPage() {
        currentPage = 1;
        updatePaginationData();
    }

    @FXML
    private void goToPreviousPage() {
        if (currentPage > 1) {
            currentPage--;
            updatePaginationData();
        }
    }

    @FXML
    private void goToNextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            updatePaginationData();
        }
    }

    @FXML
    private void goToLastPage() {
        currentPage = totalPages;
        updatePaginationData();
    }

    // ========== MÉTHODES UTILITAIRES ==========

    private String getInitials(String username) {
        if (username == null || username.trim().isEmpty()) {
            return "??";
        }
        return username.substring(0, Math.min(2, username.length())).toUpperCase();
    }

    private String getColorForUser(String username) {
        if (username == null || username.isEmpty()) {
            return "#64748b";
        }
        String[] colors = {"#3b82f6", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6", "#ec4899"};
        return colors[Math.abs(username.hashCode()) % colors.length];
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    // Méthodes utilitaires pour le style
    private VBox createStyledField(String icon, String labelText) {
        VBox box = new VBox(8);

        HBox labelBox = new HBox(8);
        labelBox.setStyle("-fx-alignment: center-left;");

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 16px;");

        Label label = new Label(labelText);
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #34495e; -fx-font-size: 13px;");

        labelBox.getChildren().addAll(iconLabel, label);
        box.getChildren().add(labelBox);

        return box;
    }

    private void styleTextField(TextField field) {
        field.setStyle(getTextFieldStyle());
        field.setPrefHeight(40);

        // Effet focus
        field.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                field.setStyle(getTextFieldStyle() + "-fx-border-color: #3498db; -fx-border-width: 2;");
            } else {
                field.setStyle(getTextFieldStyle());
            }
        });
    }

    private void styleComboBox(ComboBox<String> combo) {
        combo.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #bdc3c7; " +
                        "-fx-border-radius: 6; " +
                        "-fx-background-radius: 6; " +
                        "-fx-padding: 8; " +
                        "-fx-font-size: 13px;"
        );
        combo.setPrefHeight(40);
        combo.setPrefWidth(300);
    }

    private String getTextFieldStyle() {
        return "-fx-background-color: white; " +
                "-fx-border-color: #bdc3c7; " +
                "-fx-border-radius: 6; " +
                "-fx-background-radius: 6; " +
                "-fx-padding: 10; " +
                "-fx-font-size: 13px; " +
                "-fx-min-width: 300;";
    }

    private void showSuccessNotification(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText("✓ " + message);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #f8f9fa;");

        alert.showAndWait();
    }
}