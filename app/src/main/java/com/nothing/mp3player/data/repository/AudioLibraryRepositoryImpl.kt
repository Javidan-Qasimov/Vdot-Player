package com.nothing.mp3player.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.nothing.mp3player.domain.repository.AudioLibraryRepository
import com.nothing.mp3player.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AudioLibraryRepositoryImpl(private val context: Context) : AudioLibraryRepository {

    override suspend fun loadSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        songs.addAll(loadAppOwnedMusic())
        songs.addAll(loadIndexedMusic())
        songs
    }

    private fun loadAppOwnedMusic(): List<Song> = buildList {
        context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.listFiles()?.forEach { file ->
            val supported = file.isFile && file.extension.lowercase() in SUPPORTED_EXTENSIONS
            if (supported) {
                add(
                    Song(
                        id = file.absolutePath.hashCode().toLong(),
                        title = file.nameWithoutExtension,
                        artist = "Offline",
                        uri = Uri.fromFile(file),
                        albumId = -2L,
                        durationMs = 0L
                    )
                )
            }
        }
    }

    private fun loadIndexedMusic(): List<Song> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION
        )

        return try {
            buildList {
                context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                    null,
                    "${MediaStore.Audio.Media.DATE_ADDED} DESC"
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        add(
                            Song(
                                id = id,
                                title = cursor.getString(titleColumn) ?: "Unknown",
                                artist = cursor.getString(artistColumn) ?: "Unknown Artist",
                                uri = ContentUris.withAppendedId(
                                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                    id
                                ),
                                albumId = cursor.getLong(albumIdColumn),
                                durationMs = cursor.getLong(durationColumn)
                            )
                        )
                    }
                }
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
            emptyList()
        }
    }

    private companion object {
        val SUPPORTED_EXTENSIONS = setOf("mp3", "wav", "m4a")
    }
}
