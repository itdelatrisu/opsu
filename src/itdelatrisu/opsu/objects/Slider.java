/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014, 2015 Jeffrey Han
 *
 * opsu! is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opsu! is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with opsu!.  If not, see <http://www.gnu.org/licenses/>.
 */

package itdelatrisu.opsu.objects;

import java.util.Iterator;
import java.util.LinkedList;

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.GameMod;
import itdelatrisu.opsu.GameData;
import itdelatrisu.opsu.OsuFile;
import itdelatrisu.opsu.OsuHitObject;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.states.Game;

import org.newdawn.slick.Animation;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;

/**
 * Data type representing a slider object.
 */
public class Slider implements HitObject {
	/** Slider ball animation. */
	private static Animation sliderBall;

	/** Slider movement speed multiplier. */
	private static float sliderMultiplier = 1.0f;

	/** Rate at which slider ticks are placed. */
	private static float sliderTickRate = 1.0f;

	/** The associated OsuHitObject. */
	private OsuHitObject hitObject;

	/** The associated Game object. */
	private Game game;

	/** The associated GameData object. */
	private GameData data;

	/** The color of this slider. */
	private Color color;

	/** The underlying Bezier object. */
	private Curve bezier;

	/** The time duration of the slider, in milliseconds. */
	private float sliderTime = 0f;

	/** The time duration of the slider including repeats, in milliseconds. */
	private float sliderTimeTotal = 0f;

	/** Whether or not the result of the initial hit circle has been processed. */
	private boolean sliderClicked = false;

	/** Whether or not to show the follow circle. */
	private boolean followCircleActive = false;

	/** Whether or not the slider result ends the combo streak. */
	private boolean comboEnd;

	/** The number of repeats that have passed so far. */
	private int currentRepeats = 0;

	/** The t values of the slider ticks. */
	private float[] ticksT;

	/** The tick index in the ticksT[] array. */
	private int tickIndex = 0;

	/** Number of ticks hit and tick intervals so far. */
	private int ticksHit = 0, tickIntervals = 1;

	private abstract class Curve{
		/**
		 * Returns the point on the curve at a value t.
		 * @param t the t value [0, 1]
		 * @return the point [x, y]
		 */
		public abstract float[] pointAt(float t);

		/**
		 * Draws the full Bezier curve to the graphics context.
		 */
		public abstract void draw();

		/**
		 * Returns the angle of the first control point.
		 */
		public abstract float getEndAngle();
		
		/**
		 * Returns the angle of the last control point.
		 */
		public abstract float getStartAngle();
	}
	
	/**
	 * A two dimensional vector
	 */
	private class Vec2f{
		float x, y;
		
		/**
		 * Constructor of the (nx, ny) Vector
		 * @param nx
		 * @param ny
		 */
		public Vec2f(float nx, float ny) {
			x=nx;
			y=ny;
		}
		/**
		 * Constructor of the (0,0) Vector
		 */
		public Vec2f() {
		}
		
		/**
		 * Finds the midpoint between this Vector and "o" Vector
		 * @param o the other Vector
		 * @return midpoint vector
		 */
		public Vec2f midPoint(Vec2f o){
			return new Vec2f((x+o.x)/2, (y+o.y)/2);
		}
		/**
		 * Subtracts the "o" vector from this vector
		 * @param o the other Vector
		 * @return itself for chaining
		 */
		public Vec2f sub(Vec2f o){
			x-=o.x;
			y-=o.y;
			return this;
		}
		/**
		 * Sets this Vector to the normal of this Vector
		 * @return itself for chaining
		 */
		public Vec2f nor(){
			float nx = -y, ny =x;
			x=nx;
			y=ny;
			return this;
		}
		/**
		 * Makes a new Vector that is a copy of this Vector
		 * @return a copy of this Vector
		 */
		public Vec2f cpy(){
			return new Vec2f(x, y);
		}
		/**
		 * Adds nx to the x component and ny to the y component of this Vector
		 * @param nx
		 * @param ny
		 * @return
		 */
		public Vec2f add(float nx, float ny) {
			x+=nx;
			y+=ny;
			return this;
		}
		
		/**
		 * Finds the length of this Vector
		 * @return the length of this Vector
		 */
		public float len() {
			return (float) Math.sqrt(x*x + y*y);
		}
		/**
		 * Compares this vector to another Vector
		 * @param o the Other Vector
		 * @return true if the two Vector are numerically equal
		 */
		public boolean equals(Vec2f o){
			return x==o.x && y==o.y;
		}
		
	}
	/**
	 * Representation of a curve along a Circumscribed Circle of three points.
	 * http://en.wikipedia.org/wiki/Circumscribed_circle
	 */
	private class CircumscribedCircle extends Curve{
		/** The center of the Circumscribed Circle */
		Vec2f circleCenter; 
		
		/** The radius of the Circumscribed Circle  */
		float radius;
			
