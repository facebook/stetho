package com.facebook.stetho.heap;

import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.facebook.stetho.common.LogUtil;
import com.facebook.stetho.common.ManagedIntArray;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Formatter for the Stetho native format known as "Grimsey".
 */
@NotThreadSafe
class GrimseyConverter {
  /**
   * DDMS captures limit the stack depth to 16 which means we have to apply a heuristic
   * to guess whether a particular stack is part of a larger one already partly known.  For
   * instance, suppose the max depth was 4 and we had a set of allocations that looked like:
   *
   * <pre>
   *   java.lang.String
   *     at Foo.formatDebugInfo
   *     at Foo.doMethodInternal
   *     at Foo.method
   *     at Program.processInput
   *
   *   java.lang.Integer
   *     at Bar.doMethodInternal
   *     at Bar.method
   *     at Program.processInput
   *     at Program.main
   *
   *   java.util.ArrayList
   *     at StringBuilder.toString
   *     at StringBuilderUtil.format
   *     at Foo.formatDebugInfo
   *     at Foo.doMethodInternal
   * </pre>
   *
   * A human interpreting this can easily assume that the tree should look like:
   *
   * <pre>
   *   Program.main
   *     Program.processInput
   *       Foo.method
   *         Foo.doMethodInternal
   *           Foo.formatDebugInfo [java.lang.String]
   *             StringBuilderUtil.format
   *               StringBuilder.toString [java.util.ArrayList]
   *       Bar.method
   *         Bar.doMethodInternal [java.lang.Integer]
   * </pre>
   *
   * Our algorithm takes a pessimistic approach and assumes that any caller of the outermost
   * frame can be a possible caller and therefore may write ambiguous information in the
   * resulting format.  It's necessary that the visualizer disambiguates accordingly.
   */
  private static final int DDMS_MAX_STACK_DEPTH = 16;

  private static final int FORMAT_MAGIC = 0x12131415;
  private static final int FORMAT_VERSION = 1;

  private final CustomCodedOutputStream mOut;

  public GrimseyConverter(OutputStream out) {
    if (!(out instanceof BufferedOutputStream)) {
      out = new BufferedOutputStream(out, 2048);
    }
    mOut = new CustomCodedOutputStream(out);
  }

