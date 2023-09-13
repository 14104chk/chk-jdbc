package org.chk.jdbc.rsm.fn;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.chk.jdbc.rsm.BeanUtils;
import org.chk.jdbc.rsm.FunctionChain;
import org.chk.jdbc.rsm.Label;
import org.chk.jdbc.rsm.MappingContext;
import org.chk.jdbc.rsm.MappingException;
import org.chk.jdbc.rsm.VirtualObject;

public class MapFunction implements Function {

	private final String keyPropertyName;

	public MapFunction(MappingContext context, String[] args) throws MappingException {
		if (args.length <= 0) {
			throw new MappingException("Must specify an attribute as the Key of the Map");
		} else {
			this.keyPropertyName = args[0];
		}
	}

	@Override
	public void execute(MappingContext context, Object parentEntity, Label label, FunctionChain chain) throws MappingException, SQLException {
		BeanUtils beanUtils = context.getConfig().getBeanUtils();
		String propertyName = label.getPropertyName();
		Map map;
		if ((map = (Map) beanUtils.getProperty(parentEntity, propertyName)) == null) {
			map = new HashMap<>();
			beanUtils.setProperty(parentEntity, propertyName, map);
		}
		//
		chain.doNext();
		//
		Object entity = chain.getEntity();
		if (!(entity instanceof VirtualObject)) {
			Object key = beanUtils.getProperty(entity, keyPropertyName);
			map.putIfAbsent(key, entity);
		}
	}

	@Override
	public List<Feature> features() {
		return Arrays.asList(Feature.SET_ENTITY);
	}

	@Override
	public int order() {
		return 100;
	}
}