		/** * The three points to create the Circumscribed Circle from */
		Vec2f start ,mid ,end;
		
		/**  The three angles relative to the circle center */
		float startAng,endAng,midAng;
		
		/** The start and end angles for drawing */
		float drawStartAngle,drawEndAngle;
		
		/** Two times Pi   or  one full circle in radians */
		final float TWO_PI = (float) (2*Math.PI);
		/** Pi divided by two   or  a quarter of a circle in radians */
		final float HALF_PI = (float) (Math.PI/2);
		
		/** The number of steps in the curve to draw */
		private float step;
		
		/**
		 * Constructor
		 */
		public CircumscribedCircle(){
			this.step = hitObject.getPixelLength() / 5;
			
			//construct the three points
			start = new Vec2f(getX(0), getY(0));
			mid = new Vec2f(getX(1), getY(1));
			end = new Vec2f(getX(2), getY(2));
			
			//find the circle center
			Vec2f mida = start.midPoint(mid);
			Vec2f midb = end.midPoint(mid);
			Vec2f nora = mid.cpy().sub(start).nor();
			Vec2f norb = mid.cpy().sub(end).nor();
			
			circleCenter = intersect(mida, nora, midb, norb);
			
			//find the angles relative to the circle center
			Vec2f startAngPoint = start.cpy().sub(circleCenter);
			Vec2f midAngPoint = mid.cpy().sub(circleCenter);
			Vec2f endAngPoint = end.cpy().sub(circleCenter);
			
			startAng = (float) Math.atan2(startAngPoint.y, startAngPoint.x);
			midAng = (float) Math.atan2(midAngPoint.y, midAngPoint.x);
			endAng = (float) Math.atan2(endAngPoint.y, endAngPoint.x);
			
			
			//find angles that passes thru midAng
			if(!isIn(startAng,midAng,endAng)){
				if(Math.abs(startAng+TWO_PI-endAng)<TWO_PI && isIn(startAng+(TWO_PI),midAng,endAng)){
					startAng+=TWO_PI;
				}else if(Math.abs(startAng-(endAng+TWO_PI))<TWO_PI && isIn(startAng,midAng,endAng+(TWO_PI))){
					endAng+=TWO_PI;
				}else if(Math.abs(startAng-TWO_PI-endAng)<TWO_PI && isIn(startAng-(TWO_PI),midAng,endAng)){
					startAng-=TWO_PI;
				}else if(Math.abs(startAng-(endAng-TWO_PI))<TWO_PI && isIn(startAng,midAng,endAng-(TWO_PI))){
					endAng-=TWO_PI;
				}else{
					throw new Error("Cannot find Angles between midAng "+startAng+" "+midAng+" "+endAng);
				}

			}
			
			//Find an angle with an arc length of pixellength along this cirlce
			radius = startAngPoint.len();
			float pixelLength = hitObject.getPixelLength() * OsuHitObject.getMultiplier();
			float arcAng = pixelLength / radius; //len = theta * r  /  theta = len/r
			
			//now use it for our new end angle 
			if(endAng>startAng){
				endAng=startAng+arcAng;
			}else{
				endAng=startAng-arcAng;
			}
			
			//finds the angles to draw for repeats
			drawEndAngle = (float) ((endAng+(startAng>endAng?HALF_PI:-HALF_PI)) * 180 / Math.PI);
			drawStartAngle = (float) ((startAng+(startAng>endAng?-HALF_PI:HALF_PI)) * 180 / Math.PI);
		
		}
		/**
		 * Checks to see if "b" is between "a" and "c"
		 * @param a
		 * @param b
		 * @param c
		 * @return true if b is between a and c
		 */
		private boolean isIn(float a,float b,float c){
			return (b>a && b<c) || (b<a && b>c);
		}
		/**
		 * Finds the point of intersection between two parametric lines of  A = a + ta*t  and B = b + tb*u
		 * http://gamedev.stackexchange.com/questions/44720/line-intersection-from-parametric-equation
		 * @param a  the initial position of the line A
		 * @param ta the direction of the line A
		 * @param b  the initial position of the line B
		 * @param tb the direction of the line B
		 * @return the point at which the two lines interssect
		 */
		private Vec2f intersect(Vec2f a, Vec2f ta, Vec2f b, Vec2f tb) {
			// xy = a + ta * t = b + tb * u
			// t =(b + tb*u -a)/ta
			//t(x) == t(y)
			//(b.x + tb.x*u -a.x)/ta.x = (b.y + tb.y*u -a.y)/ta.y
			// b.x*ta.y + tb.x*u*ta.y -a.x*ta.y = b.y*ta.x + tb.y*u*ta.x -a.y*ta.x
			// tb.x*u*ta.y - tb.y*u*ta.x= b.y*ta.x  -a.y*ta.x -b.x*ta.y +a.x*ta.y 
			//u *(tb.x*ta.y - tb.y*ta.x) = (b.y-a.y)ta.x +(a.x-b.x)ta.y
			//u = ((b.y-a.y)ta.x +(a.x-b.x)ta.y) / (tb.x*ta.y - tb.y*ta.x);
			
			float des = tb.x*ta.y - tb.y*ta.x;
			if(Math.abs(des)<0.00001f){
				throw new Error("parallel ");
			}
			float u = ((b.y-a.y)*ta.x + (a.x-b.x)*ta.y) / des;
			return b.cpy().add(tb.x*u,tb.y*u);
		}
		
