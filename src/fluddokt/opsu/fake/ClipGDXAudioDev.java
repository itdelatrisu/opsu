package fluddokt.opsu.fake;

import java.net.URL;

public class ClipGDXAudioDev extends ClipImplementation {

	AudioDevicePlayer player;
	public ClipGDXAudioDev(URL url, boolean isMP3, LineListener listener) {
		//try {
			/*InputStream in;
			in = url.openStream();
			
			int len = in.available();
			System.out.println("URLLen:"+len);
			
			byte[] buf = new byte[len];
			int totalRead = 0;
			while( totalRead < len){
				int read = in.read(buf, totalRead, len - totalRead);
				if(read < 0)
					break;
				totalRead += read;
			}
			for(int i =0; i<buf.length; i++){
				System.out.print(buf[i]+" ");
			}
			System.out.println("URLLen2:"+ in.available());
			in.close();*/
		if(isMP3)
			player = new AudioDevicePlayer(new UrlInputStreamFactory(url), url.toString());
		else
			throw new Error(" Not Mp3 AudioDevice not supported");
		//} catch (IOException e) {
		//	e.printStackTrace();
		//}
	}
	
	@Override
	public void stop() {
		player.stop();
	}

	@Override
	public int play(float volume, final LineListener listener) {
		player.setVolume(volume);
		player.play();
		player.setAudioDeviceListener(new AudioDeviceListener() {
			
			@Override
			public void complete(AudioDevicePlayer thisAudioDevicePlayer) {
				listener.update(new LineEvent(LineEvent.Type.STOP));
			}
		});
		return 0;
	}
	
	public void destroy(){
		player.dispose();
	}


}
