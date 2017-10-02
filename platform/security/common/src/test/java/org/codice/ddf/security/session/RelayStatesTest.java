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
package org.codice.ddf.security.session;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import ddf.security.samlp.impl.RelayStates;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

public class RelayStatesTest {

  @Test
  public void testEncodeAndDecode() throws Exception {
    String location = "test";
    RelayStates<String> relayStates = new RelayStates<>();
    String id = relayStates.encode(location);
    MatcherAssert.assertThat(relayStates.decode(id), equalTo(location));
    assertThat(relayStates.decode(location), relayStates.decode("blah"), nullValue());
  }
}
