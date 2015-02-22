// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.protocol.module;

import android.content.Context;
import android.content.SharedPreferences;
import com.facebook.stetho.common.LogUtil;
import com.facebook.stetho.inspector.domstorage.DOMStoragePeerManager;
import com.facebook.stetho.inspector.domstorage.SharedPreferencesHelper;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcResult;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsMethod;

import com.facebook.stetho.json.ObjectMapper;
import com.facebook.stetho.json.annotation.JsonProperty;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DOMStorage implements ChromeDevtoolsDomain {
  private final Context mContext;
  private final DOMStoragePeerManager mDOMStoragePeerManager;
  private final ObjectMapper mObjectMapper = new ObjectMapper();

  public DOMStorage(Context context) {
    mContext = context;
    mDOMStoragePeerManager = new DOMStoragePeerManager(context);
  }

  @ChromeDevtoolsMethod
  public void enable(JsonRpcPeer peer, JSONObject params) {
    mDOMStoragePeerManager.addPeer(peer);
  }

  @ChromeDevtoolsMethod
  public void disable(JsonRpcPeer peer, JSONObject params) {
    mDOMStoragePeerManager.removePeer(peer);
  }

  @ChromeDevtoolsMethod
  public JsonRpcResult getDOMStorageItems(JsonRpcPeer peer, JSONObject params)
      throws JSONException {
    StorageId storage = mObjectMapper.convertValue(
        params.getJSONObject("storageId"),
        StorageId.class);

    ArrayList<List<String>> entries = new ArrayList<List<String>>();
    String prefTag = storage.securityOrigin;
    if (storage.isLocalStorage) {
      SharedPreferences prefs = mContext.getSharedPreferences(prefTag, Context.MODE_PRIVATE);
      for (Map.Entry<String, ?> prefsEntry : prefs.getAll().entrySet()) {
        ArrayList<String> entry = new ArrayList<String>(2);
        entry.add(prefsEntry.getKey());
        entry.add(SharedPreferencesHelper.valueToString(prefsEntry.getValue()));
        entries.add(entry);
      }
    }

    GetDOMStorageItemsResult result = new GetDOMStorageItemsResult();
    result.entries = entries;
    return result;
  }

  @ChromeDevtoolsMethod
  public void setDOMStorageItem(JsonRpcPeer peer, JSONObject params) throws JSONException {
    StorageId storage = mObjectMapper.convertValue(
        params.getJSONObject("storageId"),
        StorageId.class);
    String key = params.getString("key");
    String value = params.getString("value");

    if (storage.isLocalStorage) {
      SharedPreferences prefs = mContext.getSharedPreferences(
          storage.securityOrigin,
          Context.MODE_PRIVATE);
      Object exitingValue = prefs.getAll().get(key);
      SharedPreferences.Editor editor = prefs.edit();
      if (!tryAssignByType(editor, key, value, exitingValue)) {
        editor.putString(key, value);
      }
      editor.apply();
    }
  }

  @ChromeDevtoolsMethod
  public void removeDOMStorageItem(JsonRpcPeer peer, JSONObject params) throws JSONException {
    StorageId storage = mObjectMapper.convertValue(
        params.getJSONObject("storageId"),
        StorageId.class);
    String key = params.getString("key");

    if (storage.isLocalStorage) {
      SharedPreferences prefs = mContext.getSharedPreferences(
          storage.securityOrigin,
          Context.MODE_PRIVATE);
      prefs.edit().remove(key).apply();
    }
  }

  private static boolean tryAssignByType(
      SharedPreferences.Editor editor,
      String key,
      String value,
      @Nullable Object existingValue) {
    try {
      if (existingValue instanceof Integer) {
        editor.putInt(key, Integer.parseInt(value));
        return true;
      } else if (existingValue instanceof Long) {
        editor.putLong(key, Long.parseLong(value));
        return true;
      } else if (existingValue instanceof Float) {
        editor.putFloat(key, Float.parseFloat(value));
        return true;
      } else if (existingValue instanceof Boolean) {
        editor.putBoolean(key, parseBoolean(value));
        return true;
      }
    } catch (NumberFormatException e) {
      // Fall through...
    }
    return false;
  }

  private static Boolean parseBoolean(String s) {
    if ("1".equals(s) || "true".equalsIgnoreCase(s)) {
      return Boolean.TRUE;
    } else if ("0".equals(s) || "false".equalsIgnoreCase(s)) {
      return Boolean.FALSE;
    }
    return null;
  }

  public static class StorageId {
    @JsonProperty(required = true)
    public String securityOrigin;

    @JsonProperty(required = true)
    public boolean isLocalStorage;
  }

  private static class GetDOMStorageItemsResult implements JsonRpcResult {
    @JsonProperty(required = true)
    public List<List<String>> entries;
  }

  public static class DomStorageItemsClearedParams {
    @JsonProperty(required = true)
    public StorageId storageId;
  }

  public static class DomStorageItemRemovedParams {
    @JsonProperty(required = true)
    public StorageId storageId;

    @JsonProperty(required = true)
    public String key;
  }

  public static class DomStorageItemAddedParams {
    @JsonProperty(required = true)
    public StorageId storageId;

    @JsonProperty(required = true)
    public String key;

    @JsonProperty(required = true)
    public String newValue;
  }

  public static class DomStorageItemUpdatedParams {
    @JsonProperty(required = true)
    public StorageId storageId;

    @JsonProperty(required = true)
    public String key;

    @JsonProperty(required = true)
    public String oldValue;

    @JsonProperty(required = true)
    public String newValue;
  }
}
