/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.bval.jsr.xml;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Description: This class instantiated during the parsing of the XML configuration
 * data and keeps track of the annotations which should be ignored.<br/>
 */
public final class AnnotationIgnores {

    private static final Logger log = Logger.getLogger(AnnotationIgnores.class.getName());

    /**
     * Keeps track whether the 'ignore-annotations' flag is set on bean level in the
     * xml configuration. 
     * If 'ignore-annotations' is not specified: default = true
     */
    private final Map<Class<?>, Boolean> ignoreAnnotationDefaults = new HashMap<>();

    /**
     * Keeps track of explicitly excluded members (fields and properties) for a given class.
     * If a member appears in
     * the list mapped to a given class 'ignore-annotations' was explicitly set to
     * <code>true</code> in the configuration
     * for this class.
     */
    private final Map<Class<?>, Map<Member, Boolean>> ignoreAnnotationOnMember = new HashMap<>();

    private final Map<Class<?>, Boolean> ignoreAnnotationOnClass = new HashMap<>();

    private final Map<Class<?>, Map<Member, Map<Integer, Boolean>>> ignoreAnnotationOnParameter = new HashMap<>();
    private final Map<Member, Boolean> ignoreAnnotationOnReturn = new HashMap<>();
    private final Map<Member, Boolean> ignoreAnnotationOnCrossParameter = new HashMap<>();

    /**
     * Record the ignore state for a particular annotation type.
     * @param clazz
     * @param b, default true if null
     */
    public void setDefaultIgnoreAnnotation(Class<?> clazz, Boolean b) {
        ignoreAnnotationDefaults.put(clazz, b == null ? Boolean.TRUE : b);
    }

    /**
     * Learn whether the specified annotation type should be ignored.
     * @param clazz
     * @return boolean
     */
    public boolean isDefaultIgnoreAnnotation(Class<?> clazz) {
        return Boolean.TRUE.equals(ignoreAnnotationDefaults.get(clazz));
    }

    /**
     * Ignore annotations on a particular {@link Member} of a class.
     * @param member
     */
    public void setIgnoreAnnotationsOnMember(Member member, boolean value) {
        ignoreAnnotationOnMember.computeIfAbsent(member.getDeclaringClass(), k -> new HashMap<>()).put(member, value);
    }

    /**
     * Learn whether annotations should be ignored on a particular {@link Member} of a class.
     * @param member
     * @return boolean
     */
    public boolean isIgnoreAnnotations(final Member member) {
        final Class<?> clazz = member.getDeclaringClass();
        final Map<Member, Boolean> ignoreAnnotationForMembers = ignoreAnnotationOnMember.get(clazz);
        final boolean result;
        if (ignoreAnnotationForMembers != null && ignoreAnnotationForMembers.containsKey(member)) {
            result = ignoreAnnotationForMembers.get(member).booleanValue();
        } else {
            result = isDefaultIgnoreAnnotation(clazz);
        }
        if (result) {
            logMessage(member, clazz);
        }
        return result;
    }

    public void setIgnoreAnnotationsOnParameter(final Member method, final int i, final boolean value) {
        ignoreAnnotationOnParameter.computeIfAbsent(method.getDeclaringClass(), k -> new HashMap<>())
            .computeIfAbsent(method, k -> new HashMap<>()).put(i, value);
    }

    public boolean isIgnoreAnnotationOnParameter(final Member m, final int i) {
        final Map<Member, Map<Integer, Boolean>> members = ignoreAnnotationOnParameter.get(m.getDeclaringClass());
        if (members == null) {
            return false;
        }
        final Map<Integer, Boolean> indexes = members.get(m);
        return indexes != null && Boolean.TRUE.equals(indexes.get(Integer.valueOf(i)));
    }

    private void logMessage(Member member, Class<?> clazz) {
        log.log(Level.FINEST, String.format("%s level annotations are getting ignored for %s.%s",
            member instanceof Field ? "Field" : "Property", clazz.getName(), member.getName()));
    }

    /**
     * Record the ignore state of a particular class. 
     * @param clazz
     * @param b
     */
    public void setIgnoreAnnotationsOnClass(Class<?> clazz, boolean b) {
        ignoreAnnotationOnClass.put(clazz, b);
    }

    /**
     * Learn whether annotations should be ignored for a given class.
     * @param clazz to check
     * @return boolean
     */
    public boolean isIgnoreAnnotations(Class<?> clazz) {
        boolean ignoreAnnotation = ignoreAnnotationOnClass.containsKey(clazz) ? ignoreAnnotationOnClass.get(clazz)
            : isDefaultIgnoreAnnotation(clazz);
        if (ignoreAnnotation) {
            log.log(Level.FINEST, String.format("Class level annotation are getting ignored for %s", clazz.getName()));
        }
        return ignoreAnnotation;
    }

    public void setIgnoreAnnotationOnReturn(final Member method, final boolean value) {
        ignoreAnnotationOnReturn.put(method, value);
    }

    public boolean isIgnoreAnnotationOnReturn(final Member m) {
        return Boolean.TRUE.equals(ignoreAnnotationOnReturn.get(m));
    }

    public void setIgnoreAnnotationOnCrossParameter(final Member method, final boolean value) {
        ignoreAnnotationOnCrossParameter.put(method, value);
    }

    public boolean isIgnoreAnnotationOnCrossParameter(final Member m) {
        return Boolean.TRUE.equals(ignoreAnnotationOnCrossParameter.get(m));
    }
}
