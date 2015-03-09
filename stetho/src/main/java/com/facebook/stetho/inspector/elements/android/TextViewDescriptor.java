// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.elements.android;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

import com.facebook.stetho.common.Util;
import com.facebook.stetho.inspector.elements.ChainedDescriptor;
import com.facebook.stetho.inspector.elements.NodeAttribute;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

final class TextViewDescriptor extends ChainedDescriptor<TextView> {
  private static final String TEXT_ATTRIBUTE_NAME = "text";

  private final Map<TextView, ElementContext> mElementToContextMap =
      Collections.synchronizedMap(new IdentityHashMap<TextView, ElementContext>());

  @Override
  protected void onHook(final TextView element) {
    ElementContext context = new ElementContext();
    context.hook(element);
    mElementToContextMap.put(element, context);
  }

  protected void onUnhook(TextView element) {
    ElementContext context = mElementToContextMap.remove(element);
    context.unhook();
  }

  @Override
  protected int onGetAttributeCount(TextView element) {
    return (element.getText().length() == 0) ? 0 : 1;
  }

  @Override
  protected void onCopyAttributeAt(TextView element, int index, NodeAttribute outAttribute) {
    if (index != 0) {
      throw new IndexOutOfBoundsException();
    }

    CharSequence text = element.getText();
    if (text.length() == 0) {
      throw new IndexOutOfBoundsException();
    } else {
      outAttribute.name = TEXT_ATTRIBUTE_NAME;
      outAttribute.value = text.toString();
    }
  }

  private final class ElementContext implements TextWatcher {
    private TextView mElement;

    public void hook(TextView element) {
      mElement = Util.throwIfNull(element);
      mElement.addTextChangedListener(this);
    }

    public void unhook() {
      if (mElement != null) {
        mElement.removeTextChangedListener(this);
        mElement = null;
      }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
      if (s.length() == 0) {
        getListener().onAttributeRemoved(mElement, TEXT_ATTRIBUTE_NAME);
      } else {
        getListener().onAttributeModified(mElement, TEXT_ATTRIBUTE_NAME, s.toString());
      }
    }
  }
}
