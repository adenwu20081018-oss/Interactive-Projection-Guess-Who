package groupDGuessWho;

/**
 * @author James Chen, Edward Li, Aden Wu, Lucas Chua
 * 2025.01.15
 * Creating the Guess Who board game in Java with our own twist
 */

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import javax.swing.border.LineBorder;

import java.util.List;

public class Board implements ActionListener {

    // Called only after an existing character action changes its visual state.
    private java.util.function.Consumer<String> characterFeedback = action -> { };
    void setCharacterFeedback(java.util.function.Consumer<String> feedback) {
        characterFeedback = java.util.Objects.requireNonNull(feedback);
    }

    // Phase 4B adapter access only. Original layout, listeners and rules stay below.
    JFrame controlWindow() { return frame; }

    String controlName(JButton button) {
        if (button == startGame) return "startGame";
        if (button == returnToMainMenu) return "returnToMainMenu";
        if (button == revoke) return "revoke";
        if (button == continues) return "continues: " + button.getText();
        if (button == continueSmall) return "continueSmall";
        if (button == healthDisplayImage_button) return "healthDisplayImage_button";
        for (int i = 0; i < characterDisplay.length; i++) {
            if (button == characterDisplay[i]) return "character[" + i + "]: " + charactersOnScreen.get(i).getName();
        }
        return button.getText();
    }

    // List of all characters
    private List<Character1> allCharacters = new ArrayList<>();
	private List<Character1> fullCharacterList = new ArrayList<>();
	private List<Character1> selectedCharacters = new ArrayList<>();
	private List<Character1> charactersOnScreen = new ArrayList<>();
    private Set<String> askedQuestions = new HashSet<>();
    private Character1 selectedCharacter; // The character to guess
    private String response;
    private String question;
    private String yesGender = "-1";
    private String yesEyes = "-1";
    private String yesHair = "-1";
    private int beardAIGuess = -1;
    private int moustacheAIGuess = -1;
    private int bigNoseAIGuess = -1;
    private int glassesAIGuess = -1;
    private int hatAIGuess = -1;
    // Male, Female
    private int[] genderAIGuess = {-1, -1};
    // Blue, Green, Brown
    private int[] eyesAIGuess = {-1, -1, -1};
    // Black, White, Blonde, Dark brown, Light brown, Highlights
    private int[] hairAIGuess = {-1, -1, -1, -1, -1, -1};
    private String genderPlayer = "-1";
    private String eyesPlayer = "-1";
    private String hairPlayer = "-1";
    private int beardPlayer = -1;
    private int moustachePlayer = -1;
    private int bigNosePlayer = -1;
    private int glassesPlayer = -1;
    private int hatPlayer = -1;
    private int[] retraceResultDisplay = {1,1,1,1,1,1,1,1,1,1,1,0,0,0};
    private boolean retrace = false;
    private boolean identical = false;

    // Display settings
    private JFrame frame = new JFrame();
    private JPanel mainMenu_panel = new JPanel();
    private JPanel result_panel = new JPanel();
    private JPanel AIText_panel = new JPanel();
    private JPanel center_panel = new JPanel();
    private JPanel playerText_panel = new JPanel();
    private JPanel playerAction_panel = new JPanel();
    private int panelOnScreen = -1; // this is for the revoke button to identify which panel should it go to
    private JPanel revoke_panel = new JPanel();
    private JPanel playerTextShort_panel = new JPanel();
    private JPanel characteristics_panel = new JPanel();
    private JPanel gender_panel = new JPanel();
    private JPanel eyes_panel = new JPanel();
    private JPanel hair_panel = new JPanel();
    private JPanel health_panel = new JPanel();
    private JPanel menu_panel = new JPanel();
    private JPanel AI_panel = new JPanel();
    private JPanel player_panel = new JPanel();
    private JPanel continue_panel = new JPanel();
    private JPanel playerAnswer_panel = new JPanel();
    private JPanel continueSmall_panel = new JPanel();
    private int startGameStatus = 0;
    private JButton startGame = new JButton();
    private JButton returnToMainMenu = new JButton();
    private int[] characterStatus = new int[24];
    private JButton[] characterDisplay = new JButton[24];
    private JButton[] playerAction = new JButton[2];
    private JButton[] characteristics = new JButton[8];
    private JButton[] gender = new JButton[2];
    private JButton[] eyes = new JButton[3];
    private JButton[] hair = new JButton[6];
    private JButton revoke = new JButton();
    private JButton continues = new JButton();
    private JButton[] playerAnswer = new JButton[2];
    private JButton continueSmall = new JButton();

    // Images & Texts
    private JLabel AIProfile_label = new JLabel();
    private ImageIcon AIProfile = new ImageIcon("AIProfile.png");
    private JLabel AIText_label = new JLabel();
    private ImageIcon AIText = new ImageIcon("AIText.png");
    private JLabel playerProfile_label = new JLabel();
    private ImageIcon playerProfile = new ImageIcon("playerProfile.png");
    private JLabel playerTextLong_label = new JLabel();
    private ImageIcon playerTextLong = new ImageIcon("playerTextLong.png");
    private JLabel playerTextShort_label = new JLabel();
    private ImageIcon playerTextShort = new ImageIcon("playerTextShort.png");
    private ImageIcon revokeImage = new ImageIcon("revoke.png");
    private ImageIcon x62ButtonImage = new ImageIcon("62x100Button.png");
    private ImageIcon x83ButtonImage = new ImageIcon("83x100Button.png");
    private ImageIcon x100ButtonImage = new ImageIcon("100x100Button.png");
    private ImageIcon x166ButtonImage = new ImageIcon("166x100Button.png");
    private ImageIcon x250ButtonImage = new ImageIcon("250x100Button.png");
    private ImageIcon x300ButtonImage = new ImageIcon("300x100Button.png");
    private ImageIcon x600ButtonImage = new ImageIcon("600x100Button.png");
    
    private JButton healthDisplayImage_button = new JButton();
    private ImageIcon batteryImage = new ImageIcon("battery.png");
    private JLabel batteryImage_label = new JLabel();

    private boolean virusOn = false;
    private int AIHealth = 100;
    private JProgressBar AIHealthDisplay = new JProgressBar();
    private int playerHealth = 100;
    private JProgressBar playerHealthDisplay = new JProgressBar();
    
    private int continueStatus = 0;
    private int continueSmallStatus = 0;
    private boolean AIGuessStatus = false;
    private boolean firstRound = true;
    private boolean firstRoundSmall = true;
    private int roundWinner = -1;
    
    // Audio
    String filePathIntro = "Intro.wav";
    String filePath = "Suspicion_Matrix.wav";
    Player play = new Player();

