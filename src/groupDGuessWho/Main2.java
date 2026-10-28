package groupDGuessWho;
public class Main2 {
 public static void main(String[] args) {
     AIQuestionSelector aiSelector = new AIQuestionSelector();
     String questionToAsk = aiSelector.selectQuestion();
     System.out.println("AI asks: " + questionToAsk);
 }
}