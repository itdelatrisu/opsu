package itdelatrisu.opsu.replay;

import itdelatrisu.opsu.ErrorHandler;
import itdelatrisu.opsu.Options;
import itdelatrisu.opsu.ScoreData;
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.beatmap.BeatmapSetList;
import itdelatrisu.opsu.db.ScoreDB;

import java.io.File;
import java.io.IOException;

import org.newdawn.slick.util.Log;

public class ReplayImporter {
	public static void importAllReplaysFromDir(File dir) {
		for (File replayToImport : dir.listFiles()) {
			try {
				Replay r = new Replay(replayToImport);
				r.loadHeader();
				Beatmap oFile = BeatmapSetList.get().getFileFromBeatmapHash(r.beatmapHash);
				if(oFile != null){
					File replaydir = Options.getReplayDir();
					if (!replaydir.isDirectory()) {
						if (!replaydir.mkdir()) {
							ErrorHandler.error("Failed to create replay directory.", null, false);
							return;
						}
					}
					//ErrorHandler.error("Importing"+replayToImport+" forBeatmap:"+oFile, null, false);
					ScoreData data = r.getScoreData(oFile);
					File moveToFile = new File(replaydir, replayToImport.getName());
					if(
						!replayToImport.renameTo(moveToFile)
					){
						Log.warn("Rename Failed "+moveToFile);
					}
					data.replayString = replayToImport.getName().substring(0, replayToImport.getName().length()-4);
					ScoreDB.addScore(data);;
				} else {
					Log.warn("Could not find beatmap for replay "+replayToImport);
				}
			} catch (IOException e) {
				Log.warn("Failed to import replays ",e);
			}
			
		}
	}
}