    // Constructor, to initialize and setup everything
    Board() {
        // Initialize all characters
        initializeCharacters();

        // Randomly select 24 characters
        selectRandomCharacters();

        // Create the frame
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(814, 537);
        frame.setLayout(null);
        frame.getContentPane().setBackground(new Color(50, 50, 50));
        frame.setVisible(true);
        frame.setResizable(false);

        // Set all the panels
        setupPanels();

        // Add everything to the frame
        addComponentsToFrame();

        // Set up buttons
        setupButtons();

        // Clean the screen and get ready for performance
        inGamePanelsVisible(false);
        result_panel.setVisible(false);
        playerAction_panel.setVisible(false);
        revoke_panel.setVisible(false);
        playerTextShort_panel.setVisible(false);
        characteristics_panel.setVisible(false);
        gender_panel.setVisible(false);
        eyes_panel.setVisible(false);
        hair_panel.setVisible(false);
        mainMenu_panel.setVisible(false);
        playerText_panel.setVisible(false);
        playerAnswer_panel.setVisible(false);
        continueSmall_panel.setVisible(false);
        continue_panel.setVisible(false);
        
        // Add a window listener to stop music when the frame is closed
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                play.stopMusic(); // Stop the music when the window is closing
            }
        });
        
        play.playMusic(filePathIntro);
        
        mainMenuOn();
    }

    // Adding all the characters to our arraylist. Each character is an object created from the Character1 class
    private void initializeCharacters() {
        fullCharacterList.add(new Character1("AD01", "Male", "Blue", "Light brown", false, true, false, false, false));//Checked
        fullCharacterList.add(new Character1("AX02", "Male", "Brown", "Black", false, true, false, false, false));//Checked
        fullCharacterList.add(new Character1("AY03", "Female", "Brown", "Highlights", false, false, false, true, false));//Checked
        fullCharacterList.add(new Character1("AA04", "Female", "Blue", "Blonde", false, false, false, false, false));//Checked
        fullCharacterList.add(new Character1("AE05", "Female", "Brown", "Black", false, false, true, false, false));//Checked
        fullCharacterList.add(new Character1("BN06", "Female", "Brown", "Dark brown", false, false, true, true, false));//Checked
        fullCharacterList.add(new Character1("BD07", "Male", "Brown", "Dark brown", false, false, true, false, true));//Checked
        fullCharacterList.add(new Character1("BY08", "Female", "Blue", "White", false, false, false, true, false));//Checked
        fullCharacterList.add(new Character1("BL09", "Male", "Brown", "Light brown", true, false, false, false, false));//Checked
        fullCharacterList.add(new Character1("CN10", "Female", "Brown", "White", false, false, false, false, false));//Checked
        fullCharacterList.add(new Character1("CS11", "Male", "Brown", "Blonde", false, true, false, false, false));//Checked
        fullCharacterList.add(new Character1("CE12", "Female", "Brown", "Light brown", false, false, false, true, true));//Checked
        fullCharacterList.add(new Character1("DL13", "Male", "Green", "Light brown", true, true, true, false, false));//Checked
        fullCharacterList.add(new Character1("DD14", "Male", "Brown", "Blonde", true, false, false, false, true));//Checked
        fullCharacterList.add(new Character1("EA15", "Female", "Brown", "Light brown", false, false, false, false, false));//Checked
        fullCharacterList.add(new Character1("EC16", "Male", "Brown", "Blonde", false, false, false, false, true));//Checked
        fullCharacterList.add(new Character1("FH17", "Female", "Blue", "Black", false, false, false, false, false));//Checked
        fullCharacterList.add(new Character1("FS18", "Male", "Brown", "Light brown", false, false, false, false, false));//Checked
        fullCharacterList.add(new Character1("GE19", "Male", "Brown", "Black", false, false, false, false, false));//Checked
        fullCharacterList.add(new Character1("GE20", "Female", "Brown", "White", false, false, false, false, true));//Checked
        fullCharacterList.add(new Character1("HN21", "Male", "Brown", "Light brown", false, false, true, false, false));//Checked
        fullCharacterList.add(new Character1("HY22", "Female", "Brown", "Dark brown", false, false, false, false, false));//Checked
        fullCharacterList.add(new Character1("JE23", "Male", "Brown", "Blonde", true, true, false, false, false));//Checked
        fullCharacterList.add(new Character1("RC24", "Male", "Green", "Highlights", false, false, false, true, false));//Checked
        fullCharacterList.add(new Character1("KE25", "Female", "Blue", "Blonde", false, false, false, false, false));//Checked
        fullCharacterList.add(new Character1("LA26", "Female", "Green", "Black", false, false, false, false, true));//Checked
        fullCharacterList.add(new Character1("LO27", "Male", "Brown", "White", true, true, false, false, false));//Checked
        fullCharacterList.add(new Character1("LY28", "Female", "Green", "Dark brown", false, false, false, false, true));//Checked
        fullCharacterList.add(new Character1("LZ29", "Female", "Blue", "White", false, false, false, true, false));//Checked
        fullCharacterList.add(new Character1("MA30", "Female", "Brown", "Dark brown", false, false, false, false, true));//Checked
        fullCharacterList.add(new Character1("MX31", "Male", "Brown", "Dark brown", false, true, true, false, false));//Checked
        fullCharacterList.add(new Character1("MA32", "Female", "Brown", "Black", false, false, false, false, false));//Checked
        fullCharacterList.add(new Character1("ME33", "Male", "Brown", "Black", false, false, false, false, true));//Checked
        fullCharacterList.add(new Character1("NK34", "Male", "Brown", "Blonde", false, false, true, false, false));//Checked
        fullCharacterList.add(new Character1("QA35", "Female", "Brown", "Highlights", false, false, false, false, false));//Checked
        fullCharacterList.add(new Character1("PL36", "Male", "Brown", "White", false, false, false, true, false));//Checked
        fullCharacterList.add(new Character1("PR37", "Male", "Brown", "White", false, false, true, false, false));//Checked
    }
    
    /**
     * Method to select 24 random characters from the total of 37. This method uses the imported Random class to
     * randomize the characters, along with a check to make sure that there are no duplicate characters. 
     */
    private void selectRandomCharacters() {
        selectedCharacters.clear(); // Clear the previous selections
        Random random = new Random();
        while (selectedCharacters.size() < 24) {
            int index = random.nextInt(fullCharacterList.size());
            Character1 selectedCharacter = fullCharacterList.get(index);
            
            // Check if the character is already selected
            if (!selectedCharacters.contains(selectedCharacter)) {
                selectedCharacters.add(selectedCharacter);
            }
        }
        allCharacters = new ArrayList<>(selectedCharacters); // Create a new list to avoid reference issues
        charactersOnScreen = new ArrayList<>(selectedCharacters);
        selectRandomCharacter();
    }
    
    /**
     * Method to select a random character for the AI. Using very similar randomizer logic to the previous method. 
     */
    private void selectRandomCharacter() {
        if (allCharacters.isEmpty()) {
            throw new IllegalStateException("No characters available to select.");
        }

        Random random = new Random();
        int index = random.nextInt(allCharacters.size());
        selectedCharacter = allCharacters.get(index); // Randomly select a character
    }
    
    /**
     * Simple method to setup all the panels in their desired sizes with their desired bounds. 
     */
    private void setupPanels() {
        mainMenu_panel.setBackground(new Color(150, 150, 150));
        mainMenu_panel.setLayout(new BorderLayout());
        mainMenu_panel.setBounds(0, 0, 800, 500);
        
        result_panel.setBackground(new Color(150, 150, 150));
        result_panel.setLayout(new BorderLayout());
        result_panel.setBounds(0, 0, 800, 500);
        
        center_panel.setBackground(new Color(50, 50, 50));
        center_panel.setLayout(new GridLayout(3, 8, 0, 0));
        center_panel.setBounds(100, 100, 600, 300);

        AI_panel.setBackground(new Color(50, 50, 50));
        AI_panel.setBounds(0, 0, 100, 100);
        AI_panel.setLayout(new BorderLayout());
        
        player_panel.setBackground(new Color(50, 50, 50));
        player_panel.setBounds(0, 400, 100, 100);
        player_panel.setLayout(new BorderLayout());
        
        AIText_panel.setBackground(new Color(50, 50, 50));
        AIText_panel.setBounds(100, 0, 600, 100);
        AIText_panel.setLayout(new BorderLayout());
        
        playerText_panel.setBackground(new Color(50, 50, 50));
        playerText_panel.setBounds(100, 400, 600, 100);
        playerText_panel.setLayout(new BorderLayout());
        
        playerAction_panel.setBackground(new Color(50, 50, 50));
        playerAction_panel.setLayout(new GridLayout(1, 2, 0, 0));
        playerAction_panel.setBounds(100, 400, 600, 100);

        revoke_panel.setBackground(new Color(50, 50, 50));
        revoke_panel.setLayout(new BorderLayout());
        revoke_panel.setBounds(600, 400, 100, 100);
        
        playerTextShort_panel.setBackground(new Color(50, 50, 50));
        playerTextShort_panel.setBounds(100, 400, 500, 100);
        playerTextShort_panel.setLayout(new BorderLayout());

        characteristics_panel.setBackground(new Color(50, 50, 50));
        characteristics_panel.setLayout(new GridLayout(1, 8, 0, 0));
        characteristics_panel.setBounds(100, 400, 500, 100);

        gender_panel.setBackground(new Color(50, 50, 50));
        gender_panel.setLayout(new GridLayout(1, 2, 0, 0));
        gender_panel.setBounds(100, 400, 500, 100);

        eyes_panel.setBackground(new Color(50, 50, 50));
        eyes_panel.setLayout(new GridLayout(1, 3, 0, 0));
        eyes_panel.setBounds(100, 400, 500, 100);

        hair_panel.setBackground(new Color(50, 50, 50));
        hair_panel.setLayout(new GridLayout(1, 6, 0, 0));
        hair_panel.setBounds(100, 400, 500, 100);
        
        continue_panel.setBackground(new Color(50, 50, 50));
        continue_panel.setLayout(new GridLayout(1, 6, 0, 0));
        continue_panel.setBounds(100, 400, 600, 100);

        health_panel.setBackground(new Color(50, 50, 50));
        health_panel.setLayout(new BorderLayout());
        health_panel.setBounds(0, 100, 100, 300);

        menu_panel.setBackground(new Color(50, 50, 50));
        menu_panel.setLayout(new BorderLayout());
        menu_panel.setBounds(700, 0, 100, 500);
        
        playerAnswer_panel.setBackground(new Color(50, 50, 50));
        playerAnswer_panel.setLayout(new GridLayout(1, 6, 0, 0));
        playerAnswer_panel.setBounds(100, 400, 600, 100);
        
        continueSmall_panel.setBackground(new Color(50, 50, 50));
        continueSmall_panel.setLayout(new BorderLayout());
        continueSmall_panel.setBounds(600, 400, 100, 100);
    }

    /**
     * Simple method to add all objects to the frame. 
     */
    private void addComponentsToFrame() {
        frame.add(mainMenu_panel);
        frame.add(result_panel);
        frame.add(AI_panel);
        frame.add(player_panel);
        frame.add(AIText_panel);
        frame.add(center_panel);
        frame.add(playerText_panel);
        frame.add(playerAction_panel);
        frame.add(continue_panel);
        frame.add(revoke_panel);
        frame.add(playerTextShort_panel);
        frame.add(characteristics_panel);
        frame.add(gender_panel);
        frame.add(eyes_panel);
        frame.add(hair_panel);
        frame.add(health_panel);
        frame.add(menu_panel);
        frame.add(playerAnswer_panel);
        frame.add(continueSmall_panel);
    }

    /**
     * Method to setup all the buttons. Very difficult method to document, as every button has its own logic, and is
     * used for its own purpose in its own place. While this method is titled setupButtons (as it sets up mostly
     * buttons, it sets up other elements of the overall frame as well.)
     */
    private void setupButtons() {
    	//set up basic buttons
    	continue_panel.add(continues);
        JButtonSetUp(continues, 30, "CLICK TO CONFIRM: I HAVE CHOSEN MY AGENT");
        continues.setIcon(x600ButtonImage);
    
        continueSmall_panel.add(continueSmall);
        JButtonSetUp(continueSmall, 20, "CONTINUE");
        continueSmall.setIcon(x100ButtonImage);
        
        revoke_panel.add(revoke);
        JButtonSetUp(revoke, 1, "");
        revoke.setIcon(revokeImage);
        
        // Set up character buttons
        setUpButtons();
        
        // Set up player action buttons
        for (int i = 0; i < 2; i++) {
            playerAction[i] = new JButton();
            playerAction_panel.add(playerAction[i]);
            JButtonSetUp(playerAction[i], 30, "");
            playerAction[i].setIcon(x300ButtonImage);
        }
        playerAction[0].setText("Guess A Character");
        playerAction[1].setText("Check A Characteristic");
        
        // Set up characteristics buttons
        for (int i = 0; i < 8; i++) {
            characteristics[i] = new JButton();
            characteristics_panel.add(characteristics[i]);
            JButtonSetUp(characteristics[i], 15, "");
            characteristics[i].setIcon(x62ButtonImage);
        }
        characteristics[0].setText("Gender");
        characteristics[1].setText("Eyes");
        characteristics[2].setText("Hair");
        characteristics[3].setText("Beard");
        characteristics[4].setText("Moustache");
        characteristics[5].setText("Big Nose");
        characteristics[6].setText("Glasses");
        characteristics[7].setText("Hat");
        // Set up gender buttons
        for (int i = 0; i < 2; i++) {
            gender[i] = new JButton();
            gender_panel.add(gender[i]);
            JButtonSetUp(gender[i], 20, "");
            gender[i].setIcon(x250ButtonImage);
        }
        gender[0].setText("Male");
        gender[1].setText("Female");

        // Set up eyes buttons
        for (int i = 0; i < 3; i++) {
            eyes[i] = new JButton();
            eyes_panel.add(eyes[i]);
            JButtonSetUp(eyes[i], 20, "");
            eyes[i].setIcon(x166ButtonImage);
        }
        eyes[0].setText("Brown");
        eyes[1].setText("Blue");
        eyes[2].setText("Green");

        // Set up hair buttons
        for (int i = 0; i < 6; i++) {
            hair[i] = new JButton();
            hair_panel.add(hair[i]);
            JButtonSetUp(hair[i], 20, "");
            hair[i].setIcon(x83ButtonImage);
        }
        hair[0].setText("Black");
        hair[1].setText("White");
        hair[2].setText("Dark Brown");
        hair[3].setText("Light Brown");
        hair[4].setText("Blonde");
        hair[5].setText("Highlights");

        // Set up yes or no buttons
        for (int i = 0; i < 2; i++) {
            playerAnswer[i] = new JButton();
            playerAnswer_panel.add(playerAnswer[i]);
            JButtonSetUp(playerAnswer[i], 70, "");
            playerAnswer[i].setIcon(x300ButtonImage);
        }
        playerAnswer[0].setText("YES");
        playerAnswer[1].setText("NO");
        
        // Set up other images and texts
        AIProfile_label.setIcon(AIProfile);
        AI_panel.add(AIProfile_label);
        
        AIText_label.setIcon(AIText);
        AIText_label.setHorizontalTextPosition(JLabel.CENTER);
        AIText_label.setVerticalTextPosition(JLabel.CENTER);
        AIText_label.setForeground(new Color(90, 220, 80));
        AIText_label.setFont(new Font("Agency FB", Font.BOLD, 35));
        AIText_label.setText("CHOOSE YOUR AGENT");
        AIText_panel.add(AIText_label);
        
        playerProfile_label.setIcon(playerProfile);
        player_panel.add(playerProfile_label);
        
        playerTextLong_label.setIcon(playerTextLong);
        playerTextLong_label.setHorizontalTextPosition(JLabel.CENTER);
        playerTextLong_label.setVerticalTextPosition(JLabel.CENTER);
        playerTextLong_label.setForeground(new Color(40, 200, 220));
        playerTextLong_label.setFont(new Font("Agency FB", Font.BOLD, 35));
        playerText_panel.add(playerTextLong_label);
        
        playerTextShort_label.setIcon(playerTextShort);
        playerTextShort_label.setHorizontalTextPosition(JLabel.CENTER);
        playerTextShort_label.setVerticalTextPosition(JLabel.CENTER);
        playerTextShort_label.setForeground(new Color(40, 200, 220));
        playerTextShort_label.setFont(new Font("Agency FB", Font.BOLD, 35));
        playerTextShort_panel.add(playerTextShort_label);
        
        batteryImage_label.setIcon(batteryImage);
        menu_panel.add(batteryImage_label);

        // Progress bars for health
        AIHealthDisplay.setValue(AIHealth);
        AIHealthDisplay.setStringPainted(true);
        AIHealthDisplay.setForeground(new Color(50, 110, 40));
        AIHealthDisplay.setBackground(new Color(53, 53, 53));
        AIHealthDisplay.setFont(new Font("Agency FB", Font.BOLD, 35));
        health_panel.add(AIHealthDisplay, BorderLayout.NORTH);
        
        playerHealthDisplay.setValue(playerHealth);
        playerHealthDisplay.setStringPainted(true);
        playerHealthDisplay.setForeground(new Color(40, 60, 110));
        playerHealthDisplay.setBackground(new Color(53, 53, 53));
        playerHealthDisplay.setFont(new Font("Agency FB", Font.BOLD, 35));
        health_panel.add(playerHealthDisplay, BorderLayout.SOUTH);
        
        JButtonSetUp(healthDisplayImage_button, 1, "");
        healthDisplayImage_button.setIcon(new ImageIcon("healthDisplayOff.png"));
        health_panel.add(healthDisplayImage_button, BorderLayout.CENTER);
        
        startGame.setBorder(new LineBorder(Color.GRAY,0));
        startGame.setFocusable(false);
        startGame.setFocusPainted(true);
        startGame.addActionListener(this);
        startGame.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                playSound("characterClick.wav"); // Replace with your sound file path
            }
        });
        startGame.setIcon(new ImageIcon("title.png"));
        mainMenu_panel.add(startGame, BorderLayout.CENTER);
        
        returnToMainMenu.setBorder(new LineBorder(Color.GRAY,0));
        returnToMainMenu.setFocusable(false);
        returnToMainMenu.setFocusPainted(true);
        returnToMainMenu.addActionListener(this);
        returnToMainMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                playSound("characterClick.wav"); // Replace with your sound file path
            }
        });
        
        result_panel.add(returnToMainMenu, BorderLayout.CENTER);
    }

    @Override
    /**
     * This is our overarching actionPerformed method that details all the implementation of every action that
     * happens in the program. 
     * 
     * @param event = the object that is currently being interacted with
     */
    public void actionPerformed(ActionEvent event) {
    	if (event.getSource() == healthDisplayImage_button) {
    		if(virusOn) {
    			playerText_panel.setVisible(false);
    			resultMenuOn(false);
    		}
    	}
    	if (event.getSource() == playerAnswer[0]) {
    		if(AIGuessStatus) {
    			winCheck(true);
    		} else {
    			response = "yes";
    			AITracker(1);
        		AIElimination();
    		}
    	}
    	
    	if (event.getSource() == playerAnswer[1]) {
    		if(AIGuessStatus) {
    			if (identical) {
    				winCheck(false);
    			} else {
    				AIRetrace1();
    			}
    		} else {
    			response = "no";
    			AITracker(0);
        		AIElimination();
    		}
    	}
    	
    	if (event.getSource() == continueSmall) {
    		if (firstRoundSmall) {
    			playerTextShort_panel.setVisible(false);
            	continueSmall_panel.setVisible(false);
            	continue_panel.setVisible(true);
    		} else {
	    		if (retrace == false) {
	    			if (roundWinner == 0) {
	    				winCheck(false); 
	    			}else if (roundWinner == 1) {
	    				winCheck(true); 
	    			}else {
	                 AITurn();
	    			}
	    		} else if  (continueSmallStatus < 10){ 
	    			
	    			while(retraceResultDisplay[continueSmallStatus] == 0 && continueSmallStatus <= 11) {
	    				continueSmallStatus++;
	    			}
	    			if (continueSmallStatus == 0) {
	    				AIText_label.setText("YES--Is your agent " + yesGender);
	    				playerTextShort_label.setText("I should have said no");
	    			}else if (continueSmallStatus == 1) {
	    				AIText_label.setText("NO--Is your agent " + genderPlayer);
	    				playerTextShort_label.setText("I should have said yes");
	    			}else if (continueSmallStatus == 2) {
	    				AIText_label.setText("YES--Does your agent have " + yesEyes + " eyes");
	    				playerTextShort_label.setText("I should have said no");
	    			}else if (continueSmallStatus == 3) {
	    				AIText_label.setText("NO--Does your agent have " + eyesPlayer + " eyes");
	    				playerTextShort_label.setText("I should have said yes");
	    			}else if (continueSmallStatus == 4) {
	    				AIText_label.setText("YES--Is your agent's hair " + yesHair);
	    				playerTextShort_label.setText("I should have said no");
	    			}else if (continueSmallStatus == 5) {
	    				AIText_label.setText("NO--Is your agent's hair " + hairPlayer);
	    				playerTextShort_label.setText("I should have said yes");
	    			}else if (continueSmallStatus == 6) {
	    				String yesOrNo= "-1";
	    				String noOrYes = "-1";
	    				if (beardAIGuess == 1) {
	    					yesOrNo = "YES";
	    					noOrYes = "no";
	    				}else {
	    					yesOrNo = "NO";
	    					noOrYes = "yes";
	    				}
	    				AIText_label.setText(yesOrNo + "--Does your agent have a beard");
	    				playerTextShort_label.setText("I should have said "+ noOrYes);
	    			}else if (continueSmallStatus == 7) {
	    				String yesOrNo= "-1";
	    				String noOrYes = "-1";
	    				if (moustacheAIGuess == 1) {
	    					yesOrNo = "YES";
	    					noOrYes = "no";
	    				}else {
	    					yesOrNo = "NO";
	    					noOrYes = "yes";
	    				}
	    				AIText_label.setText(yesOrNo + "--Does your agent have a moustache");
	    				playerTextShort_label.setText("I should have said "+ noOrYes);
	    			}else if (continueSmallStatus == 8) {
	    				String yesOrNo= "-1";
	    				String noOrYes = "-1";
	    				if (bigNoseAIGuess == 1) {
	    					yesOrNo = "YES";
	    					noOrYes = "no";
	    				}else {
	    					yesOrNo = "NO";
	    					noOrYes = "yes";
	    				}
	    				AIText_label.setText(yesOrNo + "--Does your agent have a big nose");
	    				playerTextShort_label.setText("I should have said "+ noOrYes);
	    			}else if (continueSmallStatus == 9) {
	    				String yesOrNo= "-1";
	    				String noOrYes = "-1";
	    				if (glassesAIGuess == 1) {
	    					yesOrNo = "YES";
	    					noOrYes = "no";
	    				}else {
	    					yesOrNo = "NO";
	    					noOrYes = "yes";
	    				}
	    				AIText_label.setText(yesOrNo + "--Does your agent wear glasses");
	    				playerTextShort_label.setText("I should have said "+ noOrYes);
	    			}else if (continueSmallStatus == 10) {
	    				String yesOrNo= "-1";
	    				String noOrYes = "-1";
	    				if (hatAIGuess == 1) {
	    					yesOrNo = "YES";
	    					noOrYes = "no";
	    				}else {
	    					yesOrNo = "NO";
	    					noOrYes = "yes";
	    				}
	    				AIText_label.setText(yesOrNo + "--Does your agent wear a hat");
	    				playerTextShort_label.setText("I should have said "+ noOrYes);
	    			}
	    			continueSmallStatus++;
	    			while(retraceResultDisplay[continueSmallStatus] == 0 && continueSmallStatus <= 11) {
	    				continueSmallStatus++;
	    			}
	    			if (continueSmallStatus >= 11) {
	    				continueSmallStatus = 11;
	    			}
	    		} else {
	    			if (continueSmallStatus == 11) {
	    				AIText_label.setText("Looks like someone lose their cool~");
	    				playerTextShort_label.setText("Well...");
	    				continueSmallStatus++;
	    			} else if (continueSmallStatus == 12) {
	    				AIText_label.setText("And I won this round");
	    				playerTextShort_label.setText("...You wont beat me again");
	    				continueSmallStatus++;
	    			} else {
	    				continueSmall_panel.setVisible(false);
	    		    	playerTextShort_panel.setVisible(false);
	    				winCheck(true);
	    			}
	    		}
    		}
        }
    	
    	if (event.getSource() == continues) {
    		continueStatus += 1;
    		if (continueStatus == 1) {
    			if(firstRound) {
    				firstRoundSmall = false;
    				continues.setText("CONTINUE");
            		AIText_label.setText("Let's see whose agent will be found first");
    			}
    		}
    		if (continueStatus == 2) {
    			continues.setText("CONTINUE");
    			if(firstRound) {
    				AIText_label.setText("I'll be gentle and let you start first");
    			} else {
    				AIText_label.setText("You.. Will... LOSE!");
    			}
    			playerAction_panel.setVisible(true);
    			panelOnScreen = 2;
    			continue_panel.setVisible(false);
    		}
    		if (continueStatus > 2) {
    			continue_panel.setVisible(false);
    			playerAction_panel.setVisible(true);
    			panelOnScreen = 2;
    		}
    	}
        // Check if any of the buttons got pressed
        if (event.getSource() == startGame) {
        	if (startGameStatus == 0){
        		play.stopMusic();
        		play.playMusic(filePath);
        		startGame.setIcon(new ImageIcon("Intro1.png"));
        	}else if (startGameStatus == 1){
        		startGame.setIcon(new ImageIcon("Intro2.png"));
        	}else if (startGameStatus == 2){
        		startGame.setIcon(new ImageIcon("Intro3.png"));
        	}else if (startGameStatus == 3){
        		startGame.setIcon(new ImageIcon("Intro4.png"));
        	}else if (startGameStatus == 4){
        		mainMenu_panel.setVisible(false);
                inGamePanelsVisible(true);
        		startGame.setIcon(new ImageIcon("title.png"));
        	}
        	startGameStatus++;
        }
        if (event.getSource() == returnToMainMenu) {
            mainMenuOn();
            resetRound();
            result_panel.setVisible(false);
        }
        if (event.getSource() == playerAction[0]) { 
        	playerTextShort_label.setText("Click on a character to guess");
            playerAction_panel.setVisible(false);
            playerTextShort_panel.setVisible(true);
            revoke_panel.setVisible(true);
            panelOnScreen = 0;
        }
        if (event.getSource() == playerAction[1]) { 
            playerAction_panel.setVisible(false);
            characteristics_panel.setVisible(true);
            revoke_panel.setVisible(true);
            panelOnScreen = 1;
        }
        if (event.getSource() == revoke) { 
            if(panelOnScreen < 2) {
                playerTextShort_panel.setVisible(false);
                characteristics_panel.setVisible(false);
                revoke_panel.setVisible(false);
                playerAction_panel.setVisible(true);
                panelOnScreen = 2;
            }
            if(panelOnScreen > 2) {
                gender_panel.setVisible(false);
                eyes_panel.setVisible(false);
                hair_panel.setVisible(false);
                characteristics_panel.setVisible(true);
                panelOnScreen = 1;
            }
        }
        if (event.getSource() == characteristics[0]) { 
            characteristics_panel.setVisible(false);
            gender_panel.setVisible(true);
            panelOnScreen = 3;
        }
        if (event.getSource() == characteristics[1]) { 
            characteristics_panel.setVisible(false);
            eyes_panel.setVisible(true);
            panelOnScreen = 4;
        }
        if (event.getSource() == characteristics[2]) { 
            characteristics_panel.setVisible(false);
            hair_panel.setVisible(true);
            panelOnScreen = 5;
        }
        if (event.getSource() == characteristics[3]) { 
            boolean hasBeard = checkCharacterCharacteristic("beard");
            AIText_label.setText(hasBeard ? "YES" : "NO");
            characteristics_panel.setVisible(false);
            playerQuestionDisplay("Does your agent have a beard?");
        }
        if (event.getSource() == characteristics[4]) { 
            boolean hasMoustache = checkCharacterCharacteristic("moustache");
            AIText_label.setText(hasMoustache ? "YES" : "NO");
            characteristics_panel.setVisible(false);
            playerQuestionDisplay("Does your agent have a moustache?");
        }
        if (event.getSource() == characteristics[5]) { 
            boolean hasBigNose = checkCharacterCharacteristic("bigNose");
            AIText_label.setText(hasBigNose ? "YES" : "NO");
            characteristics_panel.setVisible(false);
            playerQuestionDisplay("Does your agent have a big nose?");
        }
        if (event.getSource() == characteristics[6]) { 
            boolean wearsGlasses = checkCharacterCharacteristic("glasses");
            AIText_label.setText(wearsGlasses ? "YES" : "NO");
            characteristics_panel.setVisible(false);
            playerQuestionDisplay("Does your agent wear glasses?");
        }
        if (event.getSource() == characteristics[7]) { 
            boolean wearsHat = checkCharacterCharacteristic("hat");
            AIText_label.setText(wearsHat ? "YES" : "NO");
            characteristics_panel.setVisible(false);
            playerQuestionDisplay("Does your agent wear a hat?");
        }
        if (event.getSource() == gender[0]) { 
            boolean isMale = checkCharacterCharacteristic("gender");
            AIText_label.setText(isMale ? "YES" : "NO");
            gender_panel.setVisible(false);
            playerQuestionDisplay("Is your agent a male?");
        }
        if (event.getSource() == gender[1]) { 
            boolean isFemale = !checkCharacterCharacteristic("gender");
            AIText_label.setText(isFemale ? "YES" : "NO");
            gender_panel.setVisible(false);
            playerQuestionDisplay("Is your agent a female?");
        }
        if (event.getSource() == eyes[0]) { 
            boolean hasBrownEyes = selectedCharacter.eyeColor.equals("Brown");
            AIText_label.setText(hasBrownEyes ? "YES" : "NO");
            eyes_panel.setVisible(false);
            playerQuestionDisplay("Does your agent have brown eyes?");
        }
        if (event.getSource() == eyes[1]) { 
            boolean hasBlueEyes = selectedCharacter.eyeColor.equals("Blue");
            AIText_label.setText(hasBlueEyes ? "YES" : "NO");
            eyes_panel.setVisible(false);
            playerQuestionDisplay("Does your agent have blue eyes?");
        }
        if (event.getSource() == eyes[2]) { 
            boolean hasGreenEyes = selectedCharacter.eyeColor.equals("Green");
            AIText_label.setText(hasGreenEyes ? "YES" : "NO");
            eyes_panel.setVisible(false);
            playerQuestionDisplay("Does your agent have green eyes?");
        }
        if (event.getSource() == hair[0]) { 
            boolean hasBlackHair = selectedCharacter.hairColor.equals("Black");
            AIText_label.setText(hasBlackHair ? "YES" : "NO");
            hair_panel.setVisible(false);
            playerQuestionDisplay("Is your agent's hair black?");
        }
        if (event.getSource() == hair[1]) { 
            boolean hasWhiteHair = selectedCharacter.hairColor.equals("White");
            AIText_label.setText(hasWhiteHair ? "YES" : "NO");
            hair_panel.setVisible(false);
            playerQuestionDisplay("Is your agent's hair white?");
        }
        if (event.getSource() == hair[2]) {
            boolean hasDarkBrownHair = selectedCharacter.hairColor.equals("Dark Brown");
            AIText_label.setText(hasDarkBrownHair ? "YES" : "NO");
            hair_panel.setVisible(false);
            playerQuestionDisplay("Is your agent's hair dark brown?");
        }
        if (event.getSource() == hair[3]) { 
            boolean hasLightBrownHair = selectedCharacter.hairColor.equals("Light Brown");
            AIText_label.setText(hasLightBrownHair ? "YES" : "NO");
            hair_panel.setVisible(false);
            playerQuestionDisplay("Is your agent's hair light brown?");
        }
        if (event.getSource() == hair[4]) { 
            boolean hasBlondeHair = selectedCharacter.hairColor.equals("Blonde");
            AIText_label.setText(hasBlondeHair ? "YES" : "NO");
            hair_panel.setVisible(false);
            playerQuestionDisplay("Is your agent's hair blonde?");
        }
        if (event.getSource() == hair[5]) { 
            boolean hasHighlights = selectedCharacter.hairColor.equals("Highlights");
            AIText_label.setText(hasHighlights ? "YES" : "NO");
            hair_panel.setVisible(false);
            playerQuestionDisplay("Is your agent's hair highlights?");
        }

        for (int i = 0; i < charactersOnScreen.size(); i++) {
        	if (event.getSource() == characterDisplay[i]) {
                String previousAppearance = String.valueOf(characterDisplay[i].getIcon());
                boolean interactive = characterDisplay[i].isShowing() && characterDisplay[i].isEnabled();
        		if (retrace == false) {
                    if (panelOnScreen == 0) {
                    	setUpImage(i, "target");
                    	if (charactersOnScreen.get(i).equals(selectedCharacter)) {
                            // Player guessed correctly
                    		if (AIHealth == 20){
                    			AIText_label.setText("...I guess you did it, but I'll be back");
                    			healthDisplayImage_button.setIcon(new ImageIcon("healthDisplayOn.png"));
                                playerTextShort_label.setText("...This is it.");
                    		} else {
	                            AIText_label.setText(charactersOnScreen.get(i).getName()+" is my agent, not bad.");
	                            playerTextShort_label.setText("You too, but I will win.");
                    		}
                    	
                            revoke_panel.setVisible(false);
                            continueSmall_panel.setVisible(true);
                        	playerTextShort_panel.setVisible(true);
                        	
                            roundWinner = 0;// Assuming false means player wins
                        } else {
                            // Player guessed incorrectly
                        	if (playerHealth == 20){
                    			AIText_label.setText("...I FOUND YOU. GOODBYE. IT WAS FUN.");
                                playerTextShort_label.setText("...I guess this is it.");
                                
                                playerTextShort_panel.setVisible(false);
                                revoke_panel.setVisible(false);
                                continueSmall_panel.setVisible(false);
                               } else {
	                            AIText_label.setText("Haha. Try again, human.");
	                            playerTextShort_label.setText("You will be defeated!");
	          
                    		}
                        	revoke_panel.setVisible(false);
                            continueSmall_panel.setVisible(true);
                        	playerTextShort_panel.setVisible(true);
                        	
                            roundWinner = 1;// Assuming true means AI wins
                        }
                
                    } else {
                        if (characterStatus[i] == 0) {
                            characterStatus[i] = 1;
                            setUpImage(i, "exclude");
                        } else if (characterStatus[i] == 1) {
                            characterStatus[i] = 0;
                            setUpImage(i, "default");
                        }
                    }
                } else {
                	if (continueSmallStatus == 0) {
	                	setUpImage(i, "target");
	                	AIRetrace2(charactersOnScreen.get(i));
                	}
                }
                // Mouse and webcam doClick both reach this same completed action.
                // Same target selected again is a no-op; toggling exclusion is a new action.
                if (interactive && !previousAppearance.equals(String.valueOf(characterDisplay[i].getIcon()))) {
                    try { characterFeedback.accept(controlName(characterDisplay[i])); }
                    catch (RuntimeException ex) {
                        System.err.println("ARDUINO feedback error; gameplay continues: " + ex.getMessage());
                    }
                }
        	}
        }
    }
    
    /**
     * A method to check whom (out of the AI or player) wins the round. The loser's health will have 20 points
     * deducted. 
     * 
     * @param AIWin boolean value dictating whether the AI wins or loses. true = AI wins, false = AI loses. 
     */
    public void winCheck(boolean AIWin) {
    	continueSmall_panel.setVisible(false);
    	playerTextShort_panel.setVisible(false);
    	if(AIWin) {
    		playerHealth -= 20;
    		playerHealthDisplay.setValue(playerHealth);
    		playerAnswer_panel.setVisible(false);
    		continue_panel.setVisible(true);
    		resetRound();
    		if(playerHealth <= 0) {
    			resultMenuOn(true);
    		}
    	} else {
    		AIHealth -= 20;
    		AIHealthDisplay.setValue(AIHealth);
    		playerAnswer_panel.setVisible(false);
    		continue_panel.setVisible(true);
    		resetRound();
    		if(AIHealth <= 0) {
    			continueSmall_panel.setVisible(false);
		    	playerTextShort_panel.setVisible(false);
		    	playerText_panel.setVisible(true);
                virusOn = true;
                playerTextLong_label.setText("Click the green button to upload the virus");
                continue_panel.setVisible(false);
    		}
    	}
    }
    
    /**
     * A method to add characteristics (not in relation to our agent characteristics) to the buttons. 
     * This method also adds a sound effect for whenever a button is clicked. 
     * 
     * @param button = the button that is being setup
     * @param textSize = size of text on the button
     * @param text = text on the button
     */
    public void JButtonSetUp(JButton button, int textSize, String text) {
    	button.setBorder(new LineBorder(Color.GRAY,0));
    	button.setFocusable(false);
    	button.setFocusPainted(true);
    	button.setHorizontalTextPosition(JLabel.CENTER);
    	button.setVerticalTextPosition(JLabel.CENTER);
    	button.setForeground(new Color(220, 130, 80));
    	button.setFont(new Font("Agency FB", Font.BOLD, textSize));
    	button.setText(text);
    	button.addActionListener(this);
    	button.addActionListener(new ActionListener() {
    	    @Override
    	    public void actionPerformed(ActionEvent e) {
    	        playSound("buttonClick.wav"); 
    	    }
    	});
    }
    
    /**
     * Method to display the question that the player will ask the AI. This is like a transition method. 
     * 
     * @param text = question
     */
    public void playerQuestionDisplay(String text) {
        characteristics_panel.setVisible(false);
        gender_panel.setVisible(false);
        eyes_panel.setVisible(false);
        hair_panel.setVisible(false);
        revoke_panel.setVisible(false);
        playerTextShort_label.setText(text);
        playerTextShort_panel.setVisible(true);
        continueSmall_panel.setVisible(true);
    	playerTextShort_panel.setVisible(true);
    	revoke_panel.setVisible(false);
        panelOnScreen = -1;
    }

    /**
     * A method to reset the game after each round. This means, setting variables to their default starting
     * values, turning certain elements of the frame off, and others on, etc. 
     */
    public void resetRound() {
    	roundWinner = -1;
    	int[] retraceResultDisplay = {1,1,1,1,1,1,1,1,1,1,1,0,0,0};
    	playerTextShort_panel.setVisible(false);
        continueStatus = 1;
        continueSmallStatus = 0;
        firstRound = false;
        firstRoundSmall = false;
        askedQuestions.clear();
        yesGender = "-1";
        yesEyes = "-1";
        yesHair = "-1";
        int[] genderAIGuess = {-1, -1};
        // Blue, Green, Brown
        int[] eyesAIGuess = {-1, -1, -1};
        // Black, White, Blonde, Dark brown, Light brown, Highlights
        int[] hairAIGuess = {-1, -1, -1, -1, -1, -1};
        beardAIGuess = -1;
        moustacheAIGuess = -1;
        bigNoseAIGuess = -1;
        glassesAIGuess = -1;
        hatAIGuess = -1;
        genderPlayer = "-1";
        eyesPlayer = "-1";
        hairPlayer = "-1";
        beardPlayer = -1;
        moustachePlayer = -1;
        bigNosePlayer = -1;
        glassesPlayer = -1;
        hatPlayer = -1;
        retrace = false;
        selectRandomCharacters(); // Re-select a character for the new round
        panelOnScreen = -1;
        AIGuessStatus = false;
        identical = false;
        
        // Update the character display
        updateButtons();
        
        AIText_label.setText("I will defeat you!!");
        continues.setText("CLICK TO CONFIRM: I HAVE RE-CHOSEN MY AGENT");
    }

    /**
     * A general method to turn most of the in game panels on/off.
     * 
     * @param status = true or false, to turn on or off
     */
    public void inGamePanelsVisible(boolean status) {    
        AI_panel.setVisible(status);
        player_panel.setVisible(status);
        AIText_panel.setVisible(status);
        center_panel.setVisible(status);
        if(firstRound) {
        	playerTextShort_panel.setVisible(status);
        	continueSmall_panel.setVisible(status);
        } else {
        	continue_panel.setVisible(status);
        }
        health_panel.setVisible(status);
        menu_panel.setVisible(status);
    }

    /**
     * General method to turn the main menu on, enabling all the elements that are present when the main game
     * screen is first seen. 
     */
    public void mainMenuOn() {
        inGamePanelsVisible(false);
        result_panel.setVisible(false);
        playerAction_panel.setVisible(false);
        revoke_panel.setVisible(false);
        playerTextShort_panel.setVisible(false);
        playerAnswer_panel.setVisible(false);
        characteristics_panel.setVisible(false);
        gender_panel.setVisible(false);
        eyes_panel.setVisible(false);
        hair_panel.setVisible(false);
        mainMenu_panel.setVisible(true);
        firstRound = true;
        firstRoundSmall = true;
        virusOn = false;
        startGameStatus = 0;
        continueStatus = 0;
        playerHealth = 100;
        AIHealth = 100;
        playerHealthDisplay.setValue(100);
        AIHealthDisplay.setValue(100);
        playerTextShort_label.setText("(Click an agent to exclude/include)");
    }

    /**
     * General method to turn the result panel on, displaying either that the player won or lost. 
     * 
     * @param AIwins = true or false, AI wins or AI loses
     */
    public void resultMenuOn(boolean AIwins) {
    	healthDisplayImage_button.setIcon(new ImageIcon("healthDisplayOff.png"));
    	continueSmall_panel.setVisible(false);
    	playerTextShort_panel.setVisible(false);
    	playerAnswer_panel.setVisible(false);
        inGamePanelsVisible(false);
        if (AIwins) { // AI WINS
        	returnToMainMenu.setIcon(new ImageIcon("lose.png"));
        } else { // PLAYER WINS
        	returnToMainMenu.setIcon(new ImageIcon("win.png"));
        }
        result_panel.setVisible(true);
    }
    
    /**
     * Method using switch case to check each characteristic of a certain character. 
     * 
     * @param characteristic the characteristic that is being checked
     * @return = if the character has that characteristic or not
     */
    private boolean checkCharacterCharacteristic(String characteristic) {
        switch (characteristic) {
            case "gender":
                return selectedCharacter.gender.equals("Male");
            case "beard":
                return selectedCharacter.hasBeard; // Returns true if the character has a beard
            case "moustache":
                return selectedCharacter.hasMoustache; // Returns true if the character has a moustache
            case "bigNose":
                return selectedCharacter.hasBigNose; // Returns true if the character has a big nose
            case "glasses":
                return selectedCharacter.wearsGlasses; // Returns true if the character wears glasses
            case "hat":
                return selectedCharacter.wearsHat; // Returns true if the character wears a hat
            default:
                return false; // If the characteristic is not recognized, return false
        }
    }
    
    // Just a thing we tried to do, where the program would pause for some time, but didn't work out.
