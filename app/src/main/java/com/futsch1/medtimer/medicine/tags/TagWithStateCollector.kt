package com.futsch1.medtimer.medicine.tags

import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.database.TagEntity

class TagWithStateCollector(
    private val doneCallback: (list: List<TagWithState>) -> Unit
) {
    private var allTags: Boolean = true

    var tags: List<TagEntity>? = null
        set(value) {
            field = value
            dataUpdated()
        }
    var fullMedicine: FullMedicineEntity? = null
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