		@Override
		public float[] pointAt(float t) {
			float ang = lerp(startAng, endAng, t);
			return new float[]{(float) (Math.cos(ang)*radius+circleCenter.x),(float) (Math.sin(ang)*radius+circleCenter.y)};
		}
		@Override
		public void draw() {
			Image hitCircle = GameImage.HITCIRCLE.getImage();
			Image hitCircleOverlay = GameImage.HITCIRCLE_OVERLAY.getImage();
			// draw overlay and hit circle
			for(int i=0; i<step; i++){
				float[] xy = pointAt(i/step);
				Utils.drawCentered(hitCircleOverlay, xy[0], xy[1], Utils.COLOR_WHITE_FADE);
			}
			for(int i=0; i<step; i++){
				float[] xy = pointAt(i/step);
				Utils.drawCentered(hitCircle, xy[0], xy[1], color);
			}
		}
		@Override
		public float getEndAngle() {
			return drawEndAngle;
		}
		@Override
		public float getStartAngle() {
			return drawStartAngle;
		}
	}
	/**
	 * Representation of a Bezier curve, the main component of a slider.
	 *
	 * @author Alex Gheorghiu (http://html5tutorial.com/how-to-draw-n-grade-bezier-curve-with-canvas-api/)
	 * @author pictuga (https://github.com/pictuga/osu-web)
	 */
	private class Bezier {
		/** The order of the Bezier curve. */
		private int order;

		/** The step size (used for drawing). */
		private float step;

		/** The curve points for drawing with step size given by 'step'. */
		private float[] curveX, curveY;

		/** The angles of the first and last control points. */
		private float startAngle, endAngle;

		/**
		 * Constructor.
		 */
		public Bezier() {
			this.order = hitObject.getSliderX().length + 1;
			this.step = 5 / hitObject.getPixelLength();

			// calculate curve points for drawing
			int N = (int) (1 / step);
			this.curveX = new float[N + 1];
			this.curveY = new float[N + 1];
			float t = 0f;
			for (int i = 0; i < N; i++, t += step) {
				float[] c = pointAt(t);
				curveX[i] = c[0];
				curveY[i] = c[1];
			}
			curveX[N] = getX(order - 1);
			curveY[N] = getY(order - 1);

			// calculate angles (if needed)
			if (hitObject.getRepeatCount() > 1) {
				float[] c1 = pointAt(0f);
				float[] c2 = pointAt(step);
				startAngle = (float) (Math.atan2(c2[1] - c1[1], c2[0] - c1[0]) * 180 / Math.PI);
				c1 = pointAt(1f);
				c2 = pointAt(1f - step);
				endAngle = (float) (Math.atan2(c2[1] - c1[1], c2[0] - c1[0]) * 180 / Math.PI);
			}
		}

		/**
		 * Returns the x coordinate of the control point at index i.
		 */
		private float getX(int i) {
			return (i == 0) ? hitObject.getX() : hitObject.getSliderX()[i - 1];
		}

		/**
		 * Returns the y coordinate of the control point at index i.
		 */
		private float getY(int i) {
			return (i == 0) ? hitObject.getY() : hitObject.getSliderY()[i - 1];
		}

		/**
		 * Returns the angle of the first control point.
		 */
		private float getStartAngle() { return startAngle; }

		/**
		 * Returns the angle of the last control point.
		 */
		private float getEndAngle() { return endAngle; }

		/**
		 * Calculates the factorial of a number.
		 */
		private long factorial(int n) {
			return (n <= 1 || n > 20) ? 1 : n * factorial(n - 1);
		}

		/**
		 * Calculates the Bernstein polynomial.
		 * @param i the index
		 * @param n the degree of the polynomial (i.e. number of points)
		 * @param t the t value [0, 1]
		 */
		private double bernstein(int i, int n, float t) {
			return factorial(n) / (factorial(i) * factorial(n-i)) *
					Math.pow(t, i) * Math.pow(1-t, n-i);
		}

