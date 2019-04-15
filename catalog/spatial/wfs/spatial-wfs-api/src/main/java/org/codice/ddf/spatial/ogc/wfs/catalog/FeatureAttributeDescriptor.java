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

import ddf.catalog.data.AttributeDescriptor;

/**
 * <b>This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library.</b>
 *
 * <p>An {@link AttributeDescriptor} that represents a property of a WFS FeatureType. An instance of
 * this attribute descriptor corresponds to a single WFS feature property. {@link #getName()}
 * returns 'ext.FeatureTypeName.FeaturePropertyName' where 'FeatureTypeName' is the name of the WFS
 * feature and 'FeaturePropertyName' is the name of the feature property represented by this
 * attribute descriptor, which is the name returned by {@link #getPropertyName()}. See {@link
 * FeatureMetacardType} for more details.
 */
public interface FeatureAttributeDescriptor extends AttributeDescriptor {
  /**
   * Returns the name of the WFS feature property.
   *
   * @return the name of the WFS feature property.
   */
  String getPropertyName();
}
