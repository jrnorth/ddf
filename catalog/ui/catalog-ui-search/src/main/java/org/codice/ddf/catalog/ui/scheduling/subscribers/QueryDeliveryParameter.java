package org.codice.ddf.catalog.ui.scheduling.subscribers;

public class QueryDeliveryParameter {
  private String name;

  private QueryDeliveryDatumType type;

  public QueryDeliveryParameter(String name, QueryDeliveryDatumType type) {
    this.name = name;
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public QueryDeliveryDatumType getType() {
    return type;
  }

  public void setType(QueryDeliveryDatumType type) {
    this.type = type;
  }
}
