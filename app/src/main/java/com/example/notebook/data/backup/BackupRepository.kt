package com.example.notebook.data.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import com.example.notebook.data.database.CategoryEntity
import com.example.notebook.data.database.NoteEntity
import com.example.notebook.data.database.NotebookDatabase
import com.example.notebook.domain.Category
import com.example.notebook.domain.NoteColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import java.util.Locale

/**
 * Export and import of all notes and categories as JSON.
 *
 * Import rules (spec):
 * - adds data and never deletes anything;
 * - a note whose uuid already exists is updated only if the file's updatedAt is newer;
 * - everything happens in one transaction: a broken file changes nothing.
 */
class BackupRepository(
    private val database: NotebookDatabase,
    private val contentResolver: ContentResolver?,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val noteDao = database.noteDao()
    private val categoryDao = database.categoryDao()

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** All notes (including archive and trash) and all categories as JSON text. */
    suspend fun exportJson(): String {
        val (categories, notes) = database.withTransaction { categoryDao.getAll() to noteDao.getAll() }
        val names = categories.associate { it.id to it.name }
        val file = BackupFile(
            formatVersion = FORMAT_VERSION,
            exportedAt = clock(),
            categories = categories.map { BackupCategory(it.name, it.createdAt) },
            notes = notes.map { note ->
                BackupNote(
                    uuid = note.uuid,
                    title = note.title,
                    content = note.content,
                    color = note.color.name,
                    category = note.categoryId?.let(names::get),
                    isPinned = note.isPinned,
                    isArchived = note.isArchived,
                    deletedAt = note.deletedAt,
                    createdAt = note.createdAt,
                    updatedAt = note.updatedAt,
                )
            },
        )
        return json.encodeToString(BackupFile.serializer(), file)
    }

    /** Reads and checks a file before anything touches the database. */
    fun parse(text: String): BackupFile {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: SerializationException) {
            throw BackupException(BackupException.Reason.NOT_A_BACKUP, e)
        } catch (e: IllegalArgumentException) {
            throw BackupException(BackupException.Reason.NOT_A_BACKUP, e)
        }
        when {
            file.formatVersion > FORMAT_VERSION -> throw BackupException(BackupException.Reason.NEWER_VERSION)
            file.formatVersion < 1 -> throw BackupException(BackupException.Reason.NOT_A_BACKUP)
            file.notes.any { it.uuid.isBlank() } -> throw BackupException(BackupException.Reason.NOT_A_BACKUP)
        }
        return file
    }

    suspend fun importJson(text: String): ImportResult {
        val file = parse(text)
        return database.withTransaction {
            // Existing categories by name, ignoring case (also for Cyrillic), like the app's own rule.
            val categoryIds = categoryDao.getAll().associateTo(HashMap()) { it.name.key() to it.id }

            suspend fun categoryId(rawName: String?): Long? {
                val name = rawName?.trim()?.take(Category.MAX_NAME_LENGTH)
                if (name.isNullOrEmpty()) return null
                return categoryIds.getOrPut(name.key()) {
                    categoryDao.insert(CategoryEntity(name = name, createdAt = clock()))
                }
            }

            // Categories without notes are imported too ("все категории").
            file.categories.forEach { categoryId(it.name) }

            var added = 0
            var updated = 0
            var skipped = 0
            for (note in file.notes) {
                val existing = noteDao.getByUuid(note.uuid)
                if (existing != null && note.updatedAt <= existing.updatedAt) {
                    skipped++
                    continue
                }
                val entity = NoteEntity(
                    id = existing?.id ?: 0,
                    uuid = note.uuid,
                    title = note.title,
                    content = note.content,
                    color = NoteColor.fromName(note.color),
                    categoryId = categoryId(note.category),
                    // An archived note is never pinned (same rule as in the app).
                    isPinned = note.isPinned && !note.isArchived,
                    isArchived = note.isArchived,
                    deletedAt = note.deletedAt,
                    createdAt = note.createdAt,
                    updatedAt = note.updatedAt,
                )
                if (existing == null) {
                    noteDao.insert(entity)
                    added++
                } else {
                    noteDao.update(entity)
                    updated++
                }
            }
            ImportResult(added, updated, skipped)
        }
    }

    /** Writes a backup to a file the user picked in the system "save" dialog. Returns the note count. */
    suspend fun exportTo(uri: Uri): Int {
        val text = exportJson()
        val resolver = checkNotNull(contentResolver)
        withContext(Dispatchers.IO) {
            try {
                // "wt" = write and truncate, so an older, longer file is fully replaced.
                val stream = resolver.openOutputStream(uri, "wt") ?: throw IOException("No output stream")
                stream.bufferedWriter(Charsets.UTF_8).use { it.write(text) }
            } catch (e: IOException) {
                throw BackupException(BackupException.Reason.IO_ERROR, e)
            } catch (e: SecurityException) {
                throw BackupException(BackupException.Reason.IO_ERROR, e)
            }
        }
        return noteDao.getAll().size
    }

    /** Imports a file the user picked in the system "open" dialog. */
    suspend fun importFrom(uri: Uri): ImportResult {
        val resolver = checkNotNull(contentResolver)
        val text = withContext(Dispatchers.IO) {
            try {
                val stream = resolver.openInputStream(uri) ?: throw IOException("No input stream")
                stream.use { input ->
                    val bytes = input.readNBytesCompat(MAX_FILE_BYTES + 1)
                    if (bytes.size > MAX_FILE_BYTES) throw BackupException(BackupException.Reason.NOT_A_BACKUP)
                    String(bytes, Charsets.UTF_8)
                }
            } catch (e: IOException) {
                throw BackupException(BackupException.Reason.IO_ERROR, e)
            } catch (e: SecurityException) {
                throw BackupException(BackupException.Reason.IO_ERROR, e)
            }
        }
        return importJson(text)
    }

    companion object {
        const val FORMAT_VERSION = 1

        /** A backup bigger than this is surely not ours; refuse instead of running out of memory. */
        private const val MAX_FILE_BYTES = 50 * 1024 * 1024
    }
}

private fun String.key(): String = lowercase(Locale.ROOT)

/** Reads at most [limit] bytes (InputStream.readNBytes needs Android 13+). */
private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(64 * 1024)
    var total = 0
    while (total < limit) {
        val read = read(buffer, 0, minOf(buffer.size, limit - total))
        if (read < 0) break
        out.write(buffer, 0, read)
        total += read
    }
    return out.toByteArray()
}
