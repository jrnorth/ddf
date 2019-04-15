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
package org.codice.ddf.spatial.ogc.wfs.catalog;

import ddf.catalog.data.MetacardType;
import java.util.List;
import javax.xml.namespace.QName;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library.</b>
 *
 * <p>A {@link MetacardType} that represents a WFS FeatureType. An instance of this metacard type
 * corresponds to a single WFS FeatureType and will contain attributes of the form
 * 'ext.FeatureTypeName.FeaturePropertyName'. 'FeatureTypeName' is the local part of the {@link
 * QName} returned by {@link #getFeatureType()}, and each 'FeaturePropertyName' is the name of one
 * of the properties of the feature type.
 */
public interface FeatureMetacardType extends MetacardType {
  /**
   * Returns a {@link QName} that describes the WFS feature type.
   *
   * @return a {@link QName} that describes the WFS feature type.
   */
  QName getFeatureType();

  /**
   * Returns the properties of the feature type whose base type is the XSD 'string' data type in the
   * 'ext.FeatureTypeName.FeaturePropertyName' format as described in the class-level javadoc.
   *
   * @return the properties of the feature type whose base type is the XSD 'string' data type in the
   *     'ext.FeatureTypeName.FeaturePropertyName' format as described in the class-level javadoc.
   */
  List<String> getTextualProperties();

  /**
   * Returns the properties of the feature type whose type is any GML type that is not
   * TimeInstantType or TimePeriodType in the 'ext.FeatureTypeName.FeaturePropertyName' format as
   * described in the class-level javadoc. See {@link #getTemporalProperties()}.
   *
   * @return the properties of the feature type whose type is any GML type that is not
   *     TimeInstantType or TimePeriodType in the 'ext.FeatureTypeName.FeaturePropertyName' format
   *     as described in the class-level javadoc.
   */
  List<String> getGmlProperties();

  /**
   * Returns the properties of the feature type whose type is the GML TimePeriodType or
   * TimeInstantType in the 'ext.FeatureTypeName.FeaturePropertyName' format as described in the
   * class-level javadoc. See {@link #getGmlProperties()}.
   *
   * @return the properties of the feature type whose type is the GML TimePeriodType or
   *     TimeInstantType in the 'ext.FeatureTypeName.FeaturePropertyName' format as described in the
   *     class-level javadoc.
   */
  List<String> getTemporalProperties();

  /**
   * Returns all the properties of the feature type in the 'ext.FeatureTypeName.FeaturePropertyName'
   * format as described in the class-level javadoc.
   *
   * @return all the properties of the feature type in the 'ext.FeatureTypeName.FeaturePropertyName'
   *     format as described in the class-level javadoc.
   */
  List<String> getProperties();
}
