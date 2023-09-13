package org.chk.jdbc.rsm.fn;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import org.chk.jdbc.rsm.BeanUtils;
import org.chk.jdbc.rsm.FunctionChain;
import org.chk.jdbc.rsm.Label;
import org.chk.jdbc.rsm.MappingContext;
import org.chk.jdbc.rsm.MappingException;
import org.chk.jdbc.rsm.Property;
import static org.chk.jdbc.rsm.VirtualObject.UNKNOWN_ENTITY;

public class EntityFunction extends WriteFunction {

	private String className;
	private Class type;

	public EntityFunction(MappingContext context, String[] args) throws MappingException {
		if (args.length <= 0) {
			this.className = "S";
		} else {
			this.className = args[0];
		}
	}

	public EntityFunction(Class type) {
		this.type = type;
	}

	@Override
	public void execute(MappingContext context, Object parentEntity, Label label, FunctionChain chain) throws MappingException, SQLException {
		if (chain.getEntity() != UNKNOWN_ENTITY) {
			return;
		}
		BeanUtils beanUtils = context.getConfig().getBeanUtils();
		Object entity;
		if ("S".equals(className)) {
			Property property = label.getProperties().get(0);
			entity = property.readValue(context, null);
		} else {
			entity = type != null ? beanUtils.newInstance(type) : beanUtils.newInstance(className);
			writeEntityProperty(context, label, entity);
		}
		chain.setEntity(entity);
		chain.doNext();
	}

	@Override
	public List<Feature> features() {
		return Arrays.asList(Feature.BUILD_ENTITY);
	}

	@Override
	public int order() {
		return 300;
	}
}
