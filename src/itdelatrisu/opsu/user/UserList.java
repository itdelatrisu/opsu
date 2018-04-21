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

package itdelatrisu.opsu.user;

import itdelatrisu.opsu.OpsuConstants;
import itdelatrisu.opsu.db.ScoreDB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * List of users.
 */
public class UserList {
	/** The name of the default user. */
	public static final String DEFAULT_USER_NAME = "Guest";

	/** The default icon identifier. */
	public static final int DEFAULT_ICON = 0;

	/** The name of the "user" when the "Auto" mod is active. */
	public static final String AUTO_USER_NAME = OpsuConstants.PROJECT_NAME;

	/** The maximum length of a user name. */
	public static final int MAX_USER_NAME_LENGTH = 16;

	/** The single class instance. */
	private static UserList list;

	/** Map of all users. */
	private Map<String, User> users = new HashMap<String, User>();

	/** The current user. */
	private User currentUser;

	/** Creates a new instance of this class (overwriting any previous instance). */
	public static void create() { list = new UserList(); }

	/** Returns the single instance of this class. */
	public static UserList get() { return list; }

	/** Builds the user list. */
	public UserList() {
		// get all users
		List<User> list = ScoreDB.getUsers();
		for (User user : list)
			users.put(user.getName().toLowerCase(), user);

		if (list.isEmpty()) {
			// create the default user
			createNewUser(DEFAULT_USER_NAME, DEFAULT_ICON);
			changeUser(DEFAULT_USER_NAME);
		} else {
			// find the current user
			if (!changeUser(ScoreDB.getCurrentUser())) {
				// user not found: use the default user (create if needed)
				if (!userExists(DEFAULT_USER_NAME))
					createNewUser(DEFAULT_USER_NAME, DEFAULT_ICON);
				changeUser(DEFAULT_USER_NAME);
			}
		}
	}

	/** Returns the number of users. */
	public int size() { return users.size(); }

	/** Returns all users. */
	public List<User> getUsers() {
		List<User> l = new ArrayList<User>(users.values());
		Collections.sort(l);
		return l;
	}

	/** Returns the current user. */
	public User getCurrentUser() { return currentUser; }

	/** Returns whether the given user exists. */
	public boolean userExists(String name) { return name != null && users.containsKey(name.toLowerCase()); }

	/** Returns the user associated with the name, or null if none. */
	public User getUser(String name) { return users.get(name.toLowerCase()); }

	/**
	 * Changes the current user.
	 * @param name the user's name
	 * @return true if the user changed, false otherwise
	 */
	public boolean changeUser(String name) {
		if (!userExists(name))
			return false;

		this.currentUser = getUser(name);
		ScoreDB.setCurrentUser(name);
		return true;
	}

	/**
	 * Creates a new user.
	 * @param name the new user's name
	 * @param icon the new user's icon
	 * @return the new User, or null if it could not be created (e.g. name exists)
	 */
	public User createNewUser(String name, int icon) {
		if (!isValidUserName(name))
			return null;

		User user = new User(name, icon);
		ScoreDB.updateUser(user);
		users.put(name.toLowerCase(), user);
		return user;
	}

	/**
	 * Deletes the given user.
	 * @param name the user's name
	 * @return true if the user was deleted, false otherwise
	 */
	public boolean deleteUser(String name) {
		if (!userExists(name) || name.equals(currentUser.getName()))
			return false;

		ScoreDB.deleteUser(name);
		users.remove(name.toLowerCase());
		return true;
	}

	/** Returns whether the given name is a valid user name. */
	public boolean isValidUserName(String name) {
		return !name.isEmpty() && name.length() <= MAX_USER_NAME_LENGTH &&
		       name.equals(name.trim()) && !userExists(name) &&
		       !name.equalsIgnoreCase(AUTO_USER_NAME);
	}
}
