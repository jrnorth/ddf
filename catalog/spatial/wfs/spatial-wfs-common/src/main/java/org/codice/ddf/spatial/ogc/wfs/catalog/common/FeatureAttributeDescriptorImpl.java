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

import ddf.catalog.data.AttributeType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import org.codice.ddf.spatial.ogc.wfs.catalog.FeatureAttributeDescriptor;

/**
 * Extension of the {@link AttributeDescriptorImpl} to allow for mapping of an actual property name,
 * which may collide with a "reserved" attributeDescriptor from {@link
 * ddf.catalog.data.impl.MetacardImpl.BASIC_METACARD}, to a name that can be used without
 * over-writing existing attributeDescriptors.
 */
public class FeatureAttributeDescriptorImpl extends AttributeDescriptorImpl
    implements FeatureAttributeDescriptor {

  private static final long serialVersionUID = 1L;

  private final String propertyName;

  public FeatureAttributeDescriptorImpl(
      String name,
      String propertyName,
      boolean indexed,
      boolean stored,
      boolean tokenized,
      boolean multivalued,
      AttributeType<?> type) {
    super(name, indexed, stored, tokenized, multivalued, type);

    this.propertyName = propertyName;
  }

  @Override
  public String getPropertyName() {
    return propertyName;
  }
}
