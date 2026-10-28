package groupDGuessWho;

import java.util.*;

class Character {
    String name;
    String gender;
    String eyes;
    String hair;
    String beard;
    String moustache;
    String bigNose;
    String glasses;
    String hat;

    public Character(String name, String gender, String eyes, String hair, String beard, String moustache, String bigNose, String glasses, String hat) {
        this.name = name;
        this.gender = gender;
        this.eyes = eyes;
        this.hair = hair;
        this.beard = beard;
        this.moustache = moustache;
        this.bigNose = bigNose;
        this.glasses = glasses;
        this.hat = hat;
    }

    @Override
    public String toString() {
        return name;
    }
}

public class CharacterGuessingGame {
    private static final List<Character> characters = Arrays.asList(
        new Character("AD01", "Male", "Blue", "Light brown", "No", "Yes", "No", "No", "No"),
        new Character("AX02", "Male", "Brown", "Black", "No", "Yes", "No", "No", "No"),
        new Character("AY03", "Female", "Brown", "Highlights", "No", "No", "No", "Yes", "No"),
        new Character("AA04", "Female", "Blue", "Blonde", "No", "No", "No", "No", "No"),
        new Character("AE05", "Female", "Brown", "Black", "No", "No", "Yes", "No", "No"),
        new Character("BN06", "Female", "Brown", "Dark brown", "No", "No", "Yes", "Yes", "No"),
        new Character("BD07", "Male", "Brown", "Dark brown", "No", "No", "Yes", "No", "Yes"),
        new Character("BY08", "Female", "Blue", "White", "No", "No", "No", "Yes", "No"),
        new Character("BL09", "Male", "Brown", "Light brown", "Yes", "No", "No", "No", "No"),
        new Character("CN10", "Female", "Brown", "White", "No", "No", "No", "No", "No"),
        new Character("CS11", "Male", "Brown", "Blonde", "No", "Yes", "No", "No", "No"),
        new Character("CE12", "Female", "Brown", "Light brown", "No", "No", "No", "Yes", "Yes"),
        new Character("DL13", "Male", "Green", "Light brown", "Yes", "Yes", "Yes", "No", "No"),
        new Character("DD14", "Male", "Brown", "Blonde", "Yes", "No", "No", "No", "Yes"),
        new Character("EA15", "Female", "Brown", "Light brown", "No", "No", "No", "No", "No"),
        new Character("EC16", "Male", "Brown", "Blonde", "No", "No", "No", "No", "Yes"),
        new Character("FH17", "Male", "Blue", "Black", "No", "No", "No", "No", "No"),
        new Character("FS18", "Male", "Brown", "Light brown", "No", "No", "No", "No", "No"),
        new Character("GE19", "Male", "Brown", "Black", "No", "No", "No", "No", "No"),
        new Character("GE20", "Male", "Brown", "White", "No", "No", "No", "No", "Yes"),
        new Character("HN21", "Male", "Brown", "Light brown", "No", "Yes", "No", "No", "No"),
        new Character("HY22", "Female", "Brown", "Dark brown", "No", "No", "No", "No", "No"),
        new Character("JE23", " Male", "Brown", "Blonde ", "Yes", "Yes", "No", "No", "No"),
        new Character("RC24", "Male", "Green", "Highlights", "No", "Yes", "No", "No", "No"),
        new Character("KE25", "Female", "Blue", "Blonde", "No", "No", "No", "No", "No"),
        new Character("LA26", "Female", "Green", "Black", "No", "No", "No", "No", "Yes"),
        new Character("LO27", "Male", "Brown", "White", "No", "Yes", "No", "No", "No"),
        new Character("LY28", "Female", "Green", "Dark brown", "No", "No", "No", "No", "Yes"),
        new Character("LZ29", "Female", "Blue", "White", "No", "No", "Yes", "No", "No"),
        new Character("MA30", "Female", "Brown", "Dark brown", "No", "No", "No", "No", "Yes"),
        new Character("MX31", "Male", "Brown", "Dark brown", "No", "Yes", "Yes", "No", "No"),
        new Character("MA32", "Female", "Brown", "Black", "No", "No", "No", "No", "No"),
        new Character("ME33", "Male", "Brown", "Black", "No", "No", "No", "No", "Yes"),
        new Character("NK34", "Male", "Brown", "Blonde", "No", "Yes", "No", "No", "No"),
        new Character("OA35", "Female", "Brown", "Highlights", "No", "No", "No", "No", "No"),
        new Character("PL36", "Male", "Brown", "White", "No", "No", "Yes", "No", "No"),
        new Character("PR37", "Male", "Blue", "White", "No", "Yes", "No", "No", "No"),
        new Character("PP38", "Male", "Brown", "Black", "Yes", "No", "No", "No", "No"),
        new Character("RL39", "Female", "Blue", "Dark brown", "No", "No", "Yes", "No", "No"),
        new Character("RD40", "Male", "Brown", "Dark brown", "Yes", "Yes", "No", "No", "No"),
        new Character("RT41", "Male", "Blue", "Dark brown", "No", "Yes", "No", "No", "No"),
        new Character("SY42", "Female", "Brown", "Black", "No", "No", "No", "No", "No"),
        new Character("SM43", "Male", "Brown", "White", "No", "No", "Yes", "No", "No"),
        new Character("SA44", "Female", "Green", "Dark brown", "No", "No", "No", "No", "No"),
        new Character("SN45", "Female", "Brown", "White", "No", "No", "No", "No", "No"),
        new Character("TM46", "Male", "Blue", "Black", "No", "No", "Yes", "No", "No"),
        new Character("VR47", "Male", "Brown", "White", "No", "No", "No", "No", "No")
    );

