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