		/**
		 * Returns the point on the Bezier curve at a value t.
		 * For curves of order greater than 4, points will be generated along
		 * a path of overlapping cubic (at most) Beziers.
		 * @param t the t value [0, 1]
		 * @return the point [x, y]
		 */
		public float[] pointAt(float t) {
			float[] c = { 0f, 0f };
			int n = order - 1;
			if (n < 4) {  // normal curve
				for (int i = 0; i <= n; i++) {
					c[0] += getX(i) * bernstein(i, n, t);
					c[1] += getY(i) * bernstein(i, n, t);
				}
			} else {  // split curve into path
				// TODO: this is probably wrong...
				int segmentCount = (n / 3) + 1;
				int segment = (int) Math.floor(t * segmentCount);
				int startIndex = 3 * segment;
				int segmentOrder = Math.min(startIndex + 3, n) - startIndex;
				float segmentT = (t * segmentCount) - segment;
				for (int i = 0; i <= segmentOrder; i++) {
					c[0] += getX(i + startIndex) * bernstein(i, segmentOrder, segmentT);
					c[1] += getY(i + startIndex) * bernstein(i, segmentOrder, segmentT);
				}
			}
			return c;
		}

		/**
		 * Draws the full Bezier curve to the graphics context.
		 */
		public void draw() {
			Image hitCircle = GameImage.HITCIRCLE.getImage();
			Image hitCircleOverlay = GameImage.HITCIRCLE_OVERLAY.getImage();

			// draw overlay and hit circle
			for (int i = curveX.length - 1; i >= 0; i--)
				Utils.drawCentered(hitCircleOverlay, curveX[i], curveY[i], Utils.COLOR_WHITE_FADE);
			for (int i = curveX.length - 1; i >= 0; i--)
				Utils.drawCentered(hitCircle, curveX[i], curveY[i], color);
		}
	}
	
	/**
	 * Representation of a Bezier curve with equal distant points.
	 * http://pomax.github.io/bezierinfo/#tracing
	 */
	private class LinearBezier extends Curve{
		/** The angles of the first and last control points for drawing. */
		private float startAngle, endAngle;
		
		/** List of Bezier curves in the set of points */
		LinkedList<Bezier2> beziers = new LinkedList<Bezier2>();
		
		/** Points along the curve at equal distance. */
		Vec2f[] curve;
		
		/** The number of points along the curve */
		int ncurve;
		
		/**
		 * Constructor
		 */
		public LinearBezier(){
			//splits points into different beziers if has the same points(Red points)
			
			int npoints =  hitObject.getSliderX().length + 1;	//The number of control points
			LinkedList<Vec2f> points = new LinkedList<Vec2f>();	// a temporary list of points to separete different bezier curves
			Vec2f lastPoi = null;
			for(int i=0; i<npoints; i++){
				Vec2f tpoi = new Vec2f(getX(i), getY(i));
				if(lastPoi!=null && tpoi.equals(lastPoi)){
					if(points.size()>=2){
						beziers.add(new Bezier2(points.toArray(new Vec2f[0])));
					}
					points.clear();
				}
				points.add(tpoi);
				lastPoi = tpoi;
				
			}
			if(points.size()<2){
				//Ending on a red point (probably) just ignore
				//throw new Error("trying to continue Beziers with less than 2 points");
			}else{
				beziers.add(new Bezier2(points.toArray(new Vec2f[0])));
				points.clear();
			}
			
			//find the length of all beziers
			//int totalDistance = 0;
			//for(Bezier2 bez : beziers){
			//	totalDistance += bez.totalDistance();
			//}
			
			
			//now try to creates points the are equal distance to eachother
			ncurve = (int) (hitObject.getPixelLength()/5f);
			curve = new Vec2f[ncurve+1];
			
			float distanceAt = 0;
			Iterator<Bezier2> ita = beziers.iterator();
			
			int curPoint=0;
			Bezier2 curBezier=ita.next();
			
			Vec2f lastCurve = curBezier.curve[0];
			float lastDistanceAt = 0;
			//length of Bezier should equal pixel length (in 640x480)
			float pixelLength = hitObject.getPixelLength()*OsuHitObject.getMultiplier();

			//For each distance, try to get in between the two points that is between it.
			for(int i=0;i<ncurve+1;i++){
				int prefDistance = (int) (i*pixelLength/ncurve);
				while(distanceAt<prefDistance){
					lastDistanceAt = distanceAt;
					lastCurve = curBezier.curve[curPoint]; 
					distanceAt+=curBezier.curveDis[curPoint++];
					
					if(curPoint >= curBezier.ncurve){
						if(ita.hasNext()){
							curBezier = ita.next();
							curPoint = 0;
						}else{
							curPoint = curBezier.ncurve -1;
						}
					}
				}
				Vec2f thisCurve = curBezier.curve[curPoint];
				//interpolate the point between the two closest distances
				if(distanceAt-lastDistanceAt > 1){
					float t = (prefDistance-lastDistanceAt)/(float)(distanceAt-lastDistanceAt);
					curve[i] = new Vec2f(	lerp(lastCurve.x,thisCurve.x,t), lerp(lastCurve.y,thisCurve.y,t));
					//System.out.println("Dis "+i+" "+prefDistance+" "+lastDistanceAt+" "+distanceAt+" "+curPoint+" "+t);
					
				}else{
					curve[i] = thisCurve;
				}
			}
			//if (hitObject.getRepeatCount() > 1) {
				Vec2f c1 = curve[0];
				int cnt = 1;
				Vec2f c2 = curve[cnt++];
				while(c2.cpy().sub(c1).len()<1){
					c2 = curve[cnt++];
				}
				startAngle = (float) (Math.atan2(c2.y - c1.y, c2.x - c1.x) * 180 / Math.PI);
				c1 = curve[ncurve-1];
				cnt= ncurve-2;
				c2 = curve[cnt];
				while(c2.cpy().sub(c1).len()<1){
					c2 = curve[cnt--];
				}
				endAngle = (float) (Math.atan2(c2.y - c1.y, c2.x - c1.x) * 180 / Math.PI);
			//}
			//System.out.println("Total Distance: "+totalDistance+" "+distanceAt+" "+beziers.size()+" "+hitObject.getPixelLength()+" "+hitObject.xMultiplier);
		}
		@Override
		public float[] pointAt(float t) {
			
			float index = t * ncurve;
			
			if((int)index>=ncurve){
				Vec2f poi = curve[ncurve-1];
				return new float[]{poi.x, poi.y};
			}
			
			Vec2f poi = curve[(int)index];
			float t2 = index - (int)index;
			Vec2f poi2 = curve[(int)index+1];
			return new float[]{lerp(poi.x,poi2.x,t2),lerp(poi.y,poi2.y,t2)};
		}
		@Override
		public void draw() {
			Image hitCircle = GameImage.HITCIRCLE.getImage();
			Image hitCircleOverlay = GameImage.HITCIRCLE_OVERLAY.getImage();

			// draw overlay and hit circle
			for (int i = curve.length - 2; i >= 0; i--)
				Utils.drawCentered(hitCircleOverlay, curve[i].x, curve[i].y, Utils.COLOR_WHITE_FADE);
			for (int i = curve.length - 2; i >= 0; i--)
				Utils.drawCentered(hitCircle, curve[i].x, curve[i].y, color);
		}
		@Override
		public float getEndAngle() {
			return endAngle;
		}
		@Override
		public float getStartAngle() {
			return startAngle;
		}
	}
	/**
	 * Representation of a Bezier curve with the distance between each point calculated.
	 */
	private class Bezier2{
		/** The control points of the Bezier curve */
		Vec2f[] points;
		
