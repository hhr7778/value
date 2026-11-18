package com.example

import com.example.model.VaultCategory
import com.example.model.formatBytesReadable
import org.junit.Assert.assertEquals
import org.junit.Test

class VaultCryptoAndSortTest {

    @Test
    fun autoSortMapsAllSupportedExtensionsCorrectly() {
        assertEquals(VaultCategory.PHOTOS, VaultCategory.autoSortForExtension("jpg"))
        assertEquals(VaultCategory.PHOTOS, VaultCategory.autoSortForExtension("PNG"))
        assertEquals(VaultCategory.PHOTOS, VaultCategory.autoSortForExtension("heic"))

        assertEquals(VaultCategory.VIDEOS, VaultCategory.autoSortForExtension("mp4"))
        assertEquals(VaultCategory.VIDEOS, VaultCategory.autoSortForExtension("mkv"))
        assertEquals(VaultCategory.VIDEOS, VaultCategory.autoSortForExtension("mov"))

        assertEquals(VaultCategory.MUSIC, VaultCategory.autoSortForExtension("mp3"))
        assertEquals(VaultCategory.MUSIC, VaultCategory.autoSortForExtension("wav"))
        assertEquals(VaultCategory.MUSIC, VaultCategory.autoSortForExtension("m4a"))

        assertEquals(VaultCategory.PDFS, VaultCategory.autoSortForExtension("pdf"))

        assertEquals(VaultCategory.DOCUMENTS, VaultCategory.autoSortForExtension("doc"))
        assertEquals(VaultCategory.DOCUMENTS, VaultCategory.autoSortForExtension("docx"))
        assertEquals(VaultCategory.DOCUMENTS, VaultCategory.autoSortForExtension("xls"))
        assertEquals(VaultCategory.DOCUMENTS, VaultCategory.autoSortForExtension("xlsx"))
        assertEquals(VaultCategory.DOCUMENTS, VaultCategory.autoSortForExtension("txt"))

        assertEquals(VaultCategory.OTHERS, VaultCategory.autoSortForExtension("zip"))
        assertEquals(VaultCategory.OTHERS, VaultCategory.autoSortForExtension("apk"))
    }

    @Test
    fun formatBytesReadableFormatsProperUnits() {
        assertEquals("0 B", formatBytesReadable(0L))
        assertEquals("512 B", formatBytesReadable(512L))
        assertEquals("2.0 KB", formatBytesReadable(2048L))
        assertEquals("1.00 GB", formatBytesReadable(1024L * 1024L * 1024L))
    }
}
