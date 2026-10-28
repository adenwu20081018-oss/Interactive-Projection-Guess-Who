package groupDGuessWho;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class AIQuestionSelector {
    private List<Character1> allCharacters = new ArrayList<>();
    private Set<String> askedQuestions = new HashSet<>(); 
    private Character1 comparisonCharacter; 

    public AIQuestionSelector() {
        allCharacters.add(new Character1("AD01", "Male", "Blue", "Light brown", false, true, false, false, false));
        allCharacters.add(new Character1("AX02", "Male", "Brown", "Black", false, true, false, false, false));
        allCharacters.add(new Character1("AY03", "Female", "Brown", "Highlights", false, false, false, true, false));
        allCharacters.add(new Character1("AA04", "Female", "Blue", "Blonde", false, false, false, false, false));
        allCharacters.add(new Character1("AE05", "Female", "Brown", "Black", false, false, true, false, false));
        allCharacters.add(new Character1("BN06", "Female", "Brown", "Dark brown", false, false, true, true, false));
        allCharacters.add(new Character1("BD07", "Male", "Brown", "Dark brown", false, false, true, false, true));
        allCharacters.add(new Character1("BY08", "Female", "Blue", "White", false, false, false, true, false));
        allCharacters.add(new Character1("BL09", "Male", "Brown", "Light brown", true, false, false, false, false));
        allCharacters.add(new Character1("CN10", "Female", "Brown", "White", false, false, false, false, false));
        allCharacters.add(new Character1("CS11", "Male", "Brown", "Blonde", false, true, false, false, false));
        allCharacters.add(new Character1("CE12", "Female", "Brown", "Light brown", false, false, false, true, true));
        allCharacters.add(new Character1("DL13", "Male", "Green", "Light brown", true, true, true, false, false));
        allCharacters.add(new Character1("DD14", "Male", "Brown", "Blonde", true, false, false, false, true));
        allCharacters.add(new Character1("EA15", "Female", "Brown", "Light brown", false, false, false, false, false));
        allCharacters.add(new Character1("EC16", "Male", "Brown", "Blonde", false, false, false, false, true));
        allCharacters.add(new Character1("FH17", "Male", "Blue", "Black", false, false, false, false, false));
        allCharacters.add(new Character1("FS18", "Male", "Brown", "Light brown", false, false, false, false, false));
        allCharacters.add(new Character1("GE19", "Male", "Brown", "Black", false, false, false, false, false));
        allCharacters.add(new Character1("GE20", "Male", "Brown", "White", false, false, false, false, true));
        allCharacters.add(new Character1("HN21", "Male", "Brown", "Light brown", false, true, false, false, false));
        allCharacters.add(new Character1("HY22", "Female", "Brown", "Dark brown", false, false, false, false, false));
        allCharacters.add(new Character1("JE23", "Male", "Brown", "Blonde", true, true, false, false, false));
        allCharacters.add(new Character1("RC24", "Male", "Green", "Highlights", false, false, false, true, false));
        allCharacters.add(new Character1("KE25", "Female", "Blue", "Blonde", false, false, false, false, false));
        allCharacters.add(new Character1("LA26", "Female", "Green", "Black", false, false, false, false, true));
        allCharacters.add(new Character1("LO27", "Male", "Brown", "White", false, true, false, false, false ));
        allCharacters.add(new Character1("LY28", "Female", "Green", "Dark brown", false, false, false, false, true));
        allCharacters.add(new Character1("LZ29", "Female", "Blue", "White", false, false, false, true, false));
        allCharacters.add(new Character1("MA30", "Female", "Brown", "Dark brown", false, false, false, false, true));
        allCharacters.add(new Character1("MX31", "Male", "Brown", "Dark brown", false, true, true, false, false));
        allCharacters.add(new Character1("MA32", "Female", "Brown", "Black", false, false, false, false, false));
        allCharacters.add(new Character1("ME33", "Male", "Brown", "Black", false, false, false, false, true));
        allCharacters.add(new Character1("NK34", "Male", "Brown", "Blonde", false, false, true, false, false));
        allCharacters.add(new Character1("OA35", "Female", "Brown", "Highlights", false, false, false, false, false));
        allCharacters.add(new Character1("PL36", "Male", "Brown", "White", false, false, false, true, false));
        allCharacters.add(new Character1("PR37", "Male", "Blue", "White", false, true, false, false, false));
        allCharacters.add(new Character1("PP38", "Male", "Brown", "Black", true, false, false, false, false));
        allCharacters.add(new Character1("RL39", "Female", "Blue", "Dark brown", false, false, false, true, false));
        allCharacters.add(new Character1("RD40", "Male", "Brown", "Dark brown", true, true, false, false, false));
        allCharacters.add(new Character1("RT41", "Male", "Blue", "Dark brown", false, true, false, false, false));
        allCharacters.add(new Character1("SY42", "Female", "Brown", "Black", false, false, false, false, false));
        allCharacters.add(new Character1("SM43", "Male", "Brown", "White", false, false, false, true, false));
        allCharacters.add(new Character1("SA44", "Female", "Green", "Dark brown", false, false, false, false, false));
        allCharacters.add(new Character1("SN45", "Female", "Brown", "White", false, false, false, false, false));
        allCharacters.add(new Character1("TM46", "Male", "Blue", "Black", false, false, false, true, false));
        allCharacters.add(new Character1("VR47", "Male", "Brown", "White", false, false, false, false, false));
        comparisonCharacter = new Character1("", "", "", "", false, false, false, false, false);
    }

    public String selectQuestion() {
        String[] questions = {
            "Does your agent have a beard?",
            "Does your agent have a moustache?",
            "Does your agent have a big nose?",
            "Does your agent wear glasses?",
            "Does your agent wear a hat?",
            "Is your agent a male?",
            "Is your agent a female?",
            "Does your agent have brown eyes?",
            "Does your agent have blue eyes?",
            "Does your agent have green eyes?",
            "Is your agent's hair black?",
            "Is your agent's hair white?",
            "Is your agent's hair dark brown?",
            "Is your agent's hair light brown?",
            "Is your agent's hair blonde?",
            "Is your agent's hair highlights?"
        };

        int totalCharacters = allCharacters.size();
        int closestDifference = Integer.MAX_VALUE;
        String bestQuestion = "";

        for (String question : questions) {
            if (askedQuestions.contains(question)) continue;

            int countWithTrait = 0;

            switch (question) {
                case "Does your agent have a beard?":
                    for (Character1 character : allCharacters) {
                        if (character.hasBeard()) countWithTrait++;
                    }
                    break;
                case "Does your agent have a moustache?":
                    for (Character1 character : allCharacters) {
                        if (character.hasMoustache()) countWithTrait++;
                    }
                    break;
                case "Does your agent have a big nose?":
                    for (Character1 character : allCharacters) {
                        if (character.hasBigNose()) countWithTrait++;
                    }
                    break;
                case "Does your agent wear glasses?":
                    for (Character1 character : allCharacters) {
                        if (character.wearsGlasses()) countWithTrait++;
                    }
                    break;
                case "Does your agent wear a hat?":
                    for (Character1 character : allCharacters) {
                        if (character.wearsHat()) countWithTrait++;
                    }
                    break;
                case "Is your agent a male?":
                    for (Character1 character : allCharacters) {
                        if (character.getGender().equals("Male")) countWithTrait++;
                    }
                    break;
                case "Is your agent a female?":
                    for (Character1 character : allCharacters) {
                        if (character.getGender().equals("Female")) countWithTrait++;
                    }
                    break;
                case "Does your agent have brown eyes?":
                    for (Character1 character : allCharacters) {
                        if (character.getEyeColor().equals("Brown")) countWithTrait++;
                    }
                    break;
                case "Does your agent have blue eyes?":
                    for (Character1 character : allCharacters) {
                        if (character.getEyeColor().equals("Blue")) countWithTrait++;
                    }
                    break;
                case "Does your agent have green eyes?":
                    for (Character1 character : allCharacters) {
                        if (character.getEyeColor().equals("Green")) countWithTrait++;
                    }
                    break;
                case "Is your agent's hair black?":
                    for (Character1 character : allCharacters) {
                        if (character.getHairColor().equals("Black")) countWithTrait++;
                    }
                    break;
                case "Is your agent's hair white?":
                    for (Character1 character : allCharacters) {
                        if (character.getHairColor().equals("White")) countWithTrait++;
                    }
                    break;
                case "Is your agent's hair dark brown?":
                    for (Character1 character : allCharacters) {
                        if (character.getHairColor().equals("Dark brown")) countWithTrait++;
                    }
                    break;
                case "Is your agent's hair light brown?":
                    for (Character1 character : allCharacters) {
                        if (character.getHairColor().equals("Light brown")) countWithTrait++;
                    }
                    break;
                case "Is your agent's hair blonde?":
                    for (Character1 character : allCharacters) {
                        if (character.getHairColor().equals("Blonde")) countWithTrait++;
                    }
                    break;
                case "Is your agent's hair highlights?":
                    for (Character1 character : allCharacters) {
                        if (character.getHairColor().equals("Highlights")) countWithTrait++;
                    }
                    break;
            }

            int countWithoutTrait = totalCharacters - countWithTrait;
            int difference = Math.abs(countWithTrait - countWithoutTrait);

            if (difference < closestDifference) {
                closestDifference = difference;
                bestQuestion = question;
            }
        }

        if (!bestQuestion.isEmpty()) {
            askedQuestions.add(bestQuestion); 
        }

        return bestQuestion;
    }

    public void filterCharacters(String question, boolean answer) {
        List<Character1> filteredCharacters = new ArrayList<>();

        for (Character1 character : allCharacters) {
            boolean hasTrait = false;

            switch (question) {
                case "Does your agent have a beard?":
                    hasTrait = character.hasBeard();
                    break;
                case "Does your agent have a moustache?":
                    hasTrait = character.hasMoustache();
                    break;
                case "Does your agent have a big nose?":
                    hasTrait = character.hasBigNose();
                    break;
                case "Does your agent wear glasses?":
                    hasTrait = character.wearsGlasses();
                    break;
                case "Does your agent wear a hat?":
                    hasTrait = character.wearsHat();
                    break;
                case "Is your agent a male?":
                    hasTrait = character.getGender().equals("Male");
                    break;
                case "Is your agent a female?":
                    hasTrait = character.getGender().equals("Female");
                    break;
                case "Does your agent have brown eyes?":
                    hasTrait = character.getEyeColor().equals("Brown");
                    break;
                case "Does your agent have blue eyes?":
                    hasTrait = character.getEyeColor().equals("Blue");
                    break;
                case "Does your agent have green eyes?":
                    hasTrait = character.getEyeColor().equals("Green");
                    break;
                case "Is your agent's hair black?":
                    hasTrait = character.getHairColor().equals("Black");
                    break;
                case "Is your agent's hair white?":
                    hasTrait = character.getHairColor().equals("White");
                    break;
                case "Is your agent's hair dark brown?":
                    hasTrait = character.getHairColor().equals("Dark brown");
                    break;
                case "Is your agent's hair light brown?":
                    hasTrait = character.getHairColor().equals ("Light brown");
                    break;
                case "Is your agent's hair blonde?":
                    hasTrait = character.getHairColor().equals("Blonde");
                    break;
                case "Is your agent's hair highlights?":
                    hasTrait = character.getHairColor().equals("Highlights");
                    break;
            }

            if (answer) {
                switch (question) {
                    case "Does your agent have a beard?":
                        comparisonCharacter.setHasBeard(true);
                        break;
                    case "Does your agent have a moustache?":
                        comparisonCharacter.setHasMoustache(true);
                        break;
                    case "Does your agent have a big nose?":
                        comparisonCharacter.setHasBigNose(true);
                        break;
                    case "Does your agent wear glasses?":
                        comparisonCharacter.setWearsGlasses(true);
                        break;
                    case "Does your agent wear a hat?":
                        comparisonCharacter.setWearsHat(true);
                        break;
                    case "Is your agent a male?":
                        comparisonCharacter.setGender("Male");
                        break;
                    case "Is your agent a female?":
                        comparisonCharacter.setGender("Female");
                        break;
                    case "Does your agent have brown eyes?":
                        comparisonCharacter.setEyeColor("Brown");
                        break;
                    case "Does your agent have blue eyes?":
                        comparisonCharacter.setEyeColor("Blue");
                        break;
                    case "Does your agent have green eyes?":
                        comparisonCharacter.setEyeColor("Green");
                        break;
                    case "Is your agent's hair black?":
                        comparisonCharacter.setHairColor("Black");
                        break;
                    case "Is your agent's hair white?":
                        comparisonCharacter.setHairColor("White");
                        break;
                    case "Is your agent's hair dark brown?":
                        comparisonCharacter.setHairColor("Dark brown");
                        break;
                    case "Is your agent's hair light brown?":
                        comparisonCharacter.setHairColor("Light brown");
                        break;
                    case "Is your agent's hair blonde?":
                        comparisonCharacter.setHairColor("Blonde");
                        break;
                    case "Is your agent's hair highlights?":
                        comparisonCharacter.setHairColor("Highlights");
                        break;
                }
            }

            if (answer && hasTrait) {
                filteredCharacters.add(character); 
            } else if (!answer && !hasTrait) {
                filteredCharacters.add(character);
            }
        }

        allCharacters = filteredCharacters;
    }

    public void playGame() {
        Scanner scanner = new Scanner(System.in);
        while (allCharacters.size() > 1) {
            String question = selectQuestion();
            System.out.println("AI: " + question);
            String response = scanner.nextLine().trim().toLowerCase();

            while (!response.equals("yes") && !response.equals("no")) {
                System.out.println("Please answer with 'yes' or 'no'.");
                response = scanner.nextLine().trim().toLowerCase();
            }

            filterCharacters(question, response.equals("yes"));
        }

        if (allCharacters.size() == 1) {
            System.out.println("AI: I guess your character is " + allCharacters.get(0).getName() + "!");
            String finalResponse = scanner.nextLine().trim().toLowerCase();

            if (finalResponse.equals("yes")) {
                System.out.println("AI: I win!");
            } else {
                System.out.println("AI: Please tell me your character's ID.");
                String playerCharacterId = scanner.nextLine().trim();
                comparisonCharacter = new Character1(playerCharacterId, "", "", "", false, false, false, false, false);
                identifyWrongAnswers();
            }
        } else {
            System.out.println("AI: I couldn't guess your character.");
        }
        scanner.close();
    }

    private void identifyWrongAnswers() {
        List<String> wrongQuestions = new ArrayList<>();
        for (String question : askedQuestions) {
            boolean playerTrait = false;
            boolean aiTrait = false;

            switch (question) {
                case "Does your agent have a beard?":
                    playerTrait = comparisonCharacter.hasBeard();
                    aiTrait = allCharacters.get(0).hasBeard();
                    break;
                case "Does your agent have a moustache?":
                    playerTrait = comparisonCharacter.hasMoustache();
                    aiTrait = allCharacters.get(0).hasMoustache();
                    break;
                case "Does your agent have a big nose?":
                    playerTrait = comparisonCharacter.hasBigNose();
                    aiTrait = allCharacters.get(0).hasBigNose();
                    break;
                case "Does your agent wear glasses?":
                    playerTrait = comparisonCharacter.wearsGlasses();
                    aiTrait = allCharacters.get(0).wearsGlasses();
                    break;
                case "Does your agent wear a hat?":
                    playerTrait = comparisonCharacter.wearsHat();
                    aiTrait = allCharacters.get(0).wearsHat();
                    break;
                case "Is your agent a male?":
                    playerTrait = comparisonCharacter.getGender().equals("Male");
                    aiTrait = allCharacters.get(0).getGender().equals("Male");
                    break;
                case "Is your agent a female?":
                    playerTrait = comparisonCharacter.getGender().equals("Female");
                    aiTrait = allCharacters.get(0).getGender().equals("Female");
                    break;
                case "Does your agent have brown eyes?":
                    playerTrait = comparisonCharacter.getEyeColor().equals("Brown");
                    aiTrait = allCharacters.get(0).getEyeColor().equals("Brown");
                    break;
                case "Does your agent have blue eyes?":
                    playerTrait = comparisonCharacter.getEyeColor().equals("Blue");
                    aiTrait = allCharacters.get(0).getEyeColor().equals("Blue");
                    break;
                case "Does your agent have green eyes?":
                    playerTrait = comparisonCharacter.getEyeColor().equals("Green");
                    aiTrait = allCharacters.get(0).getEyeColor().equals("Green");
                    break;
                case "Is your agent's hair black?":
                    playerTrait = comparisonCharacter.getHairColor().equals("Black");
                    aiTrait = allCharacters.get(0).getHairColor().equals("Black");
                    break;
                case "Is your agent's hair white?":
                    playerTrait = comparisonCharacter.getHairColor().equals("White");
                    aiTrait = allCharacters.get(0).getHairColor().equals("White");
                    break;
                case "Is your agent's hair dark brown?":
                    playerTrait = comparisonCharacter.getHairColor().equals("Dark brown");
                    aiTrait = allCharacters.get(0).getHairColor().equals("Dark brown");
                    break;
                case "Is your agent's hair light brown?":
                    playerTrait = comparisonCharacter.getHairColor().equals("Light brown");
                    aiTrait = allCharacters.get(0).getHairColor().equals("Light brown");
                    break;
                case "Is your agent's hair blonde?":
                    playerTrait = comparisonCharacter.getHairColor().equals("Blonde");
                    aiTrait = allCharacters.get(0).getHairColor().equals("Blonde");
                    break;
                case "Is your agent's hair highlights?":
                    playerTrait = comparisonCharacter.getHairColor().equals("Highlights");
                    aiTrait = allCharacters.get(0).getHairColor().equals("Highlights");
                    break;
            }
            
            if (playerTrait != aiTrait) {
                wrongQuestions.add(question);
            }
        }

        if (!wrongQuestions.isEmpty()) {
            System.out.println("AI: You answered the following questions incorrectly:");
            for (String wrongQuestion : wrongQuestions) {
                System.out.println("- " + wrongQuestion);
            }
        } else {
            System.out.println("AI: All your answers were correct!");
        }
    }

    public static void main(String[] args) {
        AIQuestionSelector game = new AIQuestionSelector();
        game.playGame();
    }
}