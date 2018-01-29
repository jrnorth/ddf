package org.codice.ddf.catalog.ui.scheduling.subscribers;

import static ddf.util.Fallible.*;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import ddf.catalog.operation.QueryResponse;
import ddf.util.Fallible;
import ddf.util.MapUtils;
import java.util.Map;
import java.util.regex.Pattern;

public class FtpDeliveryService implements QueryDeliveryService {
  public static final String SUBSCRIBER_TYPE = "FTP";

  public static final String HOSTNAME_PARAMETER_KEY = "hostname";

  public static final String PORT_PARAMETER_KEY = "port";

  public static final String USERNAME_PARAMETER_KEY = "username";

  public static final String PASSWORD_PARAMETER_KEY = "password";

  public static final ImmutableSet<QueryDeliveryParameter> PROPERTIES =
      ImmutableSet.of(
          new QueryDeliveryParameter(HOSTNAME_PARAMETER_KEY, QueryDeliveryDatumType.STRING),
          new QueryDeliveryParameter(PORT_PARAMETER_KEY, QueryDeliveryDatumType.INTEGER),
          new QueryDeliveryParameter(USERNAME_PARAMETER_KEY, QueryDeliveryDatumType.STRING),
          new QueryDeliveryParameter(PASSWORD_PARAMETER_KEY, QueryDeliveryDatumType.STRING));

  public static final Pattern FTP_HOSTNAME_PATTERN =
      Pattern.compile(
          "^([0-9]{1,3}(\\.[0-9]{1,3}){3}|[0-9a-f]{4}:([0-9a-f]{4}){7})$",
          Pattern.CASE_INSENSITIVE);

  @Override
  public String getDeliveryType() {
    return SUBSCRIBER_TYPE;
  }

  @Override
  public ImmutableCollection<QueryDeliveryParameter> getRequiredFields() {
    return PROPERTIES;
  }

  @Override
  public Fallible<?> deliver(
      Map<String, Object> queryMetacardData,
      QueryResponse queryResults,
      Map<String, Object> parameters) {
    return MapUtils.tryGet(parameters, SUBSCRIBER_TYPE_KEY, String.class)
        .tryMap(
            type -> {
              if (type.equals(SUBSCRIBER_TYPE)) {
                return error(
                    "The type of the subscriber data was expected to be \"%s\", not \"%s\"!",
                    SUBSCRIBER_TYPE, type);
              }

              return MapUtils.tryGetAndRun(
                  parameters,
                  HOSTNAME_PARAMETER_KEY,
                  String.class,
                  PORT_PARAMETER_KEY,
                  Integer.class,
                  USERNAME_PARAMETER_KEY,
                  String.class,
                  PASSWORD_PARAMETER_KEY,
                  String.class,
                  (hostname, port, username, password) -> {
                    if (FTP_HOSTNAME_PATTERN.matcher(hostname).matches()) {
                      return error("The FTP address \"%s\" is not a valid FTP address!", hostname);
                    }

                    // TODO: deliver the results via FTP
                    throw new UnsupportedOperationException();
                  });
            });
  }
}