//    public void stop(int second) {
//    	try {
//			Thread.sleep(second*1000);
//		} catch (InterruptedException e) {
//			Thread.currentThread().interrupt();
//		}
//    }
    
    /**
     * A method that implements the AI question selection logic. How it works has been documented in the
     * "AI strategy — James Chen" google doc, located inside the "2. Planning" folder. 
     * 
     * @return
     */
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
            if (askedQuestions.contains(question)) continue; // Skip if already asked

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
            askedQuestions.add(bestQuestion); // Add the selected question to the set
        }

        return bestQuestion;
    }

    /**
     * Method where the AI will get rid of all the characters which does not match the player's response.
     * (e.g. player answers no to hat, AI will get rid of all characters without a hat.)
     * 
     * @param question = the question being asked
     * @param answer = the answer the player gave
     */
    public void filterCharacters(String question, boolean answer) {
    	// We created a new list of filtered characters to prevent overlapping
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

            // Filter based on the player's answer
            if (answer && hasTrait) {
                filteredCharacters.add(character); // Keep if the answer is yes and the character has the trait
            } else if (!answer && !hasTrait) {
                filteredCharacters.add(character); // Keep if the answer is no and the character does not have the trait
            }
        }
        allCharacters = filteredCharacters; // Update the list of characters
    }
    
    /**
     * A method that is part of the main game loop. This is the first part of the AI's turn. 
     */
    public void AITurn() {
    	// There are two pairs of characters which are identical in all characteristics. Therefore, we made it
    	// so that when the AI eliminates all characters but the two identical characters, the AI will not
    	// ask forever and break, but instead choose a random one out of the two to guess. 
    	if (allCharacters.size() == 2) {
    		if (allCharacters.get(0).getName().equals("AA04") && allCharacters.get(1).getName().equals("KE25")){
    			identical = true;
    		}
    		if (allCharacters.get(1).getName().equals("AA04") && allCharacters.get(0).getName().equals("KE25")){
    			identical = true;
    		}
    		if (allCharacters.get(0).getName().equals("BY08") && allCharacters.get(1).getName().equals("LZ29")){
    			identical = true;
    		}
    		if (allCharacters.get(1).getName().equals("BY08") && allCharacters.get(0).getName().equals("LZ29")){
    			identical = true;
    		}
    	}
    	
    	if (allCharacters.size() == 1) {
    		AIGuessStatus = true; 
    		AIText_label.setText("Is your agent: " + allCharacters.get(0).getName());
    		continueSmall_panel.setVisible(false);
        	playerTextShort_panel.setVisible(false);
        	playerAnswer_panel.setVisible(true);
        } else if(identical == true){
        	AIGuessStatus = true;
        	Random random = new Random();
        	 
    		AIText_label.setText("Is your agent: " + allCharacters.get(random.nextInt(2)).getName());
    		continueSmall_panel.setVisible(false);
        	playerTextShort_panel.setVisible(false);
        	playerAnswer_panel.setVisible(true);
        	
        }else {
        	continueSmall_panel.setVisible(false);
        	playerTextShort_panel.setVisible(false);
        	playerAnswer_panel.setVisible(true);
        	question = selectQuestion();
        	AIText_label.setText(question);
        }
    }
    
    /**
     * Another part of the AI's turn
     */
    public void AIElimination() {
        filterCharacters(question, response.equals("yes"));
        playerAnswer_panel.setVisible(false);
        if (allCharacters.size() <= 0) {
    		AIRetrace1();
        }
        AIText_label.setText("ELIMINATION IN PROGRESS...");
        continue_panel.setVisible(true);
    }
    
    /**
     * More of the AI's turn. All in different methods so that we can call separate parts of the turn when we need to.
     */
    public void AIRetrace1() {
    	retrace = true;
    	AIText_label.setText("You made a mistake! You are inferior to me!");
    	playerAnswer_panel.setVisible(false);
    	playerTextLong_label.setText("Click on your agent");
    	playerText_panel.setVisible(true);
    }
    
    /**
     * More of the AI's turn
     * 
     * @param character
     */
    public void AIRetrace2(Character1 character) {
    	genderPlayer = character.getGender();
    	eyesPlayer = character.getEyeColor();
    	hairPlayer = character.getHairColor();
    	if (character.hasMoustache()) {
    		moustachePlayer = 1;
    	} else {
    		moustachePlayer = 0;
    	}
    	if (character.hasBeard()) {
    		beardPlayer = 1;
    	} else {
    		beardPlayer = 0;
    	}
    	if (character.hasBigNose()) {
    		bigNosePlayer = 1;
    	} else {
    		bigNosePlayer = 0;
    	}
    	if (character.wearsGlasses()) {
    		glassesPlayer = 1;
    	} else {
    		glassesPlayer = 0;
    	}
    	if (character.wearsHat()) {
    		hatPlayer = 1;
    	} else {
    		hatPlayer = 0;
    	}
    	
    	if(yesGender.equals(genderPlayer) || yesGender.equals("-1")) {//Player said yes when they should
    		retraceResultDisplay[0] = 0;
    	}
    	if((genderPlayer.equals("Male")&&genderAIGuess[0] != 0) ||(genderPlayer.equals("Female")&&genderAIGuess[1] != 0)) {//Player said no when they should
    		retraceResultDisplay[1] = 0;
    	}
    	if(yesEyes.equals(eyesPlayer) || yesEyes.equals("-1")) {//Player said yes when they should
    		retraceResultDisplay[2] = 0;
    	}
    	if((eyesPlayer.equals("Blue")&&eyesAIGuess[0] != 0) || (eyesPlayer.equals("Green")&&eyesAIGuess[1] != 0) || (eyesPlayer.equals("Brown")&&eyesAIGuess[2] != 0)) {//Player said no when they should
    		retraceResultDisplay[3] = 0;
    	}
    	if(yesHair.equals(hairPlayer) || yesHair.equals("-1")) {//Player said yes when they should
    		retraceResultDisplay[4] = 0;
    	}
    	if((hairPlayer.equals("Black")&&hairAIGuess[0] != 0) || (hairPlayer.equals("White")&&hairAIGuess[1] != 0) || (hairPlayer.equals("Blonde")&&hairAIGuess[2] != 0) || (hairPlayer.equals("Dark brown")&&hairAIGuess[3] != 0) || (hairPlayer.equals("Light brown")&&hairAIGuess[4] != 0) || (hairPlayer.equals("Highlights")&&hairAIGuess[5] != 0)) {//Player said no when they should
    		retraceResultDisplay[5] = 0;
    	}
    	if(beardAIGuess == beardPlayer || beardAIGuess == -1) {//Player said yes or no when they should
    		retraceResultDisplay[6] = 0;
    	}
    	if(moustacheAIGuess == moustachePlayer || moustacheAIGuess == -1) {//Player said yes or no when they should
    		retraceResultDisplay[7] = 0;
    	}
    	if(bigNoseAIGuess == bigNosePlayer || bigNoseAIGuess == -1) {//Player said yes or no when they should
    		retraceResultDisplay[8] = 0;
    	}
    	if(glassesAIGuess == glassesPlayer || glassesAIGuess == -1) {//Player said yes or no when they should
    		retraceResultDisplay[9] = 0;
    	}
    	if(hatAIGuess == hatPlayer || hatAIGuess == -1) {//Player said yes or no when they should
    		retraceResultDisplay[10] = 0;
    	}
    	
    	
    	playerText_panel.setVisible(false);
    	AIText_label.setText("But I will be gentle and point out your mistakes.");
    	continueSmall_panel.setVisible(true);
    	playerTextShort_panel.setVisible(true);
    	playerTextShort_label.setText("...Okay.");
    	
    }
    
    /**
     * A method to setup all the images of the characters. It will check every randomly selected character on the
     * board, and assign them their respective image. We also included a variant of the image which would
     * be an excluded version of the original (i.e. grey background) whenever the user clicked said character,
     * so that the user can keep track of which characters they have eliminated from the potential answer. 
     * 
     * @param i
     * @param variant
     */
    public void setUpImage(int i, String variant) {
    	if ((charactersOnScreen.get(i).getName()).equals("AD01")) {
    		if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("AD01.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("AD01exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("AD01target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("AX02")) {
            if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("AX02.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("AX02exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("AX02target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("AY03")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("AY03.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("AY03exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("AY03target.png"));
        	}
            
        } else if ((charactersOnScreen.get(i).getName()).equals("AA04")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("AA04.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("AA04exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("AA04target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("AE05")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("AE05.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("AE05exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("AE05target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("BN06")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("BN06.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("BN06exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("BN06target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("BD07")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("BD07.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("BD07exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("BD07target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("BY08")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("BY08.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("BY08exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("BY08target.png"));
        	}        
        } else if ((charactersOnScreen.get(i).getName()).equals("BL09")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("BL09.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("BL09exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("BL09target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("CN10")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("CN10.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("CN10exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("CN10target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("CS11")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("CS11.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("CS11exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("CS11target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("CE12")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("CE12.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("CE12exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("CE12target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("DL13")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("DL13.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("DL13exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("DL13target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("DD14")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("DD14.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("DD14exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("DD14target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("EA15")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("EA15.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("EA15exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("EA15target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("EC16")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("EC16.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("EC16exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("EC16target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("FH17")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("FH17.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("FH17exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("FH17target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("FS18")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("FS18.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("FS18exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("FS18target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("GE19")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("GE19.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("GE19exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("GE19target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("GE20")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("GE20.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("GE20exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("GE20target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("HN21")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("HN21.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("HN21exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("HN21target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("HY22")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("HY22.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("HY22exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("HY22target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("JE23")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("JE23.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("JE23exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("JE23target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("RC24")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("RC24.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("RC24exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("RC24target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("KE25")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("KE25.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("KE25exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("KE25target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("LA26")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("LA26.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("LA26exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("LA26target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("LO27")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("LO27.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("LO27exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("LO27target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("LY28")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("LY28.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("LY28exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("LY28target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("LZ29")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("LZ29.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("LZ29exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("LZ29target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("MA30")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("MA30.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("MA30exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("MA30target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("MX31")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("MX31.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("MX31exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("MX31target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("MA32")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("MA32.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("MA32exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("MA32target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("ME33")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("ME33.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("ME33exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("ME33target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("NK34")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("NK34.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("NK34exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("NK34target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("QA35")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("QA35.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("QA35exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("QA35target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("PL36")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("PL36.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("PL36exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("PL36target.png"));
        	}
        } else if ((charactersOnScreen.get(i).getName()).equals("PR37")) {
        	if (variant.equals("default")) {
    			characterDisplay[i].setIcon(new ImageIcon("PR37.png"));
    		} else if (variant.equals("exclude")) {
                characterDisplay[i].setIcon(new ImageIcon("PR37exclude.png"));
        	} else {
        		characterDisplay[i].setIcon(new ImageIcon("PR37target.png"));
        	}
        }
    }
    
    /**
     * This method is purely to setup the character buttons. This is separated from the previous setup buttons section
     * so that we may assign a different on click sound effect to the characters. 
     */
    public void setUpButtons() {
    	for (int i = 0; i < charactersOnScreen.size(); i++) {
    		characterDisplay[i] = new JButton();
    		center_panel.add(characterDisplay[i]);
    		characterDisplay[i].setBorder(new LineBorder(Color.GRAY,0));
    		characterDisplay[i].setFocusable(false);
    		characterDisplay[i].setFocusPainted(true);
    		characterDisplay[i].setHorizontalTextPosition(JLabel.CENTER);
    		characterDisplay[i].setVerticalTextPosition(JLabel.CENTER);
    		characterDisplay[i].setForeground(new Color(220, 130, 80));
    		characterDisplay[i].setFont(new Font("Agency FB", Font.BOLD, 30));
    		characterDisplay[i].setText("");
    		characterDisplay[i].addActionListener(this);
            characterStatus[i] = 0;
            setUpImage(i, "default");
            characterDisplay[i].addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    playSound("characterClick.wav"); // Replace with your sound file path
                }
            });
    	}
    }
    
    /**
     * Simple button updater method that goes through every character on screen, when the characters are refreshed. 
     */
    public void updateButtons() {
    	for (int i = 0; i < charactersOnScreen.size(); i++) {
    		characterStatus[i] = 0;
            setUpImage(i, "default");
        }
    }
    
    /**
     * Method to track the player's answer to the AI's questions. This way, we can compare these later to
     * the player's actual character, if the player answers no when the AI makes a guess. That is to say, 
     * if the AI has lowered its list of potential characters down to one, and makes a guess, but the player
     * denies the AI's guess, then the player must have answered a question(s) wrong. By keeping track of the player's
     * answers, we can do a comparison with the player's actual character, and display all the questions that
     * the player answered incorrectly. 
     * 
     * @param status
     */
    public void AITracker(int status) {
    	if (question.equals("Does your agent have a beard?")) {
            beardAIGuess = status;
        } else if (question.equals("Does your agent have a moustache?")) {
        	moustacheAIGuess = status;
        } else if (question.equals("Does your agent have a big nose?")) {
        	bigNoseAIGuess = status;
        } else if (question.equals("Does your agent wear glasses?")) {
        	glassesAIGuess = status;
        } else if (question.equals("Does your agent wear a hat?")) {
        	hatAIGuess = status;
        } else if (question.equals("Is your agent a male?")) {
        	if (status == 1) {
        		yesGender = "Male";
        	}
        	genderAIGuess[0] = status;
        } else if (question.equals("Is your agent a female?")) {
        	if (status == 1) {
        		yesGender = "Female";
        	}
        	genderAIGuess[1] = status;
        } else if (question.equals("Does your agent have brown eyes?")) {
        	if (status == 1) {
        		yesEyes = "Brown";
        	}
        	eyesAIGuess[2] = status;
        } else if (question.equals("Does your agent have blue eyes?")) {
        	if (status == 1) {
        		yesEyes = "Blue";
        	}
        	eyesAIGuess[0] = status;
        } else if (question.equals("Does your agent have green eyes?")) {
        	if (status == 1) {
        		yesEyes = "Green";
        	}
        	eyesAIGuess[1] = status;
        } else if (question.equals("Is your agent's hair black?")) {
        	if (status == 1) {
        		yesHair = "Black";
        	}
        	hairAIGuess[0] = status;
        } else if (question.equals("Is your agent's hair white?")) {
        	if (status == 1) {
        		yesHair = "White";
        	}
        	hairAIGuess[1] = status;
        } else if (question.equals("Is your agent's hair dark brown?")) {
        	if (status == 1) {
        		yesHair = "Dark brown";
        	}
        	hairAIGuess[3] = status;
        } else if (question.equals("Is your agent's hair light brown?")) {
        	if (status == 1) {
        		yesHair = "Light brown";
        	}
        	hairAIGuess[4] = status;
        } else if (question.equals("Is your agent's hair blonde?")) {
        	if (status == 1) {
        		yesHair = "Blonde";
        	}
        	hairAIGuess[2] = status;
        } else if (question.equals("Is your agent's hair highlights?")) {
        	if (status == 1) {
        		yesHair = "Highlights";
        	}
        	hairAIGuess[5] = status;
        }
    }
    
    /**
     * Method to play sound effects
     * 
     * @param soundFilePath
     */
    public void playSound(String soundFilePath) {
    	try {
            File soundFile = new File(soundFilePath);
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Did you know you can have a class within a class? Class-ception! 
    class Player {
    	private Clip clip;
    	
    	/**
    	 * Method to play music, using various java classes. 
    	 * 
    	 * @param musicLoc
    	 */
    	void playMusic(String musicLoc) {
            try {
                File musicPath = new File(musicLoc);
                if (musicPath.exists()) {
                    AudioInputStream audioInput = AudioSystem.getAudioInputStream(musicPath);
                    clip = AudioSystem.getClip();
                    clip.open(audioInput);
                    clip.start();
                    clip.loop(Clip.LOOP_CONTINUOUSLY);
                } else {
                    System.out.println("Couldn't find Music file");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    	

    	/**
    	 * Method to stop music, which we will use when closing the frame. Otherwise the music would run forever, 
    	 * unless the program is terminated. 
    	 */
    	void stopMusic() {
            if (clip != null && clip.isRunning()) {
                clip.stop(); // Stop the audio
                clip.close(); // Close the clip
            }
        }
    }
}
