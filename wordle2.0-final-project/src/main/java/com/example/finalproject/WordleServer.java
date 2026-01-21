package com.example.finalproject;

import java.io.*;
import java.net.*;
import java.util.*;

public class WordleServer {
    // Server configuration and shared resources
    private static final int PORT = 5001; // Server port for client connections
    private static final Map<String, ClientHandler> clients = new HashMap<>(); // Stores usernames and their handlers
    private static final List<ClientHandler> clientHandlers = new ArrayList<>(); // List of all active clients
    private static final WordManager wordManager = new WordManager(); // Manages word assignment and checking
    private static boolean gameStarted = false; // Prevents multiple timer starts
    private static Thread gameTimerThread; // Global game countdown thread

    public static void main(String[] args) {
        System.out.println("Wordle server started at port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                // Accept incoming client connections
                Socket socket = new Socket();
                socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                new Thread(handler).start(); // Handle each client in its own thread
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Sends a message to all connected clients
    public static synchronized void broadcast(String message) {
        for (ClientHandler handler : clientHandlers) {
            handler.sendMessage(message);
        }
    }

    // Builds and sends a list of connected usernames to all players
    public static synchronized void sendClientList() {
        StringBuilder list = new StringBuilder("Players: ");
        for (ClientHandler client : clientHandlers) {
            list.append(client.username).append(" ");
        }
        broadcast("SERVER: " + list.toString().trim());
    }

    // Verifies if all clients have finished their game
    public static synchronized void checkIfAllFinished() {
        for (ClientHandler client : clientHandlers) {
            if (!client.finished) return; // Someone is still playing
        }
        // Game is done, ready for reset or new round
        resetGame();
    }

    // Analyzes results and broadcasts winner or endgame outcome
    public static synchronized void announceResults() {
        if (gameTimerThread != null && gameTimerThread.isAlive()) {
            gameTimerThread.interrupt(); // Stop timer thread if still running
        }

        StringBuilder results = new StringBuilder("🏁 GAME OVER! Final Results: ");
        ClientHandler winner = null;
        long bestTime = Long.MAX_VALUE;
        int bestAttempts = Integer.MAX_VALUE;

        for (ClientHandler client : clientHandlers) {
            String line = "• " + client.username;

            if (client.guessedCorrectly) {
                long timeTaken = (client.finishTime - client.startTime) / 1000;
                line += " - " + client.attempts + " attempts, " + timeTaken + "s";

                if (client.attempts < bestAttempts ||
                        (client.attempts == bestAttempts && timeTaken < bestTime)) {
                    winner = client;
                    bestTime = timeTaken;
                    bestAttempts = client.attempts;
                }
            } else {
                line += " - ❌ Did not guess the word.";
            }

            results.append(line).append("\n");
        }

        if (winner != null) {
            results.insert(0, "🏆 WINNER: " + winner.username + " guessed the word " + clientHandlers.get(0).assignedWord + " in " + bestAttempts + " attempts and " + bestTime + "s\n\n");
        } else {
            results.insert(0, "😢 No one guessed the word correctly. The correct word was: " + clientHandlers.get(0).assignedWord + "\n\n");
        }

        broadcast("SERVER: " + results.toString());
    }

    // Clears game state so a new round can start
    public static synchronized void resetGame() {
        clientHandlers.clear();
        wordManager.resetWord();
        gameStarted = false;
    }

    // Starts a timer that auto-triggers result announcement after 2 minutes
    public static synchronized void startGameTimer() {
        gameTimerThread = new Thread(() -> {
            try {
                Thread.sleep(120000); // 2 minutes
                announceResults();
            } catch (InterruptedException e) {
                System.out.println("🛑 Game timer stopped early.");
            }
        });
        gameTimerThread.start();
    }

    // Class to manage individual player interactions and game state
    static class ClientHandler implements Runnable {
        private final Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String username;
        private String assignedWord;
        private int attempts = 0;
        private boolean finished = false;
        private boolean guessedCorrectly = false;
        private long startTime;
        private long finishTime;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        // Send message to the connected client
        public void sendMessage(String message) {
            out.println(message);
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                username = in.readLine(); // Get player name
                synchronized (clients) {
                    clients.put(username, this);
                }

                assignedWord = wordManager.getCurrentWord(); // Shared word for all clients
                System.out.println("[DEBUG] Assigned word for " + username + ": " + assignedWord);

                synchronized (clientHandlers) {
                    clientHandlers.add(this);

                    // Start game timer only once when both players join
                    if (clientHandlers.size() == 2 && !gameStarted) {
                        gameStarted = true;
                        startGameTimer();
                    }
                }

                startTime = System.currentTimeMillis(); // Begin player timer
                broadcast(username + " has joined the game!");
                sendClientList();

                // Main game loop
                while (true) {
                    String guess = in.readLine();
                    if (guess == null || finished) break;

                    if (guess.equals("TIMED_OUT")) {
                        finished = true;
                        out.println("FEEDBACK: Time's up! The word was: " + assignedWord);
                        checkIfAllFinished();
                        announceResults();
                        break;
                    }

                    // Word validation
                    if (!wordManager.isValidWord(guess)) {
                        out.println("FEEDBACK: Invalid Guess! Word not in list!");
                        continue;
                    }

                    // Feedback logic
                    attempts++;
                    String feedback = wordManager.checkGuess(assignedWord,guess);
                    out.println("FEEDBACK: " + feedback);

                    if (feedback.equals("GGGGG")) {
                        guessedCorrectly = true;
                        finished = true;
                        finishTime = System.currentTimeMillis();
                        out.println("FEEDBACK: You guessed it in " + attempts + " tries! The word was: " + assignedWord);
                        announceResults();
                        checkIfAllFinished();
                    } else if (attempts == 6) {
                        finished = true;
                        out.println("FEEDBACK:  Out of attempts! The word was: " + assignedWord);
                        checkIfAllFinished();
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        }
    }
}
