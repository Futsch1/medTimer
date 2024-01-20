package com.futsch1.medtimer;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.futsch1.medtimer.database.MedicineDao;
import com.futsch1.medtimer.database.MedicineDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile MedicineDao _medicineDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `Medicine` (`uid` INTEGER NOT NULL, `name` TEXT, PRIMARY KEY(`uid`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `Reminder` (`reminderId` INTEGER NOT NULL, `medicineRelId` INTEGER NOT NULL, `timeInMinutes` INTEGER NOT NULL, `amount` TEXT, PRIMARY KEY(`reminderId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '2973a9537f52d9d95d8dbd1d33e38951')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `Medicine`");
        db.execSQL("DROP TABLE IF EXISTS `Reminder`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsMedicine = new HashMap<String, TableInfo.Column>(2);
        _columnsMedicine.put("uid", new TableInfo.Column("uid", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicine.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMedicine = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMedicine = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMedicine = new TableInfo("Medicine", _columnsMedicine, _foreignKeysMedicine, _indicesMedicine);
        final TableInfo _existingMedicine = TableInfo.read(db, "Medicine");
        if (!_infoMedicine.equals(_existingMedicine)) {
          return new RoomOpenHelper.ValidationResult(false, "Medicine(com.futsch1.medtimer.logic.Medicine).\n"
                  + " Expected:\n" + _infoMedicine + "\n"
                  + " Found:\n" + _existingMedicine);
        }
        final HashMap<String, TableInfo.Column> _columnsReminder = new HashMap<String, TableInfo.Column>(4);
        _columnsReminder.put("reminderId", new TableInfo.Column("reminderId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReminder.put("medicineRelId", new TableInfo.Column("medicineRelId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReminder.put("timeInMinutes", new TableInfo.Column("timeInMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReminder.put("amount", new TableInfo.Column("amount", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysReminder = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesReminder = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoReminder = new TableInfo("Reminder", _columnsReminder, _foreignKeysReminder, _indicesReminder);
        final TableInfo _existingReminder = TableInfo.read(db, "Reminder");
        if (!_infoReminder.equals(_existingReminder)) {
          return new RoomOpenHelper.ValidationResult(false, "Reminder(com.futsch1.medtimer.logic.Reminder).\n"
                  + " Expected:\n" + _infoReminder + "\n"
                  + " Found:\n" + _existingReminder);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "2973a9537f52d9d95d8dbd1d33e38951", "fea356b9ed61b219150eff2f2280273f");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "Medicine","Reminder");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `Medicine`");
      _db.execSQL("DELETE FROM `Reminder`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(MedicineDao.class, MedicineDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public MedicineDao medicineDao() {
    if (_medicineDao != null) {
      return _medicineDao;
    } else {
      synchronized(this) {
        if(_medicineDao == null) {
          _medicineDao = new MedicineDao_Impl(this);
        }
        return _medicineDao;
      }
    }
  }
}
