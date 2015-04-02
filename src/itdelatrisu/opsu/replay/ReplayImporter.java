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
		System.out.println(OsuGroupList.get().beatmapHashesToFile);
		for (File replayToImport : dir.listFiles()) {
			try {
				Replay r = new Replay(replayToImport);
				r.load();
				OsuFile oFile = OsuGroupList.get().getFileFromBeatmapHash(r.beatmapHash);
				if(oFile != null){
					//ErrorHandler.error("Importing"+replayToImport+" forBeatmap:"+oFile, null, false);
					ScoreData data = r.getScoreData(oFile);
					File moveToFile = new File(Options.getReplayDir(),replayToImport.getName());
					System.out.println("Moving "+replayToImport+" to "+moveToFile);
					if(
						!replayToImport.renameTo(moveToFile)
					){
						System.out.println("Rename Failed "+moveToFile);
					}
					data.replayString = replayToImport.getName().substring(0, replayToImport.getName().length()-4);
					ScoreDB.addScore(data);;
				} else {
					//ErrorHandler.error("Could not find beatmap for replay "+replayToImport, null, false);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println(e);
			}
			
		}
	}
}
