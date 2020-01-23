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
package org.codice.grammarparser.endpoint;

import ddf.catalog.CatalogFramework;
import ddf.catalog.filter.FilterBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.codice.grammarparser.Dates;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class SearchEndpoint {
  private static final Logger LOGGER = LoggerFactory.getLogger(SearchEndpoint.class);

  private final BundleContext bundleContext;

  private final CatalogFramework catalogFramework;

  private final FilterBuilder filterBuilder;

  public SearchEndpoint(
      final BundleContext bundleContext,
      final CatalogFramework catalogFramework,
      final FilterBuilder filterBuilder) {
    LOGGER.warn("SearchEndpoint constructor");
    this.bundleContext = bundleContext;
    this.catalogFramework = catalogFramework;
    this.filterBuilder = filterBuilder;
    Dates.setFilterBuilder(filterBuilder);
  }

  @GET
  @Path("/index")
  public Response indexPage(@Context UriInfo uriInfo, @Context HttpServletRequest httpRequest) {
    LOGGER.error("foobar");
    final URL indexHtmlUrl = bundleContext.getBundle().getEntry("/index.html");
    try (final InputStream entityStream = indexHtmlUrl.openStream()) {
      return Response.ok(entityStream, "text/html").build();
    } catch (IOException e) {
      LOGGER.warn("Error serving index.html", e);
      return Response.serverError().build();
    }
  }
}
