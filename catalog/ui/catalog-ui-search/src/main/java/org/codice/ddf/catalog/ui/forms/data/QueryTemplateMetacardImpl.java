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
package org.codice.ddf.catalog.ui.forms.data;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.types.CoreAttributes;
import java.util.Collections;
import java.util.List;

public class QueryTemplateMetacardImpl extends AbstractShareableMetacardImpl {
  public QueryTemplateMetacardImpl(String title, String description) {
    super(new FormTypes.Query());
    setAttribute(CoreAttributes.TITLE, title);
    setAttribute(CoreAttributes.DESCRIPTION, description);
    setTags(Collections.singleton(FormAttributes.Query.TAG));
  }

  public QueryTemplateMetacardImpl(String title, String description, String id) {
    this(title, description);
    setId(id);
  }

  public QueryTemplateMetacardImpl(Metacard metacard) {
    super(metacard);
  }

  /**
   * Check if a given metacard is a query template metacard by checking the tags metacard attribute.
   *
   * @param metacard the metacard to check.
   * @return true if the provided metacard is a query template metacard, false otherwise.
   */
  public static boolean isQueryTemplateMetacard(Metacard metacard) {
    return metacard != null
        && metacard.getTags().stream().anyMatch(FormAttributes.Query.TAG::equals);
  }

  public String getFormsFilter() {
    List<String> values = getValues(FormAttributes.Query.FORMS_FILTER);
    if (!values.isEmpty()) {
      return values.get(0);
    }
    return null;
  }

  public QueryTemplateMetacardImpl setFormsFilter(String filterXml) {
    setAttribute(FormAttributes.Query.FORMS_FILTER, filterXml);
    return this;
  }
}
