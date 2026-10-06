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
        signatures = setOf(
            "e84de8a3c91c9806d2eefe29e4caf433d2672dcdcc8def0c5d55e98da40138bd",
        ),
        targets = listOf(AppTarget(version = "3.4.0", versionCode = 340, minSdk = 24)),
    )

    val ALL_VIDEO_PLAYER = Compatibility(
        name = "All Video Player App",
        packageName = "com.allformatplayer.streamvideoplayer",
        apkFileType = ApkFileType.XAPK_REQUIRED,
        appIconColor = 0x0072FF,
        signatures = setOf(
            "567e9fdfe00635da716359222fee014a6a8621cf4b7fc08c579eaafba59bf690",
        ),
        targets = listOf(AppTarget(version = "1.4", versionCode = 14, minSdk = 26)),
    )

    val ALPINEQUEST = Compatibility(
        name = "AlpineQuest",
        packageName = "psyberia.alpinequest.free",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x0057BD,
        targets = listOf(AppTarget(version = "2.4.0e", versionCode = 412)),
    )

    val ATLOMAPS = Compatibility(
        name = "AtloMaps",
        packageName = "com.atlogis.atlomaps",
        apkFileType = ApkFileType.XAPK,
        appIconColor = 0x0683DF,
        targets = listOf(
            AppTarget(version = "1.0.6", versionCode = 153, minSdk = 29),
            AppTarget(version = "1.1.0", versionCode = 172, minSdk = 32),
        ),
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
        targets = listOf(
            AppTarget(version = "26.30.05", versionCode = 2090263005, minSdk = 28),
            AppTarget(version = "26.38.08", versionCode = 2090263808, minSdk = 28),
        ),
    )

    val BETTERSLEEP = Compatibility(
        name = "BetterSleep",
        packageName = "ipnossoft.rma.free",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x1D204B,
        signatures = setOf(
            "22c4c3a7776a3b54b9380a6021fa86cea27fd6844792ea6c579ba9ad0d2dbd71",
        ),
        targets = listOf(AppTarget(version = "26.17", versionCode = 26910, minSdk = 26)),
    )

    val BLURWALL = Compatibility(
        name = "BlurWall",
        packageName = "apps.automan.blurwallpaper",
        apkFileType = ApkFileType.XAPK_REQUIRED,
        appIconColor = 0x101010,
        signatures = setOf(
            "9f2432d1e831e49c5fbfc4d8a089062ee6ad42f3e3f1402e9085c0973a71ef24",
        ),
        targets = listOf(AppTarget(version = "2.9.8", versionCode = 31, minSdk = 23)),
    )

    val CATZY = Compatibility(
        name = "Catzy",
        packageName = "com.nieruo.healthapp",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x4059B9,
        signatures = setOf(
            "9cf85b81cc307a9dc367d5421dbe47737724275e92781c95768a8eef9e529a39",
        ),
        targets = listOf(AppTarget(version = "1.61.0", versionCode = 281, minSdk = 24)),
    )

    val CX_FILE_EXPLORER = Compatibility(
        name = "Cx File Explorer",
        packageName = "com.cxinventor.file.explorer",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x5167F6,
        targets = listOf(
            AppTarget(version = "2.7.8", versionCode = 278, minSdk = 21),
            AppTarget(version = "2.7.9", versionCode = 279, minSdk = 21),
            AppTarget(version = "2.8.1", versionCode = 281, minSdk = 32),
        ),
    )

    val CXXDROID = Compatibility(
        name = "Cxxdroid",
        packageName = "ru.iiec.cxxdroid",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x51596D,
        targets = listOf(
            AppTarget(version = "5.6_arm64", versionCode = 1074, minSdk = 21),
            AppTarget(version = "6.0_arm64", versionCode = 1075, minSdk = 21),
        ),
    )

    val DWG_FASTVIEW = Compatibility(
        name = "DWG FastView",
        packageName = "com.gstarmc.android",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x1AACAC,
        signatures = setOf(
            "8fdce506f048af0e813622bbe20448a49fccd7606808e7b3c00c5634aa03ec5f",
        ),
        targets = listOf(
            AppTarget(version = "5.19.4", versionCode = 614, minSdk = 26),
            AppTarget(version = "5.19.6", versionCode = 616, minSdk = 26),
            AppTarget(version = "5.20.0", versionCode = 620, minSdk = 26),
            AppTarget(version = "5.21.0", versionCode = 630, minSdk = 26),
        ),
    )

    val ECHO_EQUALIZER = Compatibility(
        name = "Echo Equalizer",
        packageName = "com.hapibits.soundlift",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x2E2E45,
        signatures = setOf(
            "96cdac84105ee04a12a48110d37ec60888aa8fbff8220345888d7f9c12c2f3fa",
        ),
        targets = listOf(AppTarget(version = "9.2", versionCode = 86, minSdk = 32)),
    )

    val ECHOGRAM = Compatibility(
        name = "Echogram",
        packageName = "com.liori.echogram",
        apkFileType = ApkFileType.XAPK_REQUIRED,
        appIconColor = 0x191E31,
        signatures = setOf(
            "331fa00a81a7f2e70aeaec25ee709aa4f1f17062d61e7aa6048f14abc47f0d1a",
        ),
        targets = listOf(AppTarget(version = "1.0.7.0", versionCode = 138, minSdk = 32)),
    )

    val ETSY = Compatibility(
        name = "Etsy",
        packageName = "com.etsy.android",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0xF1641E,
        targets = listOf(AppTarget(version = "7.97.0", versionCode = 79700150, minSdk = 32)),
    )

    val FAST_STL_VIEWER = Compatibility(
        name = "Fast STL Viewer",
        packageName = "com.performance.meshview",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x1A73B2,
        signatures = setOf(
            "925c7b1df5689fef2371e1f6e71e98a3956295df95221baa06145222aa89f769",
        ),
        targets = listOf(AppTarget(version = "2.84", versionCode = 309, minSdk = 24)),
    )

    val FDDB = Compatibility(
        name = "Fddb",
        packageName = "com.fddb",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x006495,
        signatures = setOf(
            "9caf67d3efca4ffb9f888256a5963c6117c28051a0669bc3d8bb098291ea9f4f",
        ),
        targets = listOf(AppTarget(version = "v7.8.4-Build-1-gms-release", versionCode = 7080401, minSdk = 26)),
    )

    val FORUS = Compatibility(
        name = "ForusApp",
        packageName = "com.myvitale.forus",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x0772BA,
        targets = listOf(
            AppTarget(version = "3.0.15", versionCode = 96, minSdk = 26),
            AppTarget(version = "3.0.18", versionCode = 99, minSdk = 26),
        ),
    )

    val GAMMA_SCAN = Compatibility(
        name = "QR & Barcode Scanner",
        packageName = "com.gamma.scan",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x2196F3,
        targets = listOf(
            AppTarget(version = "2.2.221", versionCode = 221, minSdk = 24),
            AppTarget(version = "2.2.224", versionCode = 224, minSdk = 24),
        ),
    )

    val HINDU_CALENDAR = Compatibility(
        name = "Hindu Calendar",
        packageName = "com.alokmandavgane.hinducalendar",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0xF89532,
        signatures = setOf(
            "1dc3da6664982c69c3c7326282beaf0c6f153daa7da36bd7ec4cd4313e664651",
        ),
        targets = listOf(AppTarget(version = "9.3.0", versionCode = 156, minSdk = 28)),
    )

    val JVDROID = Compatibility(
        name = "Jvdroid",
        packageName = "ru.iiec.jvdroid",
        apkFileType = ApkFileType.APK,
        appIconColor = 0xE76F00,
        targets = listOf(AppTarget(version = "2.8", versionCode = 1041, minSdk = 21)),
    )

    val JOYN = Compatibility(
        name = "Joyn",
        packageName = "de.prosiebensat1digital.seventv",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x000F15,
        signatures = setOf("71733196d1b3d681ee981cb02563e6778585f1891ed74b1d31604c1444c6b10a"),
        targets = listOf(AppTarget(version = "6.9.0-AOS-609012264", versionCode = 609012264, minSdk = 24)),
    )

    val KICK = Compatibility(
        name = "Kick",
        packageName = "com.kick.mobile",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x53FC18,
    )

    val KEEPA = Compatibility(
        name = "Keepa",
        packageName = "com.keepa.mobile",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x256A93,
        signatures = setOf(
            "43329e3a67415def40f307e5a7b3343498db9a821f436785d0a5487c636b9bcd",
        ),
        targets = listOf(AppTarget(version = "6.2.1", versionCode = 56014, minSdk = 30)),
    )

    val KLASSIK_RADIO = Compatibility(
        name = "Klassik Radio+",
        packageName = "de.klassikradio.app",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x000000,
        targets = listOf(AppTarget(version = "p5.12.0", versionCode = 50138, minSdk = 29)),
    )

    val LED_BLINKER = Compatibility(
        name = "LED Blinker",
        packageName = "com.ledblinker",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x190C53,
        signatures = setOf(
            "40a44cbdf290932098b46c12ae0c9932c82211659a27253ddda4e5ca62780dea",
        ),
        targets = listOf(AppTarget(version = "26.01.08", versionCode = 12506, minSdk = 32)),
    )

    val MEMONEET = Compatibility(
        name = "MemoNeet",
        packageName = "com.adithya.memoneet",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x8865BC,
        signatures = setOf(
            "0b35fb5df963d4382ba19bf70923a66a056334335721a4fe395bf9365721a151",
            "c3f2c1d09fe211d59fe243da4a639bd610a4cee4ee54ad5380c03f83310378cb",
        ),
        targets = listOf(AppTarget(version = "62.6", versionCode = 626, minSdk = 24)),
    )

    val MOVIEBOX = Compatibility(
        name = "MovieBox",
        packageName = "com.community.oneroom",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x30E66D,
        signatures = setOf(
            "03d4b741632d136bc23e2de4c6adcdb7712f1980b619c183221801730bbd0410",
        ),
        targets = listOf(
            AppTarget(version = "4.0.02.0828.03", versionCode = 50020125, minSdk = 29),
            AppTarget(version = "4.0.02.0831.03", versionCode = 50020126, minSdk = 29),
            AppTarget(version = "4.0.02.0903.02", versionCode = 50020128, minSdk = 29),
            AppTarget(version = "4.0.03.0918.03", versionCode = 50020129, minSdk = 29),
        ),
    )

    val MYMOVESET = Compatibility(
        name = "MyMoveset",
        packageName = "com.soulbreakers.mymoveset",
        apkFileType = ApkFileType.XAPK,
        appIconColor = 0x9F1C3B,
        targets = listOf(AppTarget(version = "1.3.2", versionCode = 15, minSdk = 24)),
    )

    val NOTESNOOK = Compatibility(
        name = "Notesnook",
        packageName = "com.streetwriters.notesnook",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x008837,
        targets = listOf(
            AppTarget(version = "3.4.12", versionCode = 4197422, minSdk = 24),
            AppTarget(version = "3.4.13", versionCode = 4197423, minSdk = 24),
        ),
    )

    val ONE4HOME = Compatibility(
        name = "One4Home Launcher",
        packageName = "com.one4studio.one4home",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x596580,
        signatures = setOf(
            "5620d2344aa13b4fb30df6bb59df508839af348ad12f5e4f3b95fca02662b77b",
            "b87ed511109b159844ff23ab802e00d56ab9d94d1435d89edd32cf07b62551fa",
        ),
        targets = listOf(
            AppTarget(version = "0.4.72", versionCode = 284, minSdk = 32),
            AppTarget(version = "0.4.97", versionCode = 309, minSdk = 32),
            AppTarget(version = "0.4.98", versionCode = 310, minSdk = 32),
        ),
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
        targets = listOf(
            AppTarget(version = "2.95.0", versionCode = 260642, minSdk = 32),
            AppTarget(version = "2.100.0", versionCode = 260662, minSdk = 32),
        ),
    )

    val PHOTO_EDITOR_PRO = Compatibility(
        name = "Photo Editor Pro",
        packageName = "photo.editor.photoeditor.photoeditorpro",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0xFA2A80,
        signatures = setOf(
            "868aa1a8470b4214e88a5c9e65a1dbe475a32e1da7a23079ba6e0be0bd50b621",
        ),
        targets = listOf(
            AppTarget(version = "1.791.265", versionCode = 265100, minSdk = 28),
            AppTarget(version = "1.802.266", versionCode = 266201, minSdk = 32),
        ),
    )

    val POCKET_WHIP = Compatibility(
        name = "Pocket Whip",
        packageName = "com.greenstone.pocketwhip",
        apkFileType = ApkFileType.XAPK_REQUIRED,
        appIconColor = 0x232323,
        signatures = setOf(
            "27262c6f8cd7448c28622464a0f1b8ce6a848f087a131c229cc0d1846d9b0b5d",
        ),
        targets = listOf(AppTarget(version = "2.3", versionCode = 20, minSdk = 26)),
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
            AppTarget(version = "7.11.9", versionCode = 18324, minSdk = 29),
            AppTarget(version = "7.11.8", versionCode = 18323, minSdk = 29),
            AppTarget(version = "7.11.5", versionCode = 18317, minSdk = 29),
            AppTarget(version = "7.10.4", versionCode = 17667, minSdk = 29),
        ),
    )

    val PROTON_PASS = Compatibility(
        name = "Proton Pass",
        packageName = "proton.android.pass",
        apkFileType = ApkFileType.XAPK_REQUIRED,
        appIconColor = 0x6D4AFF,
        signatures = setOf(
            "dcc9439ec1a6c6a8d0203f3423ee42bcc8b970628e53cb73a0393f398dd5b853",
        ),
        targets = listOf(
            AppTarget(version = "1.40.3", versionCode = 14003373, minSdk = 27),
            AppTarget(version = "1.41.2", versionCode = 14102398, minSdk = 27),
        ),
    )

    val PROTON_VPN = Compatibility(
        name = "Proton VPN",
        packageName = "ch.protonvpn.android",
        apkFileType = ApkFileType.XAPK_REQUIRED,
        appIconColor = 0x6D4AFF,
        signatures = setOf(
            "dcc9439ec1a6c6a8d0203f3423ee42bcc8b970628e53cb73a0393f398dd5b853",
        ),
        targets = listOf(
            AppTarget(version = "5.20.39.0", versionCode = 605203900, minSdk = 26),
            AppTarget(version = "5.20.57.0", versionCode = 605205700, minSdk = 26),
        ),
    )

    val PYDROID = Compatibility(
        name = "Pydroid 3",
        packageName = "ru.iiec.pydroid3",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x3776AB,
        targets = listOf(AppTarget(version = "8.6_arm64", versionCode = 1133, minSdk = 23)),
    )

    val IIEC_APPS = arrayOf(CXXDROID, JVDROID, PYDROID)

    val QURANIFY = Compatibility(
        name = "Quranify",
        packageName = "com.mchutov.Quranify",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x141414,
        signatures = setOf(
            "6832f51be89158c630aa9a166c10781f2f68fc3bf1ef1e776e4fa2c218f0010d",
        ),
        targets = listOf(
            AppTarget(version = "2.2.8", versionCode = 77, minSdk = 33),
            AppTarget(version = "2.2.9", versionCode = 78, minSdk = 33),
        ),
    )

    val RATEGLANCE = Compatibility(
        name = "RateGlance",
        packageName = "com.sry.rateglance",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x0D192C,
        targets = listOf(AppTarget(version = "1.17.6", versionCode = 304, minSdk = 32)),
    )

    val RAINDROP = Compatibility(
        name = "Raindrop.io",
        packageName = "io.raindrop.raindropio",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x1988E0,
        signatures = setOf("ddd75c66578654d085424804cb27c7b6e04fd36616045bfee140d9756ffca563"),
        targets = listOf(AppTarget(version = "4.7.44", versionCode = 8698961, minSdk = 32)),
    )

    val READERA = Compatibility(
        name = "ReadEra",
        packageName = "org.readera",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x0061BD,
        targets = listOf(
            AppTarget(version = "26.05.20+2300", versionCode = 2300, minSdk = 16),
            AppTarget(version = "26.09.29+2320", versionCode = 2320, minSdk = 23),
        ),
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
            AppTarget(version = "Android V1.78.51", minSdk = 26),
            AppTarget(version = "Android V1.78.49", minSdk = 26),
            AppTarget(version = "Android V1.78.47", minSdk = 26),
        ),
    )

    val RUBBER_BANDS = Compatibility(
        name = "Rubber Bands",
        packageName = "app.rubberbands.fit",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0xFD8700,
        targets = listOf(
            AppTarget(version = "3.9", versionCode = 291, minSdk = 26),
            AppTarget(version = "3.11", versionCode = 297, minSdk = 26),
        ),
    )

    val SHOWLY = Compatibility(
        name = "Showly",
        packageName = "com.michaldrabik.showly2",
        apkFileType = ApkFileType.APK,
        appIconColor = 0xF44336,
        targets = listOf(
            AppTarget(version = "3.70.0", versionCode = 840, minSdk = 23),
            AppTarget(version = "3.72.0", versionCode = 843, minSdk = 28),
        ),
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
            AppTarget(version = "15.1.0", versionCode = 127812, minSdk = 32),
            AppTarget(version = "14.0.0 TV", versionCode = 227708, minSdk = 32),
            AppTarget(version = "15.1.0 TV", versionCode = 227812, minSdk = 32),
        ),
    )

    val TERABOX = Compatibility(
        name = "TeraBox",
        packageName = "com.dubox.drive",
        apkFileType = ApkFileType.XAPK,
        appIconColor = 0x226DF6,
        signatures = setOf(
            "b75fd2b70084d7e0813b8f74dd74b62ab79b2aa0ce1372331a1841bd88eed993",
        ),
        targets = listOf(
            AppTarget(version = "4.26.0", versionCode = 698, minSdk = 23),
            AppTarget(version = "4.26.5", versionCode = 699, minSdk = 23),
        ),
    )

    val TRAINLINE = Compatibility(
        name = "Trainline",
        packageName = "com.thetrainline",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x00A88F,
        targets = listOf(
            AppTarget(version = "407.0.0.178994", versionCode = 1278994, minSdk = 26),
            AppTarget(version = "415.0.0.182623", versionCode = 1282623, minSdk = 26),
            AppTarget(version = "415.0.0.182626", versionCode = 1282626, minSdk = 26),
        ),
    )

    val VLLO = Compatibility(
        name = "VLLO",
        packageName = "com.darinsoft.vimo",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0xF02050,
        targets = listOf(AppTarget(version = "13.9.0", versionCode = 130900, minSdk = 32)),
    )

    val VPNIFY = Compatibility(
        name = "vpnify",
        packageName = "com.vpn.free.hotspot.secure.vpnify",
        apkFileType = ApkFileType.XAPK_REQUIRED,
        appIconColor = 0x0A84FF,
        signatures = setOf("6a0e2299884977821273e9b69252bc9f53aad9621dd911336b569872cb2b5707"),
        targets = listOf(AppTarget(version = "2.3.0", minSdk = 29)),
    )

    val VPN_SUPER = Compatibility(
        name = "VPN Super Unlimited Proxy",
        packageName = "com.free.vpn.super.hotspot.open",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x007DFF,
        targets = listOf(
            AppTarget(version = "2.32.0", versionCode = 23200, minSdk = 32),
            AppTarget(version = "2.33.0", versionCode = 23300, minSdk = 32),
            AppTarget(version = "2.33.1", versionCode = 23301, minSdk = 32),
        ),
    )

    val YI_IOT = Compatibility(
        name = "Yi iot",
        packageName = "com.yunyi.smartcamera",
        apkFileType = ApkFileType.XAPK_REQUIRED,
        appIconColor = 0x38D880,
        targets = listOf(AppTarget(version = "5.1.7_20260914", versionCode = 3799, minSdk = 24)),
    )
}
