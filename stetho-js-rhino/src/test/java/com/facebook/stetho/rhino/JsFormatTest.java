/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.rhino;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class JsFormatTest {

  @Test
  public void testParse() {
    assertEquals("%d", JsFormat.parse("%d"));
    assertEquals("23", JsFormat.parse("%d", 23));
    assertEquals("2", JsFormat.parse("%d", 2.7));
    assertEquals("23.37", JsFormat.parse("%.2f", 23.3678));
    assertEquals("hellow world", JsFormat.parse("hellow %s", "world"));
    assertEquals("%$2d dp 5 1 2 3 4", JsFormat.parse("%$2d dp 5", 1, 2, 3 ,4));
    assertEquals("2 dp 4 1 3 4", JsFormat.parse("%2$d dp 4", 1, 2, 3 ,4));
    assertEquals("NaN dp 4 one three four", JsFormat.parse("%2$d dp 4", "one", "two", "three", "four"));
    assertEquals("two dp 4 one three four", JsFormat.parse("%2$s dp 4", "one", "two", "three", "four"));
    assertEquals("two vs two one three four", JsFormat.parse("%2$s vs %2$s", "one", "two", "three", "four"));

    assertEquals("two vs two one three four", JsFormat.parse("%2$s vs %2$s", "one", "two", "three", "four"));
    assertEquals("two vs four one three", JsFormat.parse("%2$s vs %4$s", "one", "two", "three", "four"));
    assertEquals("two vs four %.3$4f one three 1234.5678", JsFormat.parse("%2$s vs %4$s %.3$4f", "one", "two", "three", "four", 1234.5678));
    assertEquals("two vs four %.2$4f one three 1234.5678", JsFormat.parse("%2$s vs %4$s %.2$4f", "one", "two", "three", "four", 1234.5678));
    assertEquals("two vs four 1234.57 one three", JsFormat.parse("%2$s vs %4$s %.2f", "one", "two", "three", "four", 1234.5678));

    assertEquals("1234.57", JsFormat.parse("%.2f", 1234.5678));
    assertEquals("%,2f 1234.5678", JsFormat.parse("%,2f", 1234.5678));
    assertEquals("1234.57", JsFormat.parse("%.2f", 1234.5678));
    assertEquals("NaN", JsFormat.parse("%.2f", "hello"));
    assertEquals("%.2a hello", JsFormat.parse("%.2a", "hello"));
    assertEquals("% cool .2a hello", JsFormat.parse("%% cool .2a", "hello"));
    assertEquals("two vs four 1234.5678 one three", JsFormat.parse("%2$s vs %4$s %s", "one", "two", "three", "four", 1234.5678));
    assertEquals("two vs four %s", JsFormat.parse("two vs four %s"));
    assertEquals("two vs four % one three", JsFormat.parse("two vs four %", "one", "three"));
    assertEquals("two one two four oops three", JsFormat.parse("%2$s %1$s %s %4$s %s", "one", "two", "three", "four", "oops"));
  }
}
