package com.facebook.stetho.inspector.domstorage;

import android.content.Context;
import android.content.SharedPreferences;
import com.facebook.stetho.inspector.helper.ChromePeerManager;
import com.facebook.stetho.inspector.helper.PeerRegistrationListener;
import com.facebook.stetho.inspector.helper.PeersRegisteredListener;
import com.facebook.stetho.inspector.protocol.module.DOMStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DOMStoragePeerManager extends ChromePeerManager {
  private final Context mContext;

  public DOMStoragePeerManager(Context context) {
    mContext = context;
    setListener(mPeerListener);
  }

  private final PeerRegistrationListener mPeerListener = new PeersRegisteredListener() {
    private final List<DevToolsSharedPreferencesListener> mPrefsListeners =
        new ArrayList<DevToolsSharedPreferencesListener>();

    @Override
    protected synchronized void onFirstPeerRegistered() {
      // TODO: We list the tags in Page.getResourceTree as well and those are the real fixed
      // tags that will be observed by the peer.  We can fix this by making the page frames
      // dynamically update in response to DOMStorage events.  This would also allow us to
      // add new SharedPreferences tags as we observe them being created by way of
      // android.os.FileObserver.
      List<String> tags = SharedPreferencesHelper.getSharedPreferenceTags(mContext);
      for (String tag : tags) {
        SharedPreferences prefs = mContext.getSharedPreferences(tag, Context.MODE_PRIVATE);
        DevToolsSharedPreferencesListener listener =
            new DevToolsSharedPreferencesListener(prefs, tag);
        prefs.registerOnSharedPreferenceChangeListener(listener);
        mPrefsListeners.add(listener);
      }
    }

    @Override
    protected synchronized void onLastPeerUnregistered() {
      for (DevToolsSharedPreferencesListener prefsListener : mPrefsListeners) {
        prefsListener.unregister();
      }
      mPrefsListeners.clear();
    }
  };

  private class DevToolsSharedPreferencesListener
      implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final SharedPreferences mPrefs;
    private final DOMStorage.StorageId mStorageId;

    public DevToolsSharedPreferencesListener(SharedPreferences prefs, String tag) {
      mPrefs = prefs;
      mStorageId = new DOMStorage.StorageId();
      mStorageId.securityOrigin = tag;
      mStorageId.isLocalStorage = true;
    }

    public void unregister() {
      mPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
      DOMStorage.DomStorageItemRemovedParams removedParams =
          new DOMStorage.DomStorageItemRemovedParams();
      removedParams.storageId = mStorageId;
      removedParams.key = key;
      sendNotificationToPeers("DOMStorage.domStorageItemRemoved", removedParams);

      Map<String, ?> entries = sharedPreferences.getAll();
      if (entries.containsKey(key)) {
        DOMStorage.DomStorageItemAddedParams addedParams =
            new DOMStorage.DomStorageItemAddedParams();
        addedParams.storageId = mStorageId;
        addedParams.key = key;
        addedParams.newValue = SharedPreferencesHelper.valueToString(entries.get(key));
        sendNotificationToPeers("DOMStorage.domStorageItemAdded", addedParams);
      }
    }
  }
}
