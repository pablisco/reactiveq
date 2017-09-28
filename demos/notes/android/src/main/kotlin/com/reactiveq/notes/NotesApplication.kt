package com.reactiveq.notes

import android.app.Application
import android.util.Log
import reactiveq.ReactiveQ

class NotesApplication : Application() {

    companion object {

        private var _instance: NotesApplication? = null

        val instance: NotesApplication
            get() = _instance ?: throw IllegalStateException("Application has not been created yet.")

    }

    val queue = appQueue {
        NoteStateMachine(it)
        NoteRepository(it)
        EventLogger(it)
    }

    override fun onCreate() {
        super.onCreate()
        _instance = this
    }

    private fun appQueue(populate: (ReactiveQ) -> Unit): ReactiveQ {
        val queue = ReactiveQ()
        populate(queue)
        return queue
    }

    class EventLogger(queue: ReactiveQ) {

        init {
            queue.onPush<Any> {
                Log.d("QUEUE", "$it")
            }
        }

    }

}