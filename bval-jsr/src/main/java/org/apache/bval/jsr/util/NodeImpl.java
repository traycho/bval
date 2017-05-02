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
package org.apache.bval.jsr.util;

import javax.validation.ElementKind;
import javax.validation.Path;
import javax.validation.Path.Node;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NodeImpl implements Path.Node, Serializable {

    private static final long serialVersionUID = 1L;
    private static final String INDEX_OPEN = "[";
    private static final String INDEX_CLOSE = "]";

    /**
     * Append a Node to the specified StringBuilder.
     * @param node
     * @param to
     * @return to
     */
    public static StringBuilder appendNode(Node node, StringBuilder to) {
        if (node.isInIterable()) {
            to.append(INDEX_OPEN);
            if (node.getIndex() != null) {
                to.append(node.getIndex());
            } else if (node.getKey() != null) {
                to.append(node.getKey());
            }
            to.append(INDEX_CLOSE);
        }
        if (node.getName() != null) {
            if (to.length() > 0) {
                to.append(PathImpl.PROPERTY_PATH_SEPARATOR);
            }
            to.append(node.getName());
        }
        return to;
    }

    /**
     * Get a NodeImpl indexed from the preceding node (or root).
     * @param index
     * @return NodeImpl
     */
    public static NodeImpl atIndex(Integer index) {
        NodeImpl result = new NodeImpl();
        result.setIndex(index);
        return result;
    }

    /**
     * Get a NodeImpl keyed from the preceding node (or root).
     * @param key
     * @return NodeImpl
     */
    public static NodeImpl atKey(Object key) {
        NodeImpl result = new NodeImpl();
        result.setKey(key);
        return result;
    }

    private String name;
    private boolean inIterable;
    private Integer index;
    private int parameterIndex;
    private Object key;
    private ElementKind kind;
    private List<Class<?>> parameterTypes;

    /**
     * Create a new NodeImpl instance.
     * @param name
     */
    public NodeImpl(String name) {
        this.name = name;
    }

    /**
     * Create a new NodeImpl instance.
     * @param node
     */
    NodeImpl(Path.Node node) {
        this(node.getName());
        this.inIterable = node.isInIterable();
        this.index = node.getIndex();
        this.key = node.getKey();
        this.kind = node.getKind();
    }

    private NodeImpl() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInIterable() {
        return inIterable;
    }

    /**
     * Set whether this node represents a contained value of an {@link Iterable} or {@link Map}.
     * @param inIterable
     */
    public void setInIterable(boolean inIterable) {
        this.inIterable = inIterable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getIndex() {
        return index;
    }

    /**
     * Set the index of this node, implying <code>inIterable</code>.
     * @param index
     */
    public void setIndex(Integer index) {
        inIterable = true;
        this.index = index;
        this.key = null;
    }

    public void setParameterIndex(final Integer parameterIndex) {
        this.parameterIndex = parameterIndex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getKey() {
        return key;
    }

    /**
     * Set the map key of this node, implying <code>inIterable</code>.
     * @param key
     */
    public void setKey(Object key) {
        inIterable = true;
        this.key = key;
        this.index = null;
    }

    @Override
    public ElementKind getKind() {
        return kind;
    }

    public void setKind(ElementKind kind) {
        this.kind = kind;
    }

    @Override
    public <T extends Node> T as(final Class<T> nodeType) {
        if (nodeType.isInstance(this)) {
            return nodeType.cast(this);
        }
        throw new ClassCastException("Type " + nodeType + " not supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return appendNode(this, new StringBuilder()).toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }

        NodeImpl node = (NodeImpl) o;

        return inIterable == node.inIterable && Objects.equals(index, node.index) && Objects.equals(key, node.key)
            && Objects.equals(name, node.name) && kind == node.kind;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, Boolean.valueOf(inIterable), index, key, kind);
    }

    public int getParameterIndex() {
        return parameterIndex;
    }

    public List<Class<?>> getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(final List<Class<?>> parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    @SuppressWarnings("serial")
    public static class ParameterNodeImpl extends NodeImpl implements Path.ParameterNode {
        public ParameterNodeImpl(final Node cast) {
            super(cast);
            if (ParameterNodeImpl.class.isInstance(cast)) {
                setParameterIndex(ParameterNodeImpl.class.cast(cast).getParameterIndex());
            }
        }

        public ParameterNodeImpl(final String name, final int idx) {
            super(name);
            setParameterIndex(idx);
        }

        @Override
        public ElementKind getKind() {
            return ElementKind.PARAMETER;
        }
    }

    @SuppressWarnings("serial")
    public static class ConstructorNodeImpl extends NodeImpl implements Path.ConstructorNode {
        public ConstructorNodeImpl(final Node cast) {
            super(cast);
            if (NodeImpl.class.isInstance(cast)) {
                setParameterTypes(NodeImpl.class.cast(cast).parameterTypes);
            }
        }

        public ConstructorNodeImpl(final String simpleName, List<Class<?>> paramTypes) {
            super(simpleName);
            setParameterTypes(paramTypes);
        }

        @Override
        public ElementKind getKind() {
            return ElementKind.CONSTRUCTOR;
        }
    }

    @SuppressWarnings("serial")
    public static class CrossParameterNodeImpl extends NodeImpl implements Path.CrossParameterNode {
        public CrossParameterNodeImpl() {
            super("<cross-parameter>");
        }

        public CrossParameterNodeImpl(final Node cast) {
            super(cast);
        }

        @Override
        public ElementKind getKind() {
            return ElementKind.CROSS_PARAMETER;
        }
    }

    @SuppressWarnings("serial")
    public static class MethodNodeImpl extends NodeImpl implements Path.MethodNode {
        public MethodNodeImpl(final Node cast) {
            super(cast);
            if (MethodNodeImpl.class.isInstance(cast)) {
                setParameterTypes(MethodNodeImpl.class.cast(cast).getParameterTypes());
            }
        }

        public MethodNodeImpl(final String name, final List<Class<?>> classes) {
            super(name);
            setParameterTypes(classes);
        }

        @Override
        public ElementKind getKind() {
            return ElementKind.METHOD;
        }
    }

    @SuppressWarnings("serial")
    public static class ReturnValueNodeImpl extends NodeImpl implements Path.ReturnValueNode {
        public ReturnValueNodeImpl(final Node cast) {
            super(cast);
        }

        public ReturnValueNodeImpl() {
            super("<return value>");
        }

        @Override
        public ElementKind getKind() {
            return ElementKind.RETURN_VALUE;
        }
    }

    @SuppressWarnings("serial")
    public static class PropertyNodeImpl extends NodeImpl implements Path.PropertyNode {
        public PropertyNodeImpl(final String name) {
            super(name);
        }

        public PropertyNodeImpl(final Node cast) {
            super(cast);
        }

        @Override
        public ElementKind getKind() {
            return ElementKind.PROPERTY;
        }
    }

    @SuppressWarnings("serial")
    public static class BeanNodeImpl extends NodeImpl implements Path.BeanNode {
        public BeanNodeImpl() {
            // no-op
        }

        public BeanNodeImpl(final Node cast) {
            super(cast);
        }

        @Override
        public ElementKind getKind() {
            return ElementKind.BEAN;
        }
    }
}
