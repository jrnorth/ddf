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
package org.codice.ddf.catalog.ui.scheduling.subscribers;

import com.google.common.collect.ImmutableCollection;
import ddf.catalog.operation.QueryResponse;
import ddf.util.Fallible;
import java.util.Map;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public interface QueryDeliveryService {
  String DELIVERY_TYPE_KEY = "deliveryType";

  String DISPLAY_NAME_KEY = "displayName";

  Fallible<?> deliver(
      Map<String, Object> queryMetacardData,
      QueryResponse queryResults,
      String username,
      String deliveryID,
      Map<String, Object> parameters);

  /** A unique identifier for a given specific implementation of QueryDeliveryService */
  String getDeliveryType();

  /** The name of the service to be displayed in the UI dropdown of available services */
  String getDisplayName();

  /** A collection of QueryDeliveryParameters that encompass the "required fields" that */
  ImmutableCollection<QueryDeliveryParameter> getRequiredFields();
}
