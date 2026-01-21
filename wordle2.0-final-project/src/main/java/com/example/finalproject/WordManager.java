package com.example.finalproject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WordManager {
    private final List<String> words;
    private int currentIndex;

    public WordManager() {
        words = new ArrayList<>();
        loadWords(); // Load from file
        currentIndex = 0;
    }
    private void loadWords() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getClass().getResourceAsStream("/com/example/finalproject/words.txt")))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() == 5) {
                    words.add(line.trim().toUpperCase());
                }
            }

            if (words.isEmpty()) {
                System.err.println(" No words loaded from words.txt!");
            }

        } catch (Exception e) {
            System.err.println(" Error loading words.txt");
            e.printStackTrace();
        }
    }
    // Check if the word has only alphabetic characters and the right length
    boolean isValidWord(String word) {
        // Check if the word contains only alphabetic characters
        if (!word.matches("[a-zA-Z]+")) {
            return false;  // Invalid if it contains non-alphabetic characters
        }

        // Check if the word is of expected length (e.g., 5 characters for a 5-letter word game)
        if (word.length() != 5) {
            return false;  // Invalid if it doesn't match the required length
        }

        return words.contains(word.toUpperCase());
    }


    public synchronized String assignWord() {
        int randomNum = (int)(Math.random() * words.size());
        return words.get(randomNum);
    }


    public synchronized String checkGuess(String word, String guess) {
        if (word.length() != guess.length()) {
            return "Invalid guess length!";
        }

        StringBuilder result = new StringBuilder();
        boolean[] wordUsed = new boolean[word.length()]; // Track letters already used in the word
        boolean[] guessUsed = new boolean[guess.length()]; // Track letters already used in the guess

        // First step
        for (int i = 0; i < word.length(); i++) {
            if (word.charAt(i) == guess.charAt(i)) {
                result.append("G"); // Correct letter in correct place
                wordUsed[i] = true; // Mark letter in word as used
                guessUsed[i] = true; // Mark letter in guess as used
            } else {
                result.append("X"); // Incorrect letter
            }
        }

        // Second step
        for (int i = 0; i < word.length(); i++) {
            if (result.charAt(i) == 'X' && !guessUsed[i]) { // If it's not already green and not used in guess
                for (int j = 0; j < word.length(); j++) {
                    if (!wordUsed[j] && word.charAt(j) == guess.charAt(i)) {
                        result.setCharAt(i, 'Y'); // Mark as yellow
                        wordUsed[j] = true; // Mark the word letter as used
                        guessUsed[i] = true; // Mark the guess letter as used
                        break;
                    }
                }
            }
        }


        return result.toString();
    }

    private String currentWord = "";

    public synchronized String getCurrentWord() {
        if (currentWord.isEmpty()) {
            currentWord = assignWord(); // first time only
        }
        return currentWord;
    }

    public void resetWord() {
        currentWord = "";
    }
}
