/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.ui.forms.model;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import org.codice.ddf.catalog.ui.forms.model.JsonModel.FilterLeafNode;
import org.codice.ddf.catalog.ui.forms.model.JsonModel.FilterNode;
import org.codice.ddf.catalog.ui.forms.model.JsonModel.FilterTemplatedLeafNode;

public class JsonModelBuilder {
  // TODO: Add the rest of the operators
  private static final Set<String> BINARY_COMPARE_OPS = ImmutableSet.of("=", ">", ">=", "<", "<=");

  private static final Set<String> LOGIC_COMPARE_OPS = ImmutableSet.of("AND", "OR");

  private final Deque<List<FilterNode>> depth;

  private FilterNode rootNode = null;

  private FilterLeafNode nodeInProgress = null;

  private boolean complete = false;

  public JsonModelBuilder() {
    depth = new ArrayDeque<>();
  }

  public FilterNode getResult() {
    if (!complete) {
      canEnd();
      depth.clear();
      complete = true;
    }
    return rootNode;
  }

  public JsonModelBuilder beginBinaryLogicType(String operator) {
    canModify();
    canStartNew();
    if (!LOGIC_COMPARE_OPS.contains(operator)) {
      throw new IllegalArgumentException("Invalid operator for logic comparison type: " + operator);
    }
    List<FilterNode> nodes = new ArrayList<>();
    if (rootNode == null) {
      rootNode = new FilterNode(operator, nodes);
    } else {
      depth.peek().add(new FilterNode(operator, nodes));
    }
    depth.push(nodes);
    return this;
  }

  public JsonModelBuilder endBinaryLogicType() {
    canModify();
    canEnd();
    depth.pop();
    return this;
  }

  public JsonModelBuilder beginBinaryComparisonType(String operator) {
    canModify();
    canStartNew();
    if (!BINARY_COMPARE_OPS.contains(operator)) {
      throw new IllegalArgumentException(
          "Invalid operator for binary comparison type: " + operator);
    }
    nodeInProgress = new FilterLeafNode(operator);
    return this;
  }

  public JsonModelBuilder endBinaryComparisonType() {
    canModify();
    if (depth.isEmpty() && rootNode != null) {
      throw new IllegalStateException("If stack is empty, the root node should not be initialized");
    }
    if (depth.isEmpty()) {
      rootNode = nodeInProgress;
    } else {
      depth.peek().add(nodeInProgress);
    }
    nodeInProgress = null;
    return this;
  }

  public JsonModelBuilder setProperty(String property) {
    canModify();
    canSetField();
    nodeInProgress.setProperty(property);
    return this;
  }

  public JsonModelBuilder setValue(String value) {
    canModify();
    canSetField();
    nodeInProgress.setValue(value);
    return this;
  }

  public JsonModelBuilder setTemplatedValues(
      String defaultValue, String nodeId, boolean isVisible, boolean isReadOnly) {
    canModify();
    canSetField();
    nodeInProgress =
        new FilterTemplatedLeafNode(nodeInProgress, defaultValue, nodeId, isVisible, isReadOnly);
    return this;
  }

  private void canModify() {
    if (complete) {
      throw new IllegalStateException(
          "This builder's result has been retrieved and no further modification is permitted");
    }
  }

  private void canSetField() {
    if (nodeInProgress == null) {
      throw new IllegalStateException("Cannot set field, no leaf node in progress");
    }
  }

  private void canStartNew() {
    if (nodeInProgress != null) {
      throw new IllegalStateException("Cannot start node, a leaf node in progress");
    }
  }

  private void canEnd() {
    if (nodeInProgress != null) {
      throw new IllegalStateException("Cannot end node, a leaf node in progress");
    }
  }
}
