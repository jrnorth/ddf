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
package org.codice.ddf.catalog.ui.scheduling.subscribers;

import static ddf.util.Fallible.error;
import static ddf.util.Fallible.success;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.QueryResponse;
import ddf.util.Fallible;
import ddf.util.MapUtils;
import java.util.Map;
import java.util.regex.Pattern;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.codice.ddf.platform.email.SmtpClient;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class EmailDeliveryService implements QueryDeliveryService {
  public static final String DELIVERY_TYPE = "email";

  public static final String DELIVERY_TYPE_DISPLAY_NAME = "Email";

  public static final String EMAIL_PARAMETER_KEY = "email";

  public static final Pattern EMAIL_ADDRESS_PATTERN =
      Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

  public static final DateTimeFormatter EMAIL_DATE_TIME_FORMATTER =
      DateTimeFormat.forPattern("MM/dd/yyyy HH:mm");

  public static final ImmutableSet<QueryDeliveryParameter> PROPERTIES =
      ImmutableSet.of(
          new QueryDeliveryParameter(EMAIL_PARAMETER_KEY, QueryDeliveryDatumType.EMAIL));

  private String senderEmail;

  private final SmtpClient smtpClient;

  public EmailDeliveryService(String senderEmail, SmtpClient smtpClient) {
    this.senderEmail = senderEmail;
    this.smtpClient = smtpClient;
  }

  @Override
  public String getDeliveryType() {
    return DELIVERY_TYPE;
  }

  @Override
  public String getDisplayName() {
    return DELIVERY_TYPE_DISPLAY_NAME;
  }

  @Override
  public ImmutableCollection<QueryDeliveryParameter> getRequiredFields() {
    return PROPERTIES;
  }

  @Override
  public Fallible<?> deliver(
      final Map<String, Object> queryMetacardData,
      final QueryResponse queryResults,
      String username,
      String deliveryID,
      final Map<String, Object> parameters) {
    return MapUtils.tryGetAndRun(
        parameters,
        EMAIL_PARAMETER_KEY,
        String.class,
        email -> {
          if (!EMAIL_ADDRESS_PATTERN.matcher(email).matches()) {
            return error("The email address \"%s\" is not a valid email address!", email);
          }

          final InternetAddress senderAddress;
          try {
            senderAddress = new InternetAddress(senderEmail);
          } catch (AddressException exception) {
            return error(
                "There was a problem preparing the email sender address to send query results : %s",
                exception.getMessage());
          }

          final InternetAddress destinationAddress;
          try {
            destinationAddress = new InternetAddress(email);
          } catch (AddressException exception) {
            return error(
                "There was a problem preparing the email destination address to send query results : %s",
                exception.getMessage());
          }

          return MapUtils.tryGetAndRun(
              queryMetacardData,
              Metacard.TITLE,
              String.class,
              queryMetacardTitle -> {
                StringBuilder emailBody =
                    new StringBuilder(
                        String.format(
                            "Here are the results for query %s as of %s:",
                            queryMetacardTitle, EMAIL_DATE_TIME_FORMATTER.print(DateTime.now())));
                for (Result queryResult : queryResults.getResults()) {
                  emailBody.append("\n\n");

                  final Metacard metacard = queryResult.getMetacard();
                  for (AttributeDescriptor attributeDescriptor :
                      metacard.getMetacardType().getAttributeDescriptors()) {
                    final String key = attributeDescriptor.getName();

                    if (metacard.getAttribute(key) == null) {
                      continue;
                    }

                    emailBody.append(
                        String.format("\n%s: %s", key, metacard.getAttribute(key).getValue()));
                  }
                }

                final Session smtpSession = smtpClient.createSession();
                final MimeMessage message = new MimeMessage(smtpSession);

                try {
                  message.setFrom(senderAddress);
                  message.addRecipient(Message.RecipientType.TO, destinationAddress);
                  message.setSubject(
                      String.format("Scheduled query results for \"%s\"", queryMetacardTitle));
                  message.setText(emailBody.toString());
                } catch (MessagingException exception) {
                  return error(
                      "There was a problem assembling an email message for scheduled query results: %s",
                      exception.getMessage());
                }

                smtpClient.send(message);

                return success();
              });
        });
  }

  @SuppressWarnings("unused")
  public void setSenderEmail(String senderEmail) {
    this.senderEmail = senderEmail;
  }
}
