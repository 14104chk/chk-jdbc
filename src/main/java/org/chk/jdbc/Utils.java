package org.chk.jdbc;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import static java.beans.Introspector.IGNORE_ALL_BEANINFO;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.commons.lang3.StringUtils;

public class Utils {

	public static <E> E trimToNull(E obj) {
		if (obj == null) {
			return null;
		}
		try {
			BeanInfo info = Introspector.getBeanInfo(obj.getClass(), IGNORE_ALL_BEANINFO);
			for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
				if (pd.getPropertyType() == String.class) {
					Method wm = pd.getWriteMethod();
					if (wm != null) {
						Method rm = pd.getReadMethod();
						if (rm != null) {
							Object value = rm.invoke(obj);
							if (value != null) {
								wm.invoke(obj, StringUtils.trimToNull((String) value));
							}
						}
					}
				}
			}
		} catch (IntrospectionException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
			throw new RuntimeException(ex);
		}
		return obj;
	}
}
