package fluddokt.opsu.fake;

public class SoundStore {

	public static SoundStore single;
	public static SoundStore get() {
		// TODO Auto-generated method stub
		if(single==null)
			single = new SoundStore();
		return single;
	}

	public void setMusicVolume(float musicVolume) {
		// TODO Auto-generated method stub
		
	}

	public int getSourceCount() {
		// TODO Auto-generated method stub
		return 0;
	}

}
