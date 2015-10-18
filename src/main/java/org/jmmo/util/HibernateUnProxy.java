package org.jmmo.util;

import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;

import javax.persistence.Transient;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HibernateUnProxy {
    private HibernateUnProxy() {}

    /**
     * Creates initialized and non proxy copy of Hibernate entity and all its fields.
     * @param maybeProxy entity object
     * @param <T> entity type
     * @return real non proxy copy of entity
     */
    public static <T> T deepUnProxy(T maybeProxy) {
        if (maybeProxy instanceof Object[] || maybeProxy instanceof Set || maybeProxy instanceof Map || maybeProxy instanceof List) {
            //noinspection unchecked
            return (T) processValue(maybeProxy, new HashMap<Object, Object>());
        }
        else {
            return deepUnProxy(maybeProxy, new HashMap<Object, Object>());
        }
    }

    /**
     * Initialize and unproxy Hibernate entity. Only entity itself not its collections.
     * @param maybeProxy entity object
     * @param <T> entity type
     * @return real non proxy entity
     */
    @SuppressWarnings("unchecked")
    public static <T> T unProxy(T maybeProxy) {
        if (maybeProxy == null) {
            throw new IllegalArgumentException("Entity passed for unproxy is null");
        }

        Hibernate.initialize(maybeProxy);
        if (maybeProxy instanceof HibernateProxy){
            return (T) ((HibernateProxy) maybeProxy).getHibernateLazyInitializer().getImplementation();
        }
        else {
            return maybeProxy;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T deepUnProxy(T maybeProxy, Map<Object, Object> visited) {
        final T nonProxy = unProxy(maybeProxy);
        final Object cached = visited.get(nonProxy);
        if (cached != null) {
            return (T) cached;
        }

        Class<?> clazz = nonProxy.getClass();
        if (Integer.class.getPackage().equals(clazz.getPackage())) {
            visited.put(nonProxy, nonProxy);
            return nonProxy;
        }

        final T copy;
        try {
            copy = (T) clazz.newInstance();
            do {
                processFields(nonProxy, copy, clazz.getDeclaredFields(), visited);
                clazz = clazz.getSuperclass();
            } while (clazz != null);
        }
        catch (Exception e) {
            throw new IllegalArgumentException(e);
        }

        visited.put(nonProxy, copy);

        return copy;
    }

    @SuppressWarnings("unchecked")
    private static void processFields(Object owner, Object copy, Field[] fields, Map<Object, Object> visited) throws IllegalAccessException {
        for (Field field : fields) {
            final int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
                continue;
            }

            if (!field.isAccessible()) {
                field.setAccessible(true);
            }

            Object value = field.get(owner);
            if (!Modifier.isTransient(modifiers) && !field.isAnnotationPresent(Transient.class)) {
                value = processValue(value, visited);
            }

            field.set(copy, value);
        }
    }

    private static Object processValue(Object value, Map<Object, Object> visited) {
        final Object result;

        if (value instanceof HibernateProxy) {
            result = deepUnProxy(value, visited);
        }
        else if (value instanceof Object[]) {
            Object[] valueArray = (Object[]) value;
            Object[] res = (Object[]) Array.newInstance(value.getClass(), valueArray.length);
            for (int i = 0; i < valueArray.length; i++) {
                res[i] = deepUnProxy(valueArray[i], visited);
            }
            result = res;
        }
        else if (value instanceof Set) {
            Set valueSet = (Set) value;
            Set res = new HashSet();
            for (Object object : valueSet) {
                //noinspection unchecked
                res.add(deepUnProxy(object, visited));
            }
            result = res;
        }
        else if (value instanceof Map) {
            //noinspection unchecked
            Map<Object, Object> valueMap = (Map) value;
            Map res = new HashMap();
            for (Map.Entry<Object, Object> entry : valueMap.entrySet()) {
                //noinspection unchecked
                res.put(deepUnProxy(entry.getKey(), visited), deepUnProxy(entry.getValue(), visited));
            }
            result = res;
        }
        else if (value instanceof List) {
            List valueList = (List) value;
            List res = new ArrayList(valueList.size());
            for (Object object : valueList) {
                //noinspection unchecked
                res.add(deepUnProxy(object, visited));
            }
            result = res;
        }
        else {
            result = value;
        }

        return result;
    }
}