package fluddokt.opsu.fake;

public interface AudioDeviceListener {
	public void complete(AudioDevicePlayer thisAudioDevicePlayer);
	public void requestSync(AudioDevicePlayer thisAudioDevicePlayer);
}
