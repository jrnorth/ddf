package org.codice.ddf.catalog.ui.scheduling.subscribers;

public class QueryDeliveryParameter {
  public static final String NAME_STR = "name";

  public static final String TYPE_STR = "type";

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
