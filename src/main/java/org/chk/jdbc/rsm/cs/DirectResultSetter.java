package org.chk.jdbc.rsm.cs;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.chk.jdbc.rsm.MappingContext;
import org.chk.jdbc.rsm.MappingException;
import org.chk.jdbc.rsm.Property;

public class DirectResultSetter implements ColumnSetter {

	@Override
	public boolean set(MappingContext context, Object entity, Property property) throws SQLException {
		try {
			String methodName = "set" + context.getConfig().getBeanUtils().capitalize(property.getName());
			Method m = entity.getClass().getMethod(methodName, ResultSet.class, int.class);
			try {
				m.invoke(entity, context.getResultSet(), property.getColumnIndex());
				return true;
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
				throw new MappingException(ex);
			}
		} catch (NoSuchMethodException | SecurityException ex) {
			return false;
		}
	}

}
