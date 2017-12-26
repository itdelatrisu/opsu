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

package itdelatrisu.opsu.crash;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Represents a set of information to the crash report
 * 
 * @author Lyonlancer5
 */
public class CrashInfo {

	/** The name of this crash category */
	private final String categoryName;
	/** All crash sections listed in this category */
	private final List<Section> crashSections;

	/** Stack trace of the current thread */
	private StackTraceElement[] stacktrace;

	/**
	 * Constructs a new crash information category
	 * 
	 * @param categoryName
	 *            The name of this category
	 */
	public CrashInfo(String categoryName) {
		this.categoryName = categoryName;
		this.crashSections = new ArrayList<Section>();

	}

	/**
	 * Adds a section to this crash information.
	 * 
	 * @param sectionName
	 *            The name of the crash section
	 * @param value
	 *            The message of the crash section
	 */
	public void addSection(String sectionName, Object value) {
		crashSections.add(new Section(sectionName, value));
	}

	/**
	 * Adds a section to this crash information. This method is for getting
	 * values which may cause exceptions when run.
	 * 
	 * @param key
	 *            The name of the crash section
	 * @param value
	 *            The callable which would return the value of the section
	 */
	public void addSectionSafe(String key, Callable<String> value) {

		try {
			addSection(key, value.call());
		} catch (Throwable t) {
			addSection(key, t);
		}
	}

	/**
	 * Writes the crash information held by this category
	 * 
	 * @param builder
	 *            A {@link StringBuilder} instance
	 */
	public void appendTo(StringBuilder builder) {
		builder.append("-- ").append(categoryName).append(" --\n");
		for (Section section : crashSections) {
			builder.append("\n\t");
			builder.append(section.toString());
		}

		if (stacktrace != null && stacktrace.length > 0) {
			builder.append("\nStacktrace:");
			for (StackTraceElement ste : stacktrace) {
				builder.append("\n\tat ");
				builder.append(ste.toString());
			}
		}
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		appendTo(sb);
		return sb.toString();
	}

	/**
	 * Populates the crash category's stack trace.
	 */
	public void populateStackTrace() {
		stacktrace = Thread.currentThread().getStackTrace();
	}

	/**
	 * Removes the crash category's stack trace.
	 */
	public void depopulateStackTrace() {
		stacktrace = null;
	}

	/**
	 * Gets the stack trace of this category.
	 */
	public StackTraceElement[] getStackTrace() {
		return stacktrace;
	}

	/**
	 * Represents a section in this crash information category
	 */
	static class Section {

		/** The name of the information section */
		private final String name;
		/** The value of the information section */
		private final String value;

		/**
		 * Constructs a new crash info section
		 * 
		 * @param name
		 *            The section name
		 * @param value
		 *            The data of the section
		 */
		public Section(String name, Object value) {
			this.name = name;

			if (value == null)
				this.value = "~~NULL~~";
			else if (value instanceof Throwable) {
				Throwable throwable = (Throwable) value;
				this.value = "~~ERROR~~ " + throwable.getClass().getSimpleName() + " - " + throwable.getMessage();
			} else
				this.value = value.toString();
		}

		public String toString() {
			return name + ": " + value;
		}
	}

}
