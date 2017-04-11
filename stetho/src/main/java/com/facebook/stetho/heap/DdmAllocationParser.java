/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.heap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

@NotThreadSafe
public class DdmAllocationParser {
  private final ByteBuffer mData;
  @Nullable private EagerlyLoadedData mEagerlyLoadedData;
  private int mNumAllocsRead;
  private int mNumStackFramesRead;
  private int mNumStackFramesExpected;

  public DdmAllocationParser(ByteBuffer data) {
    mData = data;
    resetAllocationOffset();
  }

  public void resetAllocationOffset() {
    mData.position(getHeader().headerLen);
    mNumAllocsRead = 0;
    mNumStackFramesRead = -1;
    mNumStackFramesExpected = 0;
  }

  public boolean readNextAllocation(Allocation allocation) {
    Header header = getHeader();
    if (mNumAllocsRead == header.numAllocs) {
      return false;
    }

    int entryStartPos = mData.position();

    allocation.size = mData.getInt();
    allocation.threadId = mData.getShort() & 0xffff;
    allocation.classIndex = mData.getShort() & 0xffff;
    allocation.stackDepth = mData.get() & 0xff;

    mData.position(entryStartPos + header.entryHeaderLen);

    mNumAllocsRead++;
    mNumStackFramesRead = 0;
    mNumStackFramesExpected = allocation.stackDepth;

    return true;
  }

  public boolean readNextStackFrame(Method method) {
    if (mNumStackFramesRead == mNumStackFramesExpected) {
      return false;
    }

    int stackStartPos = mData.position();

    method.classNameIndex = mData.getShort() & 0xffff;
    method.nameIndex = mData.getShort() & 0xffff;

    method.sourceIndex = mData.getShort() & 0xffff;
    method.lineNumber = mData.getShort() & 0xffff;

    mData.position(stackStartPos + getHeader().stackFrameLen);

    mNumStackFramesRead++;
    return true;
  }

  @Nonnull
  public Header getHeader() {
    return getEagerlyLoadedData().header;
  }

  public String getClassName(int index) {
    return getEagerlyLoadedData().classNames[index];
  }

  public String getPrettyClassName(int index) {
    return formatClassName(getClassName(index));
  }

  private static String formatClassName(String raw) {
    StringBuilder pretty = new StringBuilder();

    int array = 0;
    int offset = 0;
    while (raw.charAt(offset) == '[') {
      offset++;
      array++;
    }

    int len = raw.length() - offset;

    // Handle fully qualified class name
    if (len >= 2 && raw.charAt(offset) == 'L' && raw.charAt(offset + len - 1) == ';') {
      pretty.append(raw.substring(offset + 1, offset + len - 1).replace('/', '.'));
    } else {
      String typeName = formatPrimitiveType(raw.charAt(offset));
      if (typeName != null) {
        pretty.append(typeName);
      } else {
        // Not sure what this is, just append it...
        pretty.append(raw.substring(offset));
      }
    }

    while (array-- > 0) {
      pretty.append("[]");
    }

    return pretty.toString();
  }

  @Nullable
  private static String formatPrimitiveType(char typeLabel) {
    switch (typeLabel) {
      case 'C': return "char";
      case 'B': return "byte";
      case 'Z': return "boolean";
      case 'S': return "short";
      case 'I': return "int";
      case 'J': return "long"; // not a typo, J is long :)
      case 'F': return "float";
      case 'D': return "double";
      default:
        return null;
    }
  }

  public String getMethodName(int index) {
    return getEagerlyLoadedData().methodNames[index];
  }

  public String getSourceName(int index) {
    return getEagerlyLoadedData().sourceNames[index];
  }

  @Nonnull
  private EagerlyLoadedData getEagerlyLoadedData() {
    if (mEagerlyLoadedData == null) {
      mEagerlyLoadedData = readEagerlyLoadedData(mData);
    }
    return mEagerlyLoadedData;
  }

  private static EagerlyLoadedData readEagerlyLoadedData(ByteBuffer data) {
    EagerlyLoadedData holder = new EagerlyLoadedData();
    Header header = new Header();
    holder.header = header;

    data.position(0);
    readHeader(header, data);

    holder.classNames = new String[header.numClassNames];
    holder.methodNames = new String[header.numMethodNames];
    holder.sourceNames = new String[header.numSourceNames];

    data.position(header.stringsOffset);
    readStrings(holder.classNames, data);
    readStrings(holder.methodNames, data);
    readStrings(holder.sourceNames, data);

    return holder;
  }

  private static void readHeader(Header header, ByteBuffer data) {
    header.headerLen = data.get() & 0xff;
    header.entryHeaderLen = data.get() & 0xff;
    header.stackFrameLen = data.get() & 0xff;
    header.numAllocs = data.getShort() & 0xffff;
    header.stringsOffset = data.getInt();
    header.numClassNames = data.getShort() & 0xffff;
    header.numMethodNames = data.getShort() & 0xffff;
    header.numSourceNames = data.getShort() & 0xffff;
  }

  private static void readStrings(String[] strings, ByteBuffer data) {
    try {
      for (int i = 0; i < strings.length; i++) {
        int strLength = data.getInt();
        int byteLength = strLength * 2;
        strings[i] = new String(
            data.array(),
            data.arrayOffset() + data.position(),
            byteLength,
            "UTF-16");
        data.position(data.position() + byteLength);
      }
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
  }

  private static class EagerlyLoadedData {
    public Header header;
    public String[] classNames;
    public String[] methodNames;
    public String[] sourceNames;
  }

  public static class Header {
    public int headerLen;
    public int entryHeaderLen;
    public int stackFrameLen;
    public int numAllocs;
    public int stringsOffset;
    public int numClassNames;
    public int numMethodNames;
    public int numSourceNames;
  }

  public static class Allocation {
    public int size;
    public int threadId;
    public int classIndex;
    public int stackDepth;
  }

  public static class Method {
    public int classNameIndex;
    public int nameIndex;
    public int sourceIndex;
    public int lineNumber;
  }
}
