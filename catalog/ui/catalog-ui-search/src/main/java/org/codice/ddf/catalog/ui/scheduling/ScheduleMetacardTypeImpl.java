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
package org.codice.ddf.catalog.ui.scheduling;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;
import java.util.HashSet;
import java.util.Set;

public class ScheduleMetacardTypeImpl extends MetacardTypeImpl {
  public static final String SCHEDULE_TAG = "schedule";

  public static final String SCHEDULE_TYPE_NAME = "metacard.schedule";

  // TODO: change `userId` to `username` when it can be cooridnated with the frontend
  public static final String SCHEDULE_USERNAME = "userId";

  public static final String IS_SCHEDULED = "isScheduled";

  public static final String SCHEDULE_AMOUNT = "scheduleAmount";

  public static final String SCHEDULE_UNIT = "scheduleUnit";

  public static final String SCHEDULE_START = "scheduleStart";

  public static final String SCHEDULE_END = "scheduleEnd";

  public static final String SCHEDULE_DELIVERY_IDS = "deliveryIds";

  private static final Set<AttributeDescriptor> SCHEDULE_DESCRIPTORS;

  static {
    SCHEDULE_DESCRIPTORS = new HashSet<>();

    SCHEDULE_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            SCHEDULE_USERNAME,
            false /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));

    SCHEDULE_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            IS_SCHEDULED,
            false /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.BOOLEAN_TYPE));

    SCHEDULE_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            SCHEDULE_AMOUNT,
            false /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.INTEGER_TYPE));

    SCHEDULE_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            SCHEDULE_UNIT,
            false /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));

    SCHEDULE_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            SCHEDULE_START,
            false /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));

    SCHEDULE_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            SCHEDULE_END,
            false /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));

    SCHEDULE_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            SCHEDULE_DELIVERY_IDS,
            false /* indexed */,
            true /* stored */,
            false /* tokenized */,
            true /* multivalued */,
            BasicTypes.STRING_TYPE));
  }

  public ScheduleMetacardTypeImpl() {
    this(SCHEDULE_TYPE_NAME, SCHEDULE_DESCRIPTORS);
  }

  public ScheduleMetacardTypeImpl(String name, Set<AttributeDescriptor> descriptors) {
    super(name, descriptors);
  }
}