  /**
   * Write allocations from a raw DDMS data segment.  This class is responsible for both
   * parsing and converting due to the fact that the parser can be greatly optimized
   * when translating to the Grimsey format.
   */
  public void writeDDMSData(byte[] rawData) throws IOException {
    DdmAllocationParser parser = new DdmAllocationParser(ByteBuffer.wrap(rawData));

    DdmAllocationParser.Header header = parser.getHeader();
    boolean[] knownClasses = new boolean[header.numClassNames];
    HashMap<MethodKey, Integer> knownMethods = new HashMap<>(header.numMethodNames);
    SparseArray<SparseBooleanArray> knownCallersByMethod = new SparseArray<>();

    MethodKey scratchMethodKey = new MethodKey();
    ManagedIntArray scratchStack = new ManagedIntArray(64);

    writeHeader();

    DdmAllocationParser.Allocation scratchAllocation = new DdmAllocationParser.Allocation();
    DdmAllocationParser.Method scratchMethod = new DdmAllocationParser.Method();
    while (parser.readNextAllocation(scratchAllocation)) {
      writeClassDescriptorIfNew(parser, knownClasses, scratchAllocation.classIndex);

      scratchStack.clear();
      scratchStack.ensureCapacity(scratchAllocation.stackDepth);

      // Stack traversal is from inner-most frames to outer-most.  That is, the 0th stack
      // frame is the one which performed the allocation.
      while (parser.readNextStackFrame(scratchMethod)) {
        scratchMethodKey.set(scratchMethod);

        writeClassDescriptorIfNew(parser, knownClasses, scratchMethod.classNameIndex);

        Integer methodId = knownMethods.get(scratchMethodKey);
        if (methodId == null) {
          methodId = knownMethods.size();
          StringBuilder methodName = new StringBuilder();
          methodName.append(parser.getMethodName(scratchMethod.nameIndex));
          methodName.append('(');
          methodName.append(parser.getSourceName(scratchMethod.sourceIndex));
          methodName.append(':');
          methodName.append(scratchMethod.lineNumber);
          methodName.append(')');
          writeMethodDescriptor(
              methodId,
              scratchMethod.classNameIndex,
              methodName.toString());
          knownMethods.put(new MethodKey(scratchMethodKey), methodId);
        }

        scratchStack.add(methodId);
      }

      for (int i = 0; i < scratchAllocation.stackDepth - 1; i++) {
        int methodId = scratchStack.get(i);
        int callerMethodId = scratchStack.get(i + 1);
        SparseBooleanArray callers = knownCallersByMethod.get(methodId);
        if (callers == null) {
          callers = new SparseBooleanArray(8);
          knownCallersByMethod.put(methodId, callers);
        }
        callers.put(callerMethodId, true);
      }
    }

    HashMap<ManagedIntArray, Integer> knownStacks = new HashMap<>();
    ManagedIntArray scratchFullStack = new ManagedIntArray(64);

    parser.resetAllocationOffset();

    // Loop again now that we can reconstruct full call stacks
    while (parser.readNextAllocation(scratchAllocation)) {
      scratchStack.clear();
      scratchStack.ensureCapacity(scratchAllocation.stackDepth);

      while (parser.readNextStackFrame(scratchMethod)) {
        scratchMethodKey.set(scratchMethod);
        scratchStack.add(knownMethods.get(scratchMethodKey));
      }

      // Get the outer-most known frame and work back from there to try to reconstruct
      // the full stack.
      Integer stackId = knownStacks.get(scratchStack);
      if (stackId == null) {
        stackId = knownStacks.size();
        knownStacks.put(new ManagedIntArray(scratchStack), stackId);

        if (scratchStack.size() < DDMS_MAX_STACK_DEPTH) {
          writeStackDescriptor(stackId, scratchStack);
        } else {
          scratchFullStack.clear();
          scratchFullStack.ensureCapacity(scratchStack.size() + 24);
          for (int i = 0; i < scratchStack.size(); i++) {
            scratchFullStack.add(scratchStack.get(i));
          }

          int outermostMethodId = scratchStack.get(scratchStack.size() - 1);
          SparseBooleanArray callers;
          while (true) {
            callers = knownCallersByMethod.get(outermostMethodId);
            if (callers == null || callers.size() == 0) {
              break;
            }

            int callersN = callers.size();
            if (callersN > 1) {
              // TODO: This is actually quite a serious limitation, though it is not
              // currently known to happen in practice.  Need more testing/verification...
              LogUtil.e("Ignoring multiple possible call stacks from: " + scratchStack);
              LogUtil.e(
                  " -> outermostMethodId=" + outermostMethodId +
                      "; callers=" + callers);
            }

            outermostMethodId = callers.keyAt(0);
            scratchFullStack.add(outermostMethodId);
          }

          writeStackDescriptor(stackId, scratchFullStack);
        }
      }

      writeAllocation(
          scratchAllocation.threadId,
          scratchAllocation.size,
          scratchAllocation.classIndex,
          0 /* timestamp */,
          stackId);
    }
  }

  public void close() throws IOException {
    Buffer.emptyPool();
    mOut.close();
  }

  private void writeClassDescriptorIfNew(
      DdmAllocationParser parser,
      boolean[] knownClasses,
      int classIndex)
      throws IOException {
    if (!knownClasses[classIndex]) {
      writeClassDescriptor(classIndex, parser.getPrettyClassName(classIndex));
      knownClasses[classIndex] = true;
    }
  }

