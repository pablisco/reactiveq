package com.reactiveq.notes

sealed class NoteState {

    object Loading : NoteState()
    data class LoadedWithNotes(val notes: List<Note>) : NoteState()
    object LoadedEmpty : NoteState()
    data class FailedLoading(val e: Throwable) : NoteState()

}