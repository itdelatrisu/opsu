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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Provides specific details to the error report.
 * 
 * @author Lyonlancer5
 */
public class ErrorReportCategory {

	/** The name of this error category */
	private final String categoryName;
	/** All error sections listed in this category */
	private final List<Section> sections;

	/**
	 * Constructs a new error report category
	 * 
	 * @param categoryName
	 *            The name of this category
	 */
	public ErrorReportCategory(String categoryName) {
		this.categoryName = categoryName;
		this.sections = new ArrayList<Section>();
	}

	/**
	 * Adds a section to this error category.
	 * 
	 * @param sectionName
	 *            The name of the error section
	 * @param value
	 *            The message of the error section
	 */
	public void addSection(String sectionName, String value) {
		sections.add(new Section(sectionName, value));
	}

	/**
	 * Adds a section to this error category.
	 * 
	 * @param sectionName
	 *            The name of the error section
	 * @param value
	 *            A {@link Throwable} caused by the error
	 */
	public void addSection(String sectionName, Throwable value) {
		if (value != null)
			addSection(sectionName, "[ERROR] " + value.getClass().getSimpleName() + " - " + value.getMessage());
		else
			addSection(sectionName, "[NULL]");
	}

	/**
	 * Adds a section to this error category.
	 * 
	 * @param sectionName
	 *            The name of the error section
	 * @param value
	 *            The callable which would return the value of the section
	 */
	public void addSection(String sectionName, Callable<String> value) {
		try {
			addSection(sectionName, value.call());
		} catch (Throwable t) {
			addSection(sectionName, t);
		}
	}

	/**
	 * Returns an unmodifiable view of all the sections of this error category.
	 */
	public List<Section> getSections() {
		return Collections.unmodifiableList(sections);
	}

	/**
	 * Returns the error information held by this category.
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("-- ").append(categoryName).append(" --\n");
		for (Section section : sections) {
			builder.append("\n    ");
			builder.append(section.toString());
		}
		return builder.toString();
	}

	/**
	 * Represents a section in this error report category
	 */
	public static class Section {

		/** The name of the information section */
		private final String name;
		/** The value of the information section */
		private final String value;

		/**
		 * Constructs a new error info section
		 * 
		 * @param name
		 *            The section name
		 * @param value
		 *            The data of the section
		 */
		public Section(String name, String value) {
			this.name = name;

			if (value == null)
				this.value = "[NULL]";
			else
				this.value = value.toString();
		}

		/**
		 * Gets the value of this section.
		 */
		public String getValue() {
			return value;
		}

		@Override
		public String toString() {
			return name + ": " + value;
		}
	}

}
