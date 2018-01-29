package org.codice.ddf.catalog.ui.scheduling.subscribers;

import com.google.common.collect.ImmutableCollection;
import ddf.catalog.operation.QueryResponse;
import ddf.util.Fallible;
import java.util.Map;

public interface QueryDeliveryService {
  String SUBSCRIBER_TYPE_KEY = "type";

  Fallible<?> deliver(
      Map<String, Object> queryMetacardData,
      QueryResponse queryResults,
      Map<String, Object> parameters);

  String getDeliveryType();

  ImmutableCollection<QueryDeliveryParameter> getRequiredFields();
}