  private void swapAndRelease(
      Buffer buffer,
      CustomCodedOutputStream out,
      SizeEncoding sizeEncoding) throws IOException {
    byte[] data = buffer.buf.leakByteArray();
    int size = buffer.buf.size();
    switch (sizeEncoding) {
      case PROTOBUF:
        out.writeRawVarint32(size);
        break;
      case GRIMSEY:
        out.writeRawLittleEndian32(size);
        break;
      default:
        throw new IllegalArgumentException("Unknown encoding " + sizeEncoding);
    }
    out.writeRawBytes(data, 0, size);
    buffer.release();
  }

  private void writeHeader() throws IOException {
    Buffer buf = Buffer.acquire();
    buf.out.writeUInt32(1 /* magic */, FORMAT_MAGIC);
    buf.out.writeUInt32(2 /* format_version */, FORMAT_VERSION);
    swapAndRelease(buf, mOut, SizeEncoding.GRIMSEY);
  }

  private void writeClassDescriptor(int classId, String className) throws IOException {
    Buffer frame = Buffer.acquire();
    frame.out.writeTagLengthDelim(1 /* class_descriptor */);
    Buffer data = Buffer.acquire();
    data.out.writeUInt32(1 /* class_id */, classId);
    data.out.writeString(2 /* class_name */, className);
    swapAndRelease(data, frame.out, SizeEncoding.PROTOBUF);
    swapAndRelease(frame, mOut, SizeEncoding.GRIMSEY);
  }

  private void writeMethodDescriptor(int methodId, int classId, String methodName)
      throws IOException {
    Buffer frame = Buffer.acquire();
    frame.out.writeTagLengthDelim(2 /* method_descriptor */);
    Buffer data = Buffer.acquire();
    data.out.writeUInt32(1 /* method_id */, methodId);
    data.out.writeUInt32(2 /* class_id */, classId);
    data.out.writeString(3 /* method_name */, methodName);
    swapAndRelease(data, frame.out, SizeEncoding.PROTOBUF);
    swapAndRelease(frame, mOut, SizeEncoding.GRIMSEY);
  }

  private void writeStackDescriptor(int stackId, ManagedIntArray frames) throws IOException {
    Buffer frame = Buffer.acquire();
    frame.out.writeTagLengthDelim(3 /* stack_descriptor */);
    Buffer data = Buffer.acquire();
    data.out.writeUInt32(1 /* stack_id */, stackId);
    int N = frames.size();
    for (int i = 0; i < N; i++) {
      data.out.writeUInt32(2 /* method_id */, frames.get(i));
    }
    swapAndRelease(data, frame.out, SizeEncoding.PROTOBUF);
    swapAndRelease(frame, mOut, SizeEncoding.GRIMSEY);
  }

  private void writeAllocation(
      int threadId,
      int size,
      int classId,
      int timestamp,
      int stackId) throws IOException {
    Buffer frame = Buffer.acquire();
    frame.out.writeTagLengthDelim(4 /* allocation */);
    Buffer data = Buffer.acquire();
    data.out.writeUInt32(1 /* thread_id */, threadId);
    data.out.writeUInt32(2 /* size */, size);
    data.out.writeUInt32(3 /* class_id */, classId);
    data.out.writeUInt32(4 /* timestamp */, timestamp);
    data.out.writeUInt32(5 /* stack_id */, stackId);
    swapAndRelease(data, frame.out, SizeEncoding.PROTOBUF);
    swapAndRelease(frame, mOut, SizeEncoding.GRIMSEY);
  }

  private static class StackDescriptor {
    public final int stackId;
    public final ArrayList<Integer> methods;

    public StackDescriptor(int stackId, ArrayList<Integer> methods) {
      this.stackId = stackId;
      this.methods = methods;
    }
  }

  @NotThreadSafe
  private static class MethodKey {
    public int classIndex;
    public int nameIndex;
    public int filenameIndex;
    public int lineNumber;

    public MethodKey() {
    }

