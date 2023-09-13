package org.chk.jdbc.rsm;

import java.sql.SQLException;

public interface PropertyFactory {

	Property build(String text, int columnIndex, String name, String type) throws SQLException;
}
