package com.ensah.qoe.Controller;

import com.ensah.qoe.Services.PredictionServiceAnomalies;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class AnomalyDashboardController {

    // ===== MENU =====
    @FXML private MenuItem menuTrain;
    @FXML private MenuItem menuPredict;
    @FXML private MenuItem menuEvaluate;

    // ===== CONTAINER CENTRAL =====
    @FXML private StackPane contentPane;

    // ===== PANELS =====
    @FXML private VBox trainPane;
    @FXML private VBox predictPane;
    @FXML private VBox evaluatePane;

    // ===== TRAINING =====
    @FXML private TextArea trainingLog;

    // ===== PREDICTION =====
    @FXML private TextField latField, jitField, lossField, bwField, sigField;
    @FXML private Label predictionLabel, confidenceLabel;

    // ===== EVALUATION =====
    @FXML private Label accLabel, precLabel, recallLabel, f1Label;
    @FXML private TextArea evalLog;

    @FXML
    public void initialize() {
        showTrain();
    }

    // ================= NAVIGATION =================

    @FXML
    private void showTrain() {
        trainPane.setVisible(true);
        trainPane.setManaged(true);

        predictPane.setVisible(false);
        predictPane.setManaged(false);

        evaluatePane.setVisible(false);
        evaluatePane.setManaged(false);

        contentPane.getChildren().setAll(trainPane);
    }

    @FXML
    private void showPredict() {
        trainPane.setVisible(false);
        trainPane.setManaged(false);

        predictPane.setVisible(true);
        predictPane.setManaged(true);

        evaluatePane.setVisible(false);
        evaluatePane.setManaged(false);

        contentPane.getChildren().setAll(predictPane);
    }

    @FXML
    private void showEvaluate() {
        trainPane.setVisible(false);
        trainPane.setManaged(false);

        predictPane.setVisible(false);
        predictPane.setManaged(false);

        evaluatePane.setVisible(true);
        evaluatePane.setManaged(true);

        contentPane.getChildren().setAll(evaluatePane);
    }


    // ================= TRAIN =================
    @FXML
    private void handleTrain() {

        trainingLog.clear();

        new Thread(() -> {
            try {
                String report = PredictionServiceAnomalies.trainModel();

                double acc  = PredictionServiceAnomalies.getLastAccuracy();
                double prec = PredictionServiceAnomalies.getLastPrecision();
                double rec  = PredictionServiceAnomalies.getLastRecall();

                double f1 = (prec + rec == 0) ? 0 : (2 * prec * rec) / (prec + rec);

                Platform.runLater(() -> {
                    trainingLog.setText(report);

                    // Mise à jour immédiate des métriques dans l’onglet Évaluation
                    accLabel.setText(String.format("%.2f %%", acc));
                    precLabel.setText(String.format("%.3f", prec));
                    recallLabel.setText(String.format("%.3f", rec));
                    f1Label.setText(String.format("%.3f", f1));
                });

            } catch (Exception e) {
                Platform.runLater(() ->
                        trainingLog.setText("❌ Erreur entraînement : " + e.getMessage())
                );
            }
        }).start();
    }

    // ================= PREDICT =================
    @FXML
    private void handlePredict() {

        if (!PredictionServiceAnomalies.isModelReady()) {
            predictionLabel.setText("Modèle non entraîné");
            predictionLabel.setStyle("-fx-text-fill:#e67e22;");
            return;
        }

        try {
            double lat  = Double.parseDouble(latField.getText());
            double jit  = Double.parseDouble(jitField.getText());
            double loss = Double.parseDouble(lossField.getText());
            double bw   = Double.parseDouble(bwField.getText());
            double sig  = Double.parseDouble(sigField.getText());

            var res = PredictionServiceAnomalies.predictAnomaly(lat, jit, loss, bw, sig);

            predictionLabel.setText(res.getPrediction());
            confidenceLabel.setText(
                    String.format("%.1f %%", res.getAnomalyProbability() * 100)
            );

            predictionLabel.setStyle(
                    res.getPrediction().equals("ANOMALIE")
                            ? "-fx-text-fill:#e74c3c; -fx-font-weight:bold;"
                            : "-fx-text-fill:#27ae60; -fx-font-weight:bold;"
            );

        } catch (NumberFormatException e) {
            predictionLabel.setText("Valeurs invalides");
            predictionLabel.setStyle("-fx-text-fill:#e74c3c;");
        }
    }

    // ================= EVALUATE =================
    @FXML
    private void handleEvaluate() {

        if (!PredictionServiceAnomalies.isModelReady()) {
            evalLog.setText("❌ Modèle non entraîné");
            return;
        }

        evalLog.setText(PredictionServiceAnomalies.evaluateModel());

        double acc  = PredictionServiceAnomalies.getLastAccuracy();
        double prec = PredictionServiceAnomalies.getLastPrecision();
        double rec  = PredictionServiceAnomalies.getLastRecall();
        double f1   = (prec + rec == 0) ? 0 : (2 * prec * rec) / (prec + rec);

        accLabel.setText(String.format("%.2f %%", acc));
        precLabel.setText(String.format("%.3f", prec));
        recallLabel.setText(String.format("%.3f", rec));
        f1Label.setText(String.format("%.3f", f1));
    }
}
