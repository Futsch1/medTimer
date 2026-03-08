package com.futsch1.medtimer.remindertable

import com.evrencoskun.tableview.filter.IFilterableModel
import com.evrencoskun.tableview.sort.ISortableModel

class ReminderTableCellModel(
    private val content: Any?,
    val representation: String,
    val idAsInt: Int,
    val viewTag: String?
) : ISortableModel, IFilterableModel {
    private val stringId: String = idAsInt.toString()

    override fun getId(): String {
        return this.stringId
    }

    override fun getContent(): Any? {
        return content
    }

    override fun getFilterableKeyword(): String {
        return this.representation
    }
}
