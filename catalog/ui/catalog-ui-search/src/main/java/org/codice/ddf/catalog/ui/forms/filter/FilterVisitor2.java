package org.codice.ddf.catalog.ui.forms.filter;

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

public interface FilterVisitor2 {

  void visitFilter(VisitableXmlElement<FilterType> visitable);

  // Work around for Value References not having an explicit binding
  void visitString(VisitableXmlElement<String> visitable);

  void visitLiteralType(VisitableXmlElement<LiteralType> visitable);

  void visitFunctionType(VisitableXmlElement<FunctionType> visitable);

  void visitBinaryLogicType(VisitableXmlElement<BinaryLogicOpType> visitable);

  void visitUnaryLogicType(VisitableXmlElement<UnaryLogicOpType> visitable);

  void visitBinaryTemporalType(VisitableXmlElement<BinaryTemporalOpType> visitable);

  void visitBinarySpatialType(VisitableXmlElement<BinarySpatialOpType> visitable);

  void visitDistanceBufferType(VisitableXmlElement<DistanceBufferType> visitable);

  void visitBoundingBoxType(VisitableXmlElement<BBOXType> visitable);

  void visitBinaryComparisonType(VisitableXmlElement<BinaryComparisonOpType> visitable);

  void visitPropertyIsLikeType(VisitableXmlElement<PropertyIsLikeType> visitable);

  void visitPropertyIsNullType(VisitableXmlElement<PropertyIsNullType> visitable);

  void visitPropertyIsNilType(VisitableXmlElement<PropertyIsNilType> visitable);

  void visitPropertyIsBetweenType(VisitableXmlElement<PropertyIsBetweenType> visitable);
}
