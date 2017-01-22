/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014-2017 Jeffrey Han
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
 * A two-dimensional floating-point vector.
 *
 * @author fluddokt (https://github.com/fluddokt)
 */
public class Vec2f {
	/** Vector coordinates. */
	public float x, y;

	/**
	 * Constructor of the (nx, ny) vector.
	 */
	public Vec2f(float nx, float ny) {
		x = nx;
		y = ny;
	}

	/**
	 * Constructor of the (0,0) vector.
	 */
	public Vec2f() {}

	/**
	 * Sets the x and y components of this vector.
	 * @return itself (for chaining)
	 */
	public Vec2f set(float nx, float ny) {
		x = nx;
		y = ny;
		return this;
	}

	/**
	 * Finds the midpoint between this vector and another vector.
	 * @param o the other vector
	 * @return a midpoint vector
	 */
	public Vec2f midPoint(Vec2f o) {
		return new Vec2f((x + o.x) / 2, (y + o.y) / 2);
	}

	/**
	 * Scales the vector.
	 * @param s scaler to scale by
	 * @return itself (for chaining)
	 */
	public Vec2f scale(float s) {
		x *= s;
		y *= s;
		return this;
	}

	/**
	 * Adds a vector to this vector.
	 * @param o the other vector
	 * @return itself (for chaining)
	 */
	public Vec2f add(Vec2f o) {
		x += o.x;
		y += o.y;
		return this;
	}

	/**
	 * Subtracts a vector from this vector.
	 * @param o the other vector
	 * @return itself (for chaining)
	 */
	public Vec2f sub(Vec2f o) {
		x -= o.x;
		y -= o.y;
		return this;
	}

	/**
	 * Sets this vector to the normal of this vector.
	 * @return itself (for chaining)
	 */
	public Vec2f nor() {
		float nx = -y, ny = x;
		x = nx;
		y = ny;
		return this;
	}

	/**
	 * Turns this vector into a unit vector.
	 * @return itself (for chaining)
	 */
	public Vec2f normalize() {
		float len = len();
		x /= len;
		y /= len;
		return this;
	}

	/**
	 * Returns a copy of this vector.
	 */
	public Vec2f cpy() { return new Vec2f(x, y); }

	/**
	 * Adds nx to the x component and ny to the y component of this vector.
	 * @return itself (for chaining)
	 */
	public Vec2f add(float nx, float ny) {
		x += nx;
		y += ny;
		return this;
	}

	/**
	 * Returns the length of this vector.
	 */
	public float len() { return (float) Math.sqrt(x * x + y * y); }

	/**
	 * Compares this vector to another vector.
	 * @param o the other vector
	 * @return true if the two vectors are numerically equal
	 */
	public boolean equals(Vec2f o) { return (x == o.x && y == o.y); }

	@Override
	public String toString() { return String.format("(%.3f, %.3f)", x, y); }
}
