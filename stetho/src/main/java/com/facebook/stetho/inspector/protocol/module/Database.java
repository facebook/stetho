/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.inspector.protocol.module;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.util.SparseArray;

import com.facebook.stetho.common.LogUtil;
import com.facebook.stetho.common.Util;
import com.facebook.stetho.inspector.helper.ChromePeerManager;
import com.facebook.stetho.inspector.helper.ObjectIdMapper;
import com.facebook.stetho.inspector.helper.PeersRegisteredListener;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcException;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcResult;
import com.facebook.stetho.inspector.jsonrpc.protocol.JsonRpcError;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsMethod;
import com.facebook.stetho.json.ObjectMapper;
import com.facebook.stetho.json.annotation.JsonProperty;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

public class Database implements ChromeDevtoolsDomain {
  /**
   * The protocol doesn't offer an efficient means of pagination or anything like that so
   * we'll just cap the result list to some arbitrarily large number that I think folks will
   * actually need in practice.
   * <p>
   * Note that when this limit is exceeded, a dummy row will be introduced that indicates
   * truncation occurred.
   */
  private static final int MAX_EXECUTE_RESULTS = 250;

  /**
   * Maximum length of a BLOB field before we stop trying to interpret it and just
   * return {@link #UNKNOWN_BLOB_LABEL}
   */
  private static final int MAX_BLOB_LENGTH = 512;

  /**
   * Label to use when a BLOB column cannot be converted to a string.
   */
  private static final String UNKNOWN_BLOB_LABEL = "{blob}";

  private List<DatabaseDriver2> mDatabaseDrivers;
  private final ChromePeerManager mChromePeerManager;
  private final DatabasePeerRegistrationListener mPeerListener;
  private final ObjectMapper mObjectMapper;

  /**
   * Constructs the object.
   */
  public Database() {
    mDatabaseDrivers = new ArrayList<>();
    mChromePeerManager = new ChromePeerManager();
    mPeerListener = new DatabasePeerRegistrationListener(mDatabaseDrivers);
    mChromePeerManager.setListener(mPeerListener);
    mObjectMapper = new ObjectMapper();
  }

  public void add(DatabaseDriver2 databaseDriver) {
    mDatabaseDrivers.add(databaseDriver);
  }

  @ChromeDevtoolsMethod
  public void enable(JsonRpcPeer peer, JSONObject params) {
    mChromePeerManager.addPeer(peer);
  }

  @ChromeDevtoolsMethod
  public void disable(JsonRpcPeer peer, JSONObject params) {
    mChromePeerManager.removePeer(peer);
  }

  @ChromeDevtoolsMethod
  public JsonRpcResult getDatabaseTableNames(JsonRpcPeer peer, JSONObject params)
      throws JsonRpcException {
    GetDatabaseTableNamesRequest request = mObjectMapper.convertValue(params,
        GetDatabaseTableNamesRequest.class);

    String databaseId = request.databaseId;
    DatabaseDescriptorHolder holder =
        mPeerListener.getDatabaseDescriptorHolder(databaseId);

    try {
      GetDatabaseTableNamesResponse response = new GetDatabaseTableNamesResponse();
      response.tableNames = holder.driver.getTableNames(holder.descriptor);
      return response;
    } catch (SQLiteException e) {
      throw new JsonRpcException(
          new JsonRpcError(
              JsonRpcError.ErrorCode.INVALID_REQUEST,
              e.toString(),
              null /* data */));
    }
  }

