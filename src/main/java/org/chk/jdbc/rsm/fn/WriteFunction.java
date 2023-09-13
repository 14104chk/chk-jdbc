package org.chk.jdbc.rsm.fn;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import org.chk.jdbc.rsm.FunctionChain;
import org.chk.jdbc.rsm.Label;
import org.chk.jdbc.rsm.MappingContext;
import org.chk.jdbc.rsm.MappingException;
import org.chk.jdbc.rsm.Property;
import org.chk.jdbc.rsm.VirtualObject;
import org.chk.jdbc.rsm.cs.ColumnSetter;

public class WriteFunction implements Function {

	protected Set _cantSet;

	public WriteFunction() {
	}

	public WriteFunction(MappingContext context, String[] args) throws MappingException {
	}

	@Override
	public void execute(MappingContext context, Object parentEntity, Label label, FunctionChain chain) throws MappingException, SQLException {
		Object entity = chain.getEntity();
		if (entity instanceof VirtualObject) {
			return;
		}
		writeEntityProperty(context, label, entity);
		chain.doNext();
	}

	protected void writeEntityProperty(MappingContext context, Label label, Object entity) throws MappingException, SQLException {
		write_property:
		for (Property property : label.getProperties()) {
			for (ColumnSetter setter : context.getConfig().getColumnSetters()) {
				if (setter.set(context, entity, property)) {
					continue write_property;
				}
			}
		}
	}

	protected Set getCantSet() {
		if (_cantSet == null) {
			_cantSet = new HashSet<>();
		}
		return _cantSet;
	}

	@Override
	public int order() {
		return 350;
	}
}
