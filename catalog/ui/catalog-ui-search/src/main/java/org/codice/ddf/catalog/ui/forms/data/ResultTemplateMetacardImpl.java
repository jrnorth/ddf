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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ResultTemplateMetacardImpl extends AbstractShareableMetacardImpl {
  public ResultTemplateMetacardImpl(String title, String description) {
    super(new FormTypes.Result());
    setAttribute(CoreAttributes.TITLE, title);
    setAttribute(CoreAttributes.DESCRIPTION, description);
    setTags(Collections.singleton(FormAttributes.Result.TAG));
  }

  public ResultTemplateMetacardImpl(String title, String description, String id) {
    this(title, description);
    setId(id);
  }

  public ResultTemplateMetacardImpl(Metacard metacard) {
    super(metacard);
    //    if (metacard == null) {
    //      throw new IllegalArgumentException("Metacard cannot be null");
    //    }
    //    if (!isResultTemplateMetacard(metacard)) {
    //      throw new IllegalArgumentException(
    //          format(
    //              "Wrapped metacard must be a result template metacard, but was: %n%s %nwith tags:
    // %n%s",
    //              metacard.getClass().getName(), metacard.getTags()));
    //    }
  }

  /**
   * Check if a given metacard is a result template metacard by checking the tags metacard
   * attribute.
   *
   * @param metacard the metacard to check.
   * @return true if the provided metacard is a result template metacard, false otherwise.
   */
  public static boolean isResultTemplateMetacard(Metacard metacard) {
    return metacard != null
        && metacard.getTags().stream().anyMatch(FormAttributes.Result.TAG::equals);
  }

  public Set<String> getResultDescriptors() {
    return new HashSet<>(getValues(FormAttributes.Result.DETAIL_LEVEL));
  }

  public ResultTemplateMetacardImpl setResultDescriptors(Set<String> resultDescriptors) {
    setAttribute(FormAttributes.Result.DETAIL_LEVEL, new ArrayList<>(resultDescriptors));
    return this;
  }
}
