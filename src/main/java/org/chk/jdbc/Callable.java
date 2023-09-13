package org.chk.jdbc;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementCreator;

interface Callable {

	Object call(PreparedStatementCreator psc) throws DataAccessException;
}
