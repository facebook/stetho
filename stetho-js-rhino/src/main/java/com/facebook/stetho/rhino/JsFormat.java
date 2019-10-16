/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.rhino;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;

/**
 * <p>Formatter that tries to mimic <pre>console.log()</pre>'s format as close as possible.</p>
 *
 * <p>We have to use a custom formatter because <pre>String.format()</pre> will fail with JavaScript
 * numbers. Using the conversion format %d in JavaScript causes a problem with Java's  <pre>String.format()</pre>.
 * This happens because %d expects an int/Integer but in JavaScript numbers are floats!
 * </p>
 *
 * <p>See <a href="https://developer.chrome.com/devtools/docs/console-api#consolelogobject-object">Console API</a>.</p>
 */
class JsFormat {
  /**
   * Format specifier pattern.
   * <p/>
   * <code>%[argument_index$][flags][width][.precision]conversion</code>
   */
  private static final Pattern FORMAT_SPECIFIER_PATTERN = Pattern.compile(
      "^%"
          + "([0-9]+ [$])?"  // Index
          + "([0-9]+)?"      // Width
          + "([.] [0-9]+)?"  // Precision
          + "([difs])",      // Conversion
      Pattern.COMMENTS
  );

  /**
   * Simple wrapper around a char[]. New versions of java make a full copy of
   * the string when doing substring(). With this class we avoid the copies
   * and substring is still O(1) vs O(n).
   */
  private static class ArrayCharSequence implements CharSequence {
    private final @NonNull char[] array;
    private final int start;
    private final int end;

    private ArrayCharSequence(@NonNull char[] array, int start, int end) {
      this.array = array;
      this.start = start;
      this.end = end;
    }

    @Override
    public int length() {
      return end - start;
    }

    @Override
    public char charAt(int index) {
      return array[start + index];
    }

    @NonNull
    @Override
    public CharSequence subSequence(int start, int end) {
      return new ArrayCharSequence(array, this.start + start, this.start + end);
    }

    private @NonNull CharSequence substring(int start) {
      return new ArrayCharSequence(array, this.start + start, this.start + end);
    }

    @Override
    public @NonNull String toString() {
      return new String(array, start, end - start);
    }
  }

  /**
   * Takes the arguments that console.log() would use, parses them and returns the
   * final string to output.
   *
   * @param args format and arguments
   * @return a string with the message to output
   */
  static @NonNull String parse(@NonNull Object...args) {
    // The params available, we need to know if they where taken or not
    boolean[] argsUsed = new boolean[args.length];
    // The first argument is the format and is always used
    String format = (String) args[0];
    argsUsed[0] = true;
    int nextArgIndex = 1;

    // Scan the format and find all %s patterns
    final char[] chars = format.toCharArray();
    StringBuilder buffer = new StringBuilder();
    ArrayCharSequence sequence = new ArrayCharSequence(chars, 0, chars.length);
    for (int i = 0; i < chars.length; ++i) {
      char c = chars[i];
      if (c != '%') {
        // Keep eating chars until we get a '%'
        buffer.append(c);
        continue;
      }

      // Found a %, is it a stand alone one?
      Matcher matcher = FORMAT_SPECIFIER_PATTERN.matcher(sequence.substring(i));
      if (!matcher.find()) {
        // Didn't find a valid format specifier, maybe it is a "%%" ?
        if (i + 1 < chars.length) {
          char peek = chars[i + 1];
          if (peek == '%') {
            // Found "%%" which at the end maps as a single '%'
            ++i;
          }
        }

        // A stand alone '%', we will just print it as it is
        buffer.append('%');
        continue;
      }

      // Analyze the format. We don't have named captures in android yet so we will inspect
      // the groups. They are each optional but we can find out which one is which easily.
      // Remember that we want to parse: %[argument_index$][flags][width][.precision]conversion
      //
      //  - `index` ends with '$'
      //  - `precision` starts with '.'
      //  - we don't support flags
      //  - `width` is just numbers
      //  - `conversion` is a single letter
      int groupCount = matcher.groupCount();
      int index = -1;
      int width = -1;
      int precision = -1;
      char conversion = 0;
      for (int groupIdx = 1; groupIdx <= groupCount; ++groupIdx) {
        String value = matcher.group(groupIdx);
        if (value == null || value.equals("")) {
          // Empty group, we ignore it
          continue;
        }

        if (value.endsWith("$")) {
          // The `index` (it ends with a '$')
          value = value.substring(0, value.length() - 1);
          index = Integer.parseInt(value);
          continue;
        }

        char first = value.charAt(0);
        if (first == '.') {
          // The `precision` (it starts with a dot '.')
          value = value.substring(1);
          precision = Integer.parseInt(value);
        } else if (first >= '0' && first <= '9') {
          // The `width`
          width = Integer.parseInt(value);
        } else {
          // It has to be the `conversion`
          conversion = first;
        }
      }

      // Now we try to see which argument we have to take
      String currentFormat = matcher.group();
      final Object value;
      final boolean found;
      if (index > argsUsed.length || (width > -1 && index == -1)) {
        // Index out of bounds (%1234$d), print the format as it is and ignore
        value = null;
        found = false;
      } else if (index <= argsUsed.length && index > 0) {
        // Index if valid (%3$d)
        value = args[index];
        argsUsed[index] = true;
        nextArgIndex = index + 1;
        found = true;
      } else {
        // No index provided (%d)
        if (nextArgIndex < argsUsed.length) {
          value = args[nextArgIndex];
          argsUsed[nextArgIndex] = true;
          ++nextArgIndex;
          found = true;
        } else {
          // We have way too many %d, more than we have arguments!
          value = null;
          found = false;
        }
      }

      if (!found) {
        // Just dump the placeholder text as it is and ignore
        buffer.append(currentFormat);
        i += currentFormat.length() - 1;
        continue;
      }

      // Apply the conversion
      switch (conversion) {
        case 'd':
        case 'i':
          Object l;
          if (value instanceof String) {
            try {
              l = Long.parseLong((String) value);
            } catch (NumberFormatException e) {
              l = "NaN";
            }
          } else if (value instanceof Number) {
            l = ((Number) value).intValue();
          } else {
            l = 0;
          }
          buffer.append(l);
          break;

        case 'f':
          Object d;
          if (value instanceof String) {
            try {
              d = Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
              d = "NaN";
            }
          } else if (value instanceof Number) {
            d = ((Number) value).doubleValue();
          } else {
            d = 0;
          }

          if (precision > -1 && d instanceof Number) {
            d = String.format(Locale.US, "%." + precision + "f", d);
          }
          buffer.append(d);
          break;

        case 's':
        default:
          buffer.append(value);
          break;
      }

      i += currentFormat.length() - 1;
    }

    // Concatenate all params that have not been used
    for (int j = 0; j < argsUsed.length; j++) {
      boolean argUsed = argsUsed[j];
      if (!argUsed) {
        buffer.append(" ");
        buffer.append(args[j]);
      }
    }

    return buffer.toString();
  }
}
