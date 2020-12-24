package au.com.auspost.em.ms.common.utils;

import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

public class DeepInstantiator {

    private Set<Class<?>> ignoredClasses;

    public DeepInstantiator() {
        ignoredClasses = Set.of(
                String.class,
                ZonedDateTime.class,
                LocalDate.class,
                LocalDateTime.class
        );
    }

    public DeepInstantiator(Set<Class<?>> ignoredClasses) {
        ignoredClasses = Set.of(
                String.class,
                ZonedDateTime.class,
                LocalDate.class,
                LocalDateTime.class
        );
    }


    public <T> T instantiate(Class<T> targetClass) throws Exception {
        var object = instantiateObject(targetClass);
        recursiveObjectInstantiation(object);
        return object;
    }

    private void recursiveObjectInstantiation(Object source) throws Exception {
        if (source == null || !isInstantiatable(source.getClass())) {
            return;
        }

        for (Field field : source.getClass().getDeclaredFields()) {
            var fieldIsAccessible = field.canAccess(source);
            field.setAccessible(true);

            var fieldType = field.getType();
            if (!isInstantiatable(field.getType())) {
                continue;
            }

            if (fieldType.isInterface()) {
                var instantiatedObj = instantiateObject((ParameterizedType) field.getGenericType());
                field.set(source, instantiatedObj);
                field.setAccessible(fieldIsAccessible);
                continue;
            } else {
                var instantiatedObj = instantiateObject(fieldType);
                field.set(source, instantiatedObj);
                field.setAccessible(fieldIsAccessible);
                recursiveObjectInstantiation(instantiatedObj);
            }
        }
    }

    private <T> T instantiateObject(Class<T> tClass) throws Exception {
        if (isInstantiatable(tClass)) {
            try {
                return tClass.getConstructor().newInstance();
            } catch (InstantiationException
                    | InvocationTargetException
                    | NoSuchMethodException
                    | IllegalAccessException
                    | IllegalArgumentException ex) {
                throw new Exception("Failed to instantiate object", ex);
            }

        }
        return null;
    }

    private Object instantiateObject(ParameterizedType pType) throws Exception {
        if (pType.getRawType().equals(Map.class)) {
            return instantiateHashMap(pType);
        } else if (pType.getRawType().equals(List.class)) {
            return instantiateArrayList(pType);
        } else if (pType.getRawType().equals(Set.class)) {
            return instantiateHashSet(pType);
        }
        return null;
    }

    private ArrayList<Object> instantiateArrayList(ParameterizedType listType) throws Exception {
        var list = new ArrayList<>();
        var listContentClass = (Class<?>) listType.getActualTypeArguments()[0];
        if (isInstantiatable(listContentClass)) {
            var listContent = instantiateObject(listContentClass);
            recursiveObjectInstantiation(listContent);
            list.add(listContent);
        }
        return list;
    }

    private HashSet<Object> instantiateHashSet(ParameterizedType setType) throws Exception {
        return new HashSet<>(instantiateArrayList(setType));
    }

    private HashMap<Object, Object> instantiateHashMap(ParameterizedType mapType) throws Exception {
        var map = new HashMap<>();

        var mapKeyClass = (Class<?>) mapType.getActualTypeArguments()[0];
        var mapValClass = (Class<?>) mapType.getActualTypeArguments()[1];

        Object mapKey = isInstantiatable(mapKeyClass) ? instantiateObject(mapKeyClass) : null;
        Object mapVal = isInstantiatable(mapValClass) ? instantiateObject(mapValClass) : null;

        if (mapKey == null && mapVal != null) {
            mapKey = getDefaultValue(mapKeyClass);
        }

        recursiveObjectInstantiation(mapKey);
        recursiveObjectInstantiation(mapVal);

        if (mapKey != null && mapVal != null) {
            map.put(mapKey, mapVal);
        }

        return map;
    }

    private <T> boolean isInstantiatable(Class<T> tClass) {
        return tClass != null
                && !ClassUtils.isPrimitiveOrWrapper(tClass)
                && !tClass.isEnum()
                && !ignoredClasses.contains(tClass);
    }

    private <T> Object getDefaultValue(Class<T> tClass) {
        if (tClass.equals(String.class)) {
            return "";
        } else if (tClass.equals(int.class) || tClass.equals(Integer.class)) {
            return 0;
        } else if (tClass.equals(float.class) || tClass.equals(Float.class)) {
            return 0F;
        } else if (tClass.equals(long.class) || tClass.equals(Long.class)) {
            return 0L;
        } else if (tClass.equals(double.class) || tClass.equals(Double.class)) {
            return 0D;
        }
        return null;
    }
}
