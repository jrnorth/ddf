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
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import ddf.util.MapUtils;
import org.codice.ddf.platform.email.SmtpClient;
import org.joda.time.DateTime;

import java.util.Map;
import java.util.regex.Pattern;

public class EmailDeliveryService implements QueryDeliveryService {
  public static final String DELIVERY_TYPE = "email";

  public static final String EMAIL_PARAMETER_KEY = "email";

  public static final Pattern EMAIL_ADDRESS_PATTERN =
          Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

  public static final ImmutableSet<QueryDeliveryParameter> PROPERTIES =
          ImmutableSet.of(new QueryDeliveryParameter(EMAIL_PARAMETER_KEY, QueryDeliveryDatumType.EMAIL));

  private final String senderEmail;

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
  public ImmutableCollection<QueryDeliveryParameter> getRequiredFields() {
    return PROPERTIES;
  }

  @Override
  public Fallible<?> deliver(final Metacard queryMetacard, final QueryResponse queryResults, final Map<String, Object> parameters) {
    return MapUtils.tryGetAndRun(parameters,
            EMAIL_PARAMETER_KEY,
            String.class,
            email -> {
              if (EMAIL_ADDRESS_PATTERN.matcher(email).matches()) {
                return error(
                        "The email address \"%s\" is not a valid email address!", email);
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

              StringBuilder emailBody =
                      new StringBuilder(
                              String.format("Here are the results for query %s as of %s:", queryMetacard.getTitle(), DateTime.now()));
              for (Result queryResult : queryResults.getResults()) {
                emailBody.append("\n\n");

                final Metacard metacard = queryResult.getMetacard();
                for (AttributeDescriptor attributeDescriptor :
                        metacard.getMetacardType().getAttributeDescriptors()) {
                  final String key = attributeDescriptor.getName();

                  emailBody.append(String.format("\n%s: %s", key, metacard));
                }
              }

              final Session smtpSession = smtpClient.createSession();
              final MimeMessage message = new MimeMessage(smtpSession);

              try {
                message.setFrom(senderAddress);
                message.addRecipient(Message.RecipientType.TO, destinationAddress);
                message.setSubject(String.format("Scheduled query results for \"%s\"", queryMetacard.getTitle()));
                message.setText(emailBody.toString());
              } catch (MessagingException exception) {
                return error(
                        "There was a problem assembling an email message for scheduled query results: %s",
                        exception.getMessage());
              }

              smtpClient.send(message);

              return success();
            });
  }
}