		/** Points along the curve of the Bezier curve */
		Vec2f[] curve;
		
		/** distance between this point of the curve and the last point */
		float[] curveDis;
		
		/** The number of points along the curve */
		int ncurve;
		
		/** The total distances of this Bezier */
		float totalDistance;
		
		/*
		 * Constructor
		 */
		public Bezier2(Vec2f[] points) {
			
			this.points = points;
			//approximate by finding the length of all points(which should be the max possible length of the curve)
			float approxlength = 0;
			for(int i=0;i<points.length-1;i++){
				approxlength+= points[i].cpy().sub(points[i+1]).len();
			}
			
			//subdivide the curve
			ncurve= (int)(approxlength/4);
			curve = new Vec2f[ncurve];
			for(int i=0; i<ncurve; i++){
				curve[i] = pointAt(i/(float)ncurve);
			}
			
			//find the distance of each point from the previous point
			curveDis= new float[ncurve];
			for(int i=0; i<ncurve; i++){
				if(i==0)
					curveDis[i] = 0;
				else
					curveDis[i] = curve[i].cpy().sub(curve[i-1]).len();
				totalDistance+=curveDis[i];
			}
			
			//System.out.println("New Bezier2 "+points.length+" "+approxlength+" "+totalDistance());
			
		}
		/**
		 * Returns the total Distances of this Bezier Curve
		 */
		public float totalDistance(){
			return totalDistance;
		}
		
		
		/**
		 * Returns the point on the Bezier curve at a value t.
		 * @param t the t value [0, 1]
		 * @return the point [x, y]
		 */
		public Vec2f pointAt(float t) {
			Vec2f c = new Vec2f();
			int n =  points.length-1;
			for (int i = 0; i <= n; i++) {
				c.x += points[i].x * bernstein(i, n, t);
				c.y += points[i].y * bernstein(i, n, t);
			}
			return c;
		}
		/**
		 * Calculates the factorial of a number.
		 */
		private long factorial(int n) {
			return (n <= 1 || n > 20) ? 1 : n * factorial(n - 1);
		}

