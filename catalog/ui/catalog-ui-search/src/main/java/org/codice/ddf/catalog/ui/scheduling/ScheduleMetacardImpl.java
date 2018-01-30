package org.codice.ddf.catalog.ui.scheduling;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import java.util.Collections;

public class ScheduleMetacardImpl extends MetacardImpl {

  public static final MetacardType TYPE = new ScheduleMetacardTypeImpl();

  public ScheduleMetacardImpl() {
    super(TYPE);
    setTags(Collections.singleton(ScheduleMetacardTypeImpl.SCHEDULE_TAG));
  }

  public ScheduleMetacardImpl(Metacard wrappedMetacard) {
    super(wrappedMetacard, TYPE);
    setTags(Collections.singleton(ScheduleMetacardTypeImpl.SCHEDULE_TAG));
  }
}
