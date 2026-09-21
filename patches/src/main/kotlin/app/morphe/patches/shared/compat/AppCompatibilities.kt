/*
 * Copyright (C) 2026 hxreborn
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
        targets = listOf(AppTarget(version = "2.4.0e", versionCode = 412)),
    )

    val ATVTOOLS = Compatibility(
        name = "atvTools",
        packageName = "dev.vodik7.atvtools",
        apkFileType = ApkFileType.XAPK_REQUIRED,
        appIconColor = 0x2F2F2F,
        targets = listOf(AppTarget(version = "1.3.2", versionCode = 49, minSdk = 26)),
    )

    val AUDIBLE = Compatibility(
        name = "Audible",
        packageName = "com.audible.application",
        apkFileType = ApkFileType.APK,
        appIconColor = 0xF8991C,
        targets = listOf(AppTarget(version = "26.30.05", versionCode = 2090263005, minSdk = 28)),
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

    val DWG_FASTVIEW = Compatibility(
        name = "DWG FastView",
        packageName = "com.gstarmc.android",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x1AACAC,
        targets = listOf(
            AppTarget(version = "5.19.4", versionCode = 614, minSdk = 26),
            AppTarget(version = "5.19.6", versionCode = 616, minSdk = 26),
            AppTarget(version = "5.20.0", versionCode = 620, minSdk = 26),
            AppTarget(version = "5.21.0", versionCode = 630, minSdk = 26),
        ),
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
        targets = listOf(AppTarget(version = "3.0.15", versionCode = 96, minSdk = 26)),
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

    val KLASSIK_RADIO = Compatibility(
        name = "Klassik Radio+",
        packageName = "de.klassikradio.app",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x000000,
        targets = listOf(AppTarget(version = "p5.11.1", versionCode = 50126, minSdk = 29)),
    )

    val MOVIEBOX = Compatibility(
        name = "MovieBox",
        packageName = "com.community.oneroom",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x30E66D,
        targets = listOf(
            AppTarget(version = "4.0.02.0828.03", versionCode = 50020125, minSdk = 29),
            AppTarget(version = "4.0.02.0831.03", versionCode = 50020126, minSdk = 29),
            AppTarget(version = "4.0.02.0903.02", versionCode = 50020128, minSdk = 29),
        ),
    )

    val MYMOVESET = Compatibility(
        name = "MyMoveset",
        packageName = "com.soulbreakers.mymoveset",
        apkFileType = ApkFileType.XAPK,
        appIconColor = 0x9F1C3B,
        targets = listOf(AppTarget(version = "1.3.1", versionCode = 14, minSdk = 24)),
    )

    val NOTESNOOK = Compatibility(
        name = "Notesnook",
        packageName = "com.streetwriters.notesnook",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x008837,
        targets = listOf(AppTarget(version = "3.4.12", versionCode = 4197422, minSdk = 24)),
    )

    val ONE_WEATHER = Compatibility(
        name = "1Weather",
        packageName = "com.handmark.expressweather",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x089AED,
        targets = listOf(
            AppTarget(version = "13.1.0", versionCode = 130010, minSdk = 26),
            AppTarget(version = "12.9.3", versionCode = 120093, minSdk = 26),
        ),
    )

    val PERPLEXITY = Compatibility(
        name = "Perplexity",
        packageName = "ai.perplexity.app.android",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x20808D,
        targets = listOf(AppTarget(version = "2.95.0", versionCode = 260642, minSdk = 32)),
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
        targets = listOf(
            AppTarget(version = "4.71", versionCode = 95, minSdk = 23),
            AppTarget(version = "4.70", versionCode = 92, minSdk = 23),
        ),
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
        targets = listOf(AppTarget(version = "1.14.8", versionCode = 278, minSdk = 32)),
    )

    val READERA = Compatibility(
        name = "ReadEra",
        packageName = "org.readera",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x0061BD,
        targets = listOf(AppTarget(version = "26.05.20+2300", versionCode = 2300, minSdk = 16)),
    )

    val REALME_LINK = Compatibility(
        name = "Realme Link",
        packageName = "com.realme.link",
        apkFileType = ApkFileType.XAPK_REQUIRED,
        appIconColor = 0x3575C4,
        targets = listOf(AppTarget(version = "5.5.514.11421", versionCode = 530121, minSdk = 33)),
    )

    val RINGTONE_MAKER = Compatibility(
        name = "Ringtone Maker",
        packageName = "ringtonemaker.musiccutter.customringtones.freeringtonemaker",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x243ECA,
        targets = listOf(
            AppTarget(version = "1.01.99.0909", versionCode = 10153, minSdk = 24),
            AppTarget(version = "1.01.98.0831", versionCode = 10151, minSdk = 24),
            AppTarget(version = "1.01.98.0824", versionCode = 10146, minSdk = 24),
            AppTarget(version = "1.01.97.0818", versionCode = 10145, minSdk = 24),
            AppTarget(version = "1.01.96.0716", versionCode = 10142, minSdk = 24),
            AppTarget(version = "1.01.94.0602", versionCode = 10139, minSdk = 24),
            AppTarget(version = "1.01.90.0421", versionCode = 10131, minSdk = 24),
        ),
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
        targets = listOf(AppTarget(version = "3.9", versionCode = 291, minSdk = 26)),
    )

    val SHOWLY = Compatibility(
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
            AppTarget(version = "15.0.1", versionCode = 127798, minSdk = 32),
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

    val VPNIFY = Compatibility(
        name = "vpnify",
        packageName = "com.vpn.free.hotspot.secure.vpnify",
        apkFileType = ApkFileType.XAPK_REQUIRED,
        appIconColor = 0x0A84FF,
        signatures = setOf("6a0e2299884977821273e9b69252bc9f53aad9621dd911336b569872cb2b5707"),
        targets = listOf(
            AppTarget(version = "2.2.9.9", minSdk = 29),
            AppTarget(version = "2.3.0", minSdk = 29),
        ),
    )

    val VPN_SUPER = Compatibility(
        name = "VPN Super Unlimited Proxy",
        packageName = "com.free.vpn.super.hotspot.open",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x007DFF,
        targets = listOf(AppTarget(version = "2.32.0", versionCode = 23200, minSdk = 32)),
    )
}
