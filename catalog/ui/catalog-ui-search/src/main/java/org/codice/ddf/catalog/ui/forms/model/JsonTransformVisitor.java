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

import static org.codice.ddf.catalog.ui.forms.Main.print;
import static org.codice.ddf.catalog.ui.forms.filter.VisitableFilterNode.makeVisitable;

import com.google.common.collect.ImmutableMap;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.bind.JAXBElement;
import net.opengis.filter.v_2_0.BBOXType;
import net.opengis.filter.v_2_0.BinaryComparisonOpType;
import net.opengis.filter.v_2_0.BinaryLogicOpType;
import net.opengis.filter.v_2_0.BinarySpatialOpType;
import net.opengis.filter.v_2_0.BinaryTemporalOpType;
import net.opengis.filter.v_2_0.DistanceBufferType;
import net.opengis.filter.v_2_0.FilterType;
import net.opengis.filter.v_2_0.FunctionType;
import net.opengis.filter.v_2_0.LiteralType;
import net.opengis.filter.v_2_0.PropertyIsBetweenType;
import net.opengis.filter.v_2_0.PropertyIsLikeType;
import net.opengis.filter.v_2_0.PropertyIsNilType;
import net.opengis.filter.v_2_0.PropertyIsNullType;
import net.opengis.filter.v_2_0.UnaryLogicOpType;
import org.codice.ddf.catalog.ui.forms.filter.FilterVisitor2;
import org.codice.ddf.catalog.ui.forms.filter.VisitableXmlElement;
import org.codice.ddf.catalog.ui.forms.model.JsonModel.FilterNode;

public class JsonTransformVisitor implements FilterVisitor2 {
  private static final String FORMS_FUNCTION_V1 = "forms.function.1";

  private static final Map<String, String> BINARY_COMPARE_MAPPING =
      ImmutableMap.<String, String>builder()
          .put("PropertyIsEqualTo", "=")
          .put("PropertyIsNotEqualTo", "!=")
          .put("PropertyIsLessThan", "<")
          .put("PropertyIsLessThanOrEqualTo", "<=")
          .put("PropertyIsGreaterThan", ">")
          .put("PropertyIsGreaterThanOrEqualTo", ">=")
          .build();

  private final JsonModelBuilder builder = new JsonModelBuilder();

  public JsonTransformVisitor() {}

  public FilterNode getResult() {
    return builder.getResult();
  }

  @Override
  public void visitFilter(VisitableXmlElement<FilterType> visitable) {
    JAXBElement<FilterType> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    print(localPart);

    FilterType filterType = element.getValue();

    JAXBElement<?> root =
        Stream.of(
                filterType.getComparisonOps(),
                filterType.getLogicOps(),
                filterType.getSpatialOps(),
                filterType.getTemporalOps())
            .filter(Objects::nonNull)
            .findAny()
            .orElse(null);

    if (root != null) {
      makeVisitable(root).accept(this);
      return;
    }

    // Support can be enhanced in the future, but currently these components aren't needed
    handleUnsupported(filterType.getId());
    handleUnsupported(filterType.getExtensionOps());
    // Functions are supported but not as the FIRST element of a document
    handleUnsupported(filterType.getFunction());

    print("No valid start to the filter was discovered, exiting...");
  }

  // Work around for Value References not having an explicit binding
  @Override
  public void visitString(VisitableXmlElement<String> visitable) {
    JAXBElement<String> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    print(localPart);

    builder.setProperty(element.getValue());
    print(element.getValue());
  }

  @Override
  public void visitLiteralType(VisitableXmlElement<LiteralType> visitable) {
    JAXBElement<LiteralType> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    print(localPart);

    List<Serializable> values = element.getValue().getContent();
    if (values == null || values.isEmpty()) {
      print("No values found on literal type");
      return;
    }

    // TODO: Verify assumption - we only support one literal value (schema might say otherwise)
    builder.setValue(values.get(0).toString());
    values.forEach(s -> print(s.toString()));
  }

