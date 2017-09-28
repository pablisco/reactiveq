package com.reactiveq.notes;

import reactiveq.ReactiveQ

class NoteRepository(queue: ReactiveQ) {

    init {

        queue.onPull<List<Note>> {
            withQuery<NoteQuery.All> {
//                Thread.sleep(1000)
                listOf(
                    Note("123", "First note", ""),
                    Note("124", "Second note", "")
                )
            }
        }

    }

}
