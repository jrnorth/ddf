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
package ddf.security;

import java.util.List;

/**
 * Credential storage interface.
 *
 * <p><b>This interface is for internal use only and should not be implemented by a third party.
 * </b> <i>This code is experimental. While this interface is functional and tested, it may change
 * or be removed in a future version of the library.</i>
 */
public interface Credentials {

  String CREDENTIAL_STORE_TYPE = "credentials";

  /**
   * Retrieves the user credential for the supplied owner & category
   *
   * @param owner
   * @param category
   * @return UserCredential
   */
  UserCredential getUserCredential(String owner, String category);

  /**
   * Stores an user credential for a owner
   *
   * @param owner
   * @param userCredential
   */
  void storeCredential(String owner, UserCredential userCredential);

  /**
   * Removes the credential for a username & category
   *
   * @param owner
   * @param category
   */
  void removeCredential(String owner, String category);

  /**
   * Removes all stored credentials for a given owner
   *
   * @param owner
   */
  void removeAllCredentials(String owner);

  /**
   * Retrieves all categories that a user has a password stored.
   *
   * @param owner
   * @return
   */
  List<String> getAllCategories(String owner);
}
