/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.protocol.module;

import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcResult;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsMethod;
import com.facebook.stetho.json.annotation.JsonProperty;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Target domain for Chrome 120+ compatibility.
 * Provides minimal implementation to satisfy Chrome's requirements for multi-target debugging.
 * 
 * This domain allows Chrome DevTools to discover and attach to debugging targets.
 * For Android apps, we typically have a single target representing the app itself.
 */
public class Target implements ChromeDevtoolsDomain {
    
    public Target() {
    }

    /**
     * Controls whether to discover available targets and filter them by type.
     * Chrome 120+ requires this for proper connection establishment.
     */
    @ChromeDevtoolsMethod
    public void setDiscoverTargets(JsonRpcPeer peer, JSONObject params) {
        // Minimal implementation - just acknowledge the request
        // Chrome 120+ requires this method to be present for proper connection
        // We don't need to actually implement target discovery for single-app debugging
    }

    /**
     * Retrieves a list of available targets.
     * Returns a single target representing this Android app.
     */
    @ChromeDevtoolsMethod
    public JsonRpcResult getTargets(JsonRpcPeer peer, JSONObject params) {
        GetTargetsResult result = new GetTargetsResult();
        result.targetInfos = new ArrayList<>();
        
        // Return single target representing this app
        TargetInfo info = new TargetInfo();
        info.targetId = "target-1";
        info.type = "page";
        info.title = "Android App";
        info.url = "android://";
        info.attached = true;
        
        result.targetInfos.add(info);
        return result;
    }

    /**
     * Attaches to the target with given id.
     * For single-app debugging, we're always attached.
     */
    @ChromeDevtoolsMethod
    public JsonRpcResult attachToTarget(JsonRpcPeer peer, JSONObject params) {
        AttachToTargetResult result = new AttachToTargetResult();
        result.sessionId = "session-1";
        return result;
    }

    /**
     * Detaches session with given id.
     * Minimal implementation for protocol compliance.
     */
    @ChromeDevtoolsMethod
    public void detachFromTarget(JsonRpcPeer peer, JSONObject params) {
        // Minimal implementation - just acknowledge
    }

    /**
     * Result type for getTargets method.
     */
    public static class GetTargetsResult implements JsonRpcResult {
        @JsonProperty(required = true)
        public List<TargetInfo> targetInfos;
    }

    /**
     * Information about a debugging target.
     */
    public static class TargetInfo {
        @JsonProperty(required = true)
        public String targetId;
        
        @JsonProperty(required = true)
        public String type;
        
        @JsonProperty(required = true)
        public String title;
        
        @JsonProperty(required = true)
        public String url;
        
        @JsonProperty(required = true)
        public boolean attached;
    }

    /**
     * Result type for attachToTarget method.
     */
    public static class AttachToTargetResult implements JsonRpcResult {
        @JsonProperty(required = true)
        public String sessionId;
    }
}

