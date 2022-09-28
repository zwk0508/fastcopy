package com.zwk.generate;

import com.zwk.copy.Copier;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Type;
import sun.misc.Unsafe;

import java.beans.*;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static jdk.internal.org.objectweb.asm.Opcodes.*;

public class Generator {
    /**
     * copier 缓存
     */
    private static final Map<Class<?>, Map<Class<?>, Copier>> cache = new ConcurrentHashMap<>();
    /**
     * 生成类的自增索引
     */
    private static final AtomicInteger index = new AtomicInteger(1);
    /**
     * unsafe
     */
    private static final Unsafe unsafe;

    /**
     * Object的类名
     */
    private static final String OBJECT_CLASS_NAME = "java/lang/Object";
    /**
     * 构造器名称
     */
    private static final String CONSTRUCTOR_NAME = "<init>";
    /**
     * 构造器描述符
     */
    private static final String CONSTRUCTOR_DESCRIPTOR = "()V";
    /**
     * 生成类名的前缀
     */
    private static final String CLASS_NAME_PREFIX = "com/zwk/Generator$";
    /**
     * 生成类名的后缀
     */
    private static final String CLASS_NAME_SUFFIX = "$Copier";
    /**
     * 实现的接口名
     */
    private static final String COPIER_INTERFACE_NAME = "com/zwk/copy/Copier";
    /**
     * 实现接口的方法
     */
    private static final String COPIER_METHOD_NAME = "copy";
    /**
     * 实现接口的描述符
     */
    private static final String COPIER_METHOD_DESCRIPTOR = "(Ljava/lang/Object;Ljava/lang/Object;)V";
    /**
     * 保存生成的class文件
     */
    private static final String SAVE_GENERATED_FILES = "com.zwk.saveGeneratedFiles";


    public static Copier getCopier(Object source, Object target) {
        return getCopier(source.getClass(), target.getClass());
    }

    public static Copier getCopier(Object source, Class<?> target) {
        return getCopier(source.getClass(), target);
    }

    public static Copier getCopier(Class<?> source, Object target) {
        return getCopier(source, target.getClass());
    }

    public static Copier getCopier(Class<?> source, Class<?> target) {
        return new Generator().generate(source, target);
    }

    public Copier generate(Object source, Object target) {
        validate(source, target);
        return generate(source.getClass(), target.getClass());
    }

    public Copier generate(Object source, Class<?> target) {
        validate(source, target);
        return generate(source.getClass(), target);
    }

    public Copier generate(Class<?> source, Object target) {
        validate(source, target);
        return generate(source, target.getClass());
    }

    public Copier generate(Class<?> source, Class<?> target) {
        validate(source, target);
        Map<Class<?>, Copier> classCopierMap = cache.get(source);
        if (classCopierMap != null) {
            Copier copier = classCopierMap.get(target);
            if (copier != null) {
                return copier;
            }
        }
        Map<Method, Method> readWriteMethodInfo = getReadWriteMethodInfo(source, target);

        Copier copier = generateCopier(source, target, readWriteMethodInfo);
        if (classCopierMap == null) {
            synchronized (cache) {
                classCopierMap = cache.get(source);
                if (classCopierMap == null) {
                    classCopierMap = new HashMap<>();
                }
                classCopierMap.put(target, copier);
                cache.put(source, classCopierMap);
            }
        }
        return copier;
    }

    private Copier generateCopier(Class<?> source, Class<?> target, Map<Method, Method> readWriteMethodInfo) {
        String sourceName = source.getName().replace('.', '/');
        String targetName = target.getName().replace('.', '/');
        String fullClassName = CLASS_NAME_PREFIX + index.getAndIncrement() + CLASS_NAME_SUFFIX;
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(
                V1_8,
                ACC_PUBLIC | ACC_FINAL,
                fullClassName,
                null,
                OBJECT_CLASS_NAME,
                new String[]{COPIER_INTERFACE_NAME}
        );
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, CONSTRUCTOR_NAME, CONSTRUCTOR_DESCRIPTOR, null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, OBJECT_CLASS_NAME, CONSTRUCTOR_NAME, CONSTRUCTOR_DESCRIPTOR, false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        mv = cw.visitMethod(ACC_PUBLIC, COPIER_METHOD_NAME, COPIER_METHOD_DESCRIPTOR, null, null);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, sourceName);
        mv.visitVarInsn(ASTORE, 3);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitTypeInsn(CHECKCAST, targetName);
        mv.visitVarInsn(ASTORE, 4);
        for (Map.Entry<Method, Method> entry : readWriteMethodInfo.entrySet()) {
            Method readMethod = entry.getKey();
            Method writeMethod = entry.getValue();
            mv.visitVarInsn(ALOAD, 4);
            mv.visitVarInsn(ALOAD, 3);
            String descriptor = Type.getMethodDescriptor(readMethod);
            mv.visitMethodInsn(INVOKEVIRTUAL, sourceName, readMethod.getName(), descriptor, false);
            descriptor = Type.getMethodDescriptor(writeMethod);
            mv.visitMethodInsn(INVOKEVIRTUAL, targetName, writeMethod.getName(), descriptor, false);
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        byte[] bytes = cw.toByteArray();
        String property = System.getProperty(SAVE_GENERATED_FILES);
        if (Objects.equals("true", property)) {
            File file = new File(fullClassName + ".class");
            File parentFile = file.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                outputStream.write(bytes);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
        Class<?> clazz = unsafe.defineClass(fullClassName, bytes, 0, bytes.length, ClassLoader.getSystemClassLoader(), null);
        try {
            return (Copier) clazz.newInstance();
        } catch (Exception e) {
            try {
                return (Copier) unsafe.allocateInstance(clazz);
            } catch (InstantiationException ex) {
                return (s, t) -> {
                };
            }
        }
    }

    private Map<Method, Method> getReadWriteMethodInfo(Class<?> source, Class<?> target) {
        try {
            Map<Method, Method> info = new HashMap<>();
            BeanInfo sourceBeanInfo = Introspector.getBeanInfo(source);
            BeanInfo targetBeanInfo = Introspector.getBeanInfo(target);
            PropertyDescriptor[] sourcePropertyDescriptors = sourceBeanInfo.getPropertyDescriptors();
            PropertyDescriptor[] targetPropertyDescriptors = targetBeanInfo.getPropertyDescriptors();
            Map<String, PropertyDescriptor> targetMap = Arrays.stream(targetPropertyDescriptors)
                    .collect(Collectors.toMap(FeatureDescriptor::getName, v -> v));
            for (PropertyDescriptor descriptor : sourcePropertyDescriptors) {
                String name = descriptor.getName();
                PropertyDescriptor targetDescriptor = targetMap.get(name);
                if (targetDescriptor == null) {
                    continue;
                }
                Method writeMethod = targetDescriptor.getWriteMethod();
                if (writeMethod == null) {
                    continue;
                }
                Method readMethod = descriptor.getReadMethod();
                if (readMethod == null) {
                    continue;
                }
                Class<?> parameterType = writeMethod.getParameterTypes()[0];
                Class<?> returnType = readMethod.getReturnType();
                if (!parameterType.isAssignableFrom(returnType)) {
                    continue;
                }
                info.put(readMethod, writeMethod);
            }
            return info;
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    private void validate(Object source, Object target) {
        Objects.requireNonNull(source, "source cannot be null");
        Objects.requireNonNull(target, "target cannot be null");
    }

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