    private static final List<String> questions = Arrays.asList(
        "Does your character have a beard?",
        "Does your character have a moustache?",
        "Does your character have a big nose?",
        "Does your character wear glasses?",
        "Does your character wear a hat?",
        "Is your character male?",
        "Does your character have blue eyes?",
        "Does your character have brown eyes?",
        "Does your character have green eyes?",
        "Does your character have blonde hair?",
        "Does your character have black hair?",
        "Does your character have brown hair?",
        "Does your character have dark brown hair?",
        "Does your character have highlights?",
        "Does your character have white hair?"
    );

    private static final Random random = new Random();
    private Character aiCharacter;
    private Character playerCharacter;
    private int playerScore = 0;
    private int aiScore = 0;
    private List<String> incorrectAnswers = new ArrayList<>();

    public void startGame() {
        List<Character> board = new ArrayList<>(characters);
        Collections.shuffle(board);
        board = board.subList(0, 24);
        aiCharacter = board.get(random.nextInt(board.size()));

        System.out.println("Choose your character from the following list:");
        for (int i = 0; i < board.size(); i++) {
            System.out.println((i + 1) + ". " + board.get(i).name);
        }

        Scanner scanner = new Scanner(System.in);
        int playerChoice = scanner.nextInt() - 1;
        playerCharacter = board.get(playerChoice); 
        System.out.println("You have selected: " + playerCharacter.name);
        System.out.println("AI has selected a character. Start asking questions!");

        while (playerScore < 5 && aiScore < 5) {
            String playerQuestion = getPlayerQuestion(scanner);
            boolean playerAnswer = askQuestion(playerQuestion, aiCharacter);
            System.out.println("AI's answer: " + (playerAnswer ? "Yes" : "No"));
            board.removeIf(character -> !matches(character, playerQuestion, playerAnswer));

            if (askToGuess(scanner)) {
                if (makeGuess(scanner, aiCharacter)) {
                    System.out.println("Correct! You win this round!");
                    playerScore++;
                    break;
                } else {
                    System.out.println("Wrong guess! AI wins this round.");
                    aiScore++;
                    break;
                }
            }

            if (board.size() == 1) {
                System.out.println("AI thinks your character is: " + board.get(0).name);
                System.out.print("Is this correct? (yes/no): ");
                String response = scanner.nextLine().trim().toLowerCase();
                if (response.equals("yes")) {
                    System.out.println("AI wins this round!");
                    aiScore++;
                } else {
                    System.out.print("Please identify your character: ");
                    String playerGuess = scanner.nextLine().trim();
                    if (playerGuess.equals(aiCharacter.name)) {
                        System.out.println("Correct! You win this round!");
                        playerScore++;
                    } else {
                        System.out.println("Wrong guess! AI wins this round.");
                        aiScore++;
                    }
                    displayIncorrectAnswers(); 
                }
                break; 
            }

            String aiQuestion = questions.get(random.nextInt(questions.size()));
            boolean aiAnswer = askQuestion(aiQuestion, playerCharacter);
            System.out.println("AI asks: " + aiQuestion + " - Your answer (yes/no): ");
            String playerResponse = scanner.nextLine().trim().toLowerCase();
            boolean isCorrect = playerResponse.equals("yes") == aiAnswer;
            if (!isCorrect) {
                System.out.println("AI noted an incorrect answer.");
                incorrectAnswers.add(aiQuestion); 
            }
            board.removeIf(character -> !matches(character, aiQuestion, aiAnswer));

            if (board.size() == 1) {
                System.out.println("AI thinks your character is: " + board.get(0).name);
                System.out.print("Is this correct? (yes/no): ");
                String response = scanner.nextLine().trim().toLowerCase();
                if (response.equals("yes")) {
                    System.out.println("AI wins this round!");
                    aiScore++;
                } else {
                    System.out.print("Please identify your character: ");
                    String playerGuess = scanner.nextLine().trim();
                    if (playerGuess.equals(aiCharacter.name)) {
                        System.out.println("Correct! You win this round!");
                        playerScore++;
                    } else {
                        System.out.println("Wrong guess! AI wins this round.");
                        aiScore++;
                    }
                    displayIncorrectAnswers(); 
                }
                break;
            }
        }

        System.out.println("Game Over! Final Score - Player: " + playerScore + ", AI: " + aiScore);
    }

