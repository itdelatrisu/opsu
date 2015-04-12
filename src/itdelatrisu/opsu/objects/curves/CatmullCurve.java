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

import java.util.LinkedList;

import itdelatrisu.opsu.OsuHitObject;

import org.newdawn.slick.Color;

public class CatmullCurve extends EqualDistanceMultiCurve{

	public CatmullCurve(OsuHitObject hitObject, Color color) {
		super(hitObject, color);
		LinkedList<CurveType> catmulls = new LinkedList<CurveType>();
		int ncontrolPoints = hitObject.getSliderX().length + 1;
		LinkedList<Vec2f> points = new LinkedList<Vec2f>();  // temporary list of points to separate different curves
		
		//aaa .... needs at least two points
		//aabb
		//aabc abcc
		//aabc abcd bcdd
		points.addLast(new Vec2f(getX(0),getY(0)));
		for(int i=0;i<ncontrolPoints;i++){
			points.addLast(new Vec2f(getX(i),getY(i)));
			if(points.size()>=4){
				catmulls.add(new CentripetalCatmullRom(points.toArray(new Vec2f[0])));
				points.removeFirst();
			}
		}
		points.addLast(new Vec2f(getX(ncontrolPoints-1),getY(ncontrolPoints-1)));
		if(points.size()>=4){
			catmulls.add(new CentripetalCatmullRom(points.toArray(new Vec2f[0])));
		}
		init(catmulls);
	}
}
