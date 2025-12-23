package com.ensah.qoe.Controller;

import com.ensah.qoe.Models.User;
import com.ensah.qoe.Models.UserDAO;
import com.ensah.qoe.Models.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.StageStyle;

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

        // Désactiver la colonne de remplissage automatique
        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

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

                    StackPane stack = new StackPane(avatar, initials);
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

        // Colonne Date Creation avec format
        dateCreationColumn.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(date);
                    setStyle("-fx-alignment: center-left; -fx-padding: 4 8;");
                }
            }
        });

        // Colonne Actions avec boutons
        actionsColumn.setCellFactory(column -> new TableCell<User, Void>() {
            private final Button editBtn = new Button("✏ Edit");
            private final Button deleteBtn = new Button("🗑 Delete");

            {
                editBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 6 12; -fx-background-radius: 4; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 6 12; -fx-background-radius: 4; -fx-cursor: hand;");

                editBtn.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    editUser(user);
                });

                deleteBtn.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    deleteUser(user);
                });

                // Effets hover
                editBtn.setOnMouseEntered(e ->
                        editBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 6 12; -fx-background-radius: 4; -fx-cursor: hand;")
                );
                editBtn.setOnMouseExited(e ->
                        editBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 6 12; -fx-background-radius: 4; -fx-cursor: hand;")
                );

                deleteBtn.setOnMouseEntered(e ->
                        deleteBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 6 12; -fx-background-radius: 4; -fx-cursor: hand;")
                );
                deleteBtn.setOnMouseExited(e ->
                        deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 6 12; -fx-background-radius: 4; -fx-cursor: hand;")
                );
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(8, editBtn, deleteBtn);
                    hbox.setAlignment(Pos.CENTER);
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

        // Style du ComboBox
        roleFilter.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 6; -fx-padding: 8;");

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
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.APPLICATION_MODAL);

        // Créer un conteneur principal avec ombre et coins arrondis
        StackPane rootPane = new StackPane();
        rootPane.setStyle("-fx-background-color: transparent;");

        VBox mainContainer = new VBox();
        mainContainer.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #ffffff, #f8fafc); " +
                        "-fx-background-radius: 20; " +
                        "-fx-border-radius: 20; " +
                        "-fx-border-color: #e2e8f0; " +
                        "-fx-border-width: 1; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 25, 0, 0, 10); " +
                        "-fx-padding: 0;"
        );
        mainContainer.setMaxWidth(480);

        // HEADER - Section supérieure avec gradient
        VBox headerSection = new VBox();
        headerSection.setStyle(
                "-fx-background-color: linear-gradient(to right, #3b82f6, #1d4ed8); " +
                        "-fx-background-radius: 20 20 0 0; " +
                        "-fx-padding: 30 30 20 30; " +
                        "-fx-spacing: 15;"
        );
        headerSection.setAlignment(Pos.CENTER);

        // Icône circulaire avec effet de profondeur
        StackPane iconContainer = new StackPane();
        iconContainer.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 50; " +
                        "-fx-padding: 15; " +
                        "-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.3), 10, 0, 0, 2);"
        );

        Label userIcon = new Label("👤");
        userIcon.setStyle("-fx-font-size: 32px; -fx-font-weight: bold;");

        iconContainer.getChildren().add(userIcon);

        // Titres
        VBox titleBox = new VBox(5);
        titleBox.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Create New User");
        titleLabel.setStyle(
                "-fx-font-size: 24px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-family: 'Segoe UI', Arial, sans-serif;"
        );

        Label subtitleLabel = new Label("Add a new user to your system");
        subtitleLabel.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-text-fill: rgba(255,255,255,0.9); " +
                        "-fx-font-family: 'Segoe UI', Arial, sans-serif;"
        );

        titleBox.getChildren().addAll(titleLabel, subtitleLabel);
        headerSection.getChildren().addAll(iconContainer, titleBox);

        // CONTENU - Formulaire
        VBox formContainer = new VBox(25);
        formContainer.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-padding: 30;"
        );

        GridPane formGrid = new GridPane();
        formGrid.setHgap(20);
        formGrid.setVgap(20);
        formGrid.setStyle("-fx-padding: 0;");

        // Champ Username
        VBox usernameContainer = createModernFormField("👤", "Username", "Enter username");
        TextField usernameField = new TextField();
        styleModernTextField(usernameField);
        usernameContainer.getChildren().add(usernameField);

        // Champ Password
        VBox passwordContainer = createModernFormField("🔒", "Password", "Enter password");
        PasswordField passwordField = new PasswordField();
        styleModernTextField(passwordField);
        passwordContainer.getChildren().add(passwordField);

        // Champ Email
        VBox emailContainer = createModernFormField("✉️", "Email Address", "user@example.com");
        TextField emailField = new TextField();
        styleModernTextField(emailField);

        Label emailError = new Label();
        emailError.setStyle(
                "-fx-text-fill: #ef4444; " +
                        "-fx-font-size: 12px; " +
                        "-fx-padding: 4 0 0 5; " +
                        "-fx-font-family: 'Segoe UI', Arial, sans-serif;"
        );
        emailError.setVisible(false);

        emailField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty() && !isValidEmail(newVal)) {
                emailField.setStyle(getModernTextFieldStyle() + "-fx-border-color: #ef4444;");
                emailError.setText("Please enter a valid email address");
                emailError.setVisible(true);
            } else {
                emailField.setStyle(getModernTextFieldStyle());
                emailError.setVisible(false);
            }
        });

        VBox emailWithError = new VBox(5, emailField, emailError);

        // Champ Role
        VBox roleContainer = createModernFormField("🎭", "User Role", "Select user role");
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("Client", "Administrator");
        roleCombo.setValue("Client");
        styleModernComboBox(roleCombo);

        // Ajouter un badge coloré pour chaque rôle dans le ComboBox
        roleCombo.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox cellContent = new HBox(10);
                    cellContent.setAlignment(Pos.CENTER_LEFT);

                    Label roleBadge = new Label(item);
                    roleBadge.setStyle(
                            "-fx-font-size: 13px; " +
                                    "-fx-font-weight: bold; " +
                                    "-fx-padding: 6 12; " +
                                    "-fx-background-radius: 15;"
                    );

                    if ("Administrator".equals(item)) {
                        roleBadge.setStyle(roleBadge.getStyle() +
                                "-fx-background-color: #dbeafe; " +
                                "-fx-text-fill: #1e40af;"
                        );
                    } else {
                        roleBadge.setStyle(roleBadge.getStyle() +
                                "-fx-background-color: #dcfce7; " +
                                "-fx-text-fill: #166534;"
                        );
                    }

                    cellContent.getChildren().add(roleBadge);
                    setGraphic(cellContent);
                    setText(null);
                }
            }
        });

        roleCombo.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("Select role");
                    setStyle("-fx-text-fill: #64748b;");
                } else {
                    setText(item);
                    if ("Administrator".equals(item)) {
                        setStyle("-fx-text-fill: #1e40af; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #166534; -fx-font-weight: bold;");
                    }
                }
            }
        });

        roleContainer.getChildren().add(roleCombo);

        // Ajouter les champs au grid
        formGrid.add(usernameContainer, 0, 0);
        formGrid.add(passwordContainer, 0, 1);
        formGrid.add(emailContainer, 0, 2);
        formGrid.add(emailWithError, 0, 3);
        formGrid.add(roleContainer, 0, 4);

        formContainer.getChildren().add(formGrid);

        // FOOTER - Boutons d'action
        HBox buttonContainer = new HBox(15);
        buttonContainer.setStyle(
                "-fx-background-color: #f8fafc; " +
                        "-fx-background-radius: 0 0 20 20; " +
                        "-fx-padding: 20 30 25 30; " +
                        "-fx-border-color: #f1f5f9; " +
                        "-fx-border-width: 1 0 0 0;"
        );
        buttonContainer.setAlignment(Pos.CENTER_RIGHT);

        // Bouton Cancel
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-text-fill: #64748b; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 10 25; " +
                        "-fx-background-radius: 10; " +
                        "-fx-border-color: #e2e8f0; " +
                        "-fx-border-width: 1; " +
                        "-fx-cursor: hand; " +
                        "-fx-font-family: 'Segoe UI', Arial, sans-serif;"
        );

        cancelBtn.setOnMouseEntered(e ->
                cancelBtn.setStyle(cancelBtn.getStyle() + "-fx-background-color: #f1f5f9;")
        );

        cancelBtn.setOnMouseExited(e ->
                cancelBtn.setStyle(cancelBtn.getStyle() + "-fx-background-color: transparent;")
        );

        // Bouton Create User
        Button createBtn = new Button("Create User");
        createBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #10b981, #059669); " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 12 30; " +
                        "-fx-background-radius: 10; " +
                        "-fx-cursor: hand; " +
                        "-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                        "-fx-effect: dropshadow(gaussian, rgba(16, 185, 129, 0.3), 10, 0, 0, 3);"
        );

        createBtn.setOnMouseEntered(e ->
                createBtn.setStyle(
                        "-fx-background-color: linear-gradient(to right, #059669, #047857); " +
                                "-fx-text-fill: white; " +
                                "-fx-font-weight: bold; " +
                                "-fx-font-size: 14px; " +
                                "-fx-padding: 12 30; " +
                                "-fx-background-radius: 10; " +
                                "-fx-cursor: hand; " +
                                "-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                                "-fx-effect: dropshadow(gaussian, rgba(16, 185, 129, 0.5), 15, 0, 0, 5);"
                )
        );

        createBtn.setOnMouseExited(e ->
                createBtn.setStyle(
                        "-fx-background-color: linear-gradient(to right, #10b981, #059669); " +
                                "-fx-text-fill: white; " +
                                "-fx-font-weight: bold; " +
                                "-fx-font-size: 14px; " +
                                "-fx-padding: 12 30; " +
                                "-fx-background-radius: 10; " +
                                "-fx-cursor: hand; " +
                                "-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                                "-fx-effect: dropshadow(gaussian, rgba(16, 185, 129, 0.3), 10, 0, 0, 3);"
                )
        );

        buttonContainer.getChildren().addAll(cancelBtn, createBtn);

        // Assembler tout
        mainContainer.getChildren().addAll(headerSection, formContainer, buttonContainer);
        rootPane.getChildren().add(mainContainer);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setContent(rootPane);
        dialogPane.setStyle("-fx-background-color: transparent;");

        // Ajouter les types de boutons (pour la logique)
        dialogPane.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        // Cacher les boutons par défaut
        Node closeButton = dialogPane.lookupButton(ButtonType.CANCEL);
        closeButton.setVisible(false);
        closeButton.setManaged(false);

        Node okButton = dialogPane.lookupButton(ButtonType.OK);
        okButton.setVisible(false);
        okButton.setManaged(false);

        // Actions des boutons personnalisés
        cancelBtn.setOnAction(e -> dialog.close());

        // Validation avant création
        createBtn.setOnAction(e -> {
            StringBuilder errors = new StringBuilder();
            boolean hasError = false;

            if (usernameField.getText().trim().isEmpty()) {
                errors.append("• Username is required\n");
                usernameField.setStyle(getModernTextFieldStyle() + "-fx-border-color: #ef4444;");
                hasError = true;
            } else {
                usernameField.setStyle(getModernTextFieldStyle());
            }

            if (passwordField.getText().trim().isEmpty()) {
                errors.append("• Password is required\n");
                passwordField.setStyle(getModernTextFieldStyle() + "-fx-border-color: #ef4444;");
                hasError = true;
            } else if (passwordField.getText().length() < 6) {
                errors.append("• Password must be at least 6 characters\n");
                passwordField.setStyle(getModernTextFieldStyle() + "-fx-border-color: #ef4444;");
                hasError = true;
            } else {
                passwordField.setStyle(getModernTextFieldStyle());
            }

            if (emailField.getText().trim().isEmpty()) {
                errors.append("• Email is required\n");
                emailField.setStyle(getModernTextFieldStyle() + "-fx-border-color: #ef4444;");
                hasError = true;
            } else if (!isValidEmail(emailField.getText())) {
                errors.append("• Please enter a valid email address\n");
                emailField.setStyle(getModernTextFieldStyle() + "-fx-border-color: #ef4444;");
                hasError = true;
            } else {
                emailField.setStyle(getModernTextFieldStyle());
            }

            if (hasError) {
                showAlert("Validation Error", errors.toString(), Alert.AlertType.WARNING);
            } else {
                dialog.setResult(new User() {{
                    setUsername(usernameField.getText().trim());
                    setPassword(passwordField.getText());
                    setEmail(emailField.getText().trim());
                    setRole(roleCombo.getValue().toLowerCase());
                }});
                dialog.close();
            }
        });

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                User user = new User();
                user.setUsername(usernameField.getText().trim());
                user.setPassword(passwordField.getText());
                user.setEmail(emailField.getText().trim());
                user.setRole(roleCombo.getValue().toLowerCase());
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
                    showSuccessNotification("✅ User created successfully!");
                } else {
                    showAlert("Error", "Failed to create user", Alert.AlertType.ERROR);
                }
            } catch (SQLException ex) {
                showAlert("Database Error", "Failed to create user: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    // Méthodes utilitaires pour le style moderne
    private VBox createModernFormField(String icon, String label, String placeholder) {
        VBox container = new VBox(8);

        HBox labelContainer = new HBox(10);
        labelContainer.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #4b5563;");

        Label fieldLabel = new Label(label);
        fieldLabel.setStyle(
                "-fx-font-weight: 600; " +
                        "-fx-text-fill: #374151; " +
                        "-fx-font-size: 15px; " +
                        "-fx-font-family: 'Segoe UI', Arial, sans-serif;"
        );

        labelContainer.getChildren().addAll(iconLabel, fieldLabel);
        container.getChildren().add(labelContainer);

        return container;
    }

    private void styleModernTextField(TextField field) {
        field.setPromptText(field instanceof PasswordField ? "Enter password" : "Enter value");
        field.setStyle(getModernTextFieldStyle());

        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                field.setStyle(getModernTextFieldStyle() +
                        "-fx-border-color: #3b82f6; " +
                        "-fx-border-width: 2; " +
                        "-fx-effect: dropshadow(gaussian, rgba(59, 130, 246, 0.2), 5, 0, 0, 0);"
                );
            } else {
                field.setStyle(getModernTextFieldStyle());
            }
        });
    }

    private void styleModernComboBox(ComboBox<String> combo) {
        combo.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #d1d5db; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 10; " +
                        "-fx-background-radius: 10; " +
                        "-fx-padding: 12 15; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                        "-fx-pref-width: 400;"
        );

        combo.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("Select role");
                    setStyle("-fx-text-fill: #64748b;");
                } else {
                    setText(item);
                }
            }
        });
    }

    private String getModernTextFieldStyle() {
        return "-fx-background-color: white; " +
                "-fx-border-color: #d1d5db; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 10; " +
                "-fx-background-radius: 10; " +
                "-fx-padding: 14 16; " +
                "-fx-font-size: 14px; " +
                "-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                "-fx-pref-width: 400; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.02), 2, 0, 0, 1);";
    }
    // Méthodes utilitaires pour le style du formulaire
    private VBox createFormField(String icon, String label, String placeholder) {
        VBox container = new VBox(8);

        HBox labelContainer = new HBox(8);
        labelContainer.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #4b5563;");

        Label fieldLabel = new Label(label);
        fieldLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #374151; -fx-font-size: 15px;");

        labelContainer.getChildren().addAll(iconLabel, fieldLabel);
        container.getChildren().add(labelContainer);

        return container;
    }

    private void styleFormField(TextField field) {
        field.setPromptText(field instanceof PasswordField ? "Enter password" : "Enter value");
        field.setStyle(getTextFieldStyle());

        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                field.setStyle(getTextFieldStyle() + "-fx-border-color: #3b82f6; -fx-border-width: 2;");
            } else {
                field.setStyle(getTextFieldStyle());
            }
        });
    }

    private void styleComboBox(ComboBox<String> combo) {
        combo.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #d1d5db; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 12 15; " +
                        "-fx-font-size: 14px; " +
                        "-fx-pref-width: 350;"
        );
    }

    private String getTextFieldStyle() {
        return "-fx-background-color: white; " +
                "-fx-border-color: #d1d5db; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8; " +
                "-fx-padding: 12 15; " +
                "-fx-font-size: 14px; " +
                "-fx-pref-width: 350;";
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(emailRegex);
    }

    private void showSuccessNotification(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle(
                "-fx-background-color: #f0fdf4; " +
                        "-fx-border-color: #bbf7d0; " +
                        "-fx-border-width: 1; " +
                        "-fx-background-radius: 8;"
        );

        alert.showAndWait();
    }

    private void editUser(User user) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Edit User");
        dialog.initStyle(StageStyle.UTILITY);
        dialog.initModality(Modality.APPLICATION_MODAL);

        // Style moderne pour le dialog
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e5e7eb; -fx-border-width: 1; -fx-background-radius: 12;");

        // Header avec icône et titre
        VBox header = new VBox(10);
        header.setStyle("-fx-alignment: center; -fx-padding: 25 25 15 25;");

        HBox iconContainer = new HBox();
        iconContainer.setAlignment(Pos.CENTER);
        iconContainer.setStyle("-fx-background-color: linear-gradient(to right, #f59e0b, #d97706); -fx-background-radius: 50; -fx-padding: 18;");

        Label iconLabel = new Label("✏");
        iconLabel.setStyle("-fx-font-size: 28px; -fx-text-fill: white;");
        iconContainer.getChildren().add(iconLabel);

        Label titleLabel = new Label("Edit User");
        titleLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label subtitleLabel = new Label("Update user: " + user.getUsername());
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b7280;");

        header.getChildren().addAll(iconContainer, titleLabel, subtitleLabel);

        // Contenu principal
        VBox content = new VBox(20);
        content.setStyle("-fx-padding: 20 30 30 30; -fx-background-color: #ffffff;");

        // Formulaire avec style moderne
        GridPane formGrid = new GridPane();
        formGrid.setHgap(15);
        formGrid.setVgap(25);
        formGrid.setStyle("-fx-padding: 10;");

        // Champ Username
        VBox usernameBox = createFormField("👤", "Username", "Enter username");
        TextField usernameField = new TextField(user.getUsername());
        styleFormField(usernameField);
        usernameBox.getChildren().add(usernameField);

        // Champ Email
        VBox emailBox = createFormField("✉️", "Email", "user@example.com");
        TextField emailField = new TextField(user.getEmail());
        styleFormField(emailField);

        // Validation email
        Label emailError = new Label();
        emailError.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-padding: 2 0 0 0;");
        emailError.setVisible(false);

        emailField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty() && !isValidEmail(newVal)) {
                emailField.setStyle(getTextFieldStyle() + "-fx-border-color: #ef4444;");
                emailError.setText("Please enter a valid email address");
                emailError.setVisible(true);
            } else {
                emailField.setStyle(getTextFieldStyle());
                emailError.setVisible(false);
            }
        });

        emailBox.getChildren().addAll(emailField, emailError);

        // Champ Role
        VBox roleBox = createFormField("🎭", "Role", "Select role");
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("client", "admin");
        roleCombo.setValue(user.getRole() != null ? user.getRole() : "client");
        styleComboBox(roleCombo);
        roleBox.getChildren().add(roleCombo);

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

        Label infoText = new Label("Changes will be saved immediately to the database.");
        infoText.setStyle("-fx-text-fill: #2980b9; -fx-font-size: 13px;");
        infoText.setWrapText(true);

        infoBox.getChildren().addAll(infoIcon, infoText);

        // Ajout des champs au grid
        formGrid.add(usernameBox, 0, 0);
        formGrid.add(emailBox, 0, 1);
        formGrid.add(roleBox, 0, 2);

        content.getChildren().addAll(header, new Separator(), formGrid, infoBox);

        dialogPane.setContent(content);

        // Boutons
        ButtonType saveButton = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogPane.getButtonTypes().addAll(saveButton, cancelButton);

        // Style des boutons
        Button saveBtn = (Button) dialogPane.lookupButton(saveButton);
        Button cancelBtn = (Button) dialogPane.lookupButton(cancelButton);

        // Style bouton Save
        saveBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #3b82f6, #2563eb); " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 12 35; " +
                        "-fx-background-radius: 8; " +
                        "-fx-cursor: hand;"
        );

        saveBtn.setOnMouseEntered(e ->
                saveBtn.setStyle(saveBtn.getStyle() + "-fx-background-color: linear-gradient(to right, #2563eb, #1d4ed8);")
        );

        saveBtn.setOnMouseExited(e ->
                saveBtn.setStyle(saveBtn.getStyle() + "-fx-background-color: linear-gradient(to right, #3b82f6, #2563eb);")
        );

        // Style bouton Cancel
        cancelBtn.setStyle(
                "-fx-background-color: #f3f4f6; " +
                        "-fx-text-fill: #374151; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 12 35; " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-color: #d1d5db; " +
                        "-fx-border-width: 1; " +
                        "-fx-cursor: hand;"
        );

        cancelBtn.setOnMouseEntered(e ->
                cancelBtn.setStyle(cancelBtn.getStyle() + "-fx-background-color: #e5e7eb;")
        );

        cancelBtn.setOnMouseExited(e ->
                cancelBtn.setStyle(cancelBtn.getStyle() + "-fx-background-color: #f3f4f6;")
        );

        // Validation avant sauvegarde
        saveBtn.addEventFilter(ActionEvent.ACTION, event -> {
            StringBuilder errors = new StringBuilder();

            if (usernameField.getText().trim().isEmpty()) {
                errors.append("• Username is required\n");
                usernameField.setStyle(getTextFieldStyle() + "-fx-border-color: #ef4444;");
            }

            if (emailField.getText().trim().isEmpty()) {
                errors.append("• Email is required\n");
                emailField.setStyle(getTextFieldStyle() + "-fx-border-color: #ef4444;");
            } else if (!isValidEmail(emailField.getText())) {
                errors.append("• Please enter a valid email address\n");
                emailField.setStyle(getTextFieldStyle() + "-fx-border-color: #ef4444;");
            }

            if (errors.length() > 0) {
                showAlert("Validation Error", errors.toString(), Alert.AlertType.WARNING);
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
                        showAlert("Error", "Database connection not available", Alert.AlertType.ERROR);
                        return;
                    }

                    if (userDAO.updateUser(user)) {
                        usersTable.refresh();
                        updateStatistics();
                        showSuccessNotification("✅ User updated successfully!");
                    } else {
                        showAlert("Error", "Failed to update user", Alert.AlertType.ERROR);
                    }
                } catch (SQLException e) {
                    showAlert("Database Error", "Failed to update user: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void deleteUser(User user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete User");
        alert.setHeaderText("Delete " + user.getUsername() + "?");
        alert.setContentText("This action cannot be undone. Are you sure?");

        // Style de l'alerte
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle(
                "-fx-background-color: #ffffff; " +
                        "-fx-border-color: #fca5a5; " +
                        "-fx-border-width: 1; " +
                        "-fx-background-radius: 8;"
        );

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
                        showSuccessNotification("✅ User deleted successfully!");
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
        fileChooser.setInitialFileName("users_export_" + System.currentTimeMillis() + ".csv");

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
                            user.getDateCreation() != null ? user.getDateCreation() : ""));
                }
                showSuccessNotification("✅ Users exported successfully to:\n" + file.getAbsolutePath());
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

        // Style selon le type d'alerte
        DialogPane dialogPane = alert.getDialogPane();
        switch (type) {
            case ERROR:
                dialogPane.setStyle("-fx-background-color: #fef2f2; -fx-border-color: #fecaca;");
                break;
            case WARNING:
                dialogPane.setStyle("-fx-background-color: #fffbeb; -fx-border-color: #fde68a;");
                break;
            case INFORMATION:
                dialogPane.setStyle("-fx-background-color: #f0f9ff; -fx-border-color: #bae6fd;");
                break;
        }

        alert.showAndWait();
    }

    // Méthodes utilitaires pour le style (compatibilité avec editUser)
    private VBox createStyledField(String icon, String labelText) {
        return createFormField(icon, labelText, "");
    }

    private void styleTextField(TextField field) {
        styleFormField(field);
    }
}