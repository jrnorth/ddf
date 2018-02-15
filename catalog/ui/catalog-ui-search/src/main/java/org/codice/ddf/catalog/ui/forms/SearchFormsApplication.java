package org.codice.ddf.catalog.ui.forms;

import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.codice.ddf.catalog.ui.forms.filter.VisitableFilterNode.makeVisitable;
import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;

import com.google.common.collect.ImmutableSet;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.types.CoreAttributes;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.security.service.SecurityServiceException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import net.opengis.filter.v_2_0.FilterType;
import org.apache.commons.io.IOUtils;
import org.boon.json.JsonFactory;
import org.boon.json.JsonParserFactory;
import org.boon.json.JsonSerializerFactory;
import org.boon.json.ObjectMapper;
import org.codice.ddf.catalog.ui.forms.data.FormAttributes;
import org.codice.ddf.catalog.ui.forms.data.QueryTemplateMetacardImpl;
import org.codice.ddf.catalog.ui.forms.data.ResultTemplateMetacardImpl;
import org.codice.ddf.catalog.ui.forms.model.FilterNodeValueSerializer;
import org.codice.ddf.catalog.ui.forms.model.JsonModel.FieldFilter;
import org.codice.ddf.catalog.ui.forms.model.JsonModel.FormTemplate;
import org.codice.ddf.catalog.ui.forms.model.JsonTransformVisitor;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.codice.ddf.security.common.Security;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.servlet.SparkApplication;

public class SearchFormsApplication implements SparkApplication {

  private static final String SAMPLE_1 =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
          + "<fes:Filter xmlns:fes=\"http://www.opengis.net/fes/2.0\"\n"
          + "            xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
          + "            xsi:schemaLocation=\"http://www.opengis.net/fes/2.0 http://schemas.opengis.net/filter/2.0/filterAll.xsd\">\n"
          + "    <fes:And>\n"
          + "        <fes:PropertyIsEqualTo matchAction=\"ANY\" matchCase=\"false\">\n"
          + "            <fes:ValueReference>datatype</fes:ValueReference>\n"
          + "            <fes:Literal>Image</fes:Literal>\n"
          + "        </fes:PropertyIsEqualTo>\n"
          + "        <fes:PropertyIsEqualTo matchAction=\"ANY\" matchCase=\"false\">\n"
          + "            <fes:ValueReference>title</fes:ValueReference>\n"
          + "            <fes:Function name=\"forms.function.1\">\n"
          + "                <fes:Literal/>\n"
          + "                <fes:Literal>my-id-1</fes:Literal>\n"
          + "                <fes:Literal>true</fes:Literal>\n"
          + "                <fes:Literal>false</fes:Literal>\n"
          + "            </fes:Function>\n"
          + "        </fes:PropertyIsEqualTo>\n"
          + "        <!--Maximum bitrate to show up on search results-->\n"
          + "        <fes:PropertyIsLessThanOrEqualTo matchAction=\"ANY\" matchCase=\"false\">\n"
          + "            <fes:ValueReference>media.bit-rate</fes:ValueReference>\n"
          + "            <fes:Function name=\"forms.function.1\">\n"
          + "                <fes:Literal/>\n"
          + "                <fes:Literal>my-id-2</fes:Literal>\n"
          + "                <fes:Literal>true</fes:Literal>\n"
          + "                <fes:Literal>false</fes:Literal>\n"
          + "            </fes:Function>\n"
          + "        </fes:PropertyIsLessThanOrEqualTo>\n"
          + "    </fes:And>\n"
          + "</fes:Filter>";

  private static final String SAMPLE_2 =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
          + "<fes:Filter xmlns:fes=\"http://www.opengis.net/fes/2.0\"\n"
          + "            xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
          + "            xsi:schemaLocation=\"http://www.opengis.net/fes/2.0 http://schemas.opengis.net/filter/2.0/filterAll.xsd\">\n"
          + "    <fes:Or>\n"
          + "        <fes:PropertyIsEqualTo matchAction=\"ANY\" matchCase=\"false\">\n"
          + "            <fes:ValueReference>contact.creator-name</fes:ValueReference>\n"
          + "            <fes:Function name=\"forms.function.1\">\n"
          + "                <fes:Literal/>\n"
          + "                <fes:Literal>my-id-same-source</fes:Literal>\n"
          + "                <fes:Literal>true</fes:Literal>\n"
          + "                <fes:Literal>false</fes:Literal>\n"
          + "            </fes:Function>\n"
          + "        </fes:PropertyIsEqualTo>\n"
          + "        <fes:PropertyIsEqualTo matchAction=\"ANY\" matchCase=\"false\">\n"
          + "            <fes:ValueReference>contact.publisher-name</fes:ValueReference>\n"
          + "            <fes:Function name=\"forms.function.1\">\n"
          + "                <fes:Literal/>\n"
          + "                <fes:Literal>my-id-same-source</fes:Literal>\n"
          + "                <fes:Literal>true</fes:Literal>\n"
          + "                <fes:Literal>false</fes:Literal>\n"
          + "            </fes:Function>\n"
          + "        </fes:PropertyIsEqualTo>\n"
          + "        <fes:PropertyIsEqualTo matchAction=\"ANY\" matchCase=\"false\">\n"
          + "            <fes:ValueReference>contact.point-of-contact-name</fes:ValueReference>\n"
          + "            <fes:Function name=\"forms.function.1\">\n"
          + "                <fes:Literal/>\n"
          + "                <fes:Literal>my-id-same-source</fes:Literal>\n"
          + "                <fes:Literal>true</fes:Literal>\n"
          + "                <fes:Literal>false</fes:Literal>\n"
          + "            </fes:Function>\n"
          + "        </fes:PropertyIsEqualTo>\n"
          + "        <fes:PropertyIsEqualTo matchAction=\"ANY\" matchCase=\"false\">\n"
          + "            <fes:ValueReference>contact.contributor-name</fes:ValueReference>\n"
          + "            <fes:Function name=\"forms.function.1\">\n"
          + "                <fes:Literal/>\n"
          + "                <fes:Literal>my-id-same-source</fes:Literal>\n"
          + "                <fes:Literal>true</fes:Literal>\n"
          + "                <fes:Literal>false</fes:Literal>\n"
          + "            </fes:Function>\n"
          + "        </fes:PropertyIsEqualTo>\n"
          + "    </fes:Or>\n"
          + "</fes:Filter>";

