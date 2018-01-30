package org.codice.ddf.catalog.ui.scheduling.subscribers;

import com.google.common.collect.ImmutableCollection;
import ddf.catalog.operation.QueryResponse;
import ddf.util.Fallible;
import java.util.Map;

public interface QueryDeliveryService {
  String SUBSCRIBER_TYPE_KEY = "deliveryType";

  String DISPLAY_NAME_KEY = "displayName";

  Fallible<?> deliver(
      Map<String, Object> queryMetacardData,
      QueryResponse queryResults,
      Map<String, Object> parameters);

  /** A unique identifier for a given specific implementation of QueryDeliveryService */
  String getDeliveryType();

  /** The name of the service to be displayed in the UI dropdown of available services */
  String getDisplayName();

  /** A collection of QueryDeliveryParameters that encompass the "required fields" that */
  ImmutableCollection<QueryDeliveryParameter> getRequiredFields();
}
