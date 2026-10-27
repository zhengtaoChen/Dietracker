package com.diettracker.domain.repository

import android.net.Uri
import java.io.File

/**
 * 照片文件管理（App 私有目录 `filesDir/photos/`）。
 *
 * DB 只保存相对路径（如 `photos/xxx.jpg`），绝对路径由运行时的 filesDir 推导，
 * 避免设备/重装后路径失效。删除记录或清理无主文件时，通过本接口操作真实文件。
 */
interface PhotoRepository {

    /** 将内容/文件 [Uri] 复制到私有目录，返回相对路径。 */
    suspend fun savePhoto(uri: Uri): String

    /** 删除私有目录中的照片文件（按相对路径），文件不存在时静默忽略。 */
    fun deletePhoto(relativePath: String)

    /** 相对路径解析为绝对 [File]（可能不存在）。 */
    fun resolve(relativePath: String): File

    /** 列出私有目录下所有照片文件的相对路径。 */
    fun listPhotos(): List<String>
}
