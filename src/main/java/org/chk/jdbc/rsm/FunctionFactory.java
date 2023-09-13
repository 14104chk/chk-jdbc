package org.chk.jdbc.rsm;

import org.chk.jdbc.rsm.fn.Function;

public interface FunctionFactory {

	public Function build(MappingContext context, String[] args) throws MappingException;
}