		/**
		 * Calculates the Bernstein polynomial.
		 * @param i the index
		 * @param n the degree of the polynomial (i.e. number of points)
		 * @param t the t value [0, 1]
		 */
		private double bernstein(int i, int n, float t) {
			return factorial(n) / (factorial(i) * factorial(n-i)) *
					Math.pow(t, i) * Math.pow(1-t, n-i);
		}
	}
	/**
	 * Linear interpolation of a and b at t
	 * @param a
	 * @param b
	 * @param t
	 * @return
	 */
	private float lerp(float a, float b, float t){
		return a*(1-t) + b*t;
	}
	/**
	 * "a recursive method to evaluate polynomials in Bernstein form or Bezier curves"
	 * http://en.wikipedia.org/wiki/De_Casteljau%27s_algorithm
	 */
	private float deCasteljau (float[] a, int i, int order, float t){
		if(order==0)
			return a[i];
		return lerp( deCasteljau(a,i,order-1,t), deCasteljau(a,i+1,order-1,t), t);
	}
	/**
	 * Returns the x coordinate of the control point at index i.
	 */
	private float getX(int i) {
		return (i == 0) ? hitObject.getX() : hitObject.getSliderX()[i - 1];
	}

	/**
	 * Returns the y coordinate of the control point at index i.
	 */
	private float getY(int i) {
		return (i == 0) ? hitObject.getY() : hitObject.getSliderY()[i - 1];
	}
	/**
	 * Initializes the Slider data type with images and dimensions.
	 * @param container the game container
	 * @param circleSize the map's circleSize value
	 * @param osu the associated OsuFile object
	 */
	public static void init(GameContainer container, float circleSize, OsuFile osu) {
		int diameter = (int) (96 - (circleSize * 8));
		diameter = diameter * container.getWidth() / 640;  // convert from Osupixels (640x480)

		// slider ball
		Image[] sliderBallImages;
		if (GameImage.SLIDER_BALL.hasSkinImages() ||
		    (!GameImage.SLIDER_BALL.hasSkinImage() && GameImage.SLIDER_BALL.getImages() != null))
			sliderBallImages = GameImage.SLIDER_BALL.getImages();
		else
			sliderBallImages = new Image[]{ GameImage.SLIDER_BALL.getImage() };
		for (int i = 0; i < sliderBallImages.length; i++)
			sliderBallImages[i] = sliderBallImages[i].getScaledCopy(diameter * 118 / 128, diameter * 118 / 128);
		sliderBall = new Animation(sliderBallImages, 60);

		GameImage.SLIDER_FOLLOWCIRCLE.setImage(GameImage.SLIDER_FOLLOWCIRCLE.getImage().getScaledCopy(diameter * 259 / 128, diameter * 259 / 128));
		GameImage.REVERSEARROW.setImage(GameImage.REVERSEARROW.getImage().getScaledCopy(diameter, diameter));
		GameImage.SLIDER_TICK.setImage(GameImage.SLIDER_TICK.getImage().getScaledCopy(diameter / 4, diameter / 4));

		sliderMultiplier = osu.sliderMultiplier;
		sliderTickRate = osu.sliderTickRate;
	}

	/**
	 * Constructor.
	 * @param hitObject the associated OsuHitObject
	 * @param game the associated Game object
	 * @param data the associated GameData object
	 * @param color the color of this circle
	 * @param comboEnd true if this is the last hit object in the combo
	 */
	public Slider(OsuHitObject hitObject, Game game, GameData data, Color color, boolean comboEnd) {
		this.hitObject = hitObject;
		this.game = game;
		this.data = data;
		this.color = color;
		this.comboEnd = comboEnd;
		if(hitObject.getSliderType() == 'P' && hitObject.getSliderX().length==2){
			this.bezier = new CircumscribedCircle();
		}else {
			this.bezier = new LinearBezier();
		}

	}