  @ChromeDevtoolsMethod
  public JsonRpcResult executeSQL(JsonRpcPeer peer, JSONObject params) {
    ExecuteSQLRequest request = mObjectMapper.convertValue(params,
        ExecuteSQLRequest.class);

    DatabaseDescriptorHolder holder =
        mPeerListener.getDatabaseDescriptorHolder(request.databaseId);

    try {
      return holder.driver.executeSQL(
          holder.descriptor,
          request.query,
          new DatabaseDriver.ExecuteResultHandler<ExecuteSQLResponse>() {
        @Override
        public ExecuteSQLResponse handleRawQuery() throws SQLiteException {
          ExecuteSQLResponse response = new ExecuteSQLResponse();
          // This is done because the inspector UI likes to delete rows if you give them no
          // name/value list
          response.columnNames = Collections.singletonList("success");
          response.values = Collections.singletonList("true");
          return response;
        }

        @Override
        public ExecuteSQLResponse handleSelect(Cursor result) throws SQLiteException {
          ExecuteSQLResponse response = new ExecuteSQLResponse();
          response.columnNames = Arrays.asList(result.getColumnNames());
          response.values = flattenRows(result, MAX_EXECUTE_RESULTS);
          return response;
        }

        @Override
        public ExecuteSQLResponse handleInsert(long insertedId) throws SQLiteException {
          ExecuteSQLResponse response = new ExecuteSQLResponse();
          response.columnNames = Collections.singletonList("ID of last inserted row");
          response.values = Collections.singletonList(String.valueOf(insertedId));
          return response;
        }

        @Override
        public ExecuteSQLResponse handleUpdateDelete(int count) throws SQLiteException {
          ExecuteSQLResponse response = new ExecuteSQLResponse();
          response.columnNames = Collections.singletonList("Modified rows");
          response.values = Collections.singletonList(String.valueOf(count));
          return response;
        }
      });
    } catch (RuntimeException e) {
      LogUtil.e(e, "Exception executing: %s", request.query);

      Error error = new Error();
      error.code = 0;
      error.message = e.getMessage();
      ExecuteSQLResponse response = new ExecuteSQLResponse();
      response.sqlError = error;
      return response;
    }
  }

  /**
   * Flatten all columns and all rows of a cursor to a single array.  The array cannot be
   * interpreted meaningfully without the number of columns.
   *
   * @param cursor
   * @param limit Maximum number of rows to process.
   * @return List of Java primitives matching the value type of each column, converted to
   *      strings.
   */
  private static ArrayList<String> flattenRows(Cursor cursor, int limit) {
    Util.throwIfNot(limit >= 0);
    ArrayList<String> flatList = new ArrayList<>();
    final int numColumns = cursor.getColumnCount();
    for (int row = 0; row < limit && cursor.moveToNext(); row++) {
      for (int column = 0; column < numColumns; column++) {
        switch (cursor.getType(column)) {
          case Cursor.FIELD_TYPE_NULL:
            flatList.add(null);
            break;
          case Cursor.FIELD_TYPE_INTEGER:
            flatList.add(String.valueOf(cursor.getLong(column)));
            break;
          case Cursor.FIELD_TYPE_FLOAT:
            flatList.add(String.valueOf(cursor.getDouble(column)));
            break;
          case Cursor.FIELD_TYPE_BLOB:
            flatList.add(blobToString(cursor.getBlob(column)));
            break;
          case Cursor.FIELD_TYPE_STRING:
          default:
            flatList.add(cursor.getString(column));
            break;
        }
      }
    }
    if (!cursor.isAfterLast()) {
      for (int column = 0; column < numColumns; column++) {
        flatList.add("{truncated}");
      }
    }
    return flatList;
  }

  private static String blobToString(byte[] blob) {
    if (blob.length <= MAX_BLOB_LENGTH) {
      if (fastIsAscii(blob)) {
        try {
          return new String(blob, "US-ASCII");
        } catch (UnsupportedEncodingException e) {
          // Fall through...
        }
      }
    }
    return UNKNOWN_BLOB_LABEL;
  }

  private static boolean fastIsAscii(byte[] blob) {
    for (byte b : blob) {
      if ((b & ~0x7f) != 0) {
        return false;
      }
    }
    return true;
  }

  @ThreadSafe
  private static class DatabasePeerRegistrationListener extends PeersRegisteredListener {
    private final List<DatabaseDriver2> mDatabaseDrivers;

    @GuardedBy("this")
    private final SparseArray<DatabaseDescriptorHolder> mDatabaseHolders = new SparseArray<>();

