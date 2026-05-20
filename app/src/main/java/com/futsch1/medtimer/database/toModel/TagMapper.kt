package com.futsch1.medtimer.database.toModel

import com.futsch1.medtimer.core.domain.model.Tag
import com.futsch1.medtimer.database.TagEntity

fun TagEntity.toModel(): Tag {
    return Tag(
        name = name ?: "",
        id = tagId
    )
}

fun Tag.toEntity(): TagEntity = TagEntity(name = name, tagId = id)
