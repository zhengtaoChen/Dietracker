package com.diettracker.data.repo

import android.content.Context
import android.net.Uri
import com.diettracker.domain.repository.PhotoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class PhotoRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
) : PhotoRepository {

    private val appContext: Context = context

    override suspend fun savePhoto(uri: Uri): String = withContext(Dispatchers.IO) {
        val target = File(dir().apply { mkdirs() }, "${UUID.randomUUID()}.jpg")
        val input = if (uri.scheme == "file") {
            FileInputStream(File(uri.path ?: return@withContext throw IllegalArgumentException("无效的图片路径")))
        } else {
            appContext.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("无法读取所选图片")
        }
        input.use { ins -> target.outputStream().use { out -> ins.copyTo(out) } }
        relative(target)
    }

    override fun deletePhoto(relativePath: String) {
        val file = resolve(relativePath)
        if (file.exists()) file.delete()
    }

    override fun resolve(relativePath: String): File = File(appContext.filesDir, relativePath)

    override fun listPhotos(): List<String> =
        dir().listFiles()?.map { relative(it) }?.sorted() ?: emptyList()

    private fun dir(): File = File(appContext.filesDir, DIR)

    private fun relative(file: File): String = "$DIR/${file.name}"

    companion object {
        const val DIR = "photos"
    }
}
