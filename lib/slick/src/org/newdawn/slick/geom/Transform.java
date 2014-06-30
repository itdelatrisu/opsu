package org.newdawn.slick.geom;

import org.newdawn.slick.util.FastTrig;

/**   
 * A 2 dimensional transformation that can be applied to <code>Shape</code> implemenations.   
 *    
 * @author Mark   
 */   
public class Transform {   
    /**   
     * Value for each position in the matrix   
     *    
     * |0 1 2|   
     * |3 4 5|   
     * |6 7 8|   
     */   
    private float matrixPosition[];   
   
    /**   
     * Create and identity transform   
     *   
     */   
    public Transform() {   
        matrixPosition = new float[]{1, 0, 0, 0, 1, 0, 0, 0, 1};   
    }   
    
    /**   
     * Copy a transform
     * 
     * @param other The other transform to copy
     */   
    public Transform(Transform other) {   
    	matrixPosition = new float[9];
    	for (int i=0;i<9;i++) {
    		matrixPosition[i] = other.matrixPosition[i];
    	}
    }   
       
    /**
     * Concatanate to transform into one
     * 
     * @param t1 The first transform to join
     * @param t2 The second transform to join
     */
    public Transform(Transform t1, Transform t2) {
    	this(t1);
    	concatenate(t2);
    }
    
    /**   
     * Create a transform for the given positions   
     *    
     * @param matrixPosition An array of float[6] to set up a transform   
     * @throws RuntimeException if the array is not of length 6   
     */   
    public Transform(float matrixPosition[]) {   
        if(matrixPosition.length != 6) {   
            throw new RuntimeException("The parameter must be a float array of length 6.");   
        }   
        this.matrixPosition = new float[]{matrixPosition[0], matrixPosition[1], matrixPosition[2],    
                matrixPosition[3], matrixPosition[4], matrixPosition[5],    
                0, 0, 1};   
    }   
       
    /**   
     * Create a transform for the given positions   
     *    
     * @param point00 float for the first position   
     * @param point01 float for the second position   
     * @param point02 float for the third position   
     * @param point10 float for the fourth position   
     * @param point11 float for the fifth position   
     * @param point12 float for the sixth position   
     */   
    public Transform(float point00, float point01, float point02, float point10, float point11, float point12) {   
        matrixPosition = new float[]{point00, point01, point02, point10, point11, point12, 0, 0, 1};   
    }   
       
    /**   
     * Transform the point pairs in the source array and store them in the destination array.   
     * All operations will be done before storing the results in the destination.  This way the source   
     * and destination array can be the same without worry of overwriting information before it is transformed.   
     *    
     * @param source Array of floats containing the points to be transformed   
     * @param sourceOffset Where in the array to start processing   
     * @param destination Array of floats to store the results.   
     * @param destOffset Where in the array to start storing   
     * @param numberOfPoints Number of points to be transformed   
     * @throws ArrayIndexOutOfBoundsException if sourceOffset + numberOfPoints * 2 > source.length or the same operation on the destination array   
     */   
    public void transform(float source[], int sourceOffset, float destination[], int destOffset, int numberOfPoints) {   
        //TODO performance can be improved by removing the safety to the destination array   
        float result[] = source == destination ? new float[numberOfPoints * 2] : destination;
           
        for(int i=0;i<numberOfPoints * 2;i+=2) {   
            for(int j=0;j<6;j+=3) {   
                result[i + (j / 3)] = source[i + sourceOffset] * matrixPosition[j] + source[i + sourceOffset + 1] * matrixPosition[j + 1] + 1 * matrixPosition[j + 2];   
            }   
        }   
        
        if (source == destination) {
	        //for safety of the destination, the results are copied after the entire operation.   
	        for(int i=0;i<numberOfPoints * 2;i+=2) {   
	            destination[i + destOffset] = result[i];   
	            destination[i + destOffset + 1] = result[i + 1];   
	        }   
        }
    }   
       
