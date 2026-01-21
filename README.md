# Wordle 2.0


**A modern twist on the classic word-guessing game**  

A Java-based GUI implementation of the classic word-guessing game where players have six attempts to guess a hidden five-letter word. The application provides real-time feedback on letter placement through a color-coded system and features a clean, intuitive interface. Built using JavaFX, the game maintains the core Wordle experience while offering responsive gameplay and visual feedback. 


## Contributers
This project was originally developed as part of a university group assignment.

**Contributers:**
- Jolly Soni
- Jia D'Souza
- Devishi Soni
- Eve Murphy-Beaudet
- Matthew Espinoza

**My Contributions:**
- UI design & layout
- UI and backend integration (ending the game)


---

## Technologies Used
- **Java**:To create the overall program.
- **JFX**: For the graphical user interface (GUI).
- **Git**: For version control.
- **Maven**: For the graphical user interface (GUI).

## How to Run  

### Prerequisites  
- Java Development Kit (JDK) 8 or higher.
- Git (optional, for cloning the repository.
- Maven 3.8+, for running the program.

### Steps
1. **Clone the repository**:  
   ```bash  
   git clone https://github.com/OntarioTech-CS-program/w25-csci2020u-finalproject-w25-team25.git 
   cd w25-csci2020u-finalproject-w25-team25
2. **Compile and run:**
    ```bash
    javac src/*.java  
    java src/Main
  Or import into an IDE (e.g., IntelliJ/Eclipse) and run `WordleServer.java` and then the `WordleClient.java`

3. **Play:**
- Type a 5-letter word and press `Enter`
- Repeat until you guess the word, run out of time or run out of attempts!

---

## Future Improvements

### With more time, our team would make the following improvements to our program:
- **Further Debugging**: Debug the program more thorouhgly to eliminate any unique errors and bugs.
- **Added Functionalities**: Implement more functionailities such as a scoreboard to track player scores, statistics tracking, a larger word bank to choose more, customizable themes and the ability to play with more players.
- **API Usage**: Use an API as a resource for the word bank instead of a pre-made text file.

---


## Video Demo
Here is a demo video of the app running: [Video demo](https://github.com/user-attachments/assets/753b65dc-c73e-4a7e-9a40-23e79a4b5cc3)

## Screenshots 

<div align="center">
  <img src="outOfAttempts.png" />
   The player is notified with a dialogue when they run out of attempts.
</div>

<div align="center">
  <img src="timeUp.png" />
   The player is notified with a dialogue when the time is up and the game is over.
</div>



## Resources

- **JavaFX**: https://openjfx.io/
- **Maven**: https://maven.apache.org/guides/index.html
