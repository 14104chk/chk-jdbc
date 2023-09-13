package org.chk.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.function.FailableBiFunction;
import org.apache.commons.lang3.function.FailableFunction;
import org.apache.commons.text.CaseUtils;

public class JdbcExecutor {

	private static final Pattern P_NAME = Pattern.compile("\\#\\{(.+?)\\}");
	private static final Pattern P_COLUMN = Pattern.compile("(\\w+)(\\:(\\w+))");

	private final String sql;
	private final Callable callable;
	private final FailableBiFunction<Connection, String, PreparedStatement, SQLException> preparedStatementBuilder;

	private Map<String, Object> mapParameters;
	private FailableFunction<String, Object, SimpleDataAccessException> propertiesFunction;
	private List<Object> listParameters;

	private int parametersType;

	private ObjectMapper om;

	JdbcExecutor(String sql, Callable callable) {
		this(sql, callable, Connection::prepareStatement);
	}

	JdbcExecutor(String sql, Callable callable, FailableBiFunction<Connection, String, PreparedStatement, SQLException> preparedStatementBuilder) {
		this.sql = sql;
		this.callable = callable;
		this.preparedStatementBuilder = preparedStatementBuilder;
	}

	public JdbcExecutor addParameter(String name, Object value) {
		parametersType = 1;
		if (mapParameters == null) {
			mapParameters = new HashMap<>();
		}
		mapParameters.put(name, value);
		propertiesFunction = mapParameters::get;
		return this;
	}

