 package com.futsch1.medtimer.database.toBackup

import com.futsch1.medtimer.core.domain.backup.MedicineBackup
import com.futsch1.medtimer.core.domain.backup.ReminderBackup
import com.futsch1.medtimer.core.domain.backup.ReminderEventBackup
import com.futsch1.medtimer.core.domain.backup.TagBackup
import com.futsch1.medtimer.database.MedicineEntity
import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.database.ReminderEventEntity
import com.futsch1.medtimer.database.TagEntity
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Guards against the class of bug where a field is added to a Room entity but the
 * corresponding backup DTO is forgotten, so the field silently drops out of backups.
 *
 * Fails as soon as an entity gains a field with no counterpart on its backup DTO
 * and no explicit entry in excluded explaining why it's intentionally left out.
 * When this test fails: either add the field to the backup DTO + mapper (see
 * BackupMappers.kt), or add it to excluded with a comment explaining why the
 * field doesn't need to survive a backup/restore round trip.
 */
internal class BackupFieldParityTest {

    private fun fieldNames(clazz: Class<*>): Set<String> =
        clazz.declaredFields.filterNot { it.isSynthetic }.map { it.name }.toSet()

    private fun assertBackupCoversEntity(
        entityClass: Class<*>,
        backupClass: Class<*>,
        excluded: Set<String>,
    ) {
        val entityOnly = fieldNames(entityClass) - fieldNames(backupClass) - excluded
        assertEquals(
            emptySet(),
            entityOnly,
            "${entityClass.simpleName} has fields not present on ${backupClass.simpleName}. " +
                "Add them to the backup DTO and its mapper, or add to the excluded set with a reason."
        )
    }

    @Test
    fun reminderEventEntityFieldsCoveredByBackup() {
        assertBackupCoversEntity(
            ReminderEventEntity::class.java,
            ReminderEventBackup::class.java,
            excluded = setOf(
                "reminderEventId", // autoGenerate primary key
                "notificationId", // runtime notification handle, not historical data
                "remainingRepeats", // runtime scheduling state
                "stockHandled", // runtime processing flag, re-derived on restore
                "askForAmount", // runtime UI flag, re-derived from the reminder
            )
        )
    }

    @Test
    fun reminderEntityFieldsCoveredByBackup() {
        assertBackupCoversEntity(
            ReminderEntity::class.java,
            ReminderBackup::class.java,
            excluded = setOf(
                "medicineRelId", // foreign key, reconstructed via FullMedicineBackup nesting
            )
        )
    }

    @Test
    fun medicineEntityFieldsCoveredByBackup() {
        assertBackupCoversEntity(
            MedicineEntity::class.java,
            MedicineBackup::class.java,
            excluded = setOf(
                "medicineId", // autoGenerate primary key
            )
        )
    }

    @Test
    fun tagEntityFieldsCoveredByBackup() {
        assertBackupCoversEntity(
            TagEntity::class.java,
            TagBackup::class.java,
            excluded = setOf(
                "tagId", // autoGenerate primary key
            )
        )
    }
}
