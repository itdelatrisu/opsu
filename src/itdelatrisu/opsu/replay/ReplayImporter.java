package itdelatrisu.opsu.replay;

import itdelatrisu.opsu.ErrorHandler;
import itdelatrisu.opsu.Options;
import itdelatrisu.opsu.OsuFile;
import itdelatrisu.opsu.OsuGroupList;
import itdelatrisu.opsu.ScoreData;
import itdelatrisu.opsu.db.ScoreDB;

import java.io.File;
import java.io.IOException;

public class ReplayImporter {
	public static void importAllReplaysFromDir(File dir) {
		for (File replayToImport : dir.listFiles()) {
			try {
				Replay r = new Replay(replayToImport);
				r.loadHeader();
				OsuFile oFile = OsuGroupList.get().getFileFromBeatmapHash(r.beatmapHash);
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
					System.out.println("Moving "+replayToImport+" to "+moveToFile);
					if(
						!replayToImport.renameTo(moveToFile)
					){
						System.out.println("Rename Failed "+moveToFile);
					}
					data.replayString = replayToImport.getName().substring(0, replayToImport.getName().length()-4);
					ScoreDB.addScore(data);;
				} else {
					System.out.println("Could not find beatmap for replay "+replayToImport);
					//ErrorHandler.error("Could not find beatmap for replay "+replayToImport, null, false);
				}
			} catch (IOException e) {
				//e.printStackTrace();
				System.out.println(e);
			}
			
		}
	}
}
