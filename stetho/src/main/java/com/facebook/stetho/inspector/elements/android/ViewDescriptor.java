/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.elements.android;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewDebug;

import com.facebook.stetho.common.ExceptionUtil;
import com.facebook.stetho.common.LogUtil;
import com.facebook.stetho.common.ReflectionUtil;
import com.facebook.stetho.common.StringUtil;
import com.facebook.stetho.common.android.ResourcesUtil;
import com.facebook.stetho.inspector.elements.AbstractChainedDescriptor;
import com.facebook.stetho.inspector.elements.AttributeAccumulator;
import com.facebook.stetho.inspector.elements.ComputedStyleAccumulator;
import com.facebook.stetho.inspector.elements.StyleAccumulator;
import com.facebook.stetho.inspector.elements.StyleRuleNameAccumulator;
import com.facebook.stetho.inspector.helper.IntegerFormatter;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

final class ViewDescriptor extends AbstractChainedDescriptor<View>
    implements HighlightableDescriptor<View> {
  private static final String ID_NAME = "id";
  private static final String NONE_VALUE = "(none)";
  private static final String NONE_MAPPING = "<no mapping>";
  private static final String VIEW_STYLE_RULE_NAME = "<this_view>";
  private static final String ACCESSIBILITY_STYLE_RULE_NAME = "Accessibility Properties";

  private final MethodInvoker mMethodInvoker;

  private static final boolean sHasSupportNodeInfo;

  static {
    sHasSupportNodeInfo = ReflectionUtil.tryGetClassForName(
        "androidx.core.view.accessibility.AccessibilityNodeInfoCompat") != null;
  }

  /**
   * NOTE: Only access this via {@link #getWordBoundaryPattern}.
   */
  @Nullable
  private Pattern mWordBoundaryPattern;

  /**
   * NOTE: Only access this via {@link #getViewProperties}.
   */
  @Nullable
  @GuardedBy("this")
  private volatile List<ViewCSSProperty> mViewProperties;

  private Pattern getWordBoundaryPattern() {
    if (mWordBoundaryPattern == null) {
      mWordBoundaryPattern = Pattern.compile("(?<=\\p{Lower})(?=\\p{Upper})");
    }

    return mWordBoundaryPattern;
  }

  private List<ViewCSSProperty> getViewProperties() {
    if (mViewProperties == null) {
      synchronized (this) {
        if (mViewProperties == null) {
          List<ViewCSSProperty> props = new ArrayList<>();

          for (final Method method : View.class.getDeclaredMethods()) {
            ViewDebug.ExportedProperty annotation =
                method.getAnnotation(
                    ViewDebug.ExportedProperty.class);

            if (annotation != null) {
              props.add(new MethodBackedCSSProperty(
                  method,
                  convertViewPropertyNameToCSSName(method.getName()),
                  annotation));
            }
          }

          for (final Field field : View.class.getDeclaredFields()) {
            ViewDebug.ExportedProperty annotation =
                field.getAnnotation(
                    ViewDebug.ExportedProperty.class);

            if (annotation != null) {
              props.add(new FieldBackedCSSProperty(
                  field,
                  convertViewPropertyNameToCSSName(field.getName()),
                  annotation));
            }
          }

          Collections.sort(props, new Comparator<ViewCSSProperty>() {
            @Override
            public int compare(ViewCSSProperty lhs, ViewCSSProperty rhs) {
              return lhs.getCSSName().compareTo(rhs.getCSSName());
            }
          });
          mViewProperties = Collections.unmodifiableList(props);
        }
      }
    }

    return mViewProperties;
  }

  public ViewDescriptor() {
    this(new MethodInvoker());
  }

  public ViewDescriptor(MethodInvoker methodInvoker) {
    mMethodInvoker = methodInvoker;
  }

  @Override
  protected String onGetNodeName(View element) {
    String className = element.getClass().getName();

    return
        StringUtil.removePrefix(className, "android.view.",
        StringUtil.removePrefix(className, "android.widget."));
  }

  @Override
  protected void onGetAttributes(View element, AttributeAccumulator attributes) {
    String id = getIdAttribute(element);
    if (id != null) {
      attributes.store(ID_NAME, id);
    }
  }

  @Override
  protected void onSetAttributesAsText(View element, String text) {
    Map<String, String> attributeToValueMap = parseSetAttributesAsTextArg(text);
    for (Map.Entry<String, String> entry : attributeToValueMap.entrySet()) {
      String methodName = "set" + capitalize(entry.getKey());
      String propertyValue = entry.getValue();
      mMethodInvoker.invoke(element, methodName, propertyValue);
    }
  }

  @Nullable
  private static String getIdAttribute(View element) {
    int id = element.getId();
    if (id == View.NO_ID) {
      return null;
    }
    return ResourcesUtil.getIdStringQuietly(element, element.getResources(), id);
  }

  @Override
  @Nullable
  public View getViewAndBoundsForHighlighting(View element, Rect bounds) {
    return element;
  }

  @Nullable
  @Override
  public Object getElementToHighlightAtPosition(View element, int x, int y, Rect bounds) {
    bounds.set(0, 0, element.getWidth(), element.getHeight());
    return element;
  }

  @Override
  protected void onGetStyleRuleNames(View element, StyleRuleNameAccumulator accumulator) {
    accumulator.store(VIEW_STYLE_RULE_NAME, false);
    if (sHasSupportNodeInfo) {
      accumulator.store(ACCESSIBILITY_STYLE_RULE_NAME, false);
    }
  }

  @Override
  protected void onGetStyles(View element, String ruleName, StyleAccumulator accumulator) {
    if (VIEW_STYLE_RULE_NAME.equals(ruleName)) {
      List<ViewCSSProperty> properties = getViewProperties();
      for (int i = 0, size = properties.size(); i < size; i++) {
        ViewCSSProperty property = properties.get(i);
        try {
          getStyleFromValue(
              element,
              property.getCSSName(),
              property.getValue(element),
              property.getAnnotation(),
              accumulator);
        } catch (Exception e) {
          if (e instanceof IllegalAccessException || e instanceof InvocationTargetException) {
            LogUtil.e(e, "failed to get style property " + property.getCSSName() +
                    " of element= " + element.toString());
          } else {
            throw ExceptionUtil.propagate(e);
          }
        }
      }
    } else if (ACCESSIBILITY_STYLE_RULE_NAME.equals(ruleName)) {
      if (sHasSupportNodeInfo) {
        boolean ignored = AccessibilityNodeInfoWrapper.getIgnored(element);
        getStyleFromValue(
            element,
            "ignored",
            ignored,
            null,
            accumulator);

        if (ignored) {
          getStyleFromValue(
              element,
              "ignored-reasons",
              AccessibilityNodeInfoWrapper.getIgnoredReasons(element),
              null,
              accumulator);
        }

        getStyleFromValue(
            element,
            "focusable",
            !ignored,
            null,
            accumulator);

        if (!ignored) {
          getStyleFromValue(
              element,
              "focusable-reasons",
              AccessibilityNodeInfoWrapper.getFocusableReasons(element),
              null,
              accumulator);

          getStyleFromValue(
              element,
              "focused",
              AccessibilityNodeInfoWrapper.getIsAccessibilityFocused(element),
              null,
              accumulator);

          getStyleFromValue(
              element,
              "description",
              AccessibilityNodeInfoWrapper.getDescription(element),
              null,
              accumulator);

          getStyleFromValue(
              element,
              "actions",
              AccessibilityNodeInfoWrapper.getActions(element),
              null,
              accumulator);
        }
      }
    }
  }

  @Override
  protected void onGetComputedStyles(View element, ComputedStyleAccumulator styles) {
    styles.store("left", Integer.toString(element.getLeft()));
    styles.store("top", Integer.toString(element.getTop()));
    styles.store("right", Integer.toString(element.getRight()));
    styles.store("bottom", Integer.toString(element.getBottom()));
  }

  private static boolean canIntBeMappedToString(@Nullable ViewDebug.ExportedProperty annotation) {
    return annotation != null
        && annotation.mapping() != null
        && annotation.mapping().length > 0;
  }

  private static String mapIntToStringUsingAnnotation(
      int value,
      @Nullable ViewDebug.ExportedProperty annotation) {
    if (!canIntBeMappedToString(annotation)) {
      throw new IllegalStateException("Cannot map using this annotation");
    }

    for (ViewDebug.IntToString map : annotation.mapping()) {
      if (map.from() == value) {
        return map.to();
      }
    }

    // no mapping was found even though one was expected ):
    return NONE_MAPPING;
  }

  private static boolean canFlagsBeMappedToString(@Nullable ViewDebug.ExportedProperty annotation) {
    return annotation != null
        && annotation.flagMapping() != null
        && annotation.flagMapping().length > 0;
  }

  private static String mapFlagsToStringUsingAnnotation(
      int value,
      @Nullable ViewDebug.ExportedProperty annotation) {
    if (!canFlagsBeMappedToString(annotation)) {
      throw new IllegalStateException("Cannot map using this annotation");
    }

    StringBuilder stringBuilder = null;
    boolean atLeastOneFlag = false;

    for (ViewDebug.FlagToString flagToString : annotation.flagMapping()) {
      if (flagToString.outputIf() == ((value & flagToString.mask()) == flagToString.equals())) {
        if (stringBuilder == null) {
          stringBuilder = new StringBuilder();
        }

        if (atLeastOneFlag) {
          stringBuilder.append(" | ");
        }

        stringBuilder.append(flagToString.name());
        atLeastOneFlag = true;
      }
    }

    if (atLeastOneFlag) {
      return stringBuilder.toString();
    } else {
      return NONE_MAPPING;
    }
  }

  private String convertViewPropertyNameToCSSName(String getterName) {
    // Split string by uppercase characters. Thankfully since
    // this is the android source we don't have to worry about
    // internationalization funk.

    String[] words = getWordBoundaryPattern().split(getterName);

    StringBuilder result = new StringBuilder();

    for (int i = 0; i < words.length; i++) {
      if (words[i].equals("get") || words[i].equals("m")) {
        continue;
      }

      result.append(words[i].toLowerCase());

      if (i < words.length - 1) {
        result.append('-');
      }
    }

    return result.toString();
  }

  private void getStyleFromValue(
      View element,
      String name,
      Object value,
      @Nullable ViewDebug.ExportedProperty annotation,
      StyleAccumulator styles) {

    if (name.equals(ID_NAME)) {
      getIdStyle(element, styles);
    } else if (value instanceof Integer) {
      getStyleFromInteger(name, (Integer) value, annotation, styles);
    } else if (value instanceof Float) {
      styles.store(name, String.valueOf(value), ((Float) value) == 0.0f);
    } else if (value instanceof Boolean) {
      styles.store(name, String.valueOf(value), false);
    } else if (value instanceof Short) {
      styles.store(name, String.valueOf(value), ((Short) value) == 0);
    } else if (value instanceof Long) {
      styles.store(name, String.valueOf(value), ((Long) value) == 0);
    } else if (value instanceof Double) {
      styles.store(name, String.valueOf(value), ((Double) value) == 0.0d);
    } else if (value instanceof Byte) {
      styles.store(name, String.valueOf(value), ((Byte) value) == 0);
    } else if (value instanceof Character) {
      styles.store(name, String.valueOf(value), ((Character) value) == Character.MIN_VALUE);
    } else if (value instanceof CharSequence) {
      styles.store(name, String.valueOf(value), ((CharSequence) value).length() == 0);
    } else {
      getStylesFromObject(element, name, value, annotation, styles);
    }
  }

  private void getIdStyle(
      View element,
      StyleAccumulator styles) {

    @Nullable String id = getIdAttribute(element);

    if (id == null) {
      styles.store(ID_NAME, NONE_VALUE, false);
    } else {
      styles.store(ID_NAME, id, false);
    }
  }

  private void getStyleFromInteger(
      String name,
      Integer value,
      @Nullable ViewDebug.ExportedProperty annotation,
      StyleAccumulator styles) {

    String intValueStr = IntegerFormatter.getInstance().format(value, annotation);

    if (canIntBeMappedToString(annotation)) {
      styles.store(
          name,
          intValueStr + " (" + mapIntToStringUsingAnnotation(value, annotation) + ")",
          false);
    } else if (canFlagsBeMappedToString(annotation)) {
      styles.store(
          name,
          intValueStr + " (" + mapFlagsToStringUsingAnnotation(value, annotation) + ")",
          false);
    } else {
      Boolean defaultValue = true;
      // Mappable ints should always be shown, because enums don't necessarily have
      // logical "default" values. Thus we mark all of them as not default, so that they
      // show up in the inspector.
      if (value != 0 ||
          canFlagsBeMappedToString(annotation) ||
          canIntBeMappedToString(annotation)) {
        defaultValue = false;
      }
      styles.store(name, intValueStr, defaultValue);
    }
  }

  private void getStylesFromObject(
      View view,
      String name,
      Object value,
      @Nullable ViewDebug.ExportedProperty annotation,
      StyleAccumulator styles) {
    if (annotation == null || !annotation.deepExport() || value == null) {
      return;
    }

    Field[] fields = value.getClass().getFields();

    for (Field field : fields) {
      int modifiers = field.getModifiers();
      if (Modifier.isStatic(modifiers)) {
        continue;
      }

      Object propertyValue;
      try {
          field.setAccessible(true);
          propertyValue = field.get(value);
      } catch (IllegalAccessException e) {
        LogUtil.e(
            e,
            "failed to get property of name: \"" + name + "\" of object: " + String.valueOf(value));
        return;
      }

      String propertyName = field.getName();

      switch (propertyName) {
        case "bottomMargin":
          propertyName = "margin-bottom";
          break;
        case "topMargin":
          propertyName = "margin-top";
          break;
        case "leftMargin":
          propertyName = "margin-left";
          break;
        case "rightMargin":
          propertyName = "margin-right";
          break;
        default:
          String annotationPrefix = annotation.prefix();
          propertyName = convertViewPropertyNameToCSSName(
              (annotationPrefix == null) ? propertyName : (annotationPrefix + propertyName));
          break;
      }

      ViewDebug.ExportedProperty subAnnotation =
          field.getAnnotation(ViewDebug.ExportedProperty.class);

      getStyleFromValue(
          view,
          propertyName,
          propertyValue,
          subAnnotation,
          styles);
    }
  }

  private static String capitalize(String str) {
    if (str == null || str.length() == 0 || Character.isTitleCase(str.charAt(0))) {
      return str;
    }
    StringBuilder buffer = new StringBuilder(str);
    buffer.setCharAt(0, Character.toTitleCase(buffer.charAt(0)));
    return buffer.toString();
  }

  private final class FieldBackedCSSProperty extends ViewCSSProperty {
    private final Field mField;

    public FieldBackedCSSProperty(
        Field field,
        String cssName,
        @Nullable ViewDebug.ExportedProperty annotation) {
      super(cssName, annotation);
      mField = field;
      mField.setAccessible(true);
    }

    @Override
    public Object getValue(View view) throws InvocationTargetException, IllegalAccessException {
      return mField.get(view);
    }
  }

  private final class MethodBackedCSSProperty extends ViewCSSProperty {
    private final Method mMethod;

    public MethodBackedCSSProperty(
        Method method,
        String cssName,
        @Nullable ViewDebug.ExportedProperty annotation) {
      super(cssName, annotation);
      mMethod = method;
      mMethod.setAccessible(true);
    }

    @Override
    public Object getValue(View view) throws InvocationTargetException, IllegalAccessException {
      return mMethod.invoke(view);
    }
  }

  private abstract class ViewCSSProperty {
    private final String mCSSName;
    private final ViewDebug.ExportedProperty mAnnotation;

    public ViewCSSProperty(String cssName, @Nullable ViewDebug.ExportedProperty annotation) {
      mCSSName = cssName;
      mAnnotation = annotation;
    }

    public final String getCSSName() {
      return mCSSName;
    }

    public abstract Object getValue(View view)
        throws InvocationTargetException, IllegalAccessException;

    public final @Nullable ViewDebug.ExportedProperty getAnnotation() {
      return mAnnotation;
    }
  }
}
