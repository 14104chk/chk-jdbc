package org.chk.jdbc.rsm.fn;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import org.chk.jdbc.rsm.FunctionChain;
import org.chk.jdbc.rsm.Label;
import org.chk.jdbc.rsm.MappingContext;
import org.chk.jdbc.rsm.MappingException;

public interface Function extends Comparable<Function> {

	public static enum Feature {
		SET_ENTITY(1), BUILD_ENTITY(1 << 1);
		private final int mask;

		private Feature(int mask) {
			this.mask = mask;
		}

		public int getMask() {
			return mask;
		}
	}

	public void execute(MappingContext context, Object parentEntity, Label label, FunctionChain chain) throws MappingException, SQLException;

	public default List<Feature> features() {
		return Collections.emptyList();
	}

	public int order();

	@Override
	public default int compareTo(Function o) {
		return this.order() - o.order();
	}

}
