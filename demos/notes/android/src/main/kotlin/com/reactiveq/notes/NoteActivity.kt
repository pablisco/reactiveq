package com.reactiveq.notes

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import com.reactiveq.notes.NoteEvent.LoadAllNotes
import kotlinx.android.synthetic.main.activity_main.list
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import java.io.Closeable
import kotlinx.android.synthetic.main.activity_main.empty as emptyView
import kotlinx.android.synthetic.main.activity_main.error as errorView
import kotlinx.android.synthetic.main.activity_main.loading_view as loadingView

class NoteActivity : AppCompatActivity() {

    private var stateClosable: Closeable? = null

    private val views by lazy {
        listOf<View>(emptyView, errorView, list, loadingView)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()
        stateClosable = queue().onPush<NoteState> {
            launch(UI) { render(it) }
        }
        queue().push(LoadAllNotes)
    }

    override fun onStop() {
        super.onStop()
        stateClosable?.close()
    }

    private fun render(state: NoteState) {
        when (state) {
            is NoteState.Loading -> {
                loadingView.hideOthers()
            }
            is NoteState.LoadedWithNotes -> {
                list.hideOthers()
                list.adapter = TaskAdapter(state.notes)
            }
            is NoteState.LoadedEmpty -> {
                emptyView.hideOthers()
            }
            is NoteState.FailedLoading -> {
                errorView.hideOthers()
            }
        }
    }

    private fun View.hideOthers() =
        views.forEach { it.visibility = if (it == this) VISIBLE else GONE }

    class TaskAdapter(
        private val notes: List<Note>
    ) : RecyclerView.Adapter<TaskViewHolder>() {

        override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
            holder.bind(notes[position])
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder = TaskViewHolder(parent)

        override fun getItemCount(): Int = notes.size

    }

    class TaskViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
    ) {

        private val textView = itemView.findViewById<TextView>(android.R.id.text1)

        fun bind(note: Note) {
            textView.text = note.title
        }

    }

}
