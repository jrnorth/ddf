package org.codice.ddf.catalog.ui.forms.filter;

import javax.xml.bind.JAXBElement;

public interface VisitableXmlElement<T> {

  JAXBElement<T> getElement();

  void accept(FilterVisitor2 visitor);
}