    public MethodKey(MethodKey other) {
      set(other.classIndex, other.nameIndex, other.filenameIndex, other.lineNumber);
    }

    public void set(DdmAllocationParser.Method method) {
      set(method.classNameIndex, method.nameIndex, method.sourceIndex, method.lineNumber);
    }

    public void set(int classIndex, int nameIndex, int filenameIndex, int lineNumber) {
      this.classIndex = classIndex;
      this.nameIndex = nameIndex;
      this.filenameIndex = filenameIndex;
      this.lineNumber = lineNumber;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      MethodKey methodKey = (MethodKey) o;

      if (classIndex != methodKey.classIndex) return false;
      if (nameIndex != methodKey.nameIndex) return false;
      if (filenameIndex != methodKey.filenameIndex) return false;
      return lineNumber == methodKey.lineNumber;
    }

    @Override
    public int hashCode() {
      int result = classIndex;
      result = 31 * result + nameIndex;
      result = 31 * result + filenameIndex;
      result = 31 * result + lineNumber;
      return result;
    }
  }

  private enum SizeEncoding {
    PROTOBUF,
    GRIMSEY,
  }

  private static class CustomCodedOutputStream {
    private final OutputStream mOut;

    public CustomCodedOutputStream(OutputStream out) {
      mOut = out;
    }

    public void writeUInt32(int fieldNumber, int value) throws IOException {
      writeTagVarint(fieldNumber);
      writeRawVarint32(value);
    }

    public void writeString(int fieldNumber, String value) throws IOException {
      writeTagLengthDelim(fieldNumber);
      byte[] valueBytes = value.getBytes("UTF-8");
      writeRawVarint32(valueBytes.length);
      writeRawBytes(valueBytes);
    }

    public void writeTagVarint(int fieldNumber) throws IOException {
      writeRawVarint32(makeTag(fieldNumber, 0 /* WIRETYPE_VARINT */));
    }

    public void writeTagLengthDelim(int fieldNumber) throws IOException {
      writeRawVarint32(makeTag(fieldNumber, 2 /* WIRETYPE_LENGTH_DELIMITED */));
    }

    public static int makeTag(int fieldNumber, int wireType) {
      return (fieldNumber << 3) | wireType;
    }

    public void writeRawVarint32(int value) throws IOException {
      while (true) {
        if ((value & ~0x7F) == 0) {
          writeRawByte(value);
          return;
        } else {
          writeRawByte((value & 0x7F) | 0x80);
          value >>>= 7;
        }
      }
    }

    public void writeRawLittleEndian32(int value) throws IOException {
      writeRawByte(value & 0xff);
      writeRawByte((value >> 8) & 0xff);
      writeRawByte((value >> 16) & 0xff);
      writeRawByte((value >> 24) & 0xff);
    }

    public void writeRawByte(int value) throws IOException {
      mOut.write(value);
    }

    public void writeRawBytes(byte[] value) throws IOException {
      mOut.write(value);
    }

    public void writeRawBytes(byte[] value, int offset, int length) throws IOException {
      mOut.write(value, offset, length);
    }

    public void close() throws IOException {
      mOut.close();
    }
  }

  private static class LeakyByteArrayOutputStream extends ByteArrayOutputStream {
    public byte[] leakByteArray() {
      return buf;
    }
  }

  @NotThreadSafe
  private static class Buffer {
    @Nullable private static Buffer head;
    @Nullable private Buffer next;

    public final LeakyByteArrayOutputStream buf;
    public final CustomCodedOutputStream out;

    public static Buffer acquire() {
      Buffer prevHead = head;
      if (head != null) {
        head = head.next;
        return prevHead;
      } else {
        return new Buffer();
      }
    }

    public static void emptyPool() {
      head = null;
    }

    public Buffer() {
      buf = new LeakyByteArrayOutputStream();
      out = new CustomCodedOutputStream(buf);
    }

    public void release() {
      buf.reset();

      next = head;
      head = this;
    }
  }
}
