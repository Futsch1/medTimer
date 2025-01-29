package com.futsch1.medtimer.medicine.tags

import com.futsch1.medtimer.database.MedicineWithTags
import com.futsch1.medtimer.database.Tag

class TagWithStateCollector(
    private val tagsAdapter: TagsAdapter,
    private val doneCallback: () -> Unit
) {
    private var allTags: Boolean = true

    fun allTags(allTags: Boolean) = apply { this.allTags = allTags }

    var tags: List<Tag>? = null
        set(value) {
            field = value
            dataUpdated()
        }
    var medicineWithTags: MedicineWithTags? = null
        set(value) {
            field = value
            dataUpdated()
        }

    private fun dataUpdated() {
        if (tags != null && medicineWithTags != null) {
            tagsAdapter.submitList(getTagsWithState())
            doneCallback()
        }
    }

    private fun getTagsWithState(): List<TagWithState> {
        return if (allTags) {
            tags!!.map {
                TagWithState(
                    it,
                    medicineWithTags!!.tags.contains(it)
                )
            }
        } else {
            tags!!.filter {
                medicineWithTags!!.tags.contains(it)
            }.map {
                TagWithState(
                    it,
                    true
                )
            }
        }
    }

}