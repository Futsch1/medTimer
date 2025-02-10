package com.futsch1.medtimer.medicine.tags

import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Tag

class TagWithStateCollector(
    private val doneCallback: (list: List<TagWithState>) -> Unit
) {
    private var allTags: Boolean = true

    fun allTags(allTags: Boolean) = apply { this.allTags = allTags }

    var tags: List<Tag>? = null
        set(value) {
            field = value
            dataUpdated()
        }
    var fullMedicine: FullMedicine? = null
        set(value) {
            field = value
            dataUpdated()
        }

    private fun dataUpdated() {
        if (tags != null && fullMedicine != null) {
            doneCallback(getTagsWithState())
        }
    }

    private fun getTagsWithState(): List<TagWithState> {
        return if (allTags) {
            tags!!.map {
                TagWithState(
                    it,
                    fullMedicine!!.tags.contains(it)
                )
            }
        } else {
            tags!!.filter {
                fullMedicine!!.tags.contains(it)
            }.map {
                TagWithState(
                    it,
                    true
                )
            }
        }
    }

}