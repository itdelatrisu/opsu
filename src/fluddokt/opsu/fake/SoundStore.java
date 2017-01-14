package fluddokt.opsu.fake;

public class SoundStore {

	private static Music music;
	public static SoundStore single;

	public static SoundStore get() {
		if (single == null)
			single = new SoundStore();
		return single;
	}

	public void setMusicVolume(float musicVolume) {
		if(music != null){
			music.setMusicVolume(musicVolume);
		}
	}

	public void setMusic(Music music2) {
		SoundStore.music = music2;
	}

	public void setMusicPitch(float pitch) {
		music.setPitch(pitch);
		
	}

}
