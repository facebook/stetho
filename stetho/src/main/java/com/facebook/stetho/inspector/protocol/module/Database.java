// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.stetho.inspector.protocol.module;

import com.facebook.stetho.inspector.database.DatabaseFilesProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.Build;

import com.facebook.stetho.common.Util;
import com.facebook.stetho.inspector.database.DatabasePeerManager;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcResult;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsMethod;
import com.facebook.stetho.json.ObjectMapper;
import com.facebook.stetho.json.annotation.JsonProperty;

import org.json.JSONObject;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
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

  private final DatabasePeerManager mDatabasePeerManager;
  private final ObjectMapper mObjectMapper;

  /**
   * Constructs the object with the default {@link DatabasePeerManager}.
   * @param context the context
   */
  @Deprecated
  public Database(Context context) {
    mDatabasePeerManager = new DatabasePeerManager(context);
    mObjectMapper = new ObjectMapper();
  }

  /**
   * @param context the context
   * @param databaseFilesProvider a database files provider
   */
  public Database(Context context, DatabaseFilesProvider databaseFilesProvider) {
    mDatabasePeerManager = new DatabasePeerManager(context, databaseFilesProvider);
    mObjectMapper = new ObjectMapper();
  }

  @ChromeDevtoolsMethod
  public void enable(JsonRpcPeer peer, JSONObject params) {
    mDatabasePeerManager.addPeer(peer);
  }

  @ChromeDevtoolsMethod
  public void disable(JsonRpcPeer peer, JSONObject params) {
    mDatabasePeerManager.removePeer(peer);
  }

  @ChromeDevtoolsMethod
  public JsonRpcResult getDatabaseTableNames(JsonRpcPeer peer, JSONObject params) {
    GetDatabaseTableNamesRequest request = mObjectMapper.convertValue(params,
        GetDatabaseTableNamesRequest.class);
    GetDatabaseTableNamesResponse response = new GetDatabaseTableNamesResponse();
    response.tableNames = mDatabasePeerManager.getDatabaseTableNames(request.databaseId);
    return response;
  }

  @ChromeDevtoolsMethod
  public JsonRpcResult executeSQL(JsonRpcPeer peer, JSONObject params) {
    ExecuteSQLRequest request = mObjectMapper.convertValue(params,
        ExecuteSQLRequest.class);
    try {
      return mDatabasePeerManager.executeSQL(request.databaseId, request.query,
          new DatabasePeerManager.ExecuteResultHandler<ExecuteSQLResponse>() {
        @Override
        public ExecuteSQLResponse handleResult(Cursor result) throws SQLiteException {
          ExecuteSQLResponse response = new ExecuteSQLResponse();
          response.columnNames = Arrays.asList(result.getColumnNames());
          response.values = flattenRows(result, MAX_EXECUTE_RESULTS);
          return response;
        }
      });
    } catch (SQLiteException e) {
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
   * @return List of Java primitives matching the value type of each column.
   */
  private List<Object> flattenRows(Cursor cursor, int limit) {
    Util.throwIfNot(limit >= 0);
    List<Object> flatList = new ArrayList<Object>();
    final int numColumns = cursor.getColumnCount();
    for (int row = 0; row < limit && cursor.moveToNext(); row++) {
      for (int column = 0; column < numColumns; column++) {
        switch (cursor.getType(column)) {
          case Cursor.FIELD_TYPE_NULL:
            flatList.add(null);
            break;
          case Cursor.FIELD_TYPE_INTEGER:
            flatList.add(cursor.getLong(column));
            break;
          case Cursor.FIELD_TYPE_FLOAT:
            flatList.add(cursor.getDouble(column));
            break;
          case Cursor.FIELD_TYPE_BLOB:
            flatList.add(cursor.getBlob(column));
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

  private static class GetDatabaseTableNamesRequest {
    @JsonProperty(required = true)
    public String databaseId;
  }

  private static class GetDatabaseTableNamesResponse implements JsonRpcResult {
    @JsonProperty(required = true)
    public List<String> tableNames;
  }

  private static class ExecuteSQLRequest {
    @JsonProperty(required = true)
    public String databaseId;

    @JsonProperty(required = true)
    public String query;
  }

  private static class ExecuteSQLResponse implements JsonRpcResult {
    @JsonProperty
    public List<String> columnNames;

    @JsonProperty
    public List<Object> values;

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
}
