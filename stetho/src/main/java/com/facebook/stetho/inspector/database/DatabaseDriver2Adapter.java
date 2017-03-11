package com.facebook.stetho.inspector.database;

import android.database.sqlite.SQLiteException;

import com.facebook.stetho.inspector.protocol.module.Database;
import com.facebook.stetho.inspector.protocol.module.DatabaseDescriptor;
import com.facebook.stetho.inspector.protocol.module.DatabaseDriver2;

import java.util.ArrayList;
import java.util.List;

/**
 * @deprecated Use {@link DatabaseDriver2} directly.  This is provided only for legacy
 * drivers to be adapted internally within Stetho.
 */
@Deprecated
public class DatabaseDriver2Adapter
    extends DatabaseDriver2<DatabaseDriver2Adapter.StringDatabaseDescriptor> {
  private final Database.DatabaseDriver mLegacy;

  public DatabaseDriver2Adapter(Database.DatabaseDriver legacy) {
    super(legacy.getContext());
    mLegacy = legacy;
  }

  @Override
  public List<StringDatabaseDescriptor> getDatabaseNames() {
    List<?> names = mLegacy.getDatabaseNames();
    List<StringDatabaseDescriptor> descriptors = new ArrayList<>(names.size());
    for (Object name : names) {
      descriptors.add(new StringDatabaseDescriptor(name.toString()));
    }
    return descriptors;
  }

  @SuppressWarnings("unchecked")
  public List<String> getTableNames(StringDatabaseDescriptor database) {
    return mLegacy.getTableNames(database.name);
  }

  @SuppressWarnings("unchecked")
  public Database.ExecuteSQLResponse executeSQL(
      StringDatabaseDescriptor database,
      String query,
      ExecuteResultHandler handler) throws SQLiteException {
    return mLegacy.executeSQL(database.name, query, handler);
  }

  static class StringDatabaseDescriptor implements DatabaseDescriptor {
    public final String name;

    public StringDatabaseDescriptor(String name) {
      this.name = name;
    }

    @Override
    public String name() {
      return name;
    }
  }
}
