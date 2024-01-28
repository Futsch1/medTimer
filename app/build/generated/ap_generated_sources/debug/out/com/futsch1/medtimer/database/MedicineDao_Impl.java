package com.futsch1.medtimer.database;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;
import androidx.lifecycle.LiveData;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.RelationUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import kotlin.Unit;

@SuppressWarnings({"unchecked", "deprecation"})
public final class MedicineDao_Impl implements MedicineDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Medicine> __insertionAdapterOfMedicine;

  private final EntityInsertionAdapter<Reminder> __insertionAdapterOfReminder;

  private final EntityInsertionAdapter<ReminderEvent> __insertionAdapterOfReminderEvent;

  private final EntityDeletionOrUpdateAdapter<Medicine> __deletionAdapterOfMedicine;

  private final EntityDeletionOrUpdateAdapter<Reminder> __deletionAdapterOfReminder;

  private final EntityDeletionOrUpdateAdapter<Medicine> __updateAdapterOfMedicine;

  private final EntityDeletionOrUpdateAdapter<Reminder> __updateAdapterOfReminder;

  private final EntityDeletionOrUpdateAdapter<ReminderEvent> __updateAdapterOfReminderEvent;

  public MedicineDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMedicine = new EntityInsertionAdapter<Medicine>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `Medicine` (`medicineName`,`medicineId`) VALUES (?,nullif(?, 0))";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Medicine entity) {
        if (entity.name == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.name);
        }
        statement.bindLong(2, entity.medicineId);
      }
    };
    this.__insertionAdapterOfReminder = new EntityInsertionAdapter<Reminder>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `Reminder` (`medicineRelId`,`reminderId`,`timeInMinutes`,`amount`) VALUES (?,nullif(?, 0),?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Reminder entity) {
        statement.bindLong(1, entity.medicineRelId);
        statement.bindLong(2, entity.reminderId);
        statement.bindLong(3, entity.timeInMinutes);
        if (entity.amount == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.amount);
        }
      }
    };
    this.__insertionAdapterOfReminderEvent = new EntityInsertionAdapter<ReminderEvent>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `ReminderEvent` (`reminderEventId`,`medicineName`,`amount`,`status`,`raisedTimestamp`,`processedTimestamp`,`reminderId`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final ReminderEvent entity) {
        statement.bindLong(1, entity.reminderEventId);
        if (entity.medicineName == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.medicineName);
        }
        if (entity.amount == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.amount);
        }
        if (entity.status == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, __ReminderStatus_enumToString(entity.status));
        }
        statement.bindLong(5, entity.raisedTimestamp);
        statement.bindLong(6, entity.processedTimestamp);
        statement.bindLong(7, entity.reminderId);
      }
    };
    this.__deletionAdapterOfMedicine = new EntityDeletionOrUpdateAdapter<Medicine>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `Medicine` WHERE `medicineId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Medicine entity) {
        statement.bindLong(1, entity.medicineId);
      }
    };
    this.__deletionAdapterOfReminder = new EntityDeletionOrUpdateAdapter<Reminder>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `Reminder` WHERE `reminderId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Reminder entity) {
        statement.bindLong(1, entity.reminderId);
      }
    };
    this.__updateAdapterOfMedicine = new EntityDeletionOrUpdateAdapter<Medicine>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `Medicine` SET `medicineName` = ?,`medicineId` = ? WHERE `medicineId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Medicine entity) {
        if (entity.name == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.name);
        }
        statement.bindLong(2, entity.medicineId);
        statement.bindLong(3, entity.medicineId);
      }
    };
    this.__updateAdapterOfReminder = new EntityDeletionOrUpdateAdapter<Reminder>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `Reminder` SET `medicineRelId` = ?,`reminderId` = ?,`timeInMinutes` = ?,`amount` = ? WHERE `reminderId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Reminder entity) {
        statement.bindLong(1, entity.medicineRelId);
        statement.bindLong(2, entity.reminderId);
        statement.bindLong(3, entity.timeInMinutes);
        if (entity.amount == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.amount);
        }
        statement.bindLong(5, entity.reminderId);
      }
    };
    this.__updateAdapterOfReminderEvent = new EntityDeletionOrUpdateAdapter<ReminderEvent>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `ReminderEvent` SET `reminderEventId` = ?,`medicineName` = ?,`amount` = ?,`status` = ?,`raisedTimestamp` = ?,`processedTimestamp` = ?,`reminderId` = ? WHERE `reminderEventId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final ReminderEvent entity) {
        statement.bindLong(1, entity.reminderEventId);
        if (entity.medicineName == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.medicineName);
        }
        if (entity.amount == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.amount);
        }
        if (entity.status == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, __ReminderStatus_enumToString(entity.status));
        }
        statement.bindLong(5, entity.raisedTimestamp);
        statement.bindLong(6, entity.processedTimestamp);
        statement.bindLong(7, entity.reminderId);
        statement.bindLong(8, entity.reminderEventId);
      }
    };
  }

  @Override
  public void insertMedicine(final Medicine medicine) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfMedicine.insert(medicine);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insertReminder(final Reminder reminder) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfReminder.insert(reminder);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public long insertReminderEvent(final ReminderEvent reminderEvent) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      final long _result = __insertionAdapterOfReminderEvent.insertAndReturnId(reminderEvent);
      __db.setTransactionSuccessful();
      return _result;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteMedicine(final Medicine medicine) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfMedicine.handle(medicine);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteReminder(final Reminder reminder) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfReminder.handle(reminder);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void updateMedicine(final Medicine medicine) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfMedicine.handle(medicine);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void updateReminder(final Reminder reminder) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfReminder.handle(reminder);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void updateReminderEvent(final ReminderEvent reminderEvent) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfReminderEvent.handle(reminderEvent);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public LiveData<List<MedicineWithReminders>> getMedicines() {
    final String _sql = "SELECT * FROM Medicine";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"Reminder",
        "Medicine"}, true, new Callable<List<MedicineWithReminders>>() {
      @Override
      @Nullable
      public List<MedicineWithReminders> call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
          try {
            final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "medicineName");
            final int _cursorIndexOfMedicineId = CursorUtil.getColumnIndexOrThrow(_cursor, "medicineId");
            final LongSparseArray<ArrayList<Reminder>> _collectionReminders = new LongSparseArray<ArrayList<Reminder>>();
            while (_cursor.moveToNext()) {
              final Long _tmpKey;
              if (_cursor.isNull(_cursorIndexOfMedicineId)) {
                _tmpKey = null;
              } else {
                _tmpKey = _cursor.getLong(_cursorIndexOfMedicineId);
              }
              if (_tmpKey != null) {
                if (!_collectionReminders.containsKey(_tmpKey)) {
                  _collectionReminders.put(_tmpKey, new ArrayList<Reminder>());
                }
              }
            }
            _cursor.moveToPosition(-1);
            __fetchRelationshipReminderAscomFutsch1MedtimerDatabaseReminder(_collectionReminders);
            final List<MedicineWithReminders> _result = new ArrayList<MedicineWithReminders>(_cursor.getCount());
            while (_cursor.moveToNext()) {
              final MedicineWithReminders _item;
              final Medicine _tmpMedicine;
              if (!(_cursor.isNull(_cursorIndexOfName) && _cursor.isNull(_cursorIndexOfMedicineId))) {
                final String _tmpName;
                if (_cursor.isNull(_cursorIndexOfName)) {
                  _tmpName = null;
                } else {
                  _tmpName = _cursor.getString(_cursorIndexOfName);
                }
                _tmpMedicine = new Medicine(_tmpName);
                _tmpMedicine.medicineId = _cursor.getInt(_cursorIndexOfMedicineId);
              } else {
                _tmpMedicine = null;
              }
              final ArrayList<Reminder> _tmpRemindersCollection;
              final Long _tmpKey_1;
              if (_cursor.isNull(_cursorIndexOfMedicineId)) {
                _tmpKey_1 = null;
              } else {
                _tmpKey_1 = _cursor.getLong(_cursorIndexOfMedicineId);
              }
              if (_tmpKey_1 != null) {
                _tmpRemindersCollection = _collectionReminders.get(_tmpKey_1);
              } else {
                _tmpRemindersCollection = new ArrayList<Reminder>();
              }
              _item = new MedicineWithReminders();
              _item.medicine = _tmpMedicine;
              _item.reminders = _tmpRemindersCollection;
              _result.add(_item);
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
          }
        } finally {
          __db.endTransaction();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Medicine getMedicine(final int medicineId) {
    final String _sql = "SELECT * FROM Medicine WHERE medicineId= ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, medicineId);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "medicineName");
      final int _cursorIndexOfMedicineId = CursorUtil.getColumnIndexOrThrow(_cursor, "medicineId");
      final Medicine _result;
      if (_cursor.moveToFirst()) {
        final String _tmpName;
        if (_cursor.isNull(_cursorIndexOfName)) {
          _tmpName = null;
        } else {
          _tmpName = _cursor.getString(_cursorIndexOfName);
        }
        _result = new Medicine(_tmpName);
        _result.medicineId = _cursor.getInt(_cursorIndexOfMedicineId);
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public LiveData<List<Reminder>> getReminders(final int medicineId) {
    final String _sql = "SELECT * FROM Reminder WHERE medicineRelId= ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, medicineId);
    return __db.getInvalidationTracker().createLiveData(new String[] {"Reminder"}, false, new Callable<List<Reminder>>() {
      @Override
      @Nullable
      public List<Reminder> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfMedicineRelId = CursorUtil.getColumnIndexOrThrow(_cursor, "medicineRelId");
          final int _cursorIndexOfReminderId = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderId");
          final int _cursorIndexOfTimeInMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "timeInMinutes");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final List<Reminder> _result = new ArrayList<Reminder>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Reminder _item;
            final int _tmpMedicineRelId;
            _tmpMedicineRelId = _cursor.getInt(_cursorIndexOfMedicineRelId);
            _item = new Reminder(_tmpMedicineRelId);
            _item.reminderId = _cursor.getInt(_cursorIndexOfReminderId);
            _item.timeInMinutes = _cursor.getInt(_cursorIndexOfTimeInMinutes);
            if (_cursor.isNull(_cursorIndexOfAmount)) {
              _item.amount = null;
            } else {
              _item.amount = _cursor.getString(_cursorIndexOfAmount);
            }
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Reminder getReminder(final int reminderId) {
    final String _sql = "SELECT * FROM Reminder WHERE reminderId= ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, reminderId);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfMedicineRelId = CursorUtil.getColumnIndexOrThrow(_cursor, "medicineRelId");
      final int _cursorIndexOfReminderId = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderId");
      final int _cursorIndexOfTimeInMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "timeInMinutes");
      final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
      final Reminder _result;
      if (_cursor.moveToFirst()) {
        final int _tmpMedicineRelId;
        _tmpMedicineRelId = _cursor.getInt(_cursorIndexOfMedicineRelId);
        _result = new Reminder(_tmpMedicineRelId);
        _result.reminderId = _cursor.getInt(_cursorIndexOfReminderId);
        _result.timeInMinutes = _cursor.getInt(_cursorIndexOfTimeInMinutes);
        if (_cursor.isNull(_cursorIndexOfAmount)) {
          _result.amount = null;
        } else {
          _result.amount = _cursor.getString(_cursorIndexOfAmount);
        }
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public LiveData<List<ReminderEvent>> getReminderEvents() {
    final String _sql = "SELECT * FROM ReminderEvent";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"ReminderEvent"}, false, new Callable<List<ReminderEvent>>() {
      @Override
      @Nullable
      public List<ReminderEvent> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfReminderEventId = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderEventId");
          final int _cursorIndexOfMedicineName = CursorUtil.getColumnIndexOrThrow(_cursor, "medicineName");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfRaisedTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "raisedTimestamp");
          final int _cursorIndexOfProcessedTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "processedTimestamp");
          final int _cursorIndexOfReminderId = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderId");
          final List<ReminderEvent> _result = new ArrayList<ReminderEvent>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ReminderEvent _item;
            _item = new ReminderEvent();
            _item.reminderEventId = _cursor.getInt(_cursorIndexOfReminderEventId);
            if (_cursor.isNull(_cursorIndexOfMedicineName)) {
              _item.medicineName = null;
            } else {
              _item.medicineName = _cursor.getString(_cursorIndexOfMedicineName);
            }
            if (_cursor.isNull(_cursorIndexOfAmount)) {
              _item.amount = null;
            } else {
              _item.amount = _cursor.getString(_cursorIndexOfAmount);
            }
            if (_cursor.isNull(_cursorIndexOfStatus)) {
              _item.status = null;
            } else {
              _item.status = __ReminderStatus_stringToEnum(_cursor.getString(_cursorIndexOfStatus));
            }
            _item.raisedTimestamp = _cursor.getLong(_cursorIndexOfRaisedTimestamp);
            _item.processedTimestamp = _cursor.getLong(_cursorIndexOfProcessedTimestamp);
            _item.reminderId = _cursor.getInt(_cursorIndexOfReminderId);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public LiveData<List<ReminderEvent>> getLatestReminderEvents(final int limit) {
    final String _sql = "SELECT * FROM ReminderEvent ORDER BY raisedTimestamp DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return __db.getInvalidationTracker().createLiveData(new String[] {"ReminderEvent"}, false, new Callable<List<ReminderEvent>>() {
      @Override
      @Nullable
      public List<ReminderEvent> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfReminderEventId = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderEventId");
          final int _cursorIndexOfMedicineName = CursorUtil.getColumnIndexOrThrow(_cursor, "medicineName");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfRaisedTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "raisedTimestamp");
          final int _cursorIndexOfProcessedTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "processedTimestamp");
          final int _cursorIndexOfReminderId = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderId");
          final List<ReminderEvent> _result = new ArrayList<ReminderEvent>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ReminderEvent _item;
            _item = new ReminderEvent();
            _item.reminderEventId = _cursor.getInt(_cursorIndexOfReminderEventId);
            if (_cursor.isNull(_cursorIndexOfMedicineName)) {
              _item.medicineName = null;
            } else {
              _item.medicineName = _cursor.getString(_cursorIndexOfMedicineName);
            }
            if (_cursor.isNull(_cursorIndexOfAmount)) {
              _item.amount = null;
            } else {
              _item.amount = _cursor.getString(_cursorIndexOfAmount);
            }
            if (_cursor.isNull(_cursorIndexOfStatus)) {
              _item.status = null;
            } else {
              _item.status = __ReminderStatus_stringToEnum(_cursor.getString(_cursorIndexOfStatus));
            }
            _item.raisedTimestamp = _cursor.getLong(_cursorIndexOfRaisedTimestamp);
            _item.processedTimestamp = _cursor.getLong(_cursorIndexOfProcessedTimestamp);
            _item.reminderId = _cursor.getInt(_cursorIndexOfReminderId);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public ReminderEvent getReminderEvent(final int reminderEventId) {
    final String _sql = "SELECT * FROM ReminderEvent WHERE reminderEventId= ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, reminderEventId);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfReminderEventId = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderEventId");
      final int _cursorIndexOfMedicineName = CursorUtil.getColumnIndexOrThrow(_cursor, "medicineName");
      final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
      final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
      final int _cursorIndexOfRaisedTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "raisedTimestamp");
      final int _cursorIndexOfProcessedTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "processedTimestamp");
      final int _cursorIndexOfReminderId = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderId");
      final ReminderEvent _result;
      if (_cursor.moveToFirst()) {
        _result = new ReminderEvent();
        _result.reminderEventId = _cursor.getInt(_cursorIndexOfReminderEventId);
        if (_cursor.isNull(_cursorIndexOfMedicineName)) {
          _result.medicineName = null;
        } else {
          _result.medicineName = _cursor.getString(_cursorIndexOfMedicineName);
        }
        if (_cursor.isNull(_cursorIndexOfAmount)) {
          _result.amount = null;
        } else {
          _result.amount = _cursor.getString(_cursorIndexOfAmount);
        }
        if (_cursor.isNull(_cursorIndexOfStatus)) {
          _result.status = null;
        } else {
          _result.status = __ReminderStatus_stringToEnum(_cursor.getString(_cursorIndexOfStatus));
        }
        _result.raisedTimestamp = _cursor.getLong(_cursorIndexOfRaisedTimestamp);
        _result.processedTimestamp = _cursor.getLong(_cursorIndexOfProcessedTimestamp);
        _result.reminderId = _cursor.getInt(_cursorIndexOfReminderId);
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private String __ReminderStatus_enumToString(@NonNull final ReminderEvent.ReminderStatus _value) {
    switch (_value) {
      case RAISED: return "RAISED";
      case TAKEN: return "TAKEN";
      case SKIPPED: return "SKIPPED";
      default: throw new IllegalArgumentException("Can't convert enum to string, unknown enum value: " + _value);
    }
  }

  private void __fetchRelationshipReminderAscomFutsch1MedtimerDatabaseReminder(
      @NonNull final LongSparseArray<ArrayList<Reminder>> _map) {
    if (_map.isEmpty()) {
      return;
    }
    if (_map.size() > RoomDatabase.MAX_BIND_PARAMETER_CNT) {
      RelationUtil.recursiveFetchLongSparseArray(_map, true, (map) -> {
        __fetchRelationshipReminderAscomFutsch1MedtimerDatabaseReminder(map);
        return Unit.INSTANCE;
      });
      return;
    }
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT `medicineRelId`,`reminderId`,`timeInMinutes`,`amount` FROM `Reminder` WHERE `medicineRelId` IN (");
    final int _inputSize = _map.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _stmt = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (int i = 0; i < _map.size(); i++) {
      final long _item = _map.keyAt(i);
      _stmt.bindLong(_argIndex, _item);
      _argIndex++;
    }
    final Cursor _cursor = DBUtil.query(__db, _stmt, false, null);
    try {
      final int _itemKeyIndex = CursorUtil.getColumnIndex(_cursor, "medicineRelId");
      if (_itemKeyIndex == -1) {
        return;
      }
      final int _cursorIndexOfMedicineRelId = 0;
      final int _cursorIndexOfReminderId = 1;
      final int _cursorIndexOfTimeInMinutes = 2;
      final int _cursorIndexOfAmount = 3;
      while (_cursor.moveToNext()) {
        final long _tmpKey;
        _tmpKey = _cursor.getLong(_itemKeyIndex);
        final ArrayList<Reminder> _tmpRelation = _map.get(_tmpKey);
        if (_tmpRelation != null) {
          final Reminder _item_1;
          final int _tmpMedicineRelId;
          _tmpMedicineRelId = _cursor.getInt(_cursorIndexOfMedicineRelId);
          _item_1 = new Reminder(_tmpMedicineRelId);
          _item_1.reminderId = _cursor.getInt(_cursorIndexOfReminderId);
          _item_1.timeInMinutes = _cursor.getInt(_cursorIndexOfTimeInMinutes);
          if (_cursor.isNull(_cursorIndexOfAmount)) {
            _item_1.amount = null;
          } else {
            _item_1.amount = _cursor.getString(_cursorIndexOfAmount);
          }
          _tmpRelation.add(_item_1);
        }
      }
    } finally {
      _cursor.close();
    }
  }

  private ReminderEvent.ReminderStatus __ReminderStatus_stringToEnum(@NonNull final String _value) {
    switch (_value) {
      case "RAISED": return ReminderEvent.ReminderStatus.RAISED;
      case "TAKEN": return ReminderEvent.ReminderStatus.TAKEN;
      case "SKIPPED": return ReminderEvent.ReminderStatus.SKIPPED;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }
}
