package com.futsch1.medtimer.database;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        return "INSERT OR ABORT INTO `Medicine` (`uid`,`name`) VALUES (?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Medicine entity) {
        statement.bindLong(1, entity.uid);
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
        return "DELETE FROM `Medicine` WHERE `uid` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Medicine entity) {
        statement.bindLong(1, entity.uid);
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
  public void insertMedicines(final Medicine... medicines) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfMedicine.insert(medicines);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insertReminders(final Reminder... medicines) {
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
  public List<Medicine> getMedicines() {
    final String _sql = "SELECT * FROM Medicine";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
      try {
        final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
        final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
        final List<Medicine> _result = new ArrayList<Medicine>(_cursor.getCount());
        while (_cursor.moveToNext()) {
          final Medicine _item;
          _item = new Medicine();
          _item.uid = _cursor.getInt(_cursorIndexOfUid);
          if (_cursor.isNull(_cursorIndexOfName)) {
            _item.name = null;
          } else {
            _item.name = _cursor.getString(_cursorIndexOfName);
          }
          _result.add(_item);
        }
        __db.setTransactionSuccessful();
        return _result;
      } finally {
        _cursor.close();
        _statement.release();
      }
    } finally {
      __db.endTransaction();
    }
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
