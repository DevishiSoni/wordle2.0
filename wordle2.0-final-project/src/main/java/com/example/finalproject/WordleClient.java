package com.example.finalproject;
// Main JavaFX application class for WordleClient
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Timer;
import java.util.TimerTask;


import static com.example.finalproject.WordleServer.announceResults;
import static com.example.finalproject.WordleServer.broadcast;

public class WordleClient extends Application {
    private GridPane guessGrid;
    private Label[][] gridTiles = new Label[6][5];
    private Label timerLabel;
    private PrintWriter out;
    private BufferedReader in;
    private String lastGuess = "";
    private String username;
    private int attemptsMade = 0;
    private final int maxAttempts = 6;
    private int currentRow = 0;
    private Timer gameTimer;
    private Label connectedPlayersLabel;
    private boolean gameStarted = false;
    private boolean timerStarted = false;
    private int currentCol = 0;  // Track current column
    private int currentActiveRow = 0;  // Track current active row
    private Button submitButton;

    @Override
    public void start(Stage primaryStage) {
        // Show instructions FIRST (before username prompt)
        showInstructions();

        // Then proceed with normal setup
        connectedPlayersLabel = new Label("Waiting for other player...");
        username = promptUsername();
        primaryStage.setTitle("Multiplayer Wordle - " + username);

        timerLabel = new Label("Time left: 02:00");

        guessGrid = new GridPane();
        guessGrid.setAlignment(Pos.CENTER);
        guessGrid.setHgap(5);
        guessGrid.setVgap(5);

        // Initialize grid tiles
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 5; col++) {
                Label tile = createTile("");
                gridTiles[row][col] = tile;
                guessGrid.add(tile, col, row);
            }
        }

        submitButton = new Button("Submit Guess");
        submitButton.setDisable(true);
        submitButton.setOnAction(e -> submitGuess());

        HBox legend = createLegend();  // Keep the color legend

        Label playerLabel = new Label("You are: " + username);

        // Updated layout without help button
        VBox layout = new VBox(15, connectedPlayersLabel, playerLabel, timerLabel,
                guessGrid, legend, submitButton);  // Only submit button now
        layout.setAlignment(Pos.CENTER);
        layout.setMinWidth(300);

        Scene scene = new Scene(layout, 400, 500);
        primaryStage.setScene(scene);
        primaryStage.show();
        setupGridInputHandling();
        connectToServer(username, submitButton, primaryStage);
    }
    // Show game instructions popup
    private void showInstructions() {
        Alert instructions = new Alert(Alert.AlertType.INFORMATION);
        instructions.setTitle("How to Play Wordle");
        instructions.setHeaderText("Game Instructions");

        String content = """
        1. TYPE directly into the tiles or CLICK to select a tile
        2. Use BACKSPACE to delete letters
        3. Submit with ENTER key or SUBMIT button
        4. Color hints:
           GREEN = Correct letter & position
           YELLOW = Correct letter, wrong position
           GRAY = Letter not in word
        5. You have 6 tries and 2 minutes!""";

        instructions.setContentText(content);
        instructions.showAndWait();  // This will block until user clicks OK
    }
    // Create legend for tile color meanings
    private HBox createLegend() {
        Label greenLabel = new Label("Correct position");
        Label yellowLabel = new Label("Wrong position");
        Label grayLabel = new Label("Not in word");

        Label greenBox = createTile("");
        greenBox.setStyle("-fx-background-color: green; -fx-border-color: black;");
        Label yellowBox = createTile("");
        yellowBox.setStyle("-fx-background-color: gold; -fx-border-color: black;");
        Label grayBox = createTile("");
        grayBox.setStyle("-fx-background-color: lightgray; -fx-border-color: black;");

        HBox legend = new HBox(10,
                greenBox, greenLabel,
                yellowBox, yellowLabel,
                grayBox, grayLabel
        );
        legend.setAlignment(Pos.CENTER);
        return legend;
    }
    // Handle keyboard navigation and input inside the grid
    private void setupGridInputHandling() {
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 5; col++) {
                Label tile = gridTiles[row][col];
                tile.setFocusTraversable(true);

                final int currentRow = row;
                final int currentCol = col;

                tile.setOnKeyPressed(event -> handleGridKeyPress(event, currentRow, currentCol));
                tile.setOnMouseClicked(event -> {
                    this.currentActiveRow = currentRow;
                    this.currentCol = currentCol;
                    tile.requestFocus();
                    updateSubmitButtonState();
                });
            }
        }
    }

    // Manage keyboard events for grid letters
    private void handleGridKeyPress(KeyEvent event, int row, int col) {
        Label tile = gridTiles[row][col];

        if (event.getCode().isLetterKey()) {
            tile.setText(event.getText().toUpperCase());
            if (col < 4) {
                currentCol++;
                gridTiles[row][currentCol].requestFocus();
            }
            updateSubmitButtonState();
        }
        else if (event.getCode() == KeyCode.BACK_SPACE) {
            if (!tile.getText().isEmpty()) {
                // Delete current letter if cell isn't empty
                tile.setText("");
            } else if (col > 0) {
                // Move left if current cell is already empty
                currentCol--;
                gridTiles[row][currentCol].requestFocus();
            }
            updateSubmitButtonState();
        }
        else if (event.getCode() == KeyCode.ENTER) {
            if (!submitButton.isDisabled()) {
                submitGuess();
            }
        }
    }

    // Enable submit only when a row is complete
    private void updateSubmitButtonState() {
        boolean rowComplete = true;
        for (int c = 0; c < 5; c++) {
            if (gridTiles[currentActiveRow][c].getText().isEmpty()) {
                rowComplete = false;
                break;
            }
        }
        submitButton.setDisable(!rowComplete);
    }

    // Collect letters from current row and send to server
    private void submitGuess() {
        // Only allow submission if the current row is complete
        boolean rowComplete = true;
        for (int c = 0; c < 5; c++) {
            if (gridTiles[currentActiveRow][c].getText().isEmpty()) {
                rowComplete = false;
                break;
            }
        }

        if (rowComplete) {
            StringBuilder guess = new StringBuilder();
            for (int c = 0; c < 5; c++) {
                guess.append(gridTiles[currentActiveRow][c].getText());
            }

            lastGuess = guess.toString();
            out.println(lastGuess);
            submitButton.setDisable(true); // Disable until server responds

            // Visual feedback
            for (int c = 0; c < 5; c++) {
                gridTiles[currentActiveRow][c].setStyle(
                        gridTiles[currentActiveRow][c].getStyle() +
                                "-fx-border-color: blue;"
                );
            }
        }
    }
    // Connect to WordleServer and initialize communication
    private void connectToServer(String username, Button submitButton, Stage primaryStage) {
        try {
            Socket socket = new Socket("localhost", 5001);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println(username);
            new Thread(() -> receiveMessages(submitButton, primaryStage)).start();
        } catch (IOException e) {
            showAlert("Error", "Could not connect to the server.");
            System.exit(1);
        }

    }

    private String promptUsername() {
        TextInputDialog dialog = new TextInputDialog("Player");
        dialog.setTitle("Username");
        dialog.setHeaderText("Enter your username:");
        return dialog.showAndWait().orElse("Player");
    }


    // Handle incoming messages from the server and update UI accordingly
    private void receiveMessages(Button submitButton, Stage mainStage) {
        try {
            String message;
            final Button finalSubmitButton = submitButton;
            final Stage finalMainStage = mainStage;
            while ((message = in.readLine()) != null) {
                String finalMessage = message;

                Platform.runLater(() -> {
                    if (finalMessage.startsWith("FEEDBACK: ")) {
                        String feedback = finalMessage.substring(9).trim();


                        if (feedback.toLowerCase().contains("you guessed it")) {
                            endGame("WIN", "Congratulations! You guessed the word!", null);


                        }
                        else if (feedback.toLowerCase().contains("the word was:")) {
                            String[] parts = feedback.toLowerCase().split("the word was:");
                            String correctWord = parts.length > 1 ? parts[1].trim().toUpperCase() : "[UNKNOWN]";
                            if (feedback.toLowerCase().contains("time's up!")) {
                                endGame("TIME_UP", "Time ran out!", correctWord);
                            }
                            else if (feedback.toLowerCase().contains("out of attempts!")) {
                                endGame("ATTEMPTS_EXHAUSTED", "You used all your tries!", correctWord);
                            }

                            finalSubmitButton.setDisable(true);
                            if (gameTimer != null) gameTimer.cancel();
                        }
                        else if (feedback.toLowerCase().contains("invalid guess")) {
                            showAlert("Invalid Guess", "Word not in list!");
                            // Clear current row and reset focus
                            for (int col = 0; col < 5; col++) {
                                gridTiles[currentRow][col].setText("");
                            }
                            currentCol = 0;
                            gridTiles[currentRow][currentCol].requestFocus();

                        }
                        else {
                            updateRow(currentRow, lastGuess, feedback);
                            currentRow++;
                            currentActiveRow = currentRow;  // Keep these in sync
                            currentCol = 0;
                            if (currentRow < 6) {
                                gridTiles[currentRow][currentCol].requestFocus();
                            }
                        }

                    } else if (finalMessage.startsWith("SERVER: ")) {
                        String serverMsg = finalMessage.substring(8);
                        if (serverMsg.contains("has joined the game!")) {
                            connectedPlayersLabel.setText("🔗 " + serverMsg);
                        } else if (serverMsg.contains("Players: ")) {
                            connectedPlayersLabel.setText("👥 " + serverMsg);

                            if (!gameStarted && serverMsg.trim().split(" ").length >= 3) {
                                gameStarted = true;

                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                alert.setTitle("✅ Game Starting!");
                                alert.setHeaderText(null);
                                alert.setContentText("Both players are connected. Let's go!");

                                Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
                                alertStage.initOwner(mainStage);
                                alertStage.setAlwaysOnTop(true);
                                alertStage.toFront();

                                alert.showAndWait();

                                if (!timerStarted) {
                                    submitButton.setDisable(false);
                                    currentActiveRow = 0;
                                    currentCol = 0;
                                    gridTiles[currentActiveRow][currentCol].requestFocus();

                                    // Reset timer values before starting
                                    timerLabel.setText("Time left: 02:00");
                                    startTimer();  // This will initialize a new timer
                                    timerStarted = true;
                                }
                            }
                        } else if (serverMsg.contains("WINNER:")) {
                            String correctWord = serverMsg.contains("WORD:") ?
                                    serverMsg.split("WORD:")[1].trim().toUpperCase() : null;
                            endGame("SERVER_WIN", serverMsg.replace("WINNER:", "Result:"), correctWord);


                        }
                    }
                });
            }
        } catch (IOException e) {
            Platform.runLater(() -> showAlert("Error", "Connection lost."));
        }
    }

    // Start 2-minute game timer with visual warning under 30s
    private void startTimer() {
        // Cancel existing timer if running
        if (gameTimer != null) {
            gameTimer.cancel();
            gameTimer.purge();
        }

        final int[] timeLeft = {120};
        gameTimer = new Timer(true); // Use daemon thread

        gameTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    int minutes = timeLeft[0] / 60;
                    int seconds = timeLeft[0] % 60;
                    timerLabel.setText(String.format("Time left: %02d:%02d", minutes, seconds));

                    if (timeLeft[0] <= 0) {
                        endGame("Time's Up!", "Game over!",null);
                        out.println("TIMED_OUT");
                        this.cancel();
                    }
                    // Flash red when time is running low
                    else if (timeLeft[0] <= 30) {
                        timerLabel.setStyle("-fx-text-fill: red;");
                    }
                    timeLeft[0]--;
                });
            }
        }, 0, 1000);
    }

    // Disable all grid tiles at game end
    private void disableAllTiles() {
        for (Label[] row : gridTiles) {
            for (Label tile : row) {
                tile.setDisable(true);
                tile.setStyle(tile.getStyle() + "-fx-opacity: 0.7;");
            }
        }
    }
    // the grid making
    private Label createTile(String letter) {
        Label tile = new Label(letter);
        tile.setPrefSize(50, 50);
        tile.setFont(Font.font("Arial", 24));
        tile.setAlignment(Pos.CENTER);
        tile.setStyle("-fx-border-color: black; -fx-border-width: 2px; -fx-background-color: white;");
        return tile;
    }

    // Populate current row with colors based on feedback
    private void updateRow(int rowIndex, String guess, String feedback) {
        for (int col = 0; col < 5; col++) {
            Label tile = gridTiles[rowIndex][col];
            tile.setText(String.valueOf(guess.charAt(col)));

            switch (feedback.charAt(col)) {
                case 'G':
                    tile.setStyle("-fx-background-color: green; -fx-border-color: black; -fx-text-fill: white;");
                    break;
                case 'Y':
                    tile.setStyle("-fx-background-color: gold; -fx-border-color: black; -fx-text-fill: black;");
                    break;
                default:
                    tile.setStyle("-fx-background-color: lightgray; -fx-border-color: black; -fx-text-fill: black;");
                    break;
            }
        }
    }
    // Show popup alert messages
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();

    }

    // Handles ending the game visually and logically
    private void endGame(String resultType, String details, String correctWord) {
        StringBuilder detailsBuilder = new StringBuilder(details);

        Platform.runLater(() -> {
            // Clean up game state
            if (gameTimer != null) {
                gameTimer.cancel();
                gameTimer.purge();
            }
            submitButton.setDisable(true);
            disableAllTiles();

            // Build the single popup
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game Over");

            // Set appropriate emoji and header
            switch (resultType) {
                case "WIN":
                    alert.setHeaderText("🎉 You Won! 🎉");
                    break;
                case "TIME_UP":
                    alert.setHeaderText("⏰ Time's Up! ⏰");
                    detailsBuilder.append("\nThe word was: ").append(correctWord);
                    break;
                case "ATTEMPTS_EXHAUSTED":
                    alert.setHeaderText("❌ Out of Attempts");
                    detailsBuilder.append("\nThe word was: ").append(correctWord);
                    break;
                case "SERVER_WIN":
                    alert.setHeaderText("🏆 Game Over 🏆");
                    if (correctWord != null) {
                        detailsBuilder.append("\nThe word was: ").append(correctWord);
                    }
                    break;
            }

            alert.setContentText(detailsBuilder.toString());
            alert.showAndWait();
        });
    }


    public static void main(String[] args) {
        launch(args);
    }
}