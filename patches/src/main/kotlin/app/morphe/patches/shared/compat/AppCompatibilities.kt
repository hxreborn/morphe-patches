/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Central Morphe `Compatibility` metadata so Morphe Manager shows human-readable
 * app names and icons. Targets gate patching. An unlisted app version is refused.
 */
package app.morphe.patches.shared.compat

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

@Suppress("MemberVisibilityCanBePrivate")
internal object AppCompatibilities {
    val ALL_IN_ONE_CALCULATOR = Compatibility(
        name = "All-In-One Calculator",
        packageName = "all.in.one.calculator",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x455A64,
        targets = listOf(AppTarget(version = "3.4.0", versionCode = 340, minSdk = 24)),
    )

    val ALPINEQUEST = Compatibility(
        name = "AlpineQuest",
        packageName = "psyberia.alpinequest.free",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x0057BD,
    )

    val AUDIBLE = Compatibility(
        name = "Audible",
        packageName = "com.audible.application",
        apkFileType = ApkFileType.APK,
        appIconColor = 0xF8991C,
    )

    val BETTERSLEEP = Compatibility(
        name = "BetterSleep",
        packageName = "ipnossoft.rma.free",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x1D204B,
        targets = listOf(AppTarget(version = "26.15", versionCode = 26799, minSdk = 26)),
    )

    val CX_FILE_EXPLORER = Compatibility(
        name = "Cx File Explorer",
        packageName = "com.cxinventor.file.explorer",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x5167F6,
        targets = listOf(AppTarget(version = "2.7.8", versionCode = 278, minSdk = 21)),
    )

    val ETSY = Compatibility(
        name = "Etsy",
        packageName = "com.etsy.android",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0xF1641E,
        targets = listOf(AppTarget(version = "7.90.0", versionCode = 79000153, minSdk = 32)),
    )

    val FORUS = Compatibility(
        name = "ForusApp",
        packageName = "com.myvitale.forus",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x0772BA,
    )

    val GAMMA_SCAN = Compatibility(
        name = "QR & Barcode Scanner",
        packageName = "com.gamma.scan",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x2196F3,
        targets = listOf(AppTarget(version = "2.2.221", versionCode = 221, minSdk = 24)),
    )

    val KICK = Compatibility(
        name = "Kick",
        packageName = "com.kick.mobile",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x53FC18,
    )

    val NOTESNOOK = Compatibility(
        name = "Notesnook",
        packageName = "com.streetwriters.notesnook",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x008837,
    )

    val PERPLEXITY = Compatibility(
        name = "Perplexity",
        packageName = "ai.perplexity.app.android",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x20808D,
    )

    val PHOTO_EDITOR_PRO = Compatibility(
        name = "Photo Editor Pro",
        packageName = "photo.editor.photoeditor.photoeditorpro",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0xFA2A80,
        signatures = setOf(
            "868aa1a8470b4214e88a5c9e65a1dbe475a32e1da7a23079ba6e0be0bd50b621",
        ),
        targets = listOf(AppTarget(version = "1.791.265", versionCode = 265100, minSdk = 28)),
    )

    val PROJECTIVY = Compatibility(
        name = "Projectivy Launcher",
        packageName = "com.spocky.projengmenu",
        apkFileType = ApkFileType.APK,
        appIconColor = 0xF08029,
    )

    val PROTON_MAIL = Compatibility(
        name = "Proton Mail",
        packageName = "ch.protonmail.android",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x6D4AFF,
        signatures = setOf(
            "dcc9439ec1a6c6a8d0203f3423ee42bcc8b970628e53cb73a0393f398dd5b853",
        ),
        targets = listOf(
            AppTarget(version = "7.11.5", versionCode = 18317, minSdk = 29),
            AppTarget(version = "7.10.4", versionCode = 17667, minSdk = 29),
        ),
    )

    val QURANIFY = Compatibility(
        name = "Quranify",
        packageName = "com.mchutov.Quranify",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x141414,
        signatures = setOf(
            "6832f51be89158c630aa9a166c10781f2f68fc3bf1ef1e776e4fa2c218f0010d",
        ),
        targets = listOf(AppTarget(version = "2.2.8", versionCode = 77, minSdk = 33)),
    )

    val RATEGLANCE = Compatibility(
        name = "RateGlance",
        packageName = "com.sry.rateglance",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x0D192C,
    )

    val READERA = Compatibility(
        name = "ReadEra",
        packageName = "org.readera",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x0061BD,
        targets = listOf(AppTarget(version = "26.05.20+2300", versionCode = 2300, minSdk = 16)),
    )

    val RISE = Compatibility(
        name = "RISE Sleep Tracker",
        packageName = "com.risesci.nyx",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x8E58FF,
        targets = listOf(
            AppTarget(version = "Android V1.78.49", minSdk = 26),
            AppTarget(version = "Android V1.78.47", minSdk = 26),
        ),
    )

    val RUBBER_BANDS = Compatibility(
        name = "Rubber Bands",
        packageName = "app.rubberbands.fit",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0xFD8700,
    )

    val SHOWLY = Compatibility(
        name = "Showly",
        packageName = "com.michaldrabik.showly2",
        apkFileType = ApkFileType.APK,
        appIconColor = 0xF44336,
    )

    val SHOWLY_3_70 = Compatibility(
        name = "Showly",
        packageName = "com.michaldrabik.showly2",
        apkFileType = ApkFileType.APK,
        appIconColor = 0xF44336,
        targets = listOf(AppTarget(version = "3.70.0", versionCode = 840, minSdk = 23)),
    )

    val SYMFONIUM = Compatibility(
        name = "Symfonium",
        packageName = "app.symfonik.music.player",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0xE22728,
        targets = listOf(
            AppTarget(version = "14.0.0", versionCode = 127708, minSdk = 28),
            AppTarget(version = "14.1.0", versionCode = 127734, minSdk = 32),
            AppTarget(version = "14.0.0 TV", versionCode = 227708, minSdk = 32),
        ),
    )

    val TRAINLINE = Compatibility(
        name = "Trainline",
        packageName = "com.thetrainline",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x00A88F,
        targets = listOf(AppTarget(version = "407.0.0.178994", versionCode = 1278994, minSdk = 26)),
    )
}
