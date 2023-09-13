package org.chk.jdbc.rsm;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.chk.jdbc.rsm.cs.CachedColumnSetter;
import org.chk.jdbc.rsm.cs.ColumnSetter;
import org.chk.jdbc.rsm.cs.DirectResultSetter;
import org.chk.jdbc.rsm.cs.MapSetter;
import org.chk.jdbc.rsm.cs.PojoSetter;
import org.chk.jdbc.rsm.cs.SpecificTypeSetter;
import org.chk.jdbc.rsm.fn.EntityFunction;
import org.chk.jdbc.rsm.fn.EntryFunction;
import org.chk.jdbc.rsm.fn.Function;
import org.chk.jdbc.rsm.fn.IdFunction;
import org.chk.jdbc.rsm.fn.ListFunction;
import org.chk.jdbc.rsm.fn.MapFunction;
import org.chk.jdbc.rsm.fn.WriteFunction;
import org.chk.jdbc.rsm.pr.PropertyReaders;
import org.chk.jdbc.rsm.pr.PropertyReadersImpl;

public class MappingConfig {

	protected Map<String, FunctionFactory> functionFactories = new HashMap<>();
	protected BeanUtils beanUtils;
	private PropertyReaders propertyReaders;
	protected String defaultEntityPackage;
	protected List<ColumnSetter> columnSetters;
	protected Function defaultSetEntityFunction;
	protected Map<String, Object> options = new HashMap<>();

	public static MappingConfig getDefault() {
		MappingConfig config = new MappingConfig();
		config.setBeanUtils(new BeanUtils());
		config.getBeanUtils().setConfig(config);
		//
		config.setDefaultSetEntityFunction(new Function() {
			@Override
			public void execute(MappingContext context, Object parentEntity, Label label, FunctionChain chain) throws MappingException, SQLException {
				String propertyName = label.getPropertyName();
				if (propertyName.isEmpty()) {
					if (context.getResult() == null) {
						context.setResult(context.getConfig().buildMappingResult());
					}
				}
				chain.doNext();
				Object entity = chain.getEntity();
				if (entity instanceof VirtualObject) {
					return;
				}
				if (propertyName.isEmpty()) {
					((List) context.getResult()).add(entity);
				} else {
					context.getConfig().getBeanUtils().setProperty(parentEntity, propertyName, entity);
				}
			}

			@Override
			public List<Function.Feature> features() {
				return Arrays.asList(Function.Feature.SET_ENTITY);
			}

			@Override
			public int order() {
				return 100;
			}

		});
		//
		config.registerFunctionFactory("list", ListFunction::new);
		config.registerFunctionFactory("map", MapFunction::new);
		config.registerFunctionFactory("id", IdFunction::new);
		config.registerFunctionFactory("entity", EntityFunction::new);
		config.registerFunctionFactory("entry", EntryFunction::new);
		config.registerFunctionFactory("write", WriteFunction::new);
		//
		config.propertyReaders = new PropertyReadersImpl();
		config.setColumnSetters(Arrays.asList(
				new SpecificTypeSetter(),
				new CachedColumnSetter(new MapSetter(), new DirectResultSetter(), new PojoSetter())
		));
		return config;
	}

	public List buildMappingResult() {
		return new ArrayList();
	}

	public void registerFunctionFactory(String name, FunctionFactory functionFactory) {
		this.functionFactories.put(name, functionFactory);
	}

	public void deregisterFunctionFactory(String name, FunctionFactory functionFactory) {
		this.functionFactories.remove(name);
	}

	public Function buildFunction(MappingContext context, String name, String[] args) throws SQLException {
		FunctionFactory f = functionFactories.get(name);
		if (f == null) {
			throw new MappingException(String.format("[%s] is unsupported", name));
		}
		return f.build(context, args);
	}

	public BeanUtils getBeanUtils() {
		return beanUtils;
	}

	public void setBeanUtils(BeanUtils beanUtils) {
		this.beanUtils = beanUtils;
	}

	public PropertyReaders getPropertyReaders() {
		return propertyReaders;
	}

	public void setPropertyReaders(PropertyReaders propertyReaders) {
		this.propertyReaders = propertyReaders;
	}

	public String getDefaultEntityPackage() {
		return defaultEntityPackage;
	}

	public void setDefaultEntityPackage(String defaultEntityPackage) {
		this.defaultEntityPackage = defaultEntityPackage;
	}

	public List<ColumnSetter> getColumnSetters() {
		return columnSetters;
	}

	public void setColumnSetters(List<ColumnSetter> columnSetters) {
		this.columnSetters = columnSetters;
	}

	public Function getDefaultSetEntityFunction() {
		return defaultSetEntityFunction;
	}

	public void setDefaultSetEntityFunction(Function defaultSetEntityFunction) {
		this.defaultSetEntityFunction = defaultSetEntityFunction;
	}

	public Map<String, Object> getOptions() {
		return options;
	}

	public void setOptions(Map<String, Object> options) {
		this.options = options;
	}

}
