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

final class AndroidDOMProvider implements DOMProvider, AndroidDescriptorHost {
  private final DescriptorMap mDescriptorMap;
  private final AndroidDOMRoot mDOMRoot;
  private final ViewHighlighter mHighlighter;
  private Listener mListener;

  public AndroidDOMProvider(Application application) {
    mDOMRoot = new AndroidDOMRoot(application);

    mDescriptorMap = new DescriptorMap()
        .beginInit()
        .register(Activity.class, new ActivityDescriptor())
        .register(AndroidDOMRoot.class, mDOMRoot)
        .register(Application.class, new ApplicationDescriptor());
    FragmentDescriptor.register(mDescriptorMap)
        .register(Object.class, new ObjectDescriptor())
        .register(TextView.class, new TextViewDescriptor())
        .register(View.class, new ViewDescriptor())
        .register(ViewGroup.class, new ViewGroupDescriptor())
        .register(Window.class, new WindowDescriptor())
        .setHost(this)
        .endInit();

    mHighlighter = ViewHighlighter.newInstance();
  }

  // DOMProvider implementation
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
    return mDOMRoot;
  }

  @Override
  public NodeDescriptor getNodeDescriptor(Object element) {
    return getDescriptor(element);
  }

  @Override
  public void highlightElement(Object element, int color) {
    View highlightingView = getHighlightingView(element);
    if (highlightingView == null) {
      mHighlighter.clearHighlight();
    } else {
      mHighlighter.setHighlightedView(highlightingView, color);
    }
  }

  @Override
  public void hideHighlight() {
    mHighlighter.clearHighlight();
  }

  // Descriptor.Host implementation
  @Override
  public Descriptor getDescriptor(Object element) {
    return (element == null) ? null : mDescriptorMap.get(element.getClass());
  }

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

  // AndroidDescriptorHost implementation
  @Override
  public View getHighlightingView(Object element) {
    if (element == null) {
      return null;
    }

    View highlightingView = null;
    Class<?> theClass = element.getClass();
    Descriptor lastDescriptor = null;
    while (highlightingView == null && theClass != null) {
      Descriptor descriptor = mDescriptorMap.get(theClass);
      if (descriptor == null) {
        return null;
      }

      if (descriptor != lastDescriptor && descriptor instanceof HighlightableDescriptor) {
        highlightingView = ((HighlightableDescriptor)descriptor).getViewForHighlighting(element);
      }

      lastDescriptor = descriptor;
      theClass = theClass.getSuperclass();
    }

    return highlightingView;
  }
}
