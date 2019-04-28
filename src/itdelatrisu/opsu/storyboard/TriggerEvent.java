package itdelatrisu.opsu.storyboard;

public enum TriggerEvent {
	Passing, Failing, HitSoundFinish, HitSoundWhistle, HitSoundClap, None;
	
	public static TriggerEvent toTriggerEvent(String s) {
		switch(s) {
			case "Passing": return Passing;
			case "Failing": return Failing;
			case "HitSoundFinish": return HitSoundFinish;
			case "HitSoundWhistle": return HitSoundWhistle;
			case "HitSoundClap": return HitSoundClap;
			default: return None;
		}
	}
}
