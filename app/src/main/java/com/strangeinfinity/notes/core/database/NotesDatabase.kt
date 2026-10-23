package com.strangeinfinity.notes.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.strangeinfinity.notes.core.database.dao.AttachmentDao
import com.strangeinfinity.notes.core.database.dao.FolderDao
import com.strangeinfinity.notes.core.database.dao.NoteDao
import com.strangeinfinity.notes.core.database.dao.TagDao
import com.strangeinfinity.notes.core.database.entity.AttachmentEntity
import com.strangeinfinity.notes.core.database.entity.FolderEntity
import com.strangeinfinity.notes.core.database.entity.NoteEntity
import com.strangeinfinity.notes.core.database.entity.NoteTagCrossRef
import com.strangeinfinity.notes.core.database.entity.TagEntity

@Database(
    entities = [
        NoteEntity::class,
        FolderEntity::class,
        TagEntity::class,
        NoteTagCrossRef::class,
        AttachmentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun tagDao(): TagDao
    abstract fun attachmentDao(): AttachmentDao

    companion object {
        private const val DATABASE_NAME = "strangeinfinity_notes.db"

        @Volatile
        private var INSTANCE: NotesDatabase? = null

        fun getInstance(context: Context): NotesDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NotesDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
