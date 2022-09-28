package com.zwk.utils;

import com.zwk.copy.Copier;
import com.zwk.generate.Generator;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * bean属性复制工具
 */
public class FastCopyUtil {
    /**
     * 复制属性
     *
     * @param source 源bean
     * @param target 目标bean的class，需要有无参构造函数
     * @return 目标bean
     * @throws Exception e
     */
    public static Object copy(Object source, Class<?> target) throws Exception {
        Object t = getInstance(target);
        Copier copier = Generator.getCopier(source, target);
        copier.copy(source, t);
        return t;
    }

    private static Object getInstance(Class<?> target) throws Exception {
        Constructor<?> constructor = target.getDeclaredConstructor();
        return constructor.newInstance();
    }

    /**
     * 复制属性
     *
     * @param source 源bean
     * @param target 目标bean
     */
    public static void copy(Object source, Object target) {
        Copier copier = Generator.getCopier(source, target);
        copier.copy(source, target);
    }

    /**
     * 列表属性复制
     *
     * @param source 源列表
     * @param target 目标bean的class，需要有无参构造函数
     * @param <T>    t
     * @return 目标bean 列表
     * @throws Exception e
     */
    public static <T> List<T> copy(List<?> source, Class<T> target) throws Exception {
        if (source == null) {
            throw new NullPointerException();
        }
        List<T> list = new ArrayList<>();
        for (Object o : source) {
            Object t = getInstance(target);
            copy(o, t);
            list.add((T) t);
        }
        return list;
    }
}
