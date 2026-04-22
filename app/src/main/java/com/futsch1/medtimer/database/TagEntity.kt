package com.futsch1.medtimer.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import java.util.Objects

@Entity(tableName = "Tag")
class TagEntity(@field:Expose var name: String? = null, @PrimaryKey(autoGenerate = true) var tagId: Int = 0) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        return membersEqual(other as TagEntity)
    }

    override fun hashCode(): Int {
        return Objects.hash(tagId, name)
    }

    private fun membersEqual(tag: TagEntity): Boolean {
        return tagId == tag.tagId && name == tag.name
    }
}
