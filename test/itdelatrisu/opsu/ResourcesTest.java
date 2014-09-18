package itdelatrisu.opsu;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import itdelatrisu.opsu.Resources.Origin;
import itdelatrisu.opsu.Resources.Resource;
import itdelatrisu.opsu.Resources.SoundResource;
import itdelatrisu.opsu.states.Options.OpsuOptions;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class ResourcesTest {
	@Test
	public void testOpenGameResource() throws Exception {
		OpsuOptions options = mock(OpsuOptions.class);
		Resources resources = new Resources(options);
		
		assertContent(resources.openResource("emptyFile", Origin.GAME), "game").close();
	}
	
	@Rule
	
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void testOpenSkinResource() throws Exception {
		File skinDir = temporaryFolder.newFolder();
		
		OpsuOptions options = mock(OpsuOptions.class);
		when(options.getSkinDir()).thenReturn(skinDir);
		
		Resources resources = new Resources(options);
		
		try(FileOutputStream fos = new FileOutputStream(new File(skinDir, "emptyFile"))) {
			fos.write("skin".getBytes());
		}
		assertContent(resources.openResource("emptyFile", Origin.SKIN), "skin").close();
	}

	@Test
	public void testOpenBeatmapResource() throws Exception {
		File beatmapDir = temporaryFolder.newFolder();
		
		OpsuOptions options = mock(OpsuOptions.class);
		when(options.isBeatmapSkinIgnored()).thenReturn(false);
		
		Resources resources = new Resources(options);
		
		resources.setCurrentBeatmapDir(beatmapDir);
		
		try(FileOutputStream fos = new FileOutputStream(new File(beatmapDir, "emptyFile"))) {
			fos.write("beatmap".getBytes());
		}
		assertContent(resources.openResource("emptyFile", Origin.BEATMAP), "beatmap").close();
	}
	
	@Test
	public void testLoadSoundResource() throws Exception {
		SoundResource resource = mock(SoundResource.class);
		when(resource.getName()).thenReturn("shutter");
		when(resource.getExtensions()).thenReturn(Arrays.asList(".mp3", ".wav"));
		when(resource.getOrigins()).thenReturn(Arrays.asList(Origin.SKIN, Origin.GAME));
		
		OpsuOptions options = mock(OpsuOptions.class);
		when(options.isLoadFromOrigin(any(Resource.class), any(Origin.class))).thenReturn(true);
		when(options.getSkinDir()).thenReturn(temporaryFolder.newFolder());
		
		Resources resources = new Resources(options);
		
		assertNotNull(resources.loadResource(resource, null));
	}
	
	public static InputStream assertContent(InputStream is, String string) throws IOException {
		byte[] bytes = string.getBytes();
		byte[] buf = new byte[bytes.length];
		
		assertEquals(bytes.length, is.read(buf));
		assertArrayEquals(bytes, buf);
		
		return is;
	}
}
