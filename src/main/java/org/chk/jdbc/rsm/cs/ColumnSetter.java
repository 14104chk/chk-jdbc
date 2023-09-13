package org.chk.jdbc.rsm.cs;

import java.sql.SQLException;
import org.chk.jdbc.rsm.MappingContext;
import org.chk.jdbc.rsm.Property;

public interface ColumnSetter {

	public boolean set(MappingContext context, Object entity, Property property) throws SQLException;
}