    /**   
     * Update this Transform by concatenating the given Transform to this one.   
     *    
     * @param tx The Transfrom to concatenate to this one.   
     * @return The resulting Transform   
     */   
    public Transform concatenate(Transform tx) {   
    	float[] mp = new float[9];
    	float n00 = matrixPosition[0] * tx.matrixPosition[0] + matrixPosition[1] * tx.matrixPosition[3];
    	float n01 = matrixPosition[0] * tx.matrixPosition[1] + matrixPosition[1] * tx.matrixPosition[4];
    	float n02 = matrixPosition[0] * tx.matrixPosition[2] + matrixPosition[1] * tx.matrixPosition[5] + matrixPosition[2];
    	float n10 = matrixPosition[3] * tx.matrixPosition[0] + matrixPosition[4] * tx.matrixPosition[3];
    	float n11 = matrixPosition[3] * tx.matrixPosition[1] + matrixPosition[4] * tx.matrixPosition[4];
    	float n12 = matrixPosition[3] * tx.matrixPosition[2] + matrixPosition[4] * tx.matrixPosition[5] + matrixPosition[5];
    	mp[0] = n00;
    	mp[1] = n01;
    	mp[2] = n02;
    	mp[3] = n10;
    	mp[4] = n11;
    	mp[5] = n12;
//    	
//        mp[0] = matrixPosition[0] * transform.matrixPosition[0] + matrixPosition[0] * transform.matrixPosition[3] + matrixPosition[0] * transform.matrixPosition[6]; 
//        mp[1] = matrixPosition[1] * transform.matrixPosition[1] + matrixPosition[1] * transform.matrixPosition[4] + matrixPosition[1] * transform.matrixPosition[7];
//        mp[2] = matrixPosition[2] * transform.matrixPosition[2] + matrixPosition[2] * transform.matrixPosition[5] + matrixPosition[2] * transform.matrixPosition[8]; 
//        mp[3] = matrixPosition[3] * transform.matrixPosition[0] + matrixPosition[3] * transform.matrixPosition[3] + matrixPosition[3] * transform.matrixPosition[6]; 
//        mp[4] = matrixPosition[4] * transform.matrixPosition[1] + matrixPosition[4] * transform.matrixPosition[4] + matrixPosition[4] * transform.matrixPosition[7];
//        mp[5] = matrixPosition[5] * transform.matrixPosition[2] + matrixPosition[5] * transform.matrixPosition[5] + matrixPosition[5] * transform.matrixPosition[8]; 
//        
        matrixPosition = mp;
        return this;
    }   
   
       
    /**   
     * Convert this Transform to a String.   
     *    
     * @return This Transform in human readable format.   
     */   
    public String toString() {   
        String result = "Transform[[" + matrixPosition[0] + "," + matrixPosition[1] + "," + matrixPosition[2] +    
        "][" + matrixPosition[3] + "," + matrixPosition[4] + "," + matrixPosition[5] + 
        "][" + matrixPosition[6] + "," + matrixPosition[7] + "," + matrixPosition[8] + "]]";   
           
        return result.toString();   
    }   
   
    /**   
     * Get an array representing this Transform.   
     *    
     * @return an array representing this Transform.    
     */   
    public float[] getMatrixPosition() {   
        return matrixPosition;   
    }   
       
    /**   
     * Create a new rotation Transform   
     *    
     * @param angle The angle in radians to set the transform.   
     * @return The resulting Transform   
     */   
    public static Transform createRotateTransform(float angle) {   
        return new Transform((float)FastTrig.cos(angle), -(float)FastTrig.sin(angle), 0, (float)FastTrig.sin(angle), (float)FastTrig.cos(angle), 0);   
    }   
       
    /**   
     * Create a new rotation Transform around the specified point   
     *    
     * @param angle The angle in radians to set the transform.   
     * @param x The x coordinate around which to rotate.   
     * @param y The y coordinate around which to rotate.   
     * @return The resulting Transform   
     */   
    public static Transform createRotateTransform(float angle, float x, float y) {   
        Transform temp = Transform.createRotateTransform(angle);
        float sinAngle = temp.matrixPosition[3];
        float oneMinusCosAngle = 1.0f - temp.matrixPosition[4];
        temp.matrixPosition[2] = x * oneMinusCosAngle + y * sinAngle;
        temp.matrixPosition[5] = y * oneMinusCosAngle - x * sinAngle;

        return temp;   
    }   
       
    /**   
     * Create a new translation Transform   
     *    
     * @param xOffset The amount to move in the x direction   
     * @param yOffset The amount to move in the y direction   
     * @return The resulting Transform   
     */   
    public static Transform createTranslateTransform(float xOffset, float yOffset) {   
        return new Transform(1, 0, xOffset, 0, 1, yOffset);   
    }   
       
    /**   
     * Create an new scaling Transform   
     *    
     * @param xScale The amount to scale in the x coordinate   
     * @param yScale The amount to scale in the x coordinate   
     * @return The resulting Transform   
     */   
    public static Transform createScaleTransform(float xScale, float yScale) {   
        return new Transform(xScale, 0, 0, 0, yScale, 0);   
    }
    
    /**
     * Transform the vector2f based on the matrix defined in this transform
     * 
     * @param pt The point to be transformed
     * @return The resulting point transformed by this matrix
     */
    public Vector2f transform(Vector2f pt) {
    	float[] in = new float[] {pt.x, pt.y};
    	float[] out = new float[2];
    	
    	transform(in, 0, out, 0, 1);
    	
    	return new Vector2f(out[0], out[1]);
    }
}   
