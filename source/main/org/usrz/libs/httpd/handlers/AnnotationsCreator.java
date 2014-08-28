/* ========================================================================== *
 * Copyright 2014 USRZ.com and Pier Paolo Fumagalli                           *
 * -------------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * you may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at                                    *
 *                                                                            *
 *  http://www.apache.org/licenses/LICENSE-2.0                                *
 *                                                                            *
 * Unless required by applicable law or agreed to in writing, software        *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.                                             *
 * ========================================================================== */
package org.usrz.libs.httpd.handlers;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


// TODO: move this to utils!!!!
public class AnnotationsCreator {

    private static final ConcurrentHashMap<Class<?>, Object> cache = new ConcurrentHashMap<>();

    private static boolean isEquals(Method method) {
        return method.getName().equals("equals")
            && method.getParameterCount() == 1
            && method.getParameterTypes()[0].equals(Object.class)
            && boolean.class.equals(method.getReturnType());
    }

    private static boolean isToString(Method method) {
        return method.getName().equals("toString")
            && method.getParameterCount() == 0
            && String.class.equals(method.getReturnType());
    }

    private static boolean isHashCode(Method method) {
        return method.getName().equals("hashCode")
            && method.getParameterCount() == 0
            && int.class.equals(method.getReturnType());
    }

    private static boolean isAnnotationType(Method method) {
        return method.getName().equals("annotationType")
            && method.getParameterCount() == 0
            && Class.class.equals(method.getReturnType());
    }

    private static boolean isKnownMethod(Method method) {
        return isEquals(method)
            || isToString(method)
            || isHashCode(method)
            || isAnnotationType(method);
    }

    public static <T extends Annotation> T generateAnnotation(Class<T> type) {
        Object annotation = cache.get(type);

        if (annotation == null) {

            final Set<Method> methods = new HashSet<>();
            methods.addAll(Arrays.asList(type.getMethods()));
            methods.addAll(Arrays.asList(type.getDeclaredMethods()));

            final Map<Method, Object> defaultValues = new HashMap<>();

            for (Method method: methods) {
                if (isKnownMethod(method)) continue;

                final Object value = method.getDefaultValue();
                if (value == null) throw new IllegalArgumentException("Method " + method + " does not define a default value");
                defaultValues.put(method, value);
            }

            annotation = Proxy.newProxyInstance(type.getClassLoader(), new Class[] { type }, new InvocationHandler() {

                @Override
                public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
                    if (isEquals(method)) {
                        final Object object = args[0];
                        if (object == null) return false;
                        if (object == proxy) return true;
                        return object.equals(proxy);

                    } else if (isToString(method)) {
                        final StringBuilder builder = new StringBuilder();
                        defaultValues.forEach((m, value) ->
                            builder.append(", ").append(m.getName())
                                   .append("=") .append(value));
                        if (builder.length() > 1) {
                            builder.delete(0, 2).insert(0, "(").append(")");
                        } else {
                            builder.append("()");
                        }
                        return "@" + type.getName() + builder;

                    } else if (isHashCode(method)) {
                        int hashCode = 0;
                        for (Map.Entry<Method, Object> entry: defaultValues.entrySet()){
                            final Method m = entry.getKey();
                            final Object v = entry.getValue();
                            final int vh;
                            if (v.getClass().isArray()) {
                                if (v.getClass().getComponentType().equals(boolean.class)) {
                                    vh = Arrays.hashCode((boolean[]) v);
                                } else if (v.getClass().getComponentType().equals(byte.class)) {
                                    vh = Arrays.hashCode((byte[]) v);
                                } else if (v.getClass().getComponentType().equals(char.class)) {
                                    vh = Arrays.hashCode((char[]) v);
                                } else if (v.getClass().getComponentType().equals(double.class)) {
                                    vh = Arrays.hashCode((double[]) v);
                                } else if (v.getClass().getComponentType().equals(float.class)) {
                                    vh = Arrays.hashCode((float[]) v);
                                } else if (v.getClass().getComponentType().equals(int.class)) {
                                    vh = Arrays.hashCode((int[]) v);
                                } else if (v.getClass().getComponentType().equals(long.class)) {
                                    vh = Arrays.hashCode((long[]) v);
                                } else if (v.getClass().getComponentType().equals(short.class)) {
                                    vh = Arrays.hashCode((short[]) v);
                                } else {
                                    vh = Arrays.hashCode((Object[]) v);
                                }
                            } else {
                                vh = v.hashCode();
                            }

                            hashCode += (127 * m.getName().hashCode()) ^ vh;

                        }
                        return hashCode;

                    } else if (isAnnotationType(method)) {
                        return type;

                    } else if (defaultValues.containsKey(method)) {
                        return defaultValues.get(method);

                    } else {
                        throw new UnsupportedOperationException("Unknown method " + method);
                    }
                }

            });
        }

        Object cached = cache.putIfAbsent(type, annotation);
        if (cached != null) annotation = cached;
        return type.cast(annotation);
    }
}
