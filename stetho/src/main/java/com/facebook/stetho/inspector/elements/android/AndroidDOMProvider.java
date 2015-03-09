// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.elements.android;

import android.app.Activity;
import android.app.Application;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.facebook.stetho.inspector.elements.DOMProvider;
import com.facebook.stetho.inspector.elements.Descriptor;
import com.facebook.stetho.inspector.elements.DescriptorMap;
import com.facebook.stetho.inspector.elements.NodeDescriptor;
import com.facebook.stetho.inspector.elements.ObjectDescriptor;

final class AndroidDOMProvider implements DOMProvider {
  private final Application mApplication;
  private final DescriptorMap mDescriptorMap;
  private final ViewHighlighter mHighlighter;
  private Listener mListener;

  public AndroidDOMProvider(Application application) {
    mApplication = application;

    mDescriptorMap = new DescriptorMap()
        .beginInit()
        .register(Activity.class, new ActivityDescriptor())
        .register(Application.class, new ApplicationDescriptor())
        .register(Object.class, new ObjectDescriptor())
        .register(TextView.class, new TextViewDescriptor())
        .register(View.class, new ViewDescriptor())
        .register(ViewGroup.class, new ViewGroupDescriptor())
        .register(Window.class, new WindowDescriptor())
        .setListener(new DescriptorListener())
        .endInit();

    mHighlighter = ViewHighlighter.newInstance();
  }

  @Override
  public void dispose() {
    mHighlighter.clearHighlight();
  }

  @Override
  public void setListener(Listener listener) {
    mListener = listener;
  }

  @Override
  public Object getRootElement() {
    return mApplication;
  }

  @Override
  public NodeDescriptor getNodeDescriptor(Object element) {
    return (element == null) ? null : mDescriptorMap.get(element.getClass());
  }

  @Override
  public void highlightElement(
      Object element,
      int contentColor,
      int paddingColor,
      int borderColor,
      int marginColor) {
    if (!(element instanceof View)) {
      mHighlighter.clearHighlight();
    }
    else {
      final View view = (View)element;
      mHighlighter.setHighlightedView(view, contentColor, paddingColor, borderColor, marginColor);
    }
  }

  @Override
  public void hideHighlight() {
    mHighlighter.clearHighlight();
  }

  // Forwards notifications from Descriptor to DOM
  private class DescriptorListener implements Descriptor.Listener {
    @Override
    public void onAttributeModified(Object element, String name, String value) {
      mListener.onAttributeModified(element, name, value);
    }

    @Override
    public void onAttributeRemoved(Object element, String name) {
      mListener.onAttributeRemoved(element, name);
    }

    @Override
    public void onChildInserted(Object parentElement, Object previousElement, Object childElement) {
      mListener.onChildInserted(parentElement, previousElement, childElement);
    }

    @Override
    public void onChildRemoved(Object parentElement, Object childElement) {
      mListener.onChildRemoved(parentElement, childElement);
    }
  }
}