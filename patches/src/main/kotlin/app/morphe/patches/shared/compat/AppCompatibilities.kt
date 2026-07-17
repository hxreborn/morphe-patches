/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Central Morphe `Compatibility` metadata so Morphe Manager shows human-readable
 * app names and icons. No targets: patches apply to any app version.
 */
package app.morphe.patches.shared.compat

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.Compatibility

@Suppress("MemberVisibilityCanBePrivate")
internal object AppCompatibilities {
    val FORUS = Compatibility(
        name = "ForusApp",
        packageName = "com.myvitale.forus",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x0772BA,
    )

    val PROJECTIVY = Compatibility(
        name = "Projectivy Launcher",
        packageName = "com.spocky.projengmenu",
        apkFileType = ApkFileType.APK,
        appIconColor = 0xF08029,
    )

    val SHOWLY = Compatibility(
        name = "Showly",
        packageName = "com.michaldrabik.showly2",
        apkFileType = ApkFileType.APK,
        appIconColor = 0xF44336,
    )
}
