/*
 * Copyright 2010, Andrew M Gibson
 *
 * www.andygibson.net
 *
 * This file is part of DataValve.
 *
 * DataValve is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DataValve is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with DataValve.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.fluttercode.datavalve.provider.jdbc;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * This class is used to return the results in the mapped sql dataset without
 * having to create a class to return the results in. The results are returned
 * in this class as a map of values. To access a field value, just call
 * {@link TableRow#getValue(String)} and pass in the name of the field to
 * access. The lookup is case sensitive.
 * </p>
 * 
 * @author Andy Gibson
 * 
 * 
 */
public class TableRow {

	/**
	 * The <code>values</code> map holds the values
	 */
	private Map<String, Object> values = new HashMap<String, Object>();

	/**
	 * Helper method to add a new value into the row
	 * 
	 * @param column
	 *            Name of the column to add
	 * @param value
	 *            Value of the column to add
	 */
	public void add(String column, Object value) {
		values.put(column, value);
	}

	/**
	 * Returns the value of the passed in column name for the row in the
	 * {@link ResultSet} this object represents.
	 * 
	 * @param columnName
	 *            name of the field to return
	 * @return the value of that column for this row.
	 */
	public Object getValue(String columnName) {
		if (values.containsKey(columnName)) {
			return values.get(columnName);
		}
		throw new IllegalArgumentException(String.format(
				"Dataset does not contain field '%s'", columnName));
	}

	/**
	 * A static factory method that creates and initializes a TableRow instance
	 * for the current row of the passed in {@link ResultSet}.
	 * 
	 * @param resultSet
	 *            The {@link ResultSet} containing the values we need to load
	 *            into the TableRow instance.
	 * @return a new TableRow instance initialized with the values from the
	 *         {@link ResultSet}.
	 */
	public static TableRow createTableRowFromResultSet(ResultSet resultSet) {
		TableRow result = new TableRow();
		try {
			ResultSetMetaData metadata = resultSet.getMetaData();
			for (int i = 0; i < metadata.getColumnCount(); i++) {
				int colIndex = i + 1;
				String name = metadata.getColumnName(colIndex);
				Object value = resultSet.getObject(colIndex);
				result.add(name, value);
			}
		} catch (SQLException exception) {
			exception.printStackTrace();
		}
		return result;
	}	
}