  private static final Logger LOGGER = LoggerFactory.getLogger(SearchFormsApplication.class);

  private static final ObjectMapper MAPPER =
      JsonFactory.create(
          new JsonParserFactory().usePropertyOnly(),
          new JsonSerializerFactory()
              .addPropertySerializer(new FilterNodeValueSerializer())
              .useAnnotations()
              .includeEmpty()
              .includeDefaultValues()
              .setJsonFormatForDates(false));

  private final CatalogFramework catalogFramework;

  private final EndpointUtil util;

  public SearchFormsApplication(CatalogFramework catalogFramework, EndpointUtil util) {
    this.catalogFramework = catalogFramework;
    this.util = util;
  }

  @Override
  public void init() {
    initTemporaryTestData();
    get(
        "/forms/query",
        (req, res) ->
            MAPPER.toJson(
                util.getMetacardsByFilter(FormAttributes.Query.TAG)
                    .entrySet()
                    .stream()
                    .map(Map.Entry::getValue)
                    .map(Result::getMetacard)
                    .map(this::toFormTemplate)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList())));

    get(
        "/forms/result",
        (req, res) ->
            MAPPER.toJson(
                util.getMetacardsByFilter(FormAttributes.Result.TAG)
                    .entrySet()
                    .stream()
                    .map(Map.Entry::getValue)
                    .map(Result::getMetacard)
                    .map(this::toFieldFilter)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList())));

    post(
        "/forms/query",
        APPLICATION_JSON,
        (req, res) -> {
          FormTemplate form = MAPPER.fromJson(util.safeGetBody(req), FormTemplate.class);
          String id = form.getId();
          throw new UnsupportedOperationException("Creating forms is not yet supported");
          //          Map<String, Object> incoming =
          //              JsonFactory.create().parser().parseMap(util.safeGetBody(req));
          //          Metacard saved = saveMetacard(mapToTemplate(incoming));
          //          Map<String, Object> response = templateToMap(saved);

          //          res.status(201);
          //          res.body(null);
          //          return res;
        });

    post(
        "/forms/result",
        APPLICATION_JSON,
        (req, res) -> {
          FieldFilter details = MAPPER.fromJson(util.safeGetBody(req), FieldFilter.class);
          String id = details.getId();
          throw new UnsupportedOperationException("Creating result filters is not yet supported");
          //          Map<String, Object> incoming =
          //              JsonFactory.create().parser().parseMap(util.safeGetBody(req));
          //          Metacard saved = saveMetacard(mapToTemplate(incoming));
          //          Map<String, Object> response = templateToMap(saved);

          //          res.status(201);
          //          res.body(null);
          //          return res;
        });

    put(
        "/forms/:id",
        APPLICATION_JSON,
        (req, res) -> {
          throw new UnsupportedOperationException("Updating forms is not yet supported");
          //          String id = req.params(":id");
          //          Map<String, Object> input =
          // JsonFactory.create().parser().parseMap(util.safeGetBody(req));
          //
          //          Metacard metacard = mapToTemplate(input);
          //          metacard.setAttribute(new AttributeImpl(Metacard.ID, id));
          //
          //          Metacard updated = updateMetacard(id, metacard);
          //          return util.getJson(transformer.transform(updated));
        });

    delete(
        "/forms/:id",
        APPLICATION_JSON,
        (req, res) -> {
          throw new UnsupportedOperationException("Deleting forms is not yet supported");
          //          String id = req.params(":id");
          //          catalogFramework.delete(new DeleteRequestImpl(id));
          //          return ImmutableMap.of("message", "Successfully deleted.");
        },
        util::getJson);
  }

  // TODO: StringBufferInputStream is deprecated
  private FormTemplate toFormTemplate(Metacard metacard) {
    if (!QueryTemplateMetacardImpl.isQueryTemplateMetacard(metacard)) {
      return null;
    }
    QueryTemplateMetacardImpl wrapped = new QueryTemplateMetacardImpl(metacard);
    JsonTransformVisitor visitor = new JsonTransformVisitor();
    try {
      FilterReader reader = new FilterReader();
      JAXBElement<FilterType> root =
          reader.unmarshal(new StringBufferInputStream(wrapped.getFormsFilter()), FilterType.class);
      makeVisitable(root).accept(visitor);
      return new FormTemplate(
          wrapped.getId(),
          wrapped.getTitle(),
          (String) wrapped.getAttribute(CoreAttributes.DESCRIPTION).getValue(),
          visitor.getResult());
    } catch (JAXBException e) {
      LOGGER.error("Parsing failed for metacard's filter xml", e);
    }
    return null;
  }

  private FieldFilter toFieldFilter(Metacard metacard) {
    if (!ResultTemplateMetacardImpl.isResultTemplateMetacard(metacard)) {
      return null;
    }
    ResultTemplateMetacardImpl wrapped = new ResultTemplateMetacardImpl(metacard);
    return new FieldFilter(
        wrapped.getId(),
        wrapped.getTitle(),
        (String) wrapped.getAttribute(CoreAttributes.DESCRIPTION).getValue(),
        wrapped.getResultDescriptors());
  }

  private Metacard updateMetacard(String id, Metacard metacard)
      throws SourceUnavailableException, IngestException {
    return catalogFramework
        .update(new UpdateRequestImpl(id, metacard))
        .getUpdatedMetacards()
        .get(0)
        .getNewMetacard();
  }

  private Metacard saveMetacard(Metacard metacard)
      throws IngestException, SourceUnavailableException {
    Security security = Security.getInstance();

    security.runAsAdmin(
        () -> {
          try {
            return security.runWithSubjectOrElevate(
                () ->
                    catalogFramework
                        .create(new CreateRequestImpl(metacard))
                        .getCreatedMetacards()
                        .get(0));
          } catch (SecurityServiceException | InvocationTargetException e) {
            LOGGER.debug("Unable to update Gazetteer index.", e);
          }
          return null;
        });
    return null;
  }

  private void initTemporaryTestData() {
    QueryTemplateMetacardImpl template1 =
        new QueryTemplateMetacardImpl("Example 1", "I'm an example form");
    template1.setFormsFilter(SAMPLE_1);

    QueryTemplateMetacardImpl template2 =
        new QueryTemplateMetacardImpl(
            "Example 2", "I'm another example of a form for a customized search experience");
    template2.setFormsFilter(SAMPLE_2);

    ResultTemplateMetacardImpl fieldFilter1 =
        new ResultTemplateMetacardImpl(
            "Minimal", "A collection of the fewest possible descriptors that are useful");
    fieldFilter1.setResultDescriptors(
        ImmutableSet.of(
            CoreAttributes.TITLE,
            CoreAttributes.DESCRIPTION,
            CoreAttributes.CREATED,
            CoreAttributes.RESOURCE_DOWNLOAD_URL,
            CoreAttributes.THUMBNAIL));

    ResultTemplateMetacardImpl fieldFilter2 =
        new ResultTemplateMetacardImpl("Title-Only", "Only see a metacard's title");
    fieldFilter2.setResultDescriptors(ImmutableSet.of(CoreAttributes.TITLE));

    try {
      saveMetacard(template1);
      saveMetacard(template2);
      saveMetacard(fieldFilter1);
      saveMetacard(fieldFilter2);
    } catch (IngestException | SourceUnavailableException e) {
      throw new RuntimeException("Problem ingesting test resources", e);
    }
  }

  private static String cleanlyLoadResource(String resource) {
    try (InputStream is =
        SearchFormsApplication.class.getClassLoader().getResourceAsStream(resource)) {
      return IOUtils.toString(is, "UTF-8");
    } catch (IOException e) {
      throw new UncheckedIOException("Could not init test resources", e);
    }
  }

  private static class FilterReader {
    private final JAXBContext context;

    public FilterReader() throws JAXBException {
      String pkgName = FilterType.class.getPackage().getName();
      this.context = JAXBContext.newInstance(format("%s:%s", pkgName, pkgName));
    }

    @SuppressWarnings("unchecked")
    public <T> JAXBElement<T> unmarshal(InputStream inputStream, Class<T> tClass)
        throws JAXBException {
      Unmarshaller unmarshaller = context.createUnmarshaller();
      Object result = unmarshaller.unmarshal(inputStream);
      if (result instanceof JAXBElement) {
        JAXBElement element = (JAXBElement) result;
        if (tClass.isInstance(element.getValue())) {
          return (JAXBElement<T>) element;
        }
      }
      return null;
    }
  }
}
