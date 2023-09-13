package org.chk.jdbc.rsm.pr;

import java.sql.SQLException;
import org.chk.jdbc.rsm.MappingContext;
import org.chk.jdbc.rsm.Property;

public interface PropertyReaders {

	Property buildProperty(String text, int columnIndex, String name, String type) throws SQLException;

	Object readPropertyValue(MappingContext context, Property property, Class propertyRawType, String type) throws SQLException;

	Object readPropertyValue(MappingContext context, Property property, Class propertyRawType) throws SQLException;

	PropertyReader registerPropertyReader(Object type, PropertyReader propertyReader);

	PropertyReader deregisterPropertyReader(Object type);
}
