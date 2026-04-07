package com.futsch1.medtimer.database.toModel

import com.futsch1.medtimer.database.TagEntity
import com.futsch1.medtimer.model.Tag

fun TagEntity.toModel(): Tag {
    return Tag(
        name = name,
        id = tagId
    )
}

fun Tag.toEntity(): TagEntity = TagEntity(name = name, tagId = id)
