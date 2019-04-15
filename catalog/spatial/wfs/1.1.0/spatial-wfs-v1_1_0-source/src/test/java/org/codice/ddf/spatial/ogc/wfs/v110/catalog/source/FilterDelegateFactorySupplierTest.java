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

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.filter.FilterDelegateFactory;
import org.junit.Test;

public class FilterDelegateFactorySupplierTest {
  @Test
  public void returnsNullWhenNoFilterDelegateFactoriesAvailable() {
    final FilterDelegateFactorySupplier supplier = new FilterDelegateFactorySupplier(emptyList());

    assertThat("The supplier should have returned null.", supplier.get(), is(nullValue()));
  }

  @Test
  public void returnsFirstFilterDelegateFactory() {
    final FilterDelegateFactory mockFactory1 = mock(FilterDelegateFactory.class);
    final FilterDelegateFactory mockFactory2 = mock(FilterDelegateFactory.class);
    final List<FilterDelegateFactory> factories = new ArrayList<>();
    factories.add(mockFactory1);
    factories.add(mockFactory2);
    final FilterDelegateFactorySupplier supplier = new FilterDelegateFactorySupplier(factories);

    assertThat("The supplier didn't return the right factory.", supplier.get(), is(mockFactory1));

    factories.remove(0);
    assertThat("The supplier didn't return the right factory.", supplier.get(), is(mockFactory2));
  }
}
