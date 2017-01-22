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

package itdelatrisu.opsu.ui.animations;

/*
 * These equations are copyright (c) 2001 Robert Penner, all rights reserved,
 * and are open source under the BSD License.
 * http://www.opensource.org/licenses/bsd-license.php
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the author nor the names of contributors may be used
 *   to endorse or promote products derived from this software without specific
 *   prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * Easing functions for animations.
 *
 * @author Robert Penner (<a href="http://robertpenner.com/easing/">http://robertpenner.com/easing/</a>)
 * @author CharlotteGore (<a href="https://github.com/CharlotteGore/functional-easing">https://github.com/CharlotteGore/functional-easing</a>)
 */
public enum AnimationEquation {
	/* Linear */
	LINEAR {
		@Override
		public float calc(float t) { return t; }
	},

	/* Quadratic */
	IN_QUAD {
		@Override
		public float calc(float t) { return t * t; }
	},
	OUT_QUAD {
		@Override
		public float calc(float t) { return -1 * t * (t - 2); }
	},
	IN_OUT_QUAD {
		@Override
		public float calc(float t) {
			t = t * 2;
			if (t < 1)
				return 0.5f * t * t;
			t = t - 1;
			return -0.5f * (t * (t - 2) - 1);
		}
	},

	/* Cubic */
	IN_CUBIC {
		@Override
		public float calc(float t) { return t * t * t; }
	},
	OUT_CUBIC {
		@Override
		public float calc(float t) {
			t = t - 1;
			return t * t * t + 1;
		}
	},
	IN_OUT_CUBIC {
		@Override
		public float calc(float t) {
			t = t * 2;
			if (t < 1)
				return 0.5f * t * t * t;
			t = t - 2;
			return 0.5f * (t * t * t + 2);
		}
	},

	/* Quartic */
	IN_QUART {
		@Override
		public float calc(float t) { return t * t * t * t; }
	},
	OUT_QUART {
		@Override
		public float calc(float t) {
			t = t - 1;
			return -1 * (t * t * t * t - 1);
		}
	},
	IN_OUT_QUART {
		@Override
		public float calc(float t) {
			t = t * 2;
			if (t < 1)
				return 0.5f * t * t * t * t;
			t = t - 2;
			return -0.5f * (t * t * t * t - 2);
		}
	},

	/* Quintic */
	IN_QUINT {
		@Override
		public float calc(float t) { return t * t * t * t * t; }
	},
	OUT_QUINT {
		@Override
		public float calc(float t) {
			t = t - 1;
			return (t * t * t * t * t + 1);
		}
	},
	IN_OUT_QUINT {
		@Override
		public float calc(float t) {
			t = t * 2;
			if (t < 1)
				return 0.5f * t * t * t * t * t;
			t = t - 2;
			return 0.5f * (t * t * t * t * t + 2);
		}
	},

	/* Sine */
	IN_SINE {
		@Override
		public float calc(float t) { return -1 * (float) Math.cos(t * (Math.PI / 2)) + 1; }
	},
	OUT_SINE {
		@Override
		public float calc(float t) { return (float) Math.sin(t * (Math.PI / 2)); }
	},
	IN_OUT_SINE {
		@Override
		public float calc(float t) { return (float) (Math.cos(Math.PI * t) - 1) / -2; }
	},

	/* Exponential */
	IN_EXPO {
		@Override
		public float calc(float t) { return (t == 0) ? 0 : (float) Math.pow(2, 10 * (t - 1)); }
	},
	OUT_EXPO {
		@Override
		public float calc(float t) { return (t == 1) ? 1 : (float) -Math.pow(2, -10 * t) + 1; }
	},
	IN_OUT_EXPO {
		@Override
		public float calc(float t) {
			if (t == 0 || t == 1)
				return t;
			t = t * 2;
			if (t < 1)
				return 0.5f * (float) Math.pow(2, 10 * (t - 1));
			t = t - 1;
			return 0.5f * ((float) -Math.pow(2, -10 * t) + 2);
		}
	},

