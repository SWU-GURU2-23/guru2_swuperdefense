package com.adroid.guru2_swuperdefense.data.local

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.util.UUID

/** 선택한 파일을 앱 내부 저장소로 복사·삭제한다. 원본 URI 권한에 의존하지 않아 원본이 사라져도 복사본은 열 수 있다. */
object EvidenceFileStore {
    data class StoredFile(
        val localFileName: String,
        val originalFileName: String,
        val mimeType: String?
    )

    fun copyIntoAppStorage(context: Context, sourceUri: Uri): StoredFile {
        val resolver = context.contentResolver
        val originalName = queryDisplayName(context, sourceUri)
        val safeExtension = originalName
            .substringAfterLast('.', "")
            .lowercase()
            .filter(Char::isLetterOrDigit)
            .take(10)
        val localName = buildString {
            append(UUID.randomUUID())
            if (safeExtension.isNotBlank()) {
                append('.')
                append(safeExtension)
            }
        }

        val directory = evidenceDirectory(context)
        check(directory.exists() || directory.mkdirs()) {
            "증거 저장 폴더를 만들 수 없습니다."
        }
        val destination = File(directory, localName)

        try {
            val input = requireNotNull(resolver.openInputStream(sourceUri)) {
                "선택한 파일을 열 수 없습니다."
            }
            input.use { source ->
                destination.outputStream().use(source::copyTo)
            }
        } catch (error: Throwable) {
            destination.delete()
            throw error
        }

        return StoredFile(
            localFileName = localName,
            originalFileName = originalName,
            mimeType = resolver.getType(sourceUri)
        )
    }

    fun uriString(context: Context, localFileName: String): String =
        Uri.fromFile(File(evidenceDirectory(context), localFileName)).toString()

    fun delete(context: Context, localFileName: String): Boolean =
        File(evidenceDirectory(context), localFileName).let { file ->
            !file.exists() || file.delete()
        }

    fun totalBytes(context: Context): Long =
        evidenceDirectory(context)
            .listFiles()
            .orEmpty()
            .filter(File::isFile)
            .sumOf(File::length)

    private fun evidenceDirectory(context: Context): File =
        File(context.filesDir, "evidence")

    private fun queryDisplayName(context: Context, uri: Uri): String {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (column >= 0 && cursor.moveToFirst()) {
                return cursor.getString(column)
            }
        }
        return uri.lastPathSegment ?: "evidence_file"
    }
}
