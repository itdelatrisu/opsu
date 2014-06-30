package org.newdawn.slick.geom;

/**
 * A triangulator implementation that splits the triangules of another, subdividing
 * to give a higher tesselation - and hence smoother transitions.
 * 
 * @author kevin
 */
public class OverTriangulator implements Triangulator {
	/** The triangles data */
	private float[][] triangles;
	
	/**
	 * Create a new triangulator
	 * 
	 * @param tris The original set of triangles to be sub-dividied
	 */
	public OverTriangulator(Triangulator tris) {
		triangles = new float[tris.getTriangleCount()*6*3][2];
		
		int tcount = 0;
		for (int i=0;i<tris.getTriangleCount();i++) {
			float cx = 0;
			float cy = 0;
			for (int p = 0;p < 3;p++) {
				float[] pt = tris.getTrianglePoint(i, p);
				cx += pt[0];
				cy += pt[1];
			}
			
			cx /= 3;
			cy /= 3;
			
			for (int p = 0;p < 3;p++) {
				int n = p +1;
				if (n > 2) {
					n = 0;
				}
				
				float[] pt1 = tris.getTrianglePoint(i, p);
				float[] pt2 = tris.getTrianglePoint(i, n);

				pt1[0] = (pt1[0] + pt2[0]) / 2;
				pt1[1] = (pt1[1] + pt2[1]) / 2;
				
				triangles[(tcount *3) + 0][0] = cx;
				triangles[(tcount *3) + 0][1] = cy;
				triangles[(tcount *3) + 1][0] = pt1[0];
				triangles[(tcount *3) + 1][1] = pt1[1];
				triangles[(tcount *3) + 2][0] = pt2[0];
				triangles[(tcount *3) + 2][1] = pt2[1];
				tcount++;
			}
			
			for (int p = 0;p < 3;p++) {
				int n = p +1;
				if (n > 2) {
					n = 0;
				}
				
				float[] pt1 = tris.getTrianglePoint(i, p);
				float[] pt2 = tris.getTrianglePoint(i, n);
				
				pt2[0] = (pt1[0] + pt2[0]) / 2;
				pt2[1] = (pt1[1] + pt2[1]) / 2;
				
				triangles[(tcount *3) + 0][0] = cx;
				triangles[(tcount *3) + 0][1] = cy;
				triangles[(tcount *3) + 1][0] = pt1[0];
				triangles[(tcount *3) + 1][1] = pt1[1];
				triangles[(tcount *3) + 2][0] = pt2[0];
				triangles[(tcount *3) + 2][1] = pt2[1];
				tcount++;
			}
		}
	}
	
	/**
	 * @see org.newdawn.slick.geom.Triangulator#addPolyPoint(float, float)
	 */
	public void addPolyPoint(float x, float y) {
	}

	/**
	 * @see org.newdawn.slick.geom.Triangulator#getTriangleCount()
	 */
	public int getTriangleCount() {
		return triangles.length / 3;
	}

	/**
	 * @see org.newdawn.slick.geom.Triangulator#getTrianglePoint(int, int)
	 */
	public float[] getTrianglePoint(int tri, int i) {
		float[] pt = triangles[(tri * 3)+i];
	
		return new float[] {pt[0],pt[1]};
	}

	/**
	 * @see org.newdawn.slick.geom.Triangulator#startHole()
	 */
	public void startHole() {
	}

	/**
	 * @see org.newdawn.slick.geom.Triangulator#triangulate()
	 */
	public boolean triangulate() {
		return true;
	}

}
