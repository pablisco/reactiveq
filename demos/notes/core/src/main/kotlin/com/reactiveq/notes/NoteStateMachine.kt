package com.reactiveq.notes

import kategory.Option
import kategory.Option.None
import kategory.Option.Some
import kategory.Try
import kategory.getOrElse
import kategory.orElse
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import reactiveq.ReactiveQ

class NoteStateMachine(
    private val queue: ReactiveQ
) {

    init {
        queue.onPush<NoteEvent> {
            when (it) {
                is NoteEvent.LoadAllNotes -> syncStore(NoteQuery.All)
            }
        }
    }

    private var notes: Option<Response> = None

    private fun syncStore(query: NoteQuery) {
        async(CommonPool) {
            queue {
                push(NoteState.Loading)
                notes = updateNotes(notes, query)
                push(processState(notes))
            }
        }
    }

    private fun updateNotes(currentNotes: Option<Response>, query: NoteQuery): Option<Response> =
        currentNotes.filter { it.query == query }
            .orElse {
                Some(Response(
                    query,
                    queue.pull<List<Note>>().withQuery(query).first()
                ))
            }

    private fun processState(currentNotes: Option<Response>): NoteState =
        currentNotes.map { it.result }
            .getOrElse { Try.raise(IllegalStateException("Store has no values")) }
            .fold(
                { NoteState.FailedLoading(it) },
                { NoteState.LoadedWithNotes(it) }
            )

    private data class Response(val query: NoteQuery, val result: Try<List<Note>>)

}

sealed class NoteQuery {

    object All : NoteQuery()

}
