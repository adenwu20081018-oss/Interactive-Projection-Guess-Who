package groupDGuessWho;

public class Character1 {
	String id;
	String gender;
	String eyeColor;
	String hairColor;
	boolean hasBeard;
	boolean hasMoustache;
	boolean hasBigNose;
	boolean wearsGlasses;
	boolean wearsHat;

	// Constructor
	public Character1(String id, String gender, String eyeColor, String hairColor, boolean hasBeard,
			boolean hasMoustache, boolean hasBigNose, boolean wearsGlasses, boolean wearsHat) {
		this.id = id;
		this.gender = gender;
		this.eyeColor = eyeColor;
		this.hairColor = hairColor;
		this.hasBeard = hasBeard;
		this.hasMoustache = hasMoustache;
		this.hasBigNose = hasBigNose;
		this.wearsGlasses = wearsGlasses;
		this.wearsHat = wearsHat;
	}

	// Getters for the attributes
	
	public String getName() {
        return id; 
    }
	
	public boolean hasBeard() {
		return hasBeard;
	}

	public boolean hasMoustache() {
		return hasMoustache;
	}

	public boolean hasBigNose() {
		return hasBigNose;
	}

	public boolean wearsGlasses() {
		return wearsGlasses;
	}

	public boolean wearsHat() {
		return wearsHat;
	}

	public String getGender() {
		return gender;
	}

	public String getEyeColor() {
		return eyeColor;
	}

	public String getHairColor() {
		return hairColor;
	}
	
	// Setters
	
	public void setHairColor(String hair) {
		this.hairColor = hair;
	}
	
	public void setEyeColor(String eye) {
		this.eyeColor = eye;
	}
	
	public void setGender(String gender) {
		this.gender = gender;
	}
	
	public void setWearsHat(boolean hat) {
		this.wearsHat = hat;
	}
	
	public void setWearsGlasses(boolean glasses) {
		this.wearsGlasses = glasses;
	}
	
	public void setHasBigNose(boolean bigNose) {
		this.hasBigNose = bigNose;
	}
	
	public void setHasMoustache(boolean moustache) {
		this.hasMoustache = moustache;
	}
	
	public void setHasBeard(boolean beard) {
		this.hasBeard = beard;
	}
}