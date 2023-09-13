package org.chk.jdbc.rsm.fn;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import org.chk.jdbc.rsm.BeanUtils;
import org.chk.jdbc.rsm.FunctionChain;
import org.chk.jdbc.rsm.Label;
import org.chk.jdbc.rsm.MappingContext;
import org.chk.jdbc.rsm.MappingException;
import org.chk.jdbc.rsm.VirtualObject;

public class ListFunction implements Function {

	private String className;

	public ListFunction() {
	}

	public ListFunction(MappingContext context, String[] args) {
		if (args.length <= 0) {
		} else {
			this.className = args[0];
		}
	}

	private List newList(MappingContext context) throws MappingException {
		if (className == null) {
			return context.getConfig().buildMappingResult();
		}
		return (List) context.getConfig().getBeanUtils().newInstance(className);
	}

	@Override
	public void execute(MappingContext context, Object parentEntity, Label label, FunctionChain chain) throws MappingException, SQLException {
		BeanUtils beanUtils = context.getConfig().getBeanUtils();
		String propertyName = label.getPropertyName();
		List list;
		if (propertyName.isEmpty()) {
			if ((list = (List) context.getResult()) == null) {
				list = newList(context);
				context.setResult(list);
			}
		} else {
			list = null;
		}
		//
		chain.doNext();
		//
		Object entity = chain.getEntity();
		if (entity instanceof VirtualObject) {
			return;
		}
		//
		if (list == null) {
			if ((list = (List) beanUtils.getProperty(parentEntity, propertyName)) == null) {
				list = newList(context);
				beanUtils.setProperty(parentEntity, propertyName, list);
			}
		}
		//
		if (!list.contains(entity)) {
			list.add(entity);
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
