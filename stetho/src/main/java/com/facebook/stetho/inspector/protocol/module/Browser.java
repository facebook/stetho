/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.protocol.module;

import android.os.Build;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcResult;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsMethod;
import com.facebook.stetho.json.annotation.JsonProperty;
import org.json.JSONObject;

/**
 * Browser domain for Chrome 120+ compatibility.
 * 
 * The Browser domain defines methods and events for browser managing.
 * This implementation provides version information and basic browser-level operations
 * required by Chrome 120+ DevTools.
 */
public class Browser implements ChromeDevtoolsDomain {
    
    private static final String PROTOCOL_VERSION = "1.3";
    private static final String PRODUCT = "Chrome/120.0.0.0";
    private static final String USER_AGENT = "Stetho";
    
    public Browser() {
    }

    /**
     * Returns version information about the browser/runtime.
     * Chrome 120+ uses this to verify protocol compatibility.
     */
    @ChromeDevtoolsMethod
    public JsonRpcResult getVersion(JsonRpcPeer peer, JSONObject params) {
        GetVersionResult result = new GetVersionResult();
        result.protocolVersion = PROTOCOL_VERSION;
        result.product = PRODUCT + " Android/" + Build.VERSION.RELEASE;
        result.revision = "@" + Build.ID;
        result.userAgent = USER_AGENT;
        result.jsVersion = "12.0.267.8";  // V8 version for Chrome 120
        return result;
    }

    /**
     * Close browser gracefully.
     * Minimal implementation for protocol compliance.
     */
    @ChromeDevtoolsMethod
    public void close(JsonRpcPeer peer, JSONObject params) {
        // Minimal implementation - we don't actually close the app
        // This is here for protocol compliance
    }

    /**
     * Returns the command line switches for the browser process if, and only if
     * --enable-automation is on the commandline.
     * Minimal implementation for protocol compliance.
     */
    @ChromeDevtoolsMethod
    public JsonRpcResult getCommandLine(JsonRpcPeer peer, JSONObject params) {
        GetCommandLineResult result = new GetCommandLineResult();
        result.arguments = new String[0];  // No command line arguments for Android
        return result;
    }

    /**
     * Result type for getVersion method.
     */
    public static class GetVersionResult implements JsonRpcResult {
        @JsonProperty(required = true)
        public String protocolVersion;
        
        @JsonProperty(required = true)
        public String product;
        
        @JsonProperty(required = true)
        public String revision;
        
        @JsonProperty(required = true)
        public String userAgent;
        
        @JsonProperty(required = true)
        public String jsVersion;
    }

    /**
     * Result type for getCommandLine method.
     */
    public static class GetCommandLineResult implements JsonRpcResult {
        @JsonProperty(required = true)
        public String[] arguments;
    }
}

