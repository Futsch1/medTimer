package com.futsch1.medtimer.database

import com.futsch1.medtimer.model.Tag

fun TagEntity.toModel(): Tag {
    return Tag(
        name = name,
        id = tagId
    )
}

fun Tag.toEntity(): TagEntity {
    val tagEntity = TagEntity(name)
    tagEntity.tagId = id
    return tagEntity
}
