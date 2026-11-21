package com.example.notebook.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.notebook.data.database.NotebookDatabase

/** A fresh database in memory for each test; nothing touches the phone or disk. */
fun inMemoryDatabase(): NotebookDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), NotebookDatabase::class.java)
        .allowMainThreadQueries()
        .build()

/** A clock the test controls, so dates are predictable. */
class FakeClock(var now: Long = 1_000_000L) {
    fun advance(millis: Long) { now += millis }
    val function: () -> Long = { now }
}
