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

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.initMocks;

import org.codice.ddf.spatial.ogc.wfs.v110.catalog.filter.FilterDelegateFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class FilterDelegateFactorySupplierTest {
  @Mock private BundleContext bundleContext;

  @Mock private ServiceReference<FilterDelegateFactory> mockServiceReference1;

  @Mock private FilterDelegateFactory mockFactory1;

  @Mock private ServiceReference<FilterDelegateFactory> mockServiceReference2;

  @Mock private FilterDelegateFactory mockFactory2;

  private FilterDelegateFactorySupplier supplier;

  @Before
  public void setup() {
    initMocks(this);

    doReturn("1").when(mockServiceReference1).getProperty("id");
    doReturn(mockFactory1).when(bundleContext).getService(mockServiceReference1);

    doReturn("2").when(mockServiceReference2).getProperty("id");
    doReturn(mockFactory2).when(bundleContext).getService(mockServiceReference2);

    supplier =
        new FilterDelegateFactorySupplier(
            bundleContext, asList(mockServiceReference1, mockServiceReference2));
  }

  @Test
  public void returnsDefaultFactoryWhenNoFactoryMatchesId() {
    final FilterDelegateFactory factory = supplier.get("3");
    assertThat(factory, is(notNullValue()));
    assertThat(factory, not(is(mockFactory1)));
    assertThat(factory, not(is(mockFactory2)));
  }

  @Test
  public void returnsFilterDelegateFactoryMatchingId() {
    assertThat(supplier.get("1"), is(mockFactory1));
    assertThat(supplier.get("2"), is(mockFactory2));
  }
}
