/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.utils

import com.nextcloud.client.preferences.SubFolderRule
import com.owncloud.android.utils.FileStorageUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.util.Locale

class FileStorageUtilsTest {
    @Test
    fun testInstantUploadPathSubfolder() {
        val file = File("/sdcard/DCIM/subfolder/file.jpg")
        val syncedFolderLocalPath = "/sdcard/DCIM"
        val syncedFolderRemotePath = "/Camera"
        val subFolderByDate = false
        val dateTaken = 123123123L
        val subFolderRule = SubFolderRule.YEAR_MONTH

        val result = FileStorageUtils.getInstantUploadFilePath(
            file,
            Locale.ROOT,
            syncedFolderRemotePath,
            syncedFolderLocalPath,
            dateTaken,
            subFolderByDate,
            subFolderRule
        )
        val expected = "/Camera/subfolder/file.jpg"

        assertEquals(expected, result)
    }

    @Test
    fun testInstantUploadPathNoSubfolder() {
        val file = File("/sdcard/DCIM/file.jpg")
        val syncedFolderLocalPath = "/sdcard/DCIM"
        val syncedFolderRemotePath = "/Camera"
        val subFolderByDate = false
        val dateTaken = 123123123L
        val subFolderRule = SubFolderRule.YEAR_MONTH

        val result = FileStorageUtils.getInstantUploadFilePath(
            file,
            Locale.ROOT,
            syncedFolderRemotePath,
            syncedFolderLocalPath,
            dateTaken,
            subFolderByDate,
            subFolderRule
        )
        val expected = "/Camera/file.jpg"

        assertEquals(expected, result)
    }

    @Test
    fun testInstantUploadPathEmptyDateZero() {
        val file = File("/sdcard/DCIM/file.jpg")
        val syncedFolderLocalPath = "/sdcard/DCIM"
        val syncedFolderRemotePath = "/Camera"
        val subFolderByDate = true
        val dateTaken = 0L
        var subFolderRule = SubFolderRule.YEAR_MONTH

        val result = FileStorageUtils.getInstantUploadFilePath(
            file,
            Locale.ROOT,
            syncedFolderRemotePath,
            syncedFolderLocalPath,
            dateTaken,
            subFolderByDate,
            subFolderRule
        )
        val expected = "/Camera/file.jpg"

        assertEquals(expected, result)
    }

    @Test
    fun testInstantUploadPath() {
        val file = File("/sdcard/DCIM/file.jpg")
        val syncedFolderLocalPath = "/sdcard/DCIM"
        val syncedFolderRemotePath = "/Camera"
        val subFolderByDate = false
        val dateTaken = 123123123L
        var subFolderRule = SubFolderRule.YEAR_MONTH

        val result = FileStorageUtils.getInstantUploadFilePath(
            file,
            Locale.ROOT,
            syncedFolderRemotePath,
            syncedFolderLocalPath,
            dateTaken,
            subFolderByDate,
            subFolderRule
        )
        val expected = "/Camera/file.jpg"

        assertEquals(expected, result)
    }

    @Test
    fun testInstantUploadPathWithSubfolderByDate() {
        val file = File("/sdcard/DCIM/file.jpg")
        val syncedFolderLocalPath = "/sdcard/DCIM"
        val syncedFolderRemotePath = "/Camera"
        val subFolderByDate = true
        val dateTaken = 1569918628000L
        val subFolderRule = SubFolderRule.YEAR_MONTH

        val result = FileStorageUtils.getInstantUploadFilePath(
            file,
            Locale.ROOT,
            syncedFolderRemotePath,
            syncedFolderLocalPath,
            dateTaken,
            subFolderByDate,
            subFolderRule
        )
        val expected = "/Camera/2019/10/file.jpg"

        assertEquals(expected, result)
    }

    @Test
    fun testInstantUploadPathWithSubfolderFile() {
        val file = File("/sdcard/DCIM/subfolder/file.jpg")
        val syncedFolderLocalPath = "/sdcard/DCIM"
        val syncedFolderRemotePath = "/Camera"
        val subFolderByDate = false
        val dateTaken = 123123123L
        var subFolderRule = SubFolderRule.YEAR_MONTH

        val result = FileStorageUtils.getInstantUploadFilePath(
            file,
            Locale.ROOT,
            syncedFolderRemotePath,
            syncedFolderLocalPath,
            dateTaken,
            subFolderByDate,
            subFolderRule
        )
        val expected = "/Camera/subfolder/file.jpg"

        assertEquals(expected, result)
    }

    @Test
    fun testInstantUploadPathWithSubfolderByDateWithSubfolderFile() {
        val file = File("/sdcard/DCIM/subfolder/file.jpg")
        val syncedFolderLocalPath = "/sdcard/DCIM"
        val syncedFolderRemotePath = "/Camera"
        val subFolderByDate = true
        val dateTaken = 1569918628000L
        var subFolderRule = SubFolderRule.YEAR_MONTH

        val result = FileStorageUtils.getInstantUploadFilePath(
            file,
            Locale.ROOT,
            syncedFolderRemotePath,
            syncedFolderLocalPath,
            dateTaken,
            subFolderByDate,
            subFolderRule
        )
        val expected = "/Camera/2019/10/subfolder/file.jpg"

        assertEquals(expected, result)
    }
}