    @GuardedBy("this")
    private final ObjectIdMapper mDatabaseIdMapper = new ObjectIdMapper();

    private DatabasePeerRegistrationListener(List<DatabaseDriver2> databaseDrivers) {
      mDatabaseDrivers = databaseDrivers;
    }

    public DatabaseDescriptorHolder getDatabaseDescriptorHolder(String databaseId) {
      return mDatabaseHolders.get(Integer.parseInt(databaseId));
    }

    @Override
    protected synchronized void onFirstPeerRegistered() {
      for (DatabaseDriver2<?> driver : mDatabaseDrivers) {
        for (DatabaseDescriptor desc : driver.getDatabaseNames()) {
          Integer databaseId = mDatabaseIdMapper.getIdForObject(desc);
          if (databaseId == null) {
            databaseId = mDatabaseIdMapper.putObject(desc);
            mDatabaseHolders.put(
                databaseId,
                new DatabaseDescriptorHolder(driver, desc));
          }
        }
      }
    }

    @Override
    protected synchronized void onLastPeerUnregistered() {
      mDatabaseIdMapper.clear();
      mDatabaseHolders.clear();
    }

    @Override
    protected synchronized void onPeerAdded(JsonRpcPeer peer) {
      for (int i = 0, N = mDatabaseHolders.size(); i < N; i++) {
        int id = mDatabaseHolders.keyAt(i);
        DatabaseDescriptorHolder holder = mDatabaseHolders.valueAt(i);

        Database.DatabaseObject databaseParams = new Database.DatabaseObject();
        databaseParams.id = String.valueOf(id);
        databaseParams.name = holder.descriptor.name();
        databaseParams.domain = holder.driver.getContext().getPackageName();
        databaseParams.version = "N/A";
        Database.AddDatabaseEvent eventParams = new Database.AddDatabaseEvent();
        eventParams.database = databaseParams;
        peer.invokeMethod("Database.addDatabase", eventParams, null /* callback */);
      }
    }

    @Override
    protected synchronized void onPeerRemoved(JsonRpcPeer peer) {
      // Nothing to do on each peer removal...
    }
  }

  private static class DatabaseDescriptorHolder {
    public final DatabaseDriver2 driver;
    public final DatabaseDescriptor descriptor;

    public DatabaseDescriptorHolder(DatabaseDriver2 driver, DatabaseDescriptor descriptor) {
      this.driver = driver;
      this.descriptor = descriptor;
    }
  }

  private static class GetDatabaseTableNamesRequest {
    @JsonProperty(required = true)
    public String databaseId;
  }

  private static class GetDatabaseTableNamesResponse implements JsonRpcResult {
    @JsonProperty(required = true)
    public List<String> tableNames;
  }

  public static class ExecuteSQLRequest {
    @JsonProperty(required = true)
    public String databaseId;

    @JsonProperty(required = true)
    public String query;
  }

  public static class ExecuteSQLResponse implements JsonRpcResult {
    @JsonProperty
    public List<String> columnNames;

    @JsonProperty
    public List<String> values;

    @JsonProperty
    public Error sqlError;
  }

  public static class AddDatabaseEvent {
    @JsonProperty(required = true)
    public DatabaseObject database;
  }

  public static class DatabaseObject {
    @JsonProperty(required = true)
    public String id;

    @JsonProperty(required = true)
    public String domain;

    @JsonProperty(required = true)
    public String name;

    @JsonProperty(required = true)
    public String version;
  }

  public static class Error {
    @JsonProperty(required = true)
    public String message;

    @JsonProperty(required = true)
    public int code;
  }

  /**
   * @deprecated Use {@link DatabaseDriver2} which allows for structured identifiers of database
   *     objects (such as a file path instead of just a string name) which also serves as a
   *     namespacing separation of multiple drivers.
   */
  @Deprecated
  public static abstract class DatabaseDriver extends BaseDatabaseDriver<String> {
    public DatabaseDriver(Context context) {
      super(context);
    }
  }
}
