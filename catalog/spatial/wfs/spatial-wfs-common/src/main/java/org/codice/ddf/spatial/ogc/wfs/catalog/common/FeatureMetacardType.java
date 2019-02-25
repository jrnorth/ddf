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
package org.codice.ddf.spatial.ogc.wfs.catalog.common;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.impl.types.ContactAttributes;
import ddf.catalog.data.impl.types.CoreAttributes;
import ddf.catalog.data.impl.types.DateTimeAttributes;
import ddf.catalog.data.impl.types.LocationAttributes;
import ddf.catalog.data.impl.types.MediaAttributes;
import ddf.catalog.data.impl.types.ValidationAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexContent;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaContent;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSequenceMember;
import org.apache.ws.commons.schema.XmlSchemaSimpleContent;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentExtension;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentRestriction;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeContent;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeList;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.constants.Constants;
import org.codice.ddf.spatial.ogc.wfs.catalog.MetacardTypeEnhancer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureMetacardType extends MetacardTypeImpl {

  private static final long serialVersionUID = 1L;

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureMetacardType.class);

  private final transient List<String> properties = new ArrayList<>();

  private final transient List<String> textualProperties = new ArrayList<>();

  private final transient List<String> gmlProperties = new ArrayList<>();

  private final transient List<String> temporalProperties = new ArrayList<>();

  private final transient QName featureType;

  private final transient String propertyPrefix;

  private final transient List<String> nonQueryableProperties;

  private final transient String gmlNamespace;

  private final XmlSchema schema;

  private static final String EXT_PREFIX = "ext.";

  public static final MetacardTypeEnhancer DEFAULT_METACARD_TYPE_ENHANCER =
      new MetacardTypeEnhancer() {
        @Override
        public String getFeatureName() {
          return "";
        }

        @Override
        public Set<AttributeDescriptor> getAttributeDescriptors() {
          return Collections.emptySet();
        }
      };

  public FeatureMetacardType(
      XmlSchema schema,
      final QName featureType,
      List<String> nonQueryableProperties,
      String gmlNamespace) {
    this(schema, featureType, nonQueryableProperties, gmlNamespace, DEFAULT_METACARD_TYPE_ENHANCER);
  }

  public FeatureMetacardType(
      XmlSchema schema,
      final QName featureType,
      List<String> nonQueryableProperties,
      String gmlNamespace,
      MetacardTypeEnhancer metacardTypeEnhancer) {
    super(featureType.getLocalPart(), (Set<AttributeDescriptor>) null);

    addAllDescriptors();

    this.schema = schema;
    this.featureType = featureType;
    this.nonQueryableProperties = nonQueryableProperties;
    this.propertyPrefix = EXT_PREFIX + getName() + ".";
    this.gmlNamespace = gmlNamespace;
    if (schema != null) {
      processXmlSchema(schema);
    } else {
      throw new IllegalArgumentException(
          "FeatureTypeMetacard cannot be created with a null Schema.");
    }

    Set<String> existingAttributeNames =
        getAttributeDescriptors()
            .stream()
            .map(AttributeDescriptor::getName)
            .collect(Collectors.toSet());

    metacardTypeEnhancer
        .getAttributeDescriptors()
        .stream()
        .filter(
            attributeDescriptor -> !existingAttributeNames.contains(attributeDescriptor.getName()))
        .forEach(this::add);
  }

  /**
   * we don't want to expose these in a query interface ie wfs endpoint, so we need to create new
   * attributes for each and set them to stored = false note: indexed is being used to determine
   * whether or not to query certain wfs fields so it did not seem appropriate to hide those fields
   * from the endpoint schema
   */
  private void addDescriptors(Set<AttributeDescriptor> attrDescriptors) {
    for (AttributeDescriptor descriptor : attrDescriptors) {
      AttributeDescriptorImpl basicAttributeDescriptor = (AttributeDescriptorImpl) descriptor;
      AttributeDescriptor attributeDescriptor =
          new AttributeDescriptorImpl(
              basicAttributeDescriptor.getName(),
              false,
              false,
              basicAttributeDescriptor.isTokenized(),
              basicAttributeDescriptor.isMultiValued(),
              basicAttributeDescriptor.getType());
      add(attributeDescriptor);
    }
  }

  private void addAllDescriptors() {
    addDescriptors(new CoreAttributes().getAttributeDescriptors());
    addDescriptors(new ContactAttributes().getAttributeDescriptors());
    addDescriptors(new LocationAttributes().getAttributeDescriptors());
    addDescriptors(new MediaAttributes().getAttributeDescriptors());
    addDescriptors(new DateTimeAttributes().getAttributeDescriptors());
    addDescriptors(new ValidationAttributes().getAttributeDescriptors());
    addDescriptors(new MediaAttributes().getAttributeDescriptors());
  }

  @Override
  public String getName() {
    return featureType.getLocalPart();
  }

  public String getPrefix() {
    return featureType.getPrefix();
  }

  public String getNamespaceURI() {
    return featureType.getNamespaceURI();
  }

  public QName getFeatureType() {
    return featureType;
  }

  private void processXmlSchema(XmlSchema schema) {
    Map<QName, XmlSchemaElement> elements = schema.getElements();

    for (final XmlSchemaElement element : elements.values()) {
      XmlSchemaType schemaType = element.getSchemaType();
      if (schemaType instanceof XmlSchemaComplexType) {
        processComplexType(element);
      } else if (schemaType instanceof XmlSchemaSimpleType) {
        processSimpleType(element);
      }
    }
  }

  private void processComplexType(XmlSchemaElement xmlSchemaElement) {
    if (!processGmlType(xmlSchemaElement)) {
      XmlSchemaType schemaType = xmlSchemaElement.getSchemaType();
      if (schemaType instanceof XmlSchemaComplexType) {
        XmlSchemaComplexType complexType = (XmlSchemaComplexType) schemaType;
        if (complexType.getParticle() != null) {
          processXmlSchemaParticle(complexType.getParticle());
        } else if (complexType.getContentModel() instanceof XmlSchemaComplexContent) {
          XmlSchemaContent content = complexType.getContentModel().getContent();
          if (content instanceof XmlSchemaComplexContentExtension) {
            XmlSchemaComplexContentExtension extension = (XmlSchemaComplexContentExtension) content;
            processXmlSchemaParticle(extension.getParticle());
          }
        } else if (complexType.getContentModel() instanceof XmlSchemaSimpleContent) {
          XmlSchemaContent content = complexType.getContentModel().getContent();
          if (content instanceof XmlSchemaSimpleContentExtension) {
            processSimpleContentExtension(
                xmlSchemaElement, (XmlSchemaSimpleContentExtension) content);
          } else if (content instanceof XmlSchemaSimpleContentRestriction) {
            processSimpleContentRestriction(
                xmlSchemaElement, (XmlSchemaSimpleContentRestriction) content);
          }
        }
      }
    }
  }

  private void processSimpleContentExtension(
      final XmlSchemaElement parentElement, final XmlSchemaSimpleContentExtension extension) {
    final QName baseTypeName;
    final QName extensionName = extension.getBaseTypeName();
    final XmlSchemaType schemaType = schema.getTypeByName(extensionName);
    if (schemaType != null) {
      baseTypeName = getBaseTypeQName((XmlSchemaSimpleType) schemaType);
    } else {
      baseTypeName = extensionName;
    }

    mapSchemaElement(parentElement, baseTypeName);
  }

  private void processSimpleContentRestriction(
      final XmlSchemaElement parentElement, final XmlSchemaSimpleContentRestriction restriction) {
    final QName baseTypeName = getBaseTypeQName(restriction.getBaseType());
    mapSchemaElement(parentElement, baseTypeName);
  }

  private void processXmlSchemaParticle(XmlSchemaParticle particle) {
    if (particle instanceof XmlSchemaSequence) {
      XmlSchemaSequence schemaSequence = (XmlSchemaSequence) particle;
      for (final XmlSchemaSequenceMember element : schemaSequence.getItems()) {
        if (element instanceof XmlSchemaElement) {
          XmlSchemaElement innerElement = ((XmlSchemaElement) element);
          XmlSchemaType innerEleType = innerElement.getSchemaType();
          if (innerEleType instanceof XmlSchemaComplexType) {
            processComplexType(innerElement);
          } else if (innerEleType instanceof XmlSchemaSimpleType) {
            processSimpleType(innerElement);
          } else if (innerEleType == null) {
            // Check if this is the GML location Property
            processGmlType(innerElement);
          }
        }
      }
    }
  }

  private void processSimpleType(final XmlSchemaElement xmlSchemaElement) {
    final QName baseTypeName =
        getBaseTypeQName((XmlSchemaSimpleType) xmlSchemaElement.getSchemaType());
    mapSchemaElement(xmlSchemaElement, baseTypeName);
  }

  private void mapSchemaElement(final XmlSchemaElement element, final QName elementBaseTypeName) {
    final String elementName = element.getName();
    final AttributeType<?> attributeType = toBasicType(elementBaseTypeName);

    if (attributeType != null) {
      final boolean multivalued = element.getMaxOccurs() > 1;
      add(
          new FeatureAttributeDescriptor(
              propertyPrefix + elementName,
              elementName,
              isQueryable(elementName) /* indexed */,
              true /* stored */,
              false /* tokenized */,
              multivalued,
              attributeType));
    }
    if (Constants.XSD_STRING.equals(elementBaseTypeName)) {
      textualProperties.add(propertyPrefix + elementName);
    }

    properties.add(propertyPrefix + elementName);
  }

  private QName getBaseTypeQName(final XmlSchemaSimpleType simpleType) {
    final boolean isBasicType = toBasicType(simpleType.getQName()) != null;
    if (isBasicType) {
      return simpleType.getQName();
    }

    final XmlSchemaSimpleTypeContent content = simpleType.getContent();
    if (content instanceof XmlSchemaSimpleTypeList) {
      final XmlSchemaSimpleTypeList simpleListType = (XmlSchemaSimpleTypeList) content;
      return simpleListType.getItemType() == null
          ? simpleListType.getItemTypeName()
          : getBaseTypeQName(simpleListType.getItemType());
    } else if (content instanceof XmlSchemaSimpleTypeRestriction) {
      final XmlSchemaSimpleTypeRestriction simpleRestrictionType =
          (XmlSchemaSimpleTypeRestriction) content;
      return simpleRestrictionType.getBaseType() == null
          ? simpleRestrictionType.getBaseTypeName()
          : getBaseTypeQName(simpleRestrictionType.getBaseType());
    } else {
      // TODO how to handle a union?
      return simpleType.getQName();
    }
  }

  private Boolean processGmlType(XmlSchemaElement xmlSchemaElement) {
    QName qName = xmlSchemaElement.getSchemaTypeName();
    String name = xmlSchemaElement.getName();

    if (qName != null
        && StringUtils.isNotEmpty(name)
        && qName.getNamespaceURI().equals(gmlNamespace)
        && (qName.getLocalPart().equals("TimeInstantType")
            || qName.getLocalPart().equals("TimePeriodType"))) {
      LOGGER.debug("Adding temporal property: {}{}", propertyPrefix, name);
      temporalProperties.add(propertyPrefix + name);

      boolean multiValued = xmlSchemaElement.getMaxOccurs() > 1;
      add(
          new FeatureAttributeDescriptor(
              propertyPrefix + name,
              name,
              isQueryable(name) /* indexed */,
              true /* stored */,
              false /* tokenized */,
              multiValued,
              BasicTypes.DATE_TYPE));

      properties.add(propertyPrefix + name);

      return true;
    }

    if (qName != null
        && qName.getNamespaceURI().equals(gmlNamespace)
        && StringUtils.isNotEmpty(name)) {
      LOGGER.debug("Adding geo property: {}{}", propertyPrefix, name);
      gmlProperties.add(propertyPrefix + name);

      boolean multiValued = xmlSchemaElement.getMaxOccurs() > 1;
      add(
          new FeatureAttributeDescriptor(
              propertyPrefix + name,
              name,
              isQueryable(name) /* indexed */,
              true /* stored */,
              false /* tokenized */,
              multiValued,
              BasicTypes.GEO_TYPE));

      properties.add(propertyPrefix + name);

      return true;
    }

    return false;
  }

  private AttributeType<?> toBasicType(QName qName) {

    if (Constants.XSD_STRING.equals(qName)) {
      return BasicTypes.STRING_TYPE;
    }
    if (Constants.XSD_DATETIME.equals(qName) || Constants.XSD_DATE.equals(qName)) {
      return BasicTypes.DATE_TYPE;
    }
    if (Constants.XSD_BOOLEAN.equals(qName)) {
      return BasicTypes.BOOLEAN_TYPE;
    }
    if (Constants.XSD_DOUBLE.equals(qName)) {
      return BasicTypes.DOUBLE_TYPE;
    }
    if (Constants.XSD_FLOAT.equals(qName)) {
      return BasicTypes.FLOAT_TYPE;
    }
    if (Constants.XSD_INT.equals(qName)) {
      return BasicTypes.INTEGER_TYPE;
    }
    if (Constants.XSD_LONG.equals(qName)) {
      return BasicTypes.LONG_TYPE;
    }
    if (Constants.XSD_SHORT.equals(qName)) {
      return BasicTypes.SHORT_TYPE;
    }

    // these types are unbounded and unsafe to map to any BasicTypes number values.
    // Potentially the catalog should support a BigInteger type for these types to map to
    if (Constants.XSD_INTEGER.equals(qName)
        || Constants.XSD_POSITIVEINTEGER.equals(qName)
        || Constants.XSD_NEGATIVEINTEGER.equals(qName)
        || Constants.XSD_NONPOSITIVEINTEGER.equals(qName)
        || Constants.XSD_NONNEGATIVEINTEGER.equals(qName)) {
      return BasicTypes.STRING_TYPE;
    }
    return null;
  }

  private boolean isQueryable(String propertyName) {
    return !nonQueryableProperties.contains(propertyName);
  }

  public List<String> getTextualProperties() {
    return textualProperties;
  }

  public List<String> getGmlProperties() {
    return gmlProperties;
  }

  public List<String> getProperties() {
    return properties;
  }

  public List<String> getTemporalProperties() {
    return temporalProperties;
  }
}
