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
import io.javalin.EmbeddedJavalin;
import io.javalin.Javalin;
import io.javalin.core.JavalinServlet;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codice.grammarparser.Dates;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(
  urlPatterns = {GrammarParserApplication.PATH + "/*"},
  name = "GrammarParserApplication"
)
public class GrammarParserApplication extends HttpServlet {
  private static final Logger LOGGER = LoggerFactory.getLogger(GrammarParserApplication.class);

  static final String PATH = "/search/catalog/internal/grammarparser";

  private final BundleContext bundleContext;

  private final CatalogFramework catalogFramework;

  private final FilterBuilder filterBuilder;

  private final JavalinServlet javalinServlet =
      EmbeddedJavalin.create().disableStartupBanner().contextPath(PATH).createServlet();

  public GrammarParserApplication(
      final BundleContext bundleContext,
      final CatalogFramework catalogFramework,
      final FilterBuilder filterBuilder) {
    this.bundleContext = bundleContext;
    this.catalogFramework = catalogFramework;
    this.filterBuilder = filterBuilder;
    Dates.setFilterBuilder(filterBuilder);

    final Javalin app = javalinServlet.getJavalin();
    // probably won't work since this is a bundle?
    app.enableStaticFiles("/");
  }

  @Override
  public void service(final HttpServletRequest request, final HttpServletResponse response) {
    javalinServlet.service(request, response);
  }
}
