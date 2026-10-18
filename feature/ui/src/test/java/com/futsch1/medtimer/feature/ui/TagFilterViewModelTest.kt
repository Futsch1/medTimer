package com.futsch1.medtimer.feature.ui

import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.domain.model.PersistentData
import com.futsch1.medtimer.core.domain.model.Tag
import com.futsch1.medtimer.core.domain.repository.TagRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class TagFilterViewModelTest {

    private val persistentDataDataSource: PersistentDataDataSource = mock()
    private val tagRepository: TagRepository = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        whenever(persistentDataDataSource.data).thenReturn(MutableStateFlow(PersistentData.default().copy(filterTags = setOf("1"))))
        whenever(tagRepository.hasAny()).thenReturn(flowOf(true))
        whenever(tagRepository.getAllFlow()).thenReturn(flowOf(listOf(Tag(name = "Tag A", id = 1))))
        whenever(tagRepository.getMedicineTagsFlow()).thenReturn(flowOf(emptyList()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `clearing the tag filter persists it`() {
        val viewModel = TagFilterViewModel(persistentDataDataSource, tagRepository)
        assertEquals(setOf(1), viewModel.validTagIds.value)

        viewModel.clearTagFilter()

        assertEquals(emptySet(), viewModel.validTagIds.value)
        verify(persistentDataDataSource).setFilterTags(emptySet())
    }
}
