// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.elements;

import com.facebook.stetho.common.Util;

import java.util.IdentityHashMap;
import java.util.Map;

public final class DescriptorMap {
  private final Map<Class<?>, Descriptor> mMap = new IdentityHashMap<Class<?>, Descriptor>();
  private boolean mIsInitializing;
  private Descriptor.Listener mListener;

  public DescriptorMap beginInit() {
    Util.throwIf(mIsInitializing);
    mIsInitializing = true;
    return this;
  }

  public DescriptorMap register(Class<?> elementClass, Descriptor nodeDescriptor) {
    Util.throwIfNull(elementClass);
    Util.throwIfNull(nodeDescriptor);
    Util.throwIfNot(mIsInitializing);

    if (mMap.containsKey(elementClass)) {
      throw new UnsupportedOperationException();
    }

    mMap.put(elementClass, nodeDescriptor);
    return this;
  }

  public DescriptorMap setListener(Descriptor.Listener listener) {
    Util.throwIfNull(listener);
    Util.throwIfNot(mIsInitializing);
    Util.throwIfNotNull(mListener);

    mListener = listener;

    return this;
  }

  public DescriptorMap endInit() {
    Util.throwIfNot(mIsInitializing);
    Util.throwIfNull(mListener);

    for (final Class<?> elementClass : mMap.keySet()) {
      final Descriptor nodeDescriptor = mMap.get(elementClass);

      if (nodeDescriptor instanceof ChainedDescriptor) {
        final ChainedDescriptor<?> chainedNodeDescriptor = (ChainedDescriptor<?>)nodeDescriptor;
        Class<?> superClass = elementClass.getSuperclass();
        Descriptor superNodeDescriptor = getImpl(superClass);
        chainedNodeDescriptor.setSuper(superNodeDescriptor);
      }

      nodeDescriptor.setListener(mListener);
    }

    mIsInitializing = false;
    return this;
  }

  public Descriptor get(Class<?> elementClass) {
    Util.throwIfNull(elementClass);
    Util.throwIf(mIsInitializing);
    return getImpl(elementClass);
  }

  private Descriptor getImpl(final Class<?> elementClass) {
    Class<?> theClass = elementClass;
    while (theClass != null) {
      Descriptor nodeDescriptor = mMap.get(theClass);
      if (nodeDescriptor != null) {
        return nodeDescriptor;
      }

      theClass = theClass.getSuperclass();
    }

    return null;
  }
}
