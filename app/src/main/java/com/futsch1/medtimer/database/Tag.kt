package com.futsch1.medtimer.database

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import java.util.Objects

@Entity
class Tag(@field:Expose var name: String) {
    @PrimaryKey(autoGenerate = true)
    var tagId: Int = 0

    @Ignore
    constructor() : this("")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        return membersEqual(other as Tag)
    }

    override fun hashCode(): Int {
        return Objects.hash(tagId, name)
    }

    private fun membersEqual(tag: Tag): Boolean {
        return tagId == tag.tagId && name == tag.name
    }
}
