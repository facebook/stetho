package org.apache.harmony.dalvik.ddmc;

// Linker stub for:
// https://android.googlesource.com/platform/libcore/+/android-5.1.0_r1/dalvik/src/main/java/org/apache/harmony/dalvik/ddmc/DdmVmInternal.java
public class DdmVmInternal {
    native public static void enableRecentAllocations(boolean enable);
    native public static boolean getRecentAllocationStatus();
    native public static byte[] getRecentAllocations();
}
