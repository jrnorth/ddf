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
package org.codice.ddf.catalog.transform.impl;

import ddf.catalog.content.data.ContentItem;
import ddf.catalog.data.Metacard;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.catalog.transform.TransformResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransformResponseImpl implements TransformResponse {

  private static final Logger LOGGER = LoggerFactory.getLogger(TransformResponseImpl.class);

  private final Metacard parentMetacard;

  private final List<Metacard> derivedMetacards;

  private final List<ContentItem> derivedContentItems;

  public TransformResponseImpl(
      @Nullable Metacard parentMetacard,
      List<Metacard> dervivedMetacards,
      List<ContentItem> derivedContentItems) {
    this.parentMetacard = parentMetacard;
    this.derivedMetacards = dervivedMetacards;
    this.derivedContentItems = derivedContentItems;
  }

  @Override
  public List<Metacard> getDerivedMetacards() {
    return derivedMetacards;
  }

  @Override
  public List<ContentItem> getDerivedContentItems() {
    return derivedContentItems;
  }

  @Override
  public Optional<Metacard> getParentMetacard() {
    return Optional.ofNullable(parentMetacard);
  }

  @Override
  public void close() {
    getDerivedContentItems()
        .forEach(
            contentItem -> {
              try {
                IOUtils.closeQuietly(contentItem.getInputStream());
              } catch (IOException e) {
                LOGGER.debug("Failed to get input stream from content item.", e);
              }
            });
  }
}
