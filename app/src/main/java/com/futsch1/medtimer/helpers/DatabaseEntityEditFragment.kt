package com.futsch1.medtimer.helpers

import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

abstract class DatabaseEntityEditFragment<Entity>(
    private val entityInterface: EntityInterface<Entity>,
    private val layoutId: Int
) :
    Fragment() {
    interface EntityInterface<Entity> {
        fun get(): Entity
        fun update(entity: Entity)
    }

    private val thread = HandlerThread("DatabaseEntityEditFragment")
    private var entity: Entity? = null
    private var fragmentView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.thread.start()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentView = inflater.inflate(layoutId, container, false)
        postponeEnterTransition()
        val handler = Handler(thread.looper)
        handler.post {
            entity = entityInterface.get()
            onEntityLoaded(entity!!, fragmentView!!)
            startPostponedEnterTransition()
        }
        return fragmentView!!
    }

    override fun onDestroy() {
        super.onDestroy()
        thread.quitSafely()
    }

    override fun onStop() {
        super.onStop()
        if (entity != null && fragmentView != null) {
            val newEntity = updateEntity(entity!!, fragmentView!!)
            entityInterface.update(newEntity)
        }
    }

    abstract fun onEntityLoaded(entity: Entity, fragmentView: View)
    abstract fun updateEntity(entity: Entity, fragmentView: View): Entity
}