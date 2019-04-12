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
package org.codice.ddf.spatial.ogc.wfs.v110.catalog.source;

import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.filter.FilterDelegateFactory;

public class FilterDelegateFactorySupplier implements Supplier<FilterDelegateFactory> {
  private final List<FilterDelegateFactory> filterDelegateFactories;

  public FilterDelegateFactorySupplier(final List<FilterDelegateFactory> filterDelegateFactories) {
    this.filterDelegateFactories = filterDelegateFactories;
  }

  @Nullable
  @Override
  public FilterDelegateFactory get() {
    return filterDelegateFactories.isEmpty() ? null : filterDelegateFactories.get(0);
  }
}
