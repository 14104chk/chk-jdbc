package org.chk.jdbc;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SortBuilder {

	private static final Pattern P_SORT = Pattern.compile("([\\+\\-])([^\\+\\-]+)");

	private final Map<String, String> map = new HashMap<>();
	private final String defaultSort;

	public SortBuilder(String defaultSort) {
		this.defaultSort = defaultSort;
	}

	public SortBuilder map(String prop, String column) {
		map.put(prop, column);
		return this;
	}

	public String toSQLFromKey(String sort) {
		if (sort == null) {
			return toSQL(defaultSort);
		}
		StringBuilder buff = new StringBuilder();
		Matcher m = P_SORT.matcher(sort);
		while (m.find()) {
			boolean desc = "-".equals(m.group(1));
			String column = map.get(m.group(2));
			if (column == null) {
				return toSQL(defaultSort);
			}
			buff.append(column.replace('?', desc ? '-' : '+'));
		}
		return toSQL(buff.toString());
	}

	private String toSQL(String sort) {
		StringBuilder buff = new StringBuilder();
		Matcher m = P_SORT.matcher(sort);
		while (m.find()) {
			String column = m.group(2).trim();
			if (buff.length() > 0) {
				buff.append(',');
			}
			buff.append(column);
			if ("-".equals(m.group(1))) {
				buff.append(' ').append("DESC");
			}
		}
		return buff.toString();
	}

}
