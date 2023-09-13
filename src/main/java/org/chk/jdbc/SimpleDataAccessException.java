package org.chk.jdbc;

public class SimpleDataAccessException extends org.springframework.dao.DataAccessException {

	public SimpleDataAccessException(String msg) {
		super(msg);
	}

	public SimpleDataAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