	@Override
	public void draw(int trackPosition, boolean currentObject, Graphics g) {
		float x = hitObject.getX(), y = hitObject.getY();
		float[] sliderX = hitObject.getSliderX(), sliderY = hitObject.getSliderY();
		int timeDiff = hitObject.getTime() - trackPosition;

		float approachScale = (timeDiff >= 0) ? 1 + (timeDiff * 2f / game.getApproachTime()) : 1f;
		float alpha = (approachScale > 3.3f) ? 0f : 1f - (approachScale - 1f) / 2.7f;
		float oldAlpha = color.a;
		float oldAlphaFade = Utils.COLOR_WHITE_FADE.a;
		color.a = alpha;
		Utils.COLOR_WHITE_FADE.a = alpha;

		// bezier
		bezier.draw();

		// ticks
		if (currentObject && ticksT != null) {
			Image tick = GameImage.SLIDER_TICK.getImage();
			for (int i = 0; i < ticksT.length; i++) {
				float[] c = bezier.pointAt(ticksT[i]);
				tick.drawCentered(c[0], c[1]);
			}
		}

		Image hitCircleOverlay = GameImage.HITCIRCLE_OVERLAY.getImage();
		Image hitCircle = GameImage.HITCIRCLE.getImage();

		// end circle
		//int lastIndex = sliderX.length - 1;
		float[] endPos = bezier.pointAt(1);
		Utils.drawCentered(hitCircle, endPos[0], endPos[1], color);
		Utils.drawCentered(hitCircleOverlay, endPos[0], endPos[1], Utils.COLOR_WHITE_FADE);
		
		// start circle
		Utils.drawCentered(hitCircleOverlay, x, y, Utils.COLOR_WHITE_FADE);
		Utils.drawCentered(hitCircle, x, y, color);
		if (sliderClicked)
			;  // don't draw current combo number if already clicked
		else
			data.drawSymbolNumber(hitObject.getComboNumber(), x, y,
					hitCircle.getWidth() * 0.40f / data.getDefaultSymbolImage(0).getHeight());

		color.a = oldAlpha;
		Utils.COLOR_WHITE_FADE.a = oldAlphaFade;

		// repeats
		for(int tcurRepeat = currentRepeats; tcurRepeat<=currentRepeats+1; tcurRepeat++){
			if (hitObject.getRepeatCount() - 1 > tcurRepeat) {
				Image arrow = GameImage.REVERSEARROW.getImage();
				if(tcurRepeat != currentRepeats){
					float t = getT(trackPosition, true);
					arrow.setAlpha((float) (t-Math.floor(t)));
				}else{
					arrow.setAlpha(1f);
				}
				if (tcurRepeat % 2 == 0) {  // last circle
					arrow.setRotation(bezier.getEndAngle());
					arrow.drawCentered(endPos[0], endPos[1]);
				} else {  // first circle
					arrow.setRotation(bezier.getStartAngle());
					arrow.drawCentered(x, y);
				}
			}
		}

		if (timeDiff >= 0) {
			// approach circle
			Utils.drawCentered(GameImage.APPROACHCIRCLE.getImage().getScaledCopy(approachScale), x, y, color);
		} else {
			float[] c = bezier.pointAt(getT(trackPosition, false));

			// slider ball
			Utils.drawCentered(sliderBall, c[0], c[1]);

			// follow circle
			if (followCircleActive)
				GameImage.SLIDER_FOLLOWCIRCLE.getImage().drawCentered(c[0], c[1]);
		}
	}

	/**
	 * Calculates the slider hit result.
	 * @return the hit result (GameData.HIT_* constants)
	 */
	private int hitResult() {
		int lastIndex = hitObject.getSliderX().length - 1;
		float tickRatio = (float) ticksHit / tickIntervals;

		int result;
		if (tickRatio >= 1.0f)
			result = GameData.HIT_300;
		else if (tickRatio >= 0.5f)
			result = GameData.HIT_100;
		else if (tickRatio > 0f)
			result = GameData.HIT_50;
		else
			result = GameData.HIT_MISS;

		if (currentRepeats % 2 == 0)  {// last circle
			float[] lastPos = bezier.pointAt(1);
			data.hitResult(hitObject.getTime() + (int) sliderTimeTotal, result,
					lastPos[0],lastPos[1],
					color, comboEnd, hitObject.getHitSoundType());
		}else  // first circle
			data.hitResult(hitObject.getTime() + (int) sliderTimeTotal, result,
					hitObject.getX(), hitObject.getY(), color, comboEnd, hitObject.getHitSoundType());

		return result;
	}

