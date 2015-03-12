// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.elements.android;

import android.content.res.Resources;
import android.view.View;

import com.facebook.stetho.common.LogUtil;
import com.facebook.stetho.common.StringUtil;
import com.facebook.stetho.inspector.elements.AttributeAccumulator;
import com.facebook.stetho.inspector.elements.ChainedDescriptor;

import javax.annotation.Nullable;

final class ViewDescriptor extends ChainedDescriptor<View> {
  private static final String ID_ATTRIBUTE_NAME = "id";

  @Override
  protected String onGetNodeName(View element) {
    String className = element.getClass().getName();

    return
        StringUtil.removePrefix(className, "android.view.",
        StringUtil.removePrefix(className, "android.widget."));
  }

  @Override
  protected void onCopyAttributes(View element, AttributeAccumulator attributes) {
    String id = getIdAttribute(element);
    if (id != null) {
      attributes.add(ID_ATTRIBUTE_NAME, id);
    }
  }

  private static int getResourcePackageId(int id) {
    return (id >>> 24) & 0xff;
  }

  @Nullable
  private static String getIdAttribute(View element) {
    // Adapted from View.toString()

    int id = element.getId();
    if (id == View.NO_ID) {
      return null;
    }

    String idString = null;
    Resources r = element.getResources();
    if (r != null) {
      try {
        String prefix;
        String prefixSeparator;
        switch (getResourcePackageId(id)) {
          case 0x7f:
            prefix = "";
            prefixSeparator = "";
            break;
          default:
            prefix = r.getResourcePackageName(id);
            prefixSeparator = ":";
            break;
        }

        String typeName = r.getResourceTypeName(id);
        String entryName = r.getResourceEntryName(id);

        StringBuilder sb = new StringBuilder(
            1 + prefix.length() + prefixSeparator.length() +
            typeName.length() + 1 + entryName.length());
        sb.append("@");
        sb.append(prefix);
        sb.append(prefixSeparator);
        sb.append(typeName);
        sb.append("/");
        sb.append(entryName);

        idString = sb.toString();

      } catch (Resources.NotFoundException e) {
        LogUtil.w(e, "Could not retrieve resource info for element: %s", element);
      }
    }

    if (idString == null) {
      idString = "#" + Integer.toHexString(id);
    }

    return idString;
  }
}