	/* Circular */
	IN_CIRC {
		@Override
		public float calc(float t) { return -1 * ((float) Math.sqrt(1 - t * t) - 1); }
	},
	OUT_CIRC {
		@Override
		public float calc(float t) {
			t = t - 1;
			return (float) Math.sqrt(1 - t * t);
		}
	},
	IN_OUT_CIRC {
		@Override
		public float calc(float t) {
			t = t * 2;
			if (t < 1)
				return -0.5f * ((float) Math.sqrt(1 - t * t) - 1);
			t = t - 2;
			return 0.5f * ((float) Math.sqrt(1 - t * t) + 1);
		}
	},

	/* Back */
	IN_BACK {
		@Override
		public float calc(float t) { return t * t * ((OVERSHOOT + 1) * t - OVERSHOOT); }
	},
	OUT_BACK {
		@Override
		public float calc(float t) {
			t = t - 1;
			return t * t * ((OVERSHOOT + 1) * t + OVERSHOOT) + 1;
		}
	},
	IN_OUT_BACK {
		@Override
		public float calc(float t) {
			float overshoot = OVERSHOOT * 1.525f;
			t = t * 2;
			if (t < 1)
				return 0.5f * (t * t * ((overshoot + 1) * t - overshoot));
			t = t - 2;
			return 0.5f * (t * t * ((overshoot + 1) * t + overshoot) + 2);
		}
	},

	/* Bounce */
	IN_BOUNCE {
		@Override
		public float calc(float t) { return 1 - OUT_BOUNCE.calc(1 - t); }
	},
	OUT_BOUNCE {
		@Override
		public float calc(float t) {
			if (t < 0.36363636f)
				return 7.5625f * t * t;
			else if (t < 0.72727273f) {
				t = t - 0.54545454f;
				return 7.5625f * t * t + 0.75f;
			} else if (t < 0.90909091f) {
				t = t - 0.81818182f;
				return 7.5625f * t * t + 0.9375f;
			} else {
				t = t - 0.95454546f;
				return 7.5625f * t * t + 0.984375f;
			}
		}
	},
	IN_OUT_BOUNCE {
		@Override
		public float calc(float t) {
			if (t < 0.5f)
				return IN_BOUNCE.calc(t * 2) * 0.5f;
			return OUT_BOUNCE.calc(t * 2 - 1) * 0.5f + 0.5f;
		}
	},

	/* Elastic */
	IN_ELASTIC {
		@Override
		public float calc(float t) {
			if (t == 0 || t == 1)
				return t;
			float period = 0.3f;
			t = t - 1;
			return -((float) Math.pow(2, 10 * t) * (float) Math.sin(((t - period / 4) * (Math.PI * 2)) / period));
		}
	},
	OUT_ELASTIC {
		@Override
		public float calc(float t) {
			if (t == 0 || t == 1)
				return t;
			float period = 0.3f;
			return (float) Math.pow(2, -10 * t) * (float) Math.sin((t - period / 4) * (Math.PI * 2) / period) + 1;
		}
	},
	IN_OUT_ELASTIC {
		@Override
		public float calc(float t) {
			if (t == 0 || t == 1)
				return t;
			float period = 0.44999996f;
			t = t * 2 - 1;
			if (t < 0)
				return -0.5f * ((float) Math.pow(2, 10 * t) * (float) Math.sin((t - period / 4) * (Math.PI * 2) / period));
			return (float) Math.pow(2, -10 * t) * (float) Math.sin((t - period / 4) * (Math.PI * 2) / period) * 0.5f + 1;
		}
	};

	/** Overshoot constant for "back" easings. */
	private static final float OVERSHOOT = 1.70158f;

	/**
	 * Calculates a new {@code t} value using the animation equation.
	 * @param t the raw {@code t} value [0,1]
	 * @return the new {@code t} value [0,1]
	 */
	public abstract float calc(float t);
}