	@Override
	public boolean mousePressed(int x, int y) {
		if (sliderClicked)  // first circle already processed
			return false;

		double distance = Math.hypot(hitObject.getX() - x, hitObject.getY() - y);
		int circleRadius = GameImage.HITCIRCLE.getImage().getWidth() / 2;
		if (distance < circleRadius) {
			int trackPosition = MusicController.getPosition();
			int timeDiff = Math.abs(trackPosition - hitObject.getTime());
			int[] hitResultOffset = game.getHitResultOffsets();

			int result = -1;
			if (timeDiff < hitResultOffset[GameData.HIT_50]) {
				result = GameData.HIT_SLIDER30;
				ticksHit++;
			} else if (timeDiff < hitResultOffset[GameData.HIT_MISS])
				result = GameData.HIT_MISS;
			//else not a hit

			if (result > -1) {
				sliderClicked = true;
				data.sliderTickResult(hitObject.getTime(), result,
						hitObject.getX(), hitObject.getY(), hitObject.getHitSoundType());
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean update(boolean overlap, int delta, int mouseX, int mouseY) {
		int repeatCount = hitObject.getRepeatCount();

		// slider time and tick calculations
		if (sliderTimeTotal == 0f) {
			// slider time
			this.sliderTime = game.getBeatLength() * (hitObject.getPixelLength() / sliderMultiplier) / 100f;
			this.sliderTimeTotal = sliderTime * repeatCount;

			// ticks
			float tickLengthDiv = 100f * sliderMultiplier / sliderTickRate / game.getTimingPointMultiplier();
			int tickCount = (int) Math.ceil(hitObject.getPixelLength() / tickLengthDiv) - 1;
			if (tickCount > 0) {
				this.ticksT = new float[tickCount];
				float tickTOffset = 1f / (tickCount + 1);
				float t = tickTOffset;
				for (int i = 0; i < tickCount; i++, t += tickTOffset)
					ticksT[i] = t;
			}
		}

		byte hitSound = hitObject.getHitSoundType();
		int trackPosition = MusicController.getPosition();
		int[] hitResultOffset = game.getHitResultOffsets();
		int lastIndex = hitObject.getSliderX().length - 1;
		boolean isAutoMod = GameMod.AUTO.isActive();

		if (!sliderClicked) {
			int time = hitObject.getTime();

			// start circle time passed
			if (trackPosition > time + hitResultOffset[GameData.HIT_50]) {
				sliderClicked = true;
				if (isAutoMod) {  // "auto" mod: catch any missed notes due to lag
					ticksHit++;
					data.sliderTickResult(time, GameData.HIT_SLIDER30,
							hitObject.getX(), hitObject.getY(), hitSound);
				} else
					data.sliderTickResult(time, GameData.HIT_MISS,
							hitObject.getX(), hitObject.getY(), hitSound);
			}

			// "auto" mod: send a perfect hit result
			else if (isAutoMod) {
				if (Math.abs(trackPosition - time) < hitResultOffset[GameData.HIT_300]) {
					ticksHit++;
					sliderClicked = true;
					data.sliderTickResult(time, GameData.HIT_SLIDER30,
							hitObject.getX(), hitObject.getY(), hitSound);
				}
			}
		}

		// end of slider
		if (overlap || trackPosition > hitObject.getTime() + sliderTimeTotal) {
			tickIntervals++;

			// "auto" mod: send a perfect hit result
			if (isAutoMod)
				ticksHit++;

			// check if cursor pressed and within end circle
			else if (Utils.isGameKeyPressed()) {
				float[] c = bezier.pointAt(getT(trackPosition, false));
				double distance = Math.hypot(c[0] - mouseX, c[1] - mouseY);
				int followCircleRadius = GameImage.SLIDER_FOLLOWCIRCLE.getImage().getWidth() / 2;
				if (distance < followCircleRadius)
					ticksHit++;
			}

			// calculate and send slider result
			hitResult();
			return true;
		}

		// repeats
		boolean isNewRepeat = false;
		if (repeatCount - 1 > currentRepeats) {
			float t = getT(trackPosition, true);
			if (Math.floor(t) > currentRepeats) {
				currentRepeats++;
				tickIntervals++;
				isNewRepeat = true;
			}
		}

		// ticks
		boolean isNewTick = false;
		if (ticksT != null &&
			tickIntervals < (ticksT.length * (currentRepeats + 1)) + repeatCount &&
			tickIntervals < (ticksT.length * repeatCount) + repeatCount) {
			float t = getT(trackPosition, true);
			if (t - Math.floor(t) >= ticksT[tickIndex]) {
				tickIntervals++;
				tickIndex = (tickIndex + 1) % ticksT.length;
				isNewTick = true;
			}
		}

		// holding slider...
		float[] c = bezier.pointAt(getT(trackPosition, false));
		double distance = Math.hypot(c[0] - mouseX, c[1] - mouseY);
		int followCircleRadius = GameImage.SLIDER_FOLLOWCIRCLE.getImage().getWidth() / 2;
		if ((Utils.isGameKeyPressed() && distance < followCircleRadius) || isAutoMod) {
			// mouse pressed and within follow circle
			followCircleActive = true;
			data.changeHealth(delta * GameData.HP_DRAIN_MULTIPLIER);

			// held during new repeat
			if (isNewRepeat) {
				ticksHit++;
				if (currentRepeats % 2 > 0)  // last circle
					data.sliderTickResult(trackPosition, GameData.HIT_SLIDER30,
							hitObject.getSliderX()[lastIndex], hitObject.getSliderY()[lastIndex], hitSound);
				else  // first circle
					data.sliderTickResult(trackPosition, GameData.HIT_SLIDER30,
							c[0], c[1], hitSound);
			}

			// held during new tick
			if (isNewTick) {
				ticksHit++;
				data.sliderTickResult(trackPosition, GameData.HIT_SLIDER10,
						c[0], c[1], (byte) -1);
			}
		} else {
			followCircleActive = false;

			if (isNewRepeat)
				data.sliderTickResult(trackPosition, GameData.HIT_MISS, 0, 0, (byte) -1);
			if (isNewTick)
				data.sliderTickResult(trackPosition, GameData.HIT_MISS, 0, 0, (byte) -1);
		}

		return false;
	}

	/**
	 * Returns the t value based on the given track position.
	 * @param trackPosition the current track position
	 * @param raw if false, ensures that the value lies within [0, 1] by looping repeats
	 * @return the t value: raw [0, repeats] or looped [0, 1]
	 */
	private float getT(int trackPosition, boolean raw) {
		float t = (trackPosition - hitObject.getTime()) / sliderTime;
		if (raw)
			return t;
		else {
			float floor = (float) Math.floor(t);
			return (floor % 2 == 0) ? t - floor : floor + 1 - t;
		}
	}
}