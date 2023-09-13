package org.chk.jdbc.rsm.cs;

import java.sql.SQLException;
import java.util.Map;
import org.chk.jdbc.rsm.MappingContext;
import org.chk.jdbc.rsm.MappingException;
import org.chk.jdbc.rsm.Property;

public class MapSetter implements ColumnSetter {

	@Override
	public boolean set(MappingContext context, Object entity, Property property) throws MappingException, SQLException {
		if (!(entity instanceof Map)) {
			return false;
		}
		((Map) entity).put(property.getName(), property.readValue(context, null));
		return true;
	}

}
