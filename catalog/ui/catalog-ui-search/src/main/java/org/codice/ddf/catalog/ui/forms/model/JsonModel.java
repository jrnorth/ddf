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

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import javax.xml.bind.JAXBElement;
import net.opengis.filter.v_2_0.BinaryComparisonOpType;
import net.opengis.filter.v_2_0.BinaryLogicOpType;
import net.opengis.filter.v_2_0.LiteralType;
import net.opengis.filter.v_2_0.LogicOpsType;
import net.opengis.filter.v_2_0.ObjectFactory;
import org.boon.json.annotations.JsonIgnore;
import org.boon.json.annotations.JsonProperty;

public class JsonModel {
  private JsonModel() {
    // Should not be instantiated
  }

  public static class Described {
    private final String id;

    private final String title;

    private final String description;

    public Described(String id, String title, String description) {
      this.id = id;
      this.title = title;
      this.description = description;
    }

    public String getId() {
      return id;
    }

    public String getTitle() {
      return title;
    }

    public String getDescription() {
      return description;
    }
  }

  public static class FieldFilter extends Described {
    private final Set<String> descriptors;

    public FieldFilter(String id, String title, String description, Set<String> descriptors) {
      super(id, title, description);
      this.descriptors = descriptors;
    }

    private Set<String> getDescriptors() {
      return descriptors;
    }
  }

  public static class FormTemplate extends Described {
    private FilterNode root;

    public FormTemplate(String id, String title, String description, FilterNode root) {
      super(id, title, description);
      this.root = root;
    }

    public FilterNode getRoot() {
      return root;
    }
  }

  public static class FilterNode {
    private final String type;

    @JsonIgnore private boolean isLeaf;

    @JsonProperty("filters")
    private List<FilterNode> nodes;

    public FilterNode(String type, List<FilterNode> nodes) {
      this.type = type;
      this.nodes = nodes;
      this.isLeaf = false;
    }

    public String getType() {
      return type;
    }

    public boolean isLeaf() {
      return isLeaf;
    }

    public List<FilterNode> getNodes() {
      return nodes;
    }

    protected void setLeaf(boolean isLeaf) {
      this.isLeaf = isLeaf;
    }

    public void setNodes(List<FilterNode> nodes) {
      this.nodes = nodes;
    }

    public JAXBElement exportAsJAXB(ObjectFactory factory) {
      BinaryLogicOpType op = new BinaryLogicOpType();

      for (FilterNode node : nodes) {
        op.getOps().add(node.exportAsJAXB(factory));
      }

      JAXBElement<? extends LogicOpsType> element;
      if ("AND".equals(type)) {
        element = factory.createAnd(op);
      } else {
        element = factory.createOr(op);
      }

      return element;
    }
  }

  public static class FilterLeafNode extends FilterNode {
    private String property;

    // If changed, update the FilterNodeValueSerializer as well
    private String value;

    private boolean templated;

    public FilterLeafNode(String type) {
      super(type, null);
      setLeaf(true);
      setTemplated(false);
    }

    public FilterLeafNode(FilterLeafNode node) {
      super(node.getType(), null);
      setLeaf(true);
      setProperty(node.getProperty());
      setValue(node.getValue());
      setTemplated(node.isTemplated());
    }

    public String getProperty() {
      return property;
    }

    public String getValue() {
      return value;
    }

    public boolean isTemplated() {
      return templated;
    }

    public void setProperty(String property) {
      this.property = property;
    }

    public void setValue(String value) {
      this.value = value;
    }

    public void setTemplated(boolean templated) {
      this.templated = templated;
    }

    @Override
    public JAXBElement exportAsJAXB(ObjectFactory factory) {
      // @formatter:off
      factory.createPropertyIsEqualTo(
          new BinaryComparisonOpType()
              .withExpression(
                  factory.createValueReference(property),
                  factory.createLiteral(new LiteralType().withContent((Serializable) value))));
      // @formatter:on
      return null;
    }
  }

  public static class FilterTemplatedLeafNode extends FilterLeafNode {
    // If changed, update the FilterNodeValueSerializer as well
    private String defaultValue;

    private String nodeId;

    private boolean isVisible;

    private boolean isReadOnly;

    public FilterTemplatedLeafNode(String type) {
      super(type);
      setTemplated(true);
      setDefaultValue(null);
      setNodeId(null);
      setVisible(true);
      setReadOnly(false);
    }

    public FilterTemplatedLeafNode(
        FilterLeafNode node,
        String defaultValue,
        String nodeId,
        boolean isVisible,
        boolean isReadOnly) {
      super(node);
      setTemplated(true);
      setDefaultValue(defaultValue);
      setNodeId(nodeId);
      setVisible(isVisible);
      setReadOnly(isReadOnly);
    }

    public String getDefaultValue() {
      return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
      this.defaultValue = defaultValue;
    }

    public String getNodeId() {
      return nodeId;
    }

    public void setNodeId(String nodeId) {
      this.nodeId = nodeId;
    }

    public boolean isVisible() {
      return isVisible;
    }

    public void setVisible(boolean visible) {
      isVisible = visible;
    }

    public boolean isReadOnly() {
      return isReadOnly;
    }

    public void setReadOnly(boolean readOnly) {
      isReadOnly = readOnly;
    }
  }
}
