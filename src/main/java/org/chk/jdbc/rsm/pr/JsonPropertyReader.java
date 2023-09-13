package org.chk.jdbc.rsm.pr;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.chk.jdbc.Json;
import org.chk.jdbc.rsm.MappingContext;
import org.chk.jdbc.rsm.MappingException;
import org.chk.jdbc.rsm.Property;

public class JsonPropertyReader implements PropertyReader {

	private final Json json;

	public JsonPropertyReader(Json json) {
		this.json = json;
	}

	@Override
	public Object read(MappingContext context, Property property, Class propertyRawType, ResultSet resultSet) throws SQLException {
		String text = resultSet.getString(property.getColumnIndex());
		if (resultSet.wasNull()) {
			return null;
		} else {
			try {
				return json.fromJson(text, propertyRawType == null ? Object.class : propertyRawType);
			} catch (IOException ex) {
				throw new MappingException("json:" + text, ex);
			}
		}
	}

}
