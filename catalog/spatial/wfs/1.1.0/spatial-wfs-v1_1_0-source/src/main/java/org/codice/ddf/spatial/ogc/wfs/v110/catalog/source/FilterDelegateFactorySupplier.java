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
import org.codice.ddf.spatial.ogc.wfs.catalog.FeatureMetacardType;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.filter.FilterDelegateFactory;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.filter.WfsFilterDelegate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterDelegateFactorySupplier {
  private static final Logger LOGGER = LoggerFactory.getLogger(FilterDelegateFactorySupplier.class);

  private final BundleContext bundleContext;

  private final List<ServiceReference<FilterDelegateFactory>> filterDelegateFactories;

  public FilterDelegateFactorySupplier(
      final BundleContext bundleContext,
      final List<ServiceReference<FilterDelegateFactory>> filterDelegateFactories) {
    this.bundleContext = bundleContext;
    this.filterDelegateFactories = filterDelegateFactories;
  }

  public FilterDelegateFactory get(final String id) {
    LOGGER.trace("Finding the FilterDelegateFactory with ID '{}'.", id);
    return filterDelegateFactories
        .stream()
        .filter(factory -> factory.getProperty("id").equals(id))
        .findFirst()
        .map(bundleContext::getService)
        .orElseGet(
            () -> {
              LOGGER.debug(
                  "Could not find the FilterDelegateFactory with ID '{}'. Returning the default factory.",
                  id);
              return new DefaultFilterDelegateFactory();
            });
  }

  private class DefaultFilterDelegateFactory implements FilterDelegateFactory {
    @Override
    public WfsFilterDelegate createFilterDelegate(
        final FeatureMetacardType featureMetacardType, final List<String> spatialOperators) {
      return new WfsFilterDelegateImpl(featureMetacardType, spatialOperators);
    }
  }
}
