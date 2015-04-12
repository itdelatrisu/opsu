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

package itdelatrisu.opsu.objects.curves;

/**
 * Representation of a Centripetal Catmull–Rom spline.
 * http://en.wikipedia.org/wiki/Centripetal_Catmull%E2%80%93Rom_spline
 * 
 * Currently not technically Centripetal Catmull–Rom
 * 
 * @author fluddokt (https://github.com/fluddokt)
 */
public class CentripetalCatmullRom extends CurveType{

	float [] time;
	Vec2f[] points;
	protected CentripetalCatmullRom(Vec2f[] points) {
		if (points.length != 4)
			throw new Error("need exactly 4 points");
		
		this.points = points;
		System.out.println("C");
		for(int i=0;i<4;i++){
			System.out.println(points[i]);
		}
		time = new float[4];
		time[0] = 0;
		float approxLength = 0;
		for(int i=1;i<4;i++){
			float len = 0;
			if(i>0)
				len = points[i].cpy().sub(points[i-1]).len(); 
			if(len<=0)
				len+=0.0001f;
			approxLength += len;
			//time[i] = (float) Math.sqrt(len) + time[i-1];// ^(0.5)
			time[i] = i;
			System.out.println("knot:"+i+" "+time[i]);
		}
		init(approxLength/2);
	}
	public Vec2f pointAt(float t, int start, int end){
		int ind1 = start;
		int ind2 = end;
		if(end-start==3){
			ind1 = 1;
			ind2 = 2;
		}
		//System.out.println("ST:"+start+" "+end+" "+ind1+" "+ind2);
		float div = (time[ind2] - time[ind1]);
		float v1 = (time[ind2] - t)/div;
		float v2 = (t - time[ind1])/div;
		System.out.println("div"+div+" "+v1+" "+v2);
		if(end-start==1)
			return points[start].cpy().scale(v1).add(points[end].cpy().scale(v2));
		else
			return pointAt(t,start,end-1).scale(v1).add(pointAt(t,start+1,end).scale(v2));
	}
	
	public Vec2f pointAt(float t){
		t = t * (time[2]-time[1]) + time[1];

		Vec2f A1 = points[0].cpy().scale((time[1]-t)/(time[1]-time[0])).add(points[1].cpy().scale((t-time[0])/(time[1]-time[0])));
		Vec2f A2 = points[1].cpy().scale((time[2]-t)/(time[2]-time[1])).add(points[2].cpy().scale((t-time[1])/(time[2]-time[1])));
		Vec2f A3 = points[2].cpy().scale((time[3]-t)/(time[3]-time[2])).add(points[3].cpy().scale((t-time[2])/(time[3]-time[2])));
		
		Vec2f B1 = A1.cpy().scale((time[2]-t)/(time[2]-time[0])).add(A2.cpy().scale((t-time[0])/(time[2]-time[0])));
		Vec2f B2 = A2.cpy().scale((time[3]-t)/(time[3]-time[1])).add(A3.cpy().scale((t-time[1])/(time[3]-time[1])));
		
		Vec2f C = B1.cpy().scale((time[2]-t)/(time[2]-time[1])).add(B2.cpy().scale((t-time[1])/(time[2]-time[1])));
		
		return C;
	}
	public String toString(){
		String t = "CATMULLROM";
		for(Vec2f p: points){
			t+=p;
		}
		
		t+=pointAt(0);
		t+=pointAt(1);
		
		return t;
	}
}
