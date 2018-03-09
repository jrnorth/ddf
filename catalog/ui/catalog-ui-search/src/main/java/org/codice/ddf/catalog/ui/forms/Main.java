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
package org.codice.ddf.catalog.ui.forms;

import static org.codice.ddf.catalog.ui.forms.filter.VisitableFilterNode.makeVisitable;

import java.io.InputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import net.opengis.filter.v_2_0.FilterType;
import net.opengis.filter.v_2_0.ObjectFactory;
import org.boon.json.JsonFactory;
import org.boon.json.JsonParserFactory;
import org.boon.json.JsonSerializerFactory;
import org.boon.json.ObjectMapper;
import org.codice.ddf.catalog.ui.forms.model.FilterNodeValueSerializer;
import org.codice.ddf.catalog.ui.forms.model.JsonTransformVisitor;

public class Main {
  private static final ObjectMapper MAPPER =
      JsonFactory.create(
          new JsonParserFactory().usePropertyOnly(),
          new JsonSerializerFactory()
              .addPropertySerializer(new FilterNodeValueSerializer())
              .useAnnotations()
              .includeEmpty()
              .includeDefaultValues()
              .setJsonFormatForDates(false));

  private static final InputStream CUR_TEST_FILTER;

  static {
    CUR_TEST_FILTER =
        Main.class
            .getClassLoader()
            .getResourceAsStream(
                //
                // "filter-xml-components/comparison-binary-ops/PropertyIsEqualTo.xml");
                //                "TEMP-FORM-DATA/filter-xml-components/function-ops/Function.xml");
                "TEMP-FORM-DATA/compound-examples/compound-example-2.xml");
  }

  public static void main(String[] args) throws Exception {
    Bindings bindings = new Bindings();
    JAXBElement<FilterType> root = bindings.unmarshal(CUR_TEST_FILTER, FilterType.class);

    JsonTransformVisitor visitor = new JsonTransformVisitor();
    makeVisitable(root).accept(visitor);

    print("Done... " + root.toString());
    printSeparator();

    //    BeanUtils.asPrettyJsonString(MAPPER, builder.getResult());
    print(MAPPER.toJson(visitor.getResult()));

    //        runFunction();
    //        Chainr chainr = new ChainrBuilder();

    //        JSONObject json = new JSONObject(JSON);
    //        print(XML.toString(json, "fes:Filter"));
  }

  public static void printSeparator() {
    print(
        "____________________________________________________________________"
            + System.lineSeparator());
  }

  public static void print(String msg) {
    System.out.println(msg);
  }

  static class Bindings {
    private final JAXBContext context;

    private final ObjectFactory objectFactory;

    public Bindings() throws JAXBException {
      this.context =
          JAXBContext.newInstance(
              FilterType.class.getPackage().getName()
                  + ":"
                  + FilterType.class.getPackage().getName());
      this.objectFactory = new ObjectFactory();
    }

    public <T> JAXBElement<T> unmarshal(InputStream inputStream, Class<T> tClass)
        throws JAXBException {
      Unmarshaller unmarshal = context.createUnmarshaller();
      Object unmarshalled = unmarshal.unmarshal(inputStream);
      return (JAXBElement<T>) unmarshalled;
    }
  }
}
