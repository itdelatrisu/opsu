package fluddokt.opsu.fake;

public class SoundStore {

	public static SoundStore single;

	public static SoundStore get() {
		if (single == null)
			single = new SoundStore();
		return single;
	}

	public void setMusicVolume(float musicVolume) {
		GameContainer.setMusicVolume(musicVolume);
	}

	public int getSourceCount() {
		// TODO Auto-generated method stub
		return 0;
	}

}