  @Override
  public void visitFunctionType(VisitableXmlElement<FunctionType> visitable) {
    JAXBElement<FunctionType> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    print(localPart);

    FunctionType functionType = element.getValue();
    String functionName = functionType.getName();

    // TODO: Visiting a Filter structure is common, but this interpretation of functions is not
    if (FORMS_FUNCTION_V1.equals(functionName)) {
      List<Optional<Serializable>> args =
          functionType
              .getExpression()
              .stream()
              .map(JAXBElement::getValue)
              .map(LiteralType.class::cast)
              .flatMap(
                  t ->
                      (t.getContent() == null || t.getContent().isEmpty())
                          ? Stream.of(Optional.<Serializable>empty())
                          : t.getContent().stream().map(Optional::of))
              .collect(Collectors.toList());

      args.forEach(o -> print(o.toString()));

      Function<Serializable, Boolean> boolFunc = s -> Boolean.parseBoolean((String) s);
      builder.setTemplatedValues(
          get(args, 0, String.class),
          get(args, 1, String.class),
          get(args, 2, boolFunc),
          get(args, 3, boolFunc));

    } else {
      throw new RuntimeException("Could not find a valid function name");
    }
  }

  private static <T> T get(List<Optional<Serializable>> args, int i, Class<T> expectedType) {
    return get(args, i, expectedType::cast);
  }

  private static <T> T get(
      List<Optional<Serializable>> args, int i, Function<Serializable, T> transform) {
    return transform.apply(args.get(i).orElse(null));
  }

  @Override
  public void visitBinaryLogicType(VisitableXmlElement<BinaryLogicOpType> visitable) {
    JAXBElement<BinaryLogicOpType> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    print(localPart);

    builder.beginBinaryLogicType(localPart.toUpperCase());
    element.getValue().getOps().forEach(jax -> makeVisitable(jax).accept(this));
    builder.endBinaryLogicType();
  }

  @Override
  public void visitUnaryLogicType(VisitableXmlElement<UnaryLogicOpType> visitable) {
    JAXBElement<UnaryLogicOpType> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    print(localPart);
  }

  @Override
  public void visitBinaryTemporalType(VisitableXmlElement<BinaryTemporalOpType> visitable) {
    JAXBElement<BinaryTemporalOpType> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    print(localPart);
  }

  @Override
  public void visitBinarySpatialType(VisitableXmlElement<BinarySpatialOpType> visitable) {
    JAXBElement<BinarySpatialOpType> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    print(localPart);
  }

  @Override
  public void visitDistanceBufferType(VisitableXmlElement<DistanceBufferType> visitable) {
    JAXBElement<DistanceBufferType> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    print(localPart);
  }

  @Override
  public void visitBoundingBoxType(VisitableXmlElement<BBOXType> visitable) {
    JAXBElement<BBOXType> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    print(localPart);
  }

  @Override
  public void visitBinaryComparisonType(VisitableXmlElement<BinaryComparisonOpType> visitable) {
    JAXBElement<BinaryComparisonOpType> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    print(localPart);

    String operator = BINARY_COMPARE_MAPPING.get(localPart);
    if (operator == null) {
      throw new IllegalArgumentException(
          "Cannot find mapping for binary comparison operator: " + localPart);
    }

    builder.beginBinaryComparisonType(operator);
    element.getValue().getExpression().forEach(jax -> makeVisitable(jax).accept(this));
    builder.endBinaryComparisonType();
  }

  @Override
  public void visitPropertyIsLikeType(VisitableXmlElement<PropertyIsLikeType> visitable) {
    JAXBElement<PropertyIsLikeType> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    print(localPart);
  }

  @Override
  public void visitPropertyIsNullType(VisitableXmlElement<PropertyIsNullType> visitable) {
    JAXBElement<PropertyIsNullType> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    print(localPart);
  }

  @Override
  public void visitPropertyIsNilType(VisitableXmlElement<PropertyIsNilType> visitable) {
    JAXBElement<PropertyIsNilType> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    print(localPart);
  }

  @Override
  public void visitPropertyIsBetweenType(VisitableXmlElement<PropertyIsBetweenType> visitable) {
    JAXBElement<PropertyIsBetweenType> element = visitable.getElement();
    String localPart = element.getName().getLocalPart();
    print(localPart);
  }

  private static void handleUnsupported(Object type) {
    if (type != null) {
      throw new IllegalArgumentException(
          "Encountered filter with unsupported element: " + type.getClass().getName());
    }
  }
}
