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

import javax.validation.Path;
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;

/**
 * Description: object holding the property path as a list of nodes.
 * (Implementation partially based on reference implementation)
 * <br/>
 * This class is not synchronized.
 * 
 * @version $Rev: 1498347 $ $Date: 2013-07-01 12:06:18 +0200 (lun., 01 juil. 2013) $
 */
public class PathImpl implements Path, Serializable {

    private static final long serialVersionUID = 1L;

    static final String PROPERTY_PATH_SEPARATOR = ".";

    /**
     * Builds non-root paths from expressions.
     */
    private static class PathImplBuilder implements PathNavigation.Callback<PathImpl> {
        PathImpl result = new PathImpl();

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleProperty(String name) {
            result.addProperty(name);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleIndexOrKey(String value) {
            // with no context to guide us, we can only parse ints and fall back to String keys:
            NodeImpl node;
            try {
                node = NodeImpl.atIndex(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                node = NodeImpl.atKey(value);
            }
            result.addNode(node);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public PathImpl result() {
            if (result.nodeList.isEmpty()) {
                throw new IllegalStateException();
            }
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleGenericInIterable() {
            result.addNode(NodeImpl.atIndex(null));
        }

    }

    /**
     * Returns a {@code Path} instance representing the path described by the given string. To create a root node the
     * empty string should be passed. Note: This signature is to maintain pluggability with the RI impl.
     * 
     * @param propertyPath
     *            the path as string representation.
     * @return a {@code Path} instance representing the path described by the given string.
     */
    public static PathImpl createPathFromString(String propertyPath) {
        if (propertyPath == null || propertyPath.isEmpty()) {
            return create();
        }
        return PathNavigation.navigateAndReturn(propertyPath, new PathImplBuilder());
    }

    /**
     * Create a {@link PathImpl} instance representing the specified path.
     *
     * @return PathImpl
     */
    public static PathImpl create() {
        final PathImpl path = new PathImpl();
        final NodeImpl node = new NodeImpl.BeanNodeImpl();
        path.addNode(node);
        return path;
    }

    /**
     * Copy another Path.
     * 
     * @param path
     * @return new {@link PathImpl}
     */
    public static PathImpl copy(Path path) {
        return path == null ? null : new PathImpl(path);
    }

    private static Node newNode(final Node cast) {
        if (PropertyNode.class.isInstance(cast)) {
            return new NodeImpl.PropertyNodeImpl(cast);
        }
        if (BeanNode.class.isInstance(cast)) {
            return new NodeImpl.BeanNodeImpl(cast);
        }
        if (MethodNode.class.isInstance(cast)) {
            return new NodeImpl.MethodNodeImpl(cast);
        }
        if (ConstructorNode.class.isInstance(cast)) {
            return new NodeImpl.ConstructorNodeImpl(cast);
        }
        if (ConstructorNode.class.isInstance(cast)) {
            return new NodeImpl.ConstructorNodeImpl(cast);
        }
        if (ReturnValueNode.class.isInstance(cast)) {
            return new NodeImpl.ReturnValueNodeImpl(cast);
        }
        if (ParameterNode.class.isInstance(cast)) {
            return new NodeImpl.ParameterNodeImpl(cast);
        }
        if (CrossParameterNode.class.isInstance(cast)) {
            return new NodeImpl.CrossParameterNodeImpl(cast);
        }
        return new NodeImpl(cast);
    }

    private final LinkedList<Node> nodeList = new LinkedList<>();

    private PathImpl() {
    }

    private PathImpl(Iterable<? extends Node> nodes) {
        for (final Path.Node node : nodes) {
            nodeList.add(newNode(node));
        }
    }

    /**
     * Learn whether this {@link PathImpl} points to the root of its graph.
     * 
     * @return true if no child nodes
     */
    // our implementation stores a nameless root node.
    public boolean isRootPath() {
        if (nodeList.size() != 1) {
            return false;
        }
        Path.Node first = nodeList.peekFirst();
        return !first.isInIterable() && first.getName() == null;
    }

    /**
     * Return a new {@link PathImpl} that represents <code>this</code> minus its leaf node (if present).
     * 
     * @return PathImpl
     */
    public PathImpl getPathWithoutLeafNode() {
        if (nodeList.size() < 2) {
            return null;
        }
        return new PathImpl(nodeList.subList(0, nodeList.size() - 1));
    }

    /**
     * Add a node to this {@link PathImpl}.
     * 
     * @param node
     *            to add
     */
    public void addNode(Node node) {
        if (isRootPath()) {
            nodeList.pop();
        }
        nodeList.add(node);
    }

    /**
     * Encapsulate the node manipulations needed to add a named property to this path.
     * 
     * @param name
     */
    public void addProperty(String name) {
        if (!nodeList.isEmpty()) {
            NodeImpl leaf = getLeafNode();
            if (leaf != null && leaf.isInIterable() && leaf.getName() == null) { // TODO: avoid to be here
                if (!PropertyNode.class.isInstance(leaf)) {
                    final NodeImpl tmp = new NodeImpl.PropertyNodeImpl(leaf);
                    removeLeafNode();
                    addNode(tmp);
                    leaf = tmp;
                }
                leaf.setName(name);
                return;
            }
        }

        final NodeImpl node;
        if ("<cross-parameter>".equals(name)) {
            node = new NodeImpl.CrossParameterNodeImpl();
        } else {
            node = new NodeImpl.PropertyNodeImpl(name);
        }
        addNode(node);

    }

    /**
     * Trim the leaf node from this {@link PathImpl}.
     * 
     * @return the node removed
     * @throws IllegalStateException
     *             if no nodes are found
     */
    public Node removeLeafNode() {
        if (isRootPath() || nodeList.isEmpty()) {
            throw new IllegalStateException("No nodes in path!");
        }
        try {
            return nodeList.removeLast();
        } finally {
            if (nodeList.isEmpty()) {
                nodeList.add(new NodeImpl((String) null));
            }
        }
    }

    /**
     * Get the leaf node (if any) from this {@link PathImpl}
     * 
     * @return {@link NodeImpl}
     */
    public NodeImpl getLeafNode() {
        if (nodeList.isEmpty()) {
            return null;
        }
        return (NodeImpl) nodeList.peekLast();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Path.Node> iterator() {
        return nodeList.iterator();
    }

    /**
     * Learn whether <code>path</code> is a parent to <code>this</code>.
     * 
     * @param path
     * @return <code>true</code> if our nodes begin with nodes equal to those found in <code>path</code>
     */
    public boolean isSubPathOf(Path path) {
        if (path instanceof PathImpl && ((PathImpl) path).isRootPath()) {
            return true;
        }
        Iterator<Node> pathIter = path.iterator();
        Iterator<Node> thisIter = iterator();
        while (pathIter.hasNext()) {
            Node pathNode = pathIter.next();
            if (!thisIter.hasNext()) {
                return false;
            }
            Node thisNode = thisIter.next();
            if (pathNode.isInIterable()) {
                if (!thisNode.isInIterable()) {
                    return false;
                }
                if (pathNode.getIndex() != null && !pathNode.getIndex().equals(thisNode.getIndex())) {
                    return false;
                }
                if (pathNode.getKey() != null && !pathNode.getKey().equals(thisNode.getKey())) {
                    return false;
                }
            } else if (thisNode.isInIterable()) {
                // in this case we have shown that the proposed parent is not
                // indexed, and we are, thus the paths cannot match
                return false;
            }
            if (pathNode.getName() == null || pathNode.getName().equals(thisNode.getName())) {
                continue;
            }
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Path.Node node : this) {
            NodeImpl.appendNode(node, builder);
        }
        return builder.toString();
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
        return Objects.equals(nodeList, ((PathImpl) o).nodeList);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(nodeList);
    }

}