	public <T> T executeWithBean(Object bean) {
		parametersType = 2;
		propertiesFunction = (name) -> {
			try {
				return "this".equalsIgnoreCase(name) ? bean : PropertyUtils.getProperty(bean, name);
			} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
				throw new SimpleDataAccessException(String.valueOf(bean), ex);
			}
		};
		return execute();
	}

	public <T> T executeWithArray(Object... parameters) {
		parametersType = 3;
		if (listParameters == null) {
			listParameters = new ArrayList<>();
		} else {
			listParameters.clear();
		}
		listParameters.addAll(Arrays.asList(parameters));
		return execute();
	}

	private Map<String, String> columnMapProperty(String[] columns) {
		Map<String, String> map = new LinkedHashMap<>();
		for (String t : columns) {
			Matcher m = P_COLUMN.matcher(t.trim());
			if (m.matches()) {
				map.put(m.group(1), m.replaceFirst(CaseUtils.toCamelCase(m.group(1), false, '_') + "$2"));
			} else {
				map.put(t, CaseUtils.toCamelCase(t, false, '_'));
			}
		}
		return map;
	}

	protected JdbcExecutor buildForUpdate(String... columns) {
		columns = optimizeColumns(columns);
		Map<String, String> map = columnMapProperty(columns);
		StringBuilder buff = new StringBuilder();
		for (Map.Entry<String, String> t : map.entrySet()) {
			if (buff.length() > 0) {
				buff.append(',');
			}
			buff.append(t.getKey()).append("=#{").append(t.getValue()).append("}");
		}
		return new JdbcExecutor(sql.replaceFirst("(?i)(\\?)|(\\{u\\})", buff.toString()), callable, preparedStatementBuilder);
	}

	protected String[] optimizeColumns(String... columns) {
		if (columns.length == 1 && columns[0].indexOf(',') > 0) {
			return columns[0].split(",");
		} else {
			return columns;
		}
	}

	/**
	 * UPDATE table SET {u}|? [ WHERE column=#{column} ]
	 *
	 * @param bean
	 * @param columns
	 * @return
	 */
	public int update(Object bean, String... columns) {
		return buildForUpdate(columns).executeWithBean(bean);
	}

	public int updateByID(Object bean, String... columns) {
		return new JdbcExecutor("UPDATE " + sql + " SET ? WHERE id=#{id}", callable, preparedStatementBuilder).update(bean, columns);
	}

	protected JdbcExecutor buildForInsert(Object bean, String... columns) {
		columns = optimizeColumns(columns);
		Map<String, String> map = columnMapProperty(columns);
		StringBuilder p1 = new StringBuilder();
		for (Map.Entry<String, String> e : map.entrySet()) {
			if (p1.length() > 0) {
				p1.append(',');
			}
			p1.append(e.getKey());
		}
		StringBuilder p2 = new StringBuilder();
		for (Map.Entry<String, String> e : map.entrySet()) {
			if (p2.length() > 0) {
				p2.append(',');
			}
			p2.append("#{").append(e.getValue()).append("}");
		}
		return new JdbcExecutor(sql.replaceFirst("(?i)(\\?)|(\\{i\\})", String.format("(%s) VALUES (%s)", p1, p2)), callable, preparedStatementBuilder);
	}

	/**
	 * INSERT table {i}|?
	 *
	 * @param bean
	 * @param columns
	 * @return
	 */
	public int insert(Object bean, String... columns) {
		return buildForInsert(bean, columns).executeWithBean(bean);
	}

	public int insertSimple(Object bean, String... columns) {
		return new JdbcExecutor("INSERT INTO " + sql + " ?", callable, preparedStatementBuilder).insert(bean, columns);
	}

	public int insertOnDuplicateKeyUpdate(Object bean, boolean ifNull, String[] constantColumns, String... updatableColumns) {
		updatableColumns = optimizeColumns(updatableColumns);
		Map<String, String> map2 = columnMapProperty(updatableColumns);
		StringBuilder p2 = new StringBuilder();
		for (Map.Entry<String, String> e : map2.entrySet()) {
			if (p2.length() > 0) {
				p2.append(',');
			}
			if (ifNull) {
				p2.append(e.getKey()).append("=IFNULL(VALUES(").append(e.getKey()).append("),").append(e.getKey()).append(")");
			} else {
				p2.append(e.getKey()).append("=VALUES(").append(e.getKey()).append(")");
			}
		}
		String[] columns = Arrays.copyOf(constantColumns, constantColumns.length + updatableColumns.length);
		System.arraycopy(updatableColumns, 0, columns, constantColumns.length, updatableColumns.length);
		return new JdbcExecutor("INSERT INTO " + sql + " {i} ON DUPLICATE KEY UPDATE " + p2, callable, preparedStatementBuilder).insert(bean, columns);
	}

	public int insertOnDuplicateKeyUpdate(Object bean, boolean ifNull, String... columns) {
		columns = optimizeColumns(columns);
		String[] constantColumns = null, updatableColumns = null;
		for (int i = 0; i < columns.length; i++) {
			if ("/".equals(columns[i])) {
				constantColumns = Arrays.copyOfRange(columns, 0, i);
				updatableColumns = Arrays.copyOfRange(columns, i + 1, columns.length);
				break;
			}
		}
		if (constantColumns == null || updatableColumns == null) {
			throw new IllegalArgumentException();
		}
		return insertOnDuplicateKeyUpdate(bean, ifNull, constantColumns, updatableColumns);
	}

	private String createPreparedStatement(List<Object> parameters, String fn, Object value) throws JsonProcessingException {
		if (value != null && fn != null) {
			switch (fn) {
				case "in":
					Collection c = null;
					if (value instanceof Collection) {
						c = (Collection) value;
					} else if (value.getClass().isArray()) {
						c = new ArrayList();
						for (int i = 0, n = Array.getLength(value); i < n; i++) {
							c.add(Array.get(value, i));
						}
					} else if (value instanceof Stream) {
						c = (Collection) ((Stream) value).collect(Collectors.toSet());
					}
					if (c != null) {
						parameters.addAll(c);
						return c.stream().map(e -> "?").collect(Collectors.joining(",")).toString();
					}
					break;
				case "json":
					if (om == null) {
						om = new ObjectMapper();
					}
					parameters.add(om.writeValueAsString(value));
					return "?";
			}
		}
		parameters.add(value);
		return "?";
	}

	private PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		String nativeSQL = sql;
		List<Object> parameters = new ArrayList<>();
		try {
			switch (parametersType) {
				case 1:
				case 2:
					StringBuffer sb = new StringBuffer();
					Matcher m = P_NAME.matcher(sql);
					while (m.find()) {
						String property = m.group(1), name, fn;
						Matcher m2 = P_COLUMN.matcher(property.trim());
						if (m2.matches()) {
							name = m2.group(1);
							fn = m2.group(3);
						} else {
							name = property;
							fn = null;
						}
						m.appendReplacement(sb, createPreparedStatement(parameters, fn, propertiesFunction.apply(name)));
					}
					m.appendTail(sb);
					nativeSQL = sb.toString();
					break;
				case 3:
					parameters.addAll(listParameters);
					break;
			}
			PreparedStatement pstmt = preparedStatementBuilder.apply(con, nativeSQL);
			for (int i = 0, n = parameters.size(); i < n; i++) {
				Object value = parameters.get(i);
				if (value instanceof Enum) {
					pstmt.setString(i + 1, ((Enum) value).name());
				} else if (value instanceof String) {
					pstmt.setString(i + 1, (String) value);
				} else {
					pstmt.setObject(i + 1, value);
				}
			}
			return pstmt;
		} catch (JsonProcessingException ex) {
			throw new SimpleDataAccessException(null, ex);
		}
	}

	public <T> T execute() {
		return (T) callable.call(this::createPreparedStatement);
	}

}
