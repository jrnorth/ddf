package org.codice.ddf.catalog.ui.forms.data;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractShareableMetacardImpl extends MetacardImpl {
  public AbstractShareableMetacardImpl(MetacardType type) {
    super(type);
  }

  public AbstractShareableMetacardImpl(Metacard metacard) {
    super(metacard);
  }

  public AbstractShareableMetacardImpl(Metacard metacard, MetacardType type) {
    super(metacard, type);
  }

  /**
   * Compute the symmetric difference between the sharing permissions of two workspaces.
   *
   * @param m - metacard to diff against
   * @return
   */
  public Set<String> diffSharing(Metacard m) {
    //    if (isResultTemplateMetacard(m)) {
    //      return Sets.symmetricDifference(getSharing(), from(m).getSharing());
    //    }
    return Collections.emptySet();
  }

  protected List<String> getValues(String attribute) {
    Attribute attr = getAttribute(attribute);
    if (attr != null) {
      return attr.getValues().stream().map(String::valueOf).collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  public String getOwner() {
    List<String> values = getValues(Core.METACARD_OWNER);
    if (!values.isEmpty()) {
      return values.get(0);
    }
    return null;
  }

  public void setOwner(String email) {
    setAttribute(Core.METACARD_OWNER, email);
  }

  public Set<String> getSharing() {
    return new HashSet<>(getValues(FormAttributes.Sharing.FORM_SHARING));
  }

  public void setSharing(Set<String> sharing) {
    setAttribute(FormAttributes.Sharing.FORM_SHARING, new ArrayList<>(sharing));
  }
}
