package com.reactiveq.notes

import android.content.Context
import reactiveq.ReactiveQ


fun Context.queue() : ReactiveQ = NotesApplication.instance.queue