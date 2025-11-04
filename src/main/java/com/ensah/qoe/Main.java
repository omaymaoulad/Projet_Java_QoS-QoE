import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import Models.DBConnection;


public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);

            String css = getClass().getResource("/css/style.css").toExternalForm();
            scene.getStylesheets().add(css);

            primaryStage.setTitle("Connexion - Mon Application");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);

            try {
                Image icon = new Image(getClass().getResourceAsStream("/images/logo.png"));
                primaryStage.getIcons().add(icon);
            } catch (Exception e) {
                System.out.println("⚠️ Logo non trouvé");
            }

            primaryStage.show();
            primaryStage.centerOnScreen();

            System.out.println("✅ Application démarrée avec succès !");

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement de l'application");
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        DBConnection.closeConnection();
        System.out.println("👋 Application fermée");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
