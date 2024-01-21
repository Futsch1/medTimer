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

  private final EntityDeletionOrUpdateAdapter<Medicine> __deletionAdapterOfMedicine;

  private final EntityDeletionOrUpdateAdapter<Reminder> __deletionAdapterOfReminder;

  public MedicineDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMedicine = new EntityInsertionAdapter<Medicine>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `Medicine` (`medicineId`,`name`) VALUES (?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Medicine entity) {
        statement.bindLong(1, entity.medicineId);
        if (entity.name == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.name);
        }
      }
    };
    this.__insertionAdapterOfReminder = new EntityInsertionAdapter<Reminder>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `Reminder` (`reminderId`,`medicineRelId`,`timeInMinutes`,`amount`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Reminder entity) {
        statement.bindLong(1, entity.reminderId);
        statement.bindLong(2, entity.medicineRelId);
        statement.bindLong(3, entity.timeInMinutes);
        if (entity.amount == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.amount);
        }
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
  }

  @Override
  public void insertMedicine(final Medicine medicineEntities) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfMedicine.insert(medicineEntities);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insertReminder(final Reminder medicines) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfReminder.insert(medicines);
      __db.setTransactionSuccessful();
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
            final int _cursorIndexOfMedicineId = CursorUtil.getColumnIndexOrThrow(_cursor, "medicineId");
            final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
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
              if (!(_cursor.isNull(_cursorIndexOfMedicineId) && _cursor.isNull(_cursorIndexOfName))) {
                _tmpMedicine = new Medicine();
                _tmpMedicine.medicineId = _cursor.getInt(_cursorIndexOfMedicineId);
                if (_cursor.isNull(_cursorIndexOfName)) {
                  _tmpMedicine.name = null;
                } else {
                  _tmpMedicine.name = _cursor.getString(_cursorIndexOfName);
                }
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
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
    _stringBuilder.append("SELECT `reminderId`,`medicineRelId`,`timeInMinutes`,`amount` FROM `Reminder` WHERE `medicineRelId` IN (");
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
      final int _cursorIndexOfReminderId = 0;
      final int _cursorIndexOfMedicineRelId = 1;
      final int _cursorIndexOfTimeInMinutes = 2;
      final int _cursorIndexOfAmount = 3;
      while (_cursor.moveToNext()) {
        final long _tmpKey;
        _tmpKey = _cursor.getLong(_itemKeyIndex);
        final ArrayList<Reminder> _tmpRelation = _map.get(_tmpKey);
        if (_tmpRelation != null) {
          final Reminder _item_1;
          _item_1 = new Reminder();
          _item_1.reminderId = _cursor.getInt(_cursorIndexOfReminderId);
          _item_1.medicineRelId = _cursor.getInt(_cursorIndexOfMedicineRelId);
          _item_1.timeInMinutes = _cursor.getLong(_cursorIndexOfTimeInMinutes);
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
}
