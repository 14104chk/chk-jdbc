package org.chk.jdbc;

import java.io.Closeable;

public interface SilentCloseable extends Closeable {

	@Override
	public void close();

}