    private void displayIncorrectAnswers() {
        if (!incorrectAnswers.isEmpty()) {
            System.out.println("You answered the following questions incorrectly:");
            for (String question : incorrectAnswers) {
                System.out.println("- " + question);
            }
        } else {
            System.out.println("You answered all questions correctly!");
        }
    }

    private String getPlayerQuestion(Scanner scanner) {
        System.out.println("Choose a question from the list:");
        for (int i = 0; i < questions.size(); i++) {
            System.out.println((i + 1) + ". " + questions.get(i));
        }
        int choice = scanner.nextInt() - 1;
        scanner.nextLine(); 
        return questions.get(choice);
    }

    private boolean askToGuess(Scanner scanner) {
        System.out.print("Do you want to guess your character? (yes/no): ");
        String response = scanner.nextLine().trim().toLowerCase();
        return response.equals("yes");
    }

    private boolean makeGuess(Scanner scanner, Character aiCharacter) {
        System.out.print("Please enter your guess: ");
        String guess = scanner.nextLine().trim();
        return guess.equals(aiCharacter.name);
    }

    private boolean askQuestion(String question, Character character) {
        switch (question) {
            case "Does your character have a beard?":
                return character.beard.equals("Yes");
            case "Does your character have a moustache?":
                return character.moustache.equals("Yes");
            case "Does your character have a big nose?":
                return character.bigNose.equals("Yes");
            case "Does your character wear glasses?":
                return character.glasses.equals("Yes");
            case "Does your character wear a hat?":
                return character.hat.equals("Yes");
            case "Is your character male?":
                return character.gender.equals("Male");
            case "Does your character have blue eyes?":
                return character.eyes.equals("Blue");
            case "Does your character have brown eyes?":
                return character.eyes.equals("Brown");
            case "Does your character have green eyes?":
                return character.eyes.equals("Green");
            case "Does your character have blonde hair?":
                return character.hair.equals("Blonde");
            case "Does your character have black hair?":
                return character.hair.equals("Black");
            case "Does your character have brown hair?":
                return character.hair.equals("Brown");
            case "Does your character have dark brown hair?":
                return character.hair.equals("Dark brown");
            case "Does your character have highlights?":
                return character.hair.equals("Highlights");
            case "Does your character have white hair?":
                return character.hair.equals("White");
            default:
                return false;
        }
    }

    private boolean matches(Character character, String question, boolean answer) {
        switch (question) {
            case "Does your character have a beard?":
                return character.beard.equals(answer ? "Yes" : "No");
            case "Does your character have a moustache?":
                return character.moustache.equals(answer ? "Yes" : "No");
            case "Does your character have a big nose?":
                return character.bigNose.equals(answer ? "Yes" : "No");
            case "Does your character wear glasses?":
                return character.glasses.equals(answer ? "Yes" : "No");
            case "Does your character wear a hat?":
                return character.hat.equals(answer ? "Yes" : "No");
            case "Is your character male?":
                return character.gender.equals(answer ? "Male" : "Female");
            case "Does your character have blue eyes?":
                return character.eyes.equals("Blue") == answer;
            case "Does your character have brown eyes?":
                return character.eyes.equals("Brown") == answer;
            case "Does your character have green eyes?":
                return character.eyes.equals("Green") == answer;
            case "Does your character have blonde hair?":
                return character.hair.equals("Blonde") == answer;
            case "Does your character have black hair?":
                return character.hair.equals("Black") == answer;
            case "Does your character have brown hair?":
                return character.hair.equals("Brown") == answer;
            case "Does your character have dark brown hair?":
                return character.hair.equals("Dark brown") == answer;
            case "Does your character have highlights?":
                return character.hair.equals("Highlights") == answer;
            case "Does your character have white hair?":
                return character.hair.equals("White") == answer;
            default:
                return false;
        }
    }

    public static void main(String[] args) {
        CharacterGuessingGame game = new CharacterGuessingGame();
        game.startGame();
    }
}