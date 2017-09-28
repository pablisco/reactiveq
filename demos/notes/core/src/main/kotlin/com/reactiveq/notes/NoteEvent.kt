package com.reactiveq.notes

sealed class NoteEvent {

    object LoadAllNotes : NoteEvent()
    data class CreateNote(val note: Note): NoteEvent()
    data class Updatenote(val oldNote: Note, val newNote: Note): NoteEvent()
    data class DeleteNote(val note: Note)

}