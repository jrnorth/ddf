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
