package com.futsch1.medtimer.medicine.tags

import com.futsch1.medtimer.model.Medicine
import com.futsch1.medtimer.model.Tag

class TagWithStateCollector(
    private val doneCallback: (list: List<TagWithState>) -> Unit
) {
    private var allTags: Boolean = true

    var tags: List<Tag>? = null
        set(value) {
            field = value
            dataUpdated()
        }
    var medicine: Medicine? = null
        set(value) {
            field = value
            dataUpdated()
        }

    private fun dataUpdated() {
        if (tags != null && medicine != null) {
            doneCallback(getTagsWithState())
        }
    }

    private fun getTagsWithState(): List<TagWithState> {
        return if (allTags) {
            tags!!.map {
                TagWithState(
                    it,
                    medicine!!.tags.contains(it)
                )
            }
        } else {
            tags!!.filter {
                medicine!!.tags.contains(it)
            }.map {
                TagWithState(
                    it,
                    true
                )
            }
        }
    }

}