/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.compat

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

@Suppress("MemberVisibilityCanBePrivate")
internal object AppCompatibilities {
    val ACALENDAR = Compatibility(
        name = "aCalendar",
        packageName = "org.withouthat.acalendar",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x0277BD,
        signatures = setOf(
            "6e3434746912534281a97d9db0c05d02b7c87b9c4486aa534658bd42fda43a1d",
        ),
        targets = listOf(
            AppTarget(version = "2.9.5", versionCode = 2009500, minSdk = 21),
            AppTarget(version = "2.9.6", versionCode = 2009600, minSdk = 21),
        ),
    )

    val ALBO = Compatibility(
        name = "Albo",
        packageName = "com.thesortdapp.sortd",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x1E1E1E,
        signatures = setOf(
            "ae23b6062bfe3fea5e0372c58ee809e0ee3eb3b356350cfaa2734dd6742c15b9",
        ),
        targets = listOf(AppTarget(version = "1.25.1", versionCode = 996, minSdk = 26)),
    )

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

    val ALOA = Compatibility(
        name = "Aloa",
        packageName = "com.getaloa.app",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0xFAB2AA,
        signatures = setOf(
            "48e539b414808f7a13343b5e10310d268f63030ba3cd479db290048ca0a77e8f",
        ),
        targets = listOf(
            AppTarget(version = "1.3.3", versionCode = 29, minSdk = 24),
            AppTarget(version = "1.3.4", versionCode = 30, minSdk = 24),
        ),
    )

    val ALPINEQUEST = Compatibility(
        name = "AlpineQuest",
        packageName = "psyberia.alpinequest.free",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x0057BD,
        targets = listOf(AppTarget(version = "2.4.1", versionCode = 414)),
    )

    val ANYTRACKER = Compatibility(
        name = "AnyTracker",
        packageName = "com.shervinkoushan.anyTracker",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x0D4AD8,
        signatures = setOf(
            "5e95a289ea73c30af95199eed6d64a2abf0ffc59c578f7686f24c5ce902ea26c",
        ),
        targets = listOf(
            AppTarget(version = "7.5.4", versionCode = 386, minSdk = 24),
            AppTarget(version = "7.5.6", versionCode = 388, minSdk = 24),
        ),
    )

    val ARC_PLAYER = Compatibility(
        name = "Arc Player",
        packageName = "com.videoplayer.arcplayer",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x528AEA,
        signatures = setOf(
            "51b6ef995b6ccd1a3fff12872d04fc7b69f01bf5ff523a3ea016c9beca1d70a3",
        ),
        targets = listOf(AppTarget(version = "1.4.2.141", versionCode = 141, minSdk = 23)),
    )

    val ATHAN = Compatibility(
        name = "Athan",
        packageName = "com.athan",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x514EFF,
        signatures = setOf(
            "9028baa741e6a6b2833e33ccca4db8bf9b82340f6093642ef3f1d569c942ede4",
        ),
        targets = listOf(AppTarget(version = "11.0.6", versionCode = 2017033171, minSdk = 24)),
    )

    val ATLOMAPS = Compatibility(
        name = "AtloMaps",
        packageName = "com.atlogis.atlomaps",
        apkFileType = ApkFileType.XAPK,
        appIconColor = 0x0683DF,
        targets = listOf(
            AppTarget(version = "1.0.6", versionCode = 153, minSdk = 26),
            AppTarget(version = "1.1.0", versionCode = 172, minSdk = 26),
            AppTarget(version = "2.0.0", versionCode = 174, minSdk = 26),
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
            AppTarget(version = "26.41.06", versionCode = 2090264106, minSdk = 28),
            AppTarget(version = "26.41.07", versionCode = 2090264107, minSdk = 28),
        ),
    )

    val AUDIOBOOK_UNLIMITED = Compatibility(
        name = "Audiobook Unlimited for Kindle",
        packageName = "com.oneletters.audiobooksai",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x2890E9,
        signatures = setOf(
            "2c2c9d68b4564d29e20fea9a363b7597b15e246ddbc553f5353e07c2da956d0e",
        ),
        targets = listOf(AppTarget(version = "1.7.23", versionCode = 196, minSdk = 24)),
    )

    val AUDIOLAB = Compatibility(
        name = "AudioLab",
        packageName = "com.hitrolab.audioeditor",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0xFA4C3F,
        signatures = setOf(
            "d441c034a8cd999a79ec5b09f0d03627d8689a53e0441a04aaa4a16315e5cec0",
        ),
        targets = listOf(AppTarget(version = "1.3.33", versionCode = 5163, minSdk = 26)),
    )

    val AUTOMATAG = Compatibility(
        name = "AutomaTag",
        packageName = "com.fillobotto.mp3tagger",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x0288D1,
        signatures = setOf(
            "538fbf04f304c4a9a6dc48638bcab88620e445c3be2b1d671bb3228fb04ff6d8",
        ),
        targets = listOf(AppTarget(version = "2.3.0.10", versionCode = 797, minSdk = 24)),
    )

    val BANGLA_CALENDAR = Compatibility(
        name = "Bangla Calendar",
        packageName = "com.outscar.bd.bongcal",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x006A4E,
        signatures = setOf(
            "d06b27d0bbcec5f0a462796ae16688880cc5740ca33abc9a96f4aaaf107f2cad",
        ),
        targets = listOf(AppTarget(version = "3.0.01c", versionCode = 302400127, minSdk = 24)),
    )

    val BEREAL = Compatibility(
        name = "BeReal",
        packageName = "com.bereal.ft",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x000000,
        signatures = setOf(
            "2efdbd738f3fdae901ecb96ead2d7b9b1ee6d2ceab911bdf7b1a3df812f3cfc4",
        ),
        targets = listOf(AppTarget(version = "3.97.1", versionCode = 3599414, minSdk = 26)),
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

    val BIOBIRD = Compatibility(
        name = "Biobird Flashcards",
        packageName = "com.biobird.exvee",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x075948,
        signatures = setOf(
            "908874fc41478d0780790e5c8a178727f83b2182c147f20689a3d5404fd4ded0",
        ),
        targets = listOf(
            AppTarget(version = "2.0.2", versionCode = 38, minSdk = 23),
            AppTarget(version = "3.0.0", versionCode = 41, minSdk = 24),
        ),
    )

    val BLOCKIT = Compatibility(
        name = "blockit",
        packageName = "com.hypenet.focused",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0xE3E1DD,
        signatures = setOf(
            "2ae4c0e21d3b244c69d6f122c2a1df907ac4c8d08f21c74425ff63c2b74efb1b",
        ),
        targets = listOf(
            AppTarget(version = "2.2.7", versionCode = 152, minSdk = 28),
        ),
    )

    val BLURWALL = Compatibility(
        name = "BlurWall",
        packageName = "apps.automan.blurwallpaper",
        apkFileType = ApkFileType.XAPK_REQUIRED,
        appIconColor = 0x101010,
        signatures = setOf(
            "9f2432d1e831e49c5fbfc4d8a089062ee6ad42f3e3f1402e9085c0973a71ef24",
        ),
        targets = listOf(
            AppTarget(version = "2.9.8", versionCode = 31, minSdk = 23),
            AppTarget(version = "3.0.0", versionCode = 32, minSdk = 24),
        ),
    )

    val BOTIM = Compatibility(
        name = "botim",
        packageName = "im.thebot.messenger",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x1F8EFA,
        signatures = setOf(
            "4a75cf385d6aa8530a1a34ba4ed089da6bf1ae6989f62757839fd01e4f2fcd99",
        ),
        targets = listOf(AppTarget(version = "4.19.1", versionCode = 7132, minSdk = 24)),
    )

    val CALCNOTE = Compatibility(
        name = "CalcNote",
        packageName = "com.burton999.notecal",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x3DABE1,
        signatures = setOf(
            "81a63b3b52728ce385b0897c6f6537dfe994ae950a605dcea41feebb8150d569",
        ),
        targets = listOf(AppTarget(version = "2.25.111", versionCode = 1523, minSdk = 24)),
    )

    val CASTLE = Compatibility(
        name = "Castle",
        packageName = "com.external.castle",
        apkFileType = ApkFileType.APK,
        appIconColor = 0xE53935,
        signatures = setOf(
            "917ee66c56caf0a5201a17ac8eddff1575aeac1c6a1091a6e0d29b4c08176130",
        ),
        targets = listOf(
            AppTarget(version = "2.1.1", versionCode = 148, minSdk = 23),
            AppTarget(version = "2.1.3", minSdk = 23),
        ),
    )

    val CASTLE_FOR_TV = Compatibility(
        name = "Castle for TV",
        packageName = "com.gxnet.castle.indiatv",
        apkFileType = ApkFileType.APK,
        appIconColor = 0xE53935,
        signatures = setOf(
            "17c6e44afc6b4c627fecdd30c27db7ad97cda41f728b74160ad55377dc4c7078",
        ),
        targets = listOf(AppTarget(version = "1.1.5", versionCode = 33, minSdk = 21)),
    )

    val CATZY = Compatibility(
        name = "Catzy",
        packageName = "com.nieruo.healthapp",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x4059B9,
        signatures = setOf(
            "9cf85b81cc307a9dc367d5421dbe47737724275e92781c95768a8eef9e529a39",
        ),
        targets = listOf(
            AppTarget(version = "1.61.0", versionCode = 281, minSdk = 24),
        ),
    )

    val CLINK = Compatibility(
        name = "Clink",
        packageName = "ltd.anti.clink",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0xF7F7F8,
        signatures = setOf(
            "8ea7bd2c094bd6d2dfbd605764c42ffd57c3d2ecf781b9aaf0135ff06b0cbde0",
        ),
        targets = listOf(AppTarget(version = "1.9.2", versionCode = 194, minSdk = 28)),
    )

    val CLIP_STUDIO_PAINT = Compatibility(
        name = "Clip Studio Paint",
        packageName = "jp.co.celsys.clipstudiopaint.googleplay",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x3F9BF5,
        signatures = setOf(
            "2c7507b2c986a1e3750c0d4876d6b1e8035b873a5d221149c167293b3e5449ca",
        ),
        targets = listOf(AppTarget(version = "5.1.4", versionCode = 260903140, minSdk = 33)),
    )

    val CLONE_APP = Compatibility(
        name = "Clone App",
        packageName = "com.pengyou.cloneapp",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x508CEC,
        signatures = setOf(
            "828b91795e6ef35a08c2fafe039bec74beffa824bf063167bc30257b14c81127",
        ),
        targets = listOf(AppTarget(version = "4.2.2", versionCode = 820, minSdk = 32)),
    )

    val CODDY = Compatibility(
        name = "Coddy",
        packageName = "tech.coddy.coddyapp",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x29A9E2,
        signatures = setOf(
            "70442accd302f59833b6193984fcdb50bbae68ba6c1e07f88dbb97ca1d8d6728",
        ),
        targets = listOf(AppTarget(version = "2.2.9", versionCode = 152, minSdk = 32)),
    )

    val CUBASIS_LE_3 = Compatibility(
        name = "Cubasis LE 3",
        packageName = "com.steinberg.cubasisle3",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x1B5E88,
        signatures = setOf(
            "b655d27a9bafa034143c2a742979fef80da4a44a7cdf1b75b388eb416665570a",
        ),
        targets = listOf(AppTarget(version = "3.8.6", versionCode = 3080603, minSdk = 28)),
    )

    val CX_FILE_EXPLORER = Compatibility(
        name = "Cx File Explorer",
        packageName = "com.cxinventor.file.explorer",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x5167F6,
        targets = listOf(
            AppTarget(version = "2.7.8", versionCode = 278, minSdk = 21),
            AppTarget(version = "2.7.9", versionCode = 279, minSdk = 21),
            AppTarget(version = "2.8.1", versionCode = 281, minSdk = 21),
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

    val DEPTHPIX = Compatibility(
        name = "DepthPix",
        packageName = "com.androholic.wallpapersdepthpix",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x926FC5,
        signatures = setOf(
            "73ddcf1fcf184dd559d6239ea2de84c2a6f08d4224b78c4491dc921b5f11d54e",
        ),
        targets = listOf(AppTarget(version = "1.0.21", versionCode = 59, minSdk = 24)),
    )

    val DESH_TELUGU_KEYBOARD = Compatibility(
        name = "Desh Telugu Keyboard",
        packageName = "com.telug.keyboard.for.android",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x08231C,
        signatures = setOf(
            "26728e842b7473b4c79b72324ee107ece06bce213520028c8973aeb2edb987af",
        ),
        targets = listOf(AppTarget(version = "17.6.8", versionCode = 11768, minSdk = 32)),
    )

    val DEVCHECK = Compatibility(
        name = "DevCheck",
        packageName = "flar2.devcheck",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x1E88E5,
        signatures = setOf(
            "f816aa5ff72263d1a9a6afb81e4c25e72fbf9002444f80a6e043f080c7010990",
        ),
        targets = listOf(
            AppTarget(version = "6.57", versionCode = 657, minSdk = 24),
            AppTarget(version = "6.60", versionCode = 660, minSdk = 24),
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
            AppTarget(version = "5.19.4", versionCode = 614, minSdk = 23),
            AppTarget(version = "5.19.6", versionCode = 616, minSdk = 23),
            AppTarget(version = "5.20.0", versionCode = 620, minSdk = 23),
            AppTarget(version = "5.21.0", versionCode = 630, minSdk = 23),
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
        targets = listOf(AppTarget(version = "9.7", versionCode = 91, minSdk = 24)),
    )

    val ECHOGRAM = Compatibility(
        name = "Echogram",
        packageName = "com.liori.echogram",
        apkFileType = ApkFileType.XAPK_REQUIRED,
        appIconColor = 0x191E31,
        signatures = setOf(
            "331fa00a81a7f2e70aeaec25ee709aa4f1f17062d61e7aa6048f14abc47f0d1a",
        ),
        targets = listOf(AppTarget(version = "1.0.7.0", versionCode = 138, minSdk = 26)),
    )

    val ETSY = Compatibility(
        name = "Etsy",
        packageName = "com.etsy.android",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0xF1641E,
        targets = listOf(
            AppTarget(version = "7.97.0", versionCode = 79700150, minSdk = 29),
            AppTarget(version = "7.99.0", versionCode = 79900161, minSdk = 32),
            AppTarget(version = "8.0.0", versionCode = 80000162, minSdk = 32),
        ),
    )

    val FAST_STL_VIEWER = Compatibility(
        name = "Fast STL Viewer",
        packageName = "com.performance.meshview",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x1A73B2,
        signatures = setOf(
            "925c7b1df5689fef2371e1f6e71e98a3956295df95221baa06145222aa89f769",
        ),
        targets = listOf(AppTarget(version = "2.93", versionCode = 339, minSdk = 24)),
    )

    val FDDB = Compatibility(
        name = "Fddb",
        packageName = "com.fddb",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x006495,
        signatures = setOf(
            "9caf67d3efca4ffb9f888256a5963c6117c28051a0669bc3d8bb098291ea9f4f",
        ),
        targets = listOf(
            AppTarget(version = "v7.8.4-Build-1-gms-release", versionCode = 7080401, minSdk = 26),
            AppTarget(version = "v7.8.5-Build-1-gms-release", versionCode = 7080501, minSdk = 26),
        ),
    )

    val FOCUS_NOW = Compatibility(
        name = "FocusNow",
        packageName = "com.ferdinandobermeier.focusmate",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0xFF5800,
        signatures = setOf(
            "7ea799fb96bbbe7cc574ce4667a75dcb96291fe8cf5d61f31d053d4f28aea3fa",
        ),
        targets = listOf(AppTarget(version = "3.8.0", versionCode = 237, minSdk = 24)),
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
            AppTarget(version = "2.2.227", versionCode = 227, minSdk = 24),
        ),
    )

    val GLASS_WIDGETS = Compatibility(
        name = "Glass Widgets",
        packageName = "com.jndapp.glass.widgets",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x000000,
        signatures = setOf(
            "6566216a168c676d7290543751611e836d79c88a0c9e616c9ba3db9688c9737b",
        ),
        targets = listOf(AppTarget(version = "1.0.10", versionCode = 11, minSdk = 32)),
    )

    val HINDU_CALENDAR = Compatibility(
        name = "Hindu Calendar",
        packageName = "com.alokmandavgane.hinducalendar",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0xF89532,
        signatures = setOf(
            "1dc3da6664982c69c3c7326282beaf0c6f153daa7da36bd7ec4cd4313e664651",
        ),
        targets = listOf(AppTarget(version = "9.4.0", versionCode = 177, minSdk = 28)),
    )

    val IMO = Compatibility(
        name = "imo",
        packageName = "com.imo.android.imoim",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x1B4A91,
        signatures = setOf(
            "44483f98c17bf49fbde08d3376af09c6fc1f9370c7a046c35a3dca45f5785150",
        ),
        targets = listOf(
            AppTarget(version = "2026.08.1041", versionCode = 26081041, minSdk = 32),
            AppTarget(version = "2026.09.1051", versionCode = 26091051, minSdk = 32),
        ),
    )

    val INTERNET_SPEED_METER_LITE = Compatibility(
        name = "Internet Speed Meter Lite",
        packageName = "com.internet.speed.meter.lite",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x3070C0,
        signatures = setOf(
            "802dcbe0db7955225c2f432aafed521681b835e274e0337aba8477b39c0261cf",
        ),
        targets = listOf(AppTarget(version = "1.7.0-lite", versionCode = 57, minSdk = 28)),
    )

    val IOTOTOY = Compatibility(
        name = "IoToToy",
        packageName = "com.hardcodedjoy.iototoy",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0xDEEEEE,
        signatures = setOf(
            "8bae701239d3a6167dace90b3676b2c46a7aca80e4c136a95e2511dcec5a9fca",
        ),
        targets = listOf(AppTarget(version = "1.1.0", versionCode = 11, minSdk = 24)),
    )

    val JUJO = Compatibility(
        name = "Jujo",
        packageName = "com.dev2a.junk_journal",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0xE8B4A0,
        signatures = setOf(
            "851f5a9aa9e8287bdade98d0e41a0bdd13a685064307040133626697f16445e4",
        ),
        targets = listOf(
            AppTarget(version = "1.2.5", versionCode = 104, minSdk = 24),
            AppTarget(version = "1.2.6", versionCode = 106, minSdk = 24),
        ),
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
        targets = listOf(AppTarget(version = "6.9.2-AOS-609282180", versionCode = 609282180, minSdk = 24)),
    )

    val JOYN_AT = Compatibility(
        name = "Joyn Austria",
        packageName = "at.zappn",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x000F15,
        signatures = setOf("8827dfbc7a7acf9ff37cfc7b6c1e544f80dc1fb8a235a8ca8174b08028e5d089"),
        targets = listOf(AppTarget(version = "6.9.0-AOS-JOYN_AT-2212264", versionCode = 2212264, minSdk = 24)),
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
        targets = listOf(AppTarget(version = "6.5.1", versionCode = 57461, minSdk = 30)),
    )

    val KLASSIK_RADIO = Compatibility(
        name = "Klassik Radio+",
        packageName = "de.klassikradio.app",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x000000,
        targets = listOf(AppTarget(version = "p5.12.0", versionCode = 50138, minSdk = 24)),
    )

    val LAGGUY_WALLPAPERS = Compatibility(
        name = "Lagguy Wallpapers",
        packageName = "com.lagguy.wallpapers",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0xFFFFFF,
        signatures = setOf(
            "d39a40cf5446332eff3e40b9b203b5a8aa9b1f1d834f683df5bb1cc4d1d45edf",
        ),
        targets = listOf(AppTarget(version = "40.0.44", versionCode = 37, minSdk = 24)),
    )

    val LED_BLINKER = Compatibility(
        name = "LED Blinker",
        packageName = "com.ledblinker",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x190C53,
        signatures = setOf(
            "40a44cbdf290932098b46c12ae0c9932c82211659a27253ddda4e5ca62780dea",
        ),
        targets = listOf(AppTarget(version = "26.01.08", versionCode = 12506, minSdk = 24)),
    )

    val LINGODEER = Compatibility(
        name = "LingoDeer",
        packageName = "com.lingodeer",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x38D06C,
        signatures = setOf(
            "b341b1d855ee55eb2c6c9287eaafc6449c0b34065e170af7bf5bc21a03f05bf2",
        ),
        targets = listOf(AppTarget(version = "2.99.413", versionCode = 677, minSdk = 24)),
    )

    val LUMO = Compatibility(
        name = "Proton Lumo",
        packageName = "me.proton.android.lumo",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x6D4AFF,
        signatures = setOf(
            "a747eaee28611e25217b793c07bf302234405cfd1e19068162556c79e5bf04f0",
            "dcc9439ec1a6c6a8d0203f3423ee42bcc8b970628e53cb73a0393f398dd5b853",
            "a5769b88d3c500f9c58c6939a7b09dc293630c8b93b69f4457888060bf1faa43",
        ),
        targets = listOf(
            AppTarget(version = "2.1.4-gms", versionCode = 78, minSdk = 29),
            AppTarget(version = "2.1.5-nogms", versionCode = 79, minSdk = 29),
        ),
    )

    val MEDICNEET = Compatibility(
        name = "MedicNEET",
        packageName = "com.halalfire.medicneet",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x4D8E78,
        signatures = setOf(
            "befd82717721a7d025fb5783b48469bcf222536a224a7466098459f0ba1f4559",
        ),
        targets = listOf(AppTarget(version = "1.5.138", versionCode = 259, minSdk = 24)),
    )

    val MAGIC_EARTH = Compatibility(
        name = "Magic Earth",
        packageName = "com.generalmagic.magicearth",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x9D85FD,
        signatures = setOf(
            "fa91bd3a8bfa2b03bda48422b238d584f23921154a5279e721f2f90a74d38806",
        ),
        targets = listOf(
            AppTarget(version = "7.1.26.26.21DB1F1B.3C81F7001", versionCode = 2026112516, minSdk = 28),
            AppTarget(version = "7.1.26.38.B4684D03.6207A61BA", versionCode = 2026141716, minSdk = 32),
        ),
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
        targets = listOf(
            AppTarget(version = "62.6", versionCode = 626, minSdk = 24),
            AppTarget(version = "62.9", versionCode = 629, minSdk = 24),
        ),
    )

    val MINISHARE = Compatibility(
        name = "MiniShare",
        packageName = "com.omniashare.minishare",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x2CADC7,
        signatures = setOf(
            "cf0684e9abe941d8ad027981fc82d853746c4b0604b31962dcf4d3eea9d55fb0",
        ),
        targets = listOf(AppTarget(version = "1.6.6", versionCode = 18, minSdk = 21)),
    )

    val MONET = Compatibility(
        name = "Monet Launcher",
        packageName = "com.klevico.monet",
        apkFileType = ApkFileType.XAPK,
        appIconColor = 0x657085,
        signatures = setOf(
            "1b3d405d34ab4a1ba98c26ac0efdf7b79371724fa039c8614e162c402f984af1",
        ),
        targets = listOf(
            AppTarget(version = "1.1.0", versionCode = 134, minSdk = 26),
        ),
    )

    val MOON_READER = Compatibility(
        name = "Moon+ Reader",
        packageName = "com.flyersoft.moonreader",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x5899FC,
        signatures = setOf(
            "5ac646ebc68017e4cc582b11b1c2ae5d29b15d9b0ac3a54fc60972417b41c9ad",
        ),
        targets = listOf(
            AppTarget(version = "10.7", versionCode = 1007000, minSdk = 21),
            AppTarget(version = "10.8", versionCode = 1008000, minSdk = 21),
        ),
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
            AppTarget(version = "4.0.03.0918.03", versionCode = 50020129, minSdk = 29),
            AppTarget(version = "4.0.03.1022.03", versionCode = 50020133, minSdk = 29),
        ),
    )

    val MOVIEBOX_TV = Compatibility(
        name = "MovieBox TV",
        packageName = "com.community.mbox.tv",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x30E66D,
        signatures = setOf(
            "03d4b741632d136bc23e2de4c6adcdb7712f1980b619c183221801730bbd0410",
        ),
        targets = listOf(
            AppTarget(version = "1.2.0.0923.web_socialmedia", versionCode = 50040018, minSdk = 29),
        ),
    )

    val MUSICOLET = Compatibility(
        name = "Musicolet",
        packageName = "in.krosbits.musicolet",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x65FFA0,
        signatures = setOf(
            "8a4d4121f541e3644c1fd1e5c3502eb4bea49792cafd26e600d413cd1f10851f",
        ),
        targets = listOf(AppTarget(version = "6.14.1 build540", versionCode = 54000, minSdk = 28)),
    )

    val MUSICOLET_CAST_PLUGIN = Compatibility(
        name = "Cast Plug-in for Musicolet",
        packageName = "in.krosbits.castplugin",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x44AF6D,
        signatures = setOf(
            "8a4d4121f541e3644c1fd1e5c3502eb4bea49792cafd26e600d413cd1f10851f",
        ),
        targets = listOf(AppTarget(version = "1.9", versionCode = 9, minSdk = 24)),
    )

    val MUSIXMATCH = Compatibility(
        name = "Musixmatch",
        packageName = "com.musixmatch.android.lyrify",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0xFA522E,
        signatures = setOf(
            "6696e2cf978935c637f52aaefe196760bc3d62b4ff92bcc5e3f7ed015a6d9b23",
        ),
        targets = listOf(AppTarget(version = "8.4.2", versionCode = 2026061901, minSdk = 24)),
    )

    val MYMOVESET = Compatibility(
        name = "MyMoveset",
        packageName = "com.soulbreakers.mymoveset",
        apkFileType = ApkFileType.XAPK,
        appIconColor = 0x9F1C3B,
        targets = listOf(
            AppTarget(version = "1.4.0", versionCode = 16, minSdk = 24),
            AppTarget(version = "1.5.0", versionCode = 17, minSdk = 24),
        ),
    )

    val MYSCRIPT_MATH = Compatibility(
        name = "MyScript Math",
        packageName = "com.myscript.math",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0xFF1A1A,
        signatures = setOf(
            "405515fc77f1c69d3631d7fb0e63a848819b0bcfcd72c8cc14c91378b24b6fb4",
            "5f87e50754e55d725b5343bffbbb12fca88f5992118b2feb407e738178227cb1",
        ),
        targets = listOf(AppTarget(version = "1.11.0", versionCode = 2001812, minSdk = 26)),
    )

    val MYSCRIPT_NOTES = Compatibility(
        name = "MyScript Notes",
        packageName = "com.myscript.nebo",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x007FFD,
        signatures = setOf(
            "12645b76721993f0f3faf82dddecd6c96355260bf44d06ce6246caf2fcf3ada8",
        ),
        targets = listOf(AppTarget(version = "7.4.5", versionCode = 181541, minSdk = 26)),
    )

    val MY_APK = Compatibility(
        name = "My APK",
        packageName = "com.andatsoft.myapk.fwa",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x5D3FC6,
        signatures = setOf(
            "3fa505df0e471b20b0bf0511cd05c0ecfa2fa53a9bbed6bf51886e1b7c0db90b",
        ),
        targets = listOf(AppTarget(version = "4.1.1", versionCode = 3231, minSdk = 26)),
    )

    val NARA_BABY = Compatibility(
        name = "Nara Baby",
        packageName = "com.naraorganics.nara",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0xFF5D01,
        signatures = setOf(
            "de6c7a92a739929e4a32b5a1e10b79c6e1b5bac0162e20453c1a6eb1c1b365c6",
        ),
        targets = listOf(AppTarget(version = "2.8.3", versionCode = 407, minSdk = 24)),
    )

    val NEETPREP = Compatibility(
        name = "NEETprep",
        packageName = "com.lernr.app",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0xE5A020,
        signatures = setOf(
            "dd68677619d49bb48e69dbefc748488a3a7a70d1f0569de0e298e342d3354aa0",
        ),
        targets = listOf(AppTarget(version = "15.0.93", versionCode = 832, minSdk = 28)),
    )

    val NEETPREP_ESSENTIAL = Compatibility(
        name = "NEETprep Essential",
        packageName = "com.gooded.essential",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0xE5A020,
        signatures = setOf(
            "205e64b5fc537e9d80d96da86cff1a4b57dec493691493d4abb46b19a93fe9b7",
        ),
        targets = listOf(AppTarget(version = "1.2.95", versionCode = 308, minSdk = 24)),
    )

    val NETWORK_ANALYZER = Compatibility(
        name = "Network Analyzer",
        packageName = "net.techet.netanalyzerlite.an",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x3B4A5C,
        signatures = setOf(
            "3961d83e6d20d3363e07c6fe330601ef6ece07d44cda2fe46a0048bc70eacdaf",
        ),
        targets = listOf(AppTarget(version = "5.1", versionCode = 19502, minSdk = 29)),
    )

    val NOTEIN = Compatibility(
        name = "Notein",
        packageName = "com.orion.notein.global",
        apkFileType = ApkFileType.XAPK,
        appIconColor = 0x0088FF,
        signatures = setOf(
            "740847519c431975924d4ea55d10ef2dad77d43c6565a9b6d3218c4c00c3279d",
        ),
        targets = listOf(AppTarget(version = "v2.1.37.0", versionCode = 409, minSdk = 26)),
    )

    val NOTESNOOK = Compatibility(
        name = "Notesnook",
        packageName = "com.streetwriters.notesnook",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x008837,
        targets = listOf(
            AppTarget(version = "3.4.12", versionCode = 4197422, minSdk = 24),
            AppTarget(version = "3.4.13", versionCode = 4197423, minSdk = 24),
            AppTarget(version = "3.4.14", versionCode = 4197424, minSdk = 24),
            AppTarget(version = "3.4.15", versionCode = 4197425, minSdk = 24),
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
        targets = listOf(AppTarget(version = "0.5.104", versionCode = 416, minSdk = 29)),
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

    val OPAL = Compatibility(
        name = "Opal",
        packageName = "com.withopal.opal",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x000000,
        signatures = setOf(
            "e9b037f4be157ee608de6aa922cda5cdbe614633171cc94544c40f08e9509286",
        ),
        targets = listOf(AppTarget(version = "4.16.0", versionCode = 4016000, minSdk = 29)),
    )

    val PERPLEXITY = Compatibility(
        name = "Perplexity",
        packageName = "ai.perplexity.app.android",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x20808D,
        targets = listOf(
            AppTarget(version = "2.95.0", versionCode = 260642, minSdk = 26),
            AppTarget(version = "2.100.0", versionCode = 260662, minSdk = 26),
            AppTarget(version = "2.101.0", versionCode = 260665, minSdk = 26),
        ),
    )

    val PHOTONE = Compatibility(
        name = "Photone",
        packageName = "io.lightray.photone",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x000000,
        signatures = setOf(
            "4f3a4b830ee2f3b8a79622949dbda2c2b159cb27a3c18d926672d1eaab96b88e",
        ),
        targets = listOf(AppTarget(version = "1.5.4", versionCode = 5624, minSdk = 24)),
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
            AppTarget(version = "1.802.266", versionCode = 266201, minSdk = 28),
        ),
    )

    val PERIOD_CALENDAR = Compatibility(
        name = "Period Tracker and Calendar",
        packageName = "com.lbrc.PeriodCalendar",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0xE74779,
        signatures = setOf(
            "626c56a43dab2f279a257008783c7e752d1ad9ad46cfff9fd858ce68d3ff10ab",
        ),
        targets = listOf(AppTarget(version = "13.4.0", versionCode = 21304000, minSdk = 24)),
    )

    val PLECO = Compatibility(
        name = "Pleco",
        packageName = "com.pleco.chinesesystem",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x005A94,
        signatures = setOf(
            "780a2a639a3ccb65621e0caccf18fa38c91f6cbf351d453742ebe2a114b09624",
        ),
        targets = listOf(AppTarget(version = "3.2.101", versionCode = 3460, minSdk = 28)),
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

    val POE = Compatibility(
        name = "Poe",
        packageName = "com.poe.android",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x000000,
        signatures = setOf(
            "099909df5cf774e2a55b3d35a352ac33931dd218969cfcc766e570ee74c5170f",
        ),
        targets = listOf(AppTarget(version = "a3.77.2", versionCode = 3077006, minSdk = 26)),
    )

    val PRAYER_TIMES = Compatibility(
        name = "Prayer Times and Qibla",
        packageName = "com.reworewo.prayertimes",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x0E9567,
        signatures = setOf(
            "cd1f0d9a62d0479c4b71bbc4d519698269d6dc499d3953de7528087e515a1968",
        ),
        targets = listOf(AppTarget(version = "3.8.3", versionCode = 742, minSdk = 24)),
    )

    val PRIVACY_NOTES = Compatibility(
        name = "Privacy Notes",
        packageName = "app.privacynotes",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x1E40AF,
        signatures = setOf(
            "98a0bb5aaf694f6bea50adcb4a36d4b57a3c3e42f67aff08fbea4689696caa87",
        ),
        targets = listOf(AppTarget(version = "0.654.2", versionCode = 654002, minSdk = 24)),
    )

    val PROJECTIVY = Compatibility(
        name = "Projectivy Launcher",
        packageName = "com.spocky.projengmenu",
        apkFileType = ApkFileType.APK,
        appIconColor = 0xF08029,
        targets = listOf(
            AppTarget(version = "4.72", versionCode = 97, minSdk = 23),
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
            AppTarget(version = "7.11.11", versionCode = 18326, minSdk = 29),
        ),
    )

    val PROTON_AUTHENTICATOR = Compatibility(
        name = "Proton Authenticator",
        packageName = "proton.android.authenticator",
        apkFileType = ApkFileType.XAPK_REQUIRED,
        appIconColor = 0x6D4AFF,
        signatures = setOf(
            "dcc9439ec1a6c6a8d0203f3423ee42bcc8b970628e53cb73a0393f398dd5b853",
        ),
        targets = listOf(
            AppTarget(version = "1.3.10", versionCode = 10310372, minSdk = 27),
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
            AppTarget(version = "1.41.3", versionCode = 14103401, minSdk = 27),
            AppTarget(version = "1.41.4", versionCode = 14104406, minSdk = 27),
            AppTarget(version = "1.41.5", versionCode = 14105409, minSdk = 27),
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
            AppTarget(version = "5.20.74.0", versionCode = 605207400, minSdk = 26),
            AppTarget(version = "5.20.98.0", versionCode = 605209800, minSdk = 26),
        ),
    )

    val PSIPHON = Compatibility(
        name = "Psiphon",
        packageName = "com.psiphon3.subscription",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0xF5A623,
        signatures = setOf(
            "76dbef15f67726d451a12359b8579c0d7a9f635d526aa37424df131632f17810",
        ),
        targets = listOf(AppTarget(version = "2.5.11", versionCode = 506, minSdk = 24)),
    )

    val PUBLIC = Compatibility(
        name = "Public",
        packageName = "com.cardfeed.video_public",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0xEA1A1A,
        signatures = setOf(
            "38687807198e57d53bef507efdc11a6f9bf19bf7c7643b89650edff812ba2c00",
        ),
        targets = listOf(AppTarget(version = "3.3.4", versionCode = 9070834, minSdk = 24)),
    )

    val PYDROID = Compatibility(
        name = "Pydroid 3",
        packageName = "ru.iiec.pydroid3",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x3776AB,
        targets = listOf(AppTarget(version = "8.6_arm64", versionCode = 1133, minSdk = 23)),
    )

    val IIEC_APPS = arrayOf(CXXDROID, JVDROID, PYDROID)

    val QUICK_CURSOR = Compatibility(
        name = "Quick Cursor",
        packageName = "com.quickcursor",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x95585D,
        signatures = setOf(
            "137c725e8407123e717393db8559382f96677fcbab153624603796a98ae71250",
        ),
        targets = listOf(AppTarget(version = "3.0.2", versionCode = 250, minSdk = 24)),
    )

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
            AppTarget(version = "2.2.91", versionCode = 82, minSdk = 33),
        ),
    )

    val RATEGLANCE = Compatibility(
        name = "RateGlance",
        packageName = "com.sry.rateglance",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x0D192C,
        targets = listOf(
            AppTarget(version = "1.17.6", versionCode = 304, minSdk = 26),
            AppTarget(version = "1.18.6", versionCode = 318, minSdk = 26),
        ),
    )

    val RADIO_JAVAN = Compatibility(
        name = "Radio Javan",
        packageName = "com.radiojavan.androidradio",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0xE31A1A,
        signatures = setOf("9ed9acdad7d1e0b3eb51e1e06cccdef979624cb3b6bc18fddcf7165e36b06c44"),
        targets = listOf(
            AppTarget(version = "11.20.1", versionCode = 452, minSdk = 29),
            AppTarget(version = "11.22.1", versionCode = 467, minSdk = 32),
        ),
    )

    val RAINDROP = Compatibility(
        name = "Raindrop.io",
        packageName = "io.raindrop.raindropio",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x1988E0,
        signatures = setOf("ddd75c66578654d085424804cb27c7b6e04fd36616045bfee140d9756ffca563"),
        targets = listOf(
            AppTarget(version = "4.7.44", versionCode = 8698961, minSdk = 24),
            AppTarget(version = "4.8.0", versionCode = 8699017, minSdk = 24),
        ),
    )

    val READERA = Compatibility(
        name = "ReadEra",
        packageName = "org.readera",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x0061BD,
        targets = listOf(
            AppTarget(version = "26.05.20+2300", versionCode = 2300, minSdk = 16),
            AppTarget(version = "26.09.29+2320", versionCode = 2320, minSdk = 23),
            AppTarget(version = "26.10.18+2330", versionCode = 2330, minSdk = 23),
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
        targets = listOf(AppTarget(version = "1.02.01.1023", versionCode = 10159, minSdk = 24)),
    )

    val MAY_BULLET_JOURNAL = Compatibility(
        name = "May: Beautiful Bullet Journal",
        packageName = "com.azeravn.bulletjournal",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0xF2CF7F,
        signatures = setOf("89c58e85cc8fa88481577d6c237043f975c44765ef98f622385ecafac2f9694c"),
        targets = listOf(AppTarget(version = "1.0.216", versionCode = 216, minSdk = 24)),
    )

    val RISE = Compatibility(
        name = "RISE Sleep Tracker",
        packageName = "com.risesci.nyx",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0x8E58FF,
        targets = listOf(AppTarget(version = "Android V1.79.3", versionCode = 1792671827, minSdk = 26)),
    )

    val RUBBER_BANDS = Compatibility(
        name = "Rubber Bands",
        packageName = "app.rubberbands.fit",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0xFD8700,
        targets = listOf(
            AppTarget(version = "3.11", versionCode = 297, minSdk = 26),
            AppTarget(version = "3.14", versionCode = 300, minSdk = 29),
        ),
    )

    val SERIALIZD = Compatibility(
        name = "Serializd",
        packageName = "com.serializdmobile",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x44BBA4,
        signatures = setOf(
            "497a36a272a57be44383a0d2f21c532c968ea9d0e6b93ae66afd53c1d3aefa55",
        ),
        targets = listOf(AppTarget(version = "1.992", versionCode = 1992, minSdk = 24)),
    )

    val SHEYTOON = Compatibility(
        name = "Sheytoon",
        packageName = "com.triovent.datingapp",
        apkFileType = ApkFileType.XAPK_REQUIRED,
        appIconColor = 0x01EBFA,
        signatures = setOf(
            "82b7283e0421735bf62ff3dc9890695b9cfc0b1b1696e48318c931c5cf6a9c7c",
        ),
        targets = listOf(AppTarget(version = "6.7", versionCode = 3030, minSdk = 26)),
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

    val SQCOM = Compatibility(
        name = "SQCOM",
        packageName = "com.sqcom.shangqing",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x4880F8,
        signatures = setOf(
            "a931de7526cdbb08051147215c8283266f53a54ac26ac1b41d1e9d943a6a5b59",
        ),
        targets = listOf(AppTarget(version = "1.1.0", versionCode = 1001000, minSdk = 26)),
    )

    val STEEZY = Compatibility(
        name = "STEEZY",
        packageName = "co.steezy.app",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x0E7AFE,
        signatures = setOf(
            "72222080cdb4e8e9ef66d0ac120b85c52ef223815bae6c1a07d2b11cd373722e",
        ),
        targets = listOf(AppTarget(version = "5.23.0", versionCode = 299, minSdk = 24)),
    )

    val STORYWISE = Compatibility(
        name = "Storywise",
        packageName = "com.storywise.storywise",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0xD9543D,
        signatures = setOf(
            "a8434fb26c0ef24c8bffe4cca13ad81322adfb580854db3a7adf7ee783a60512",
        ),
        targets = listOf(AppTarget(version = "1.0.3", versionCode = 34, minSdk = 24)),
    )

    val SYGIC = Compatibility(
        name = "Sygic",
        packageName = "com.sygic.aura",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x1E90FF,
        signatures = setOf(
            "982d28261c4cbcc2ff8c9ce397d66af80150a0f4e684aed8eef328c0af169019",
        ),
        targets = listOf(
            AppTarget(version = "26.6.1-120585", versionCode = 272606010, minSdk = 26),
            AppTarget(version = "26.6.2-121046", versionCode = 272606020, minSdk = 26),
        ),
    )

    val SYMFONIUM = Compatibility(
        name = "Symfonium",
        packageName = "app.symfonik.music.player",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0xE22728,
        targets = listOf(
            AppTarget(version = "14.0.0", versionCode = 127708, minSdk = 28),
            AppTarget(version = "14.1.0", versionCode = 127734, minSdk = 28),
            AppTarget(version = "15.0.1", versionCode = 127798, minSdk = 26),
            AppTarget(version = "15.1.0", versionCode = 127812, minSdk = 26),
            AppTarget(version = "14.0.0 TV", versionCode = 227708, minSdk = 28),
            AppTarget(version = "15.1.0 TV", versionCode = 227812, minSdk = 26),
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
        targets = listOf(AppTarget(version = "4.27.5", versionCode = 702, minSdk = 23)),
    )

    val THREEWAY = Compatibility(
        name = "3way",
        packageName = "app.threesome.dating",
        apkFileType = ApkFileType.XAPK_REQUIRED,
        appIconColor = 0xFF5B66,
        signatures = setOf(
            "5f7a94bcfa985d974a41a58899a6b5f1481d266fd82d7dae904d83d9e3f60415",
        ),
        targets = listOf(AppTarget(version = "3.1.0", versionCode = 1053, minSdk = 26)),
    )

    val TRAINLINE = Compatibility(
        name = "Trainline",
        packageName = "com.thetrainline",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x00A88F,
        targets = listOf(AppTarget(version = "438.1.0.184284", versionCode = 1284284, minSdk = 26)),
    )

    val VIDEO_EDITOR = Compatibility(
        name = "Video Editor",
        packageName = "videoeditor.videomaker.videoeditorforyoutube",
        apkFileType = ApkFileType.APKS,
        appIconColor = 0xFF9800,
        signatures = setOf(
            "97c78f212f647fc96059ef8b81f239bc047be8f285ae59486a53a5e1ed09349a",
        ),
        targets = listOf(AppTarget(version = "1.621.196", versionCode = 196, minSdk = 26)),
    )

    val U_DICTIONARY = Compatibility(
        name = "U Dictionary",
        packageName = "com.youdao.hindict",
        apkFileType = ApkFileType.APK,
        appIconColor = 0xE8442F,
        signatures = setOf(
            "544b87c75e50d264962b1379931af73c85a75f7416ec3672d4afb261e553eb6c",
        ),
        targets = listOf(AppTarget(version = "6.7.5", versionCode = 399, minSdk = 23)),
    )

    val UPTCL = Compatibility(
        name = "UPTCL",
        packageName = "com.ufoneselfcare",
        apkFileType = ApkFileType.APK,
        appIconColor = 0xF58220,
        signatures = setOf(
            "9ea10f551f0670ad451dd99fbdfafe7d45dda2addce3f2bdd1bec9d50ee99539",
        ),
        targets = listOf(AppTarget(version = "11.0.41", versionCode = 220, minSdk = 23)),
    )

    val VIBER = Compatibility(
        name = "Viber",
        packageName = "com.viber.voip",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x7360F2,
        signatures = setOf(
            "6e9857865c0d4568e02ef76dc838ed282a3d24b12c7027b3926190d99b42fe09",
        ),
        targets = listOf(AppTarget(version = "29.2.0.0", versionCode = 1290200, minSdk = 32)),
    )

    val VLLO = Compatibility(
        name = "VLLO",
        packageName = "com.darinsoft.vimo",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0xF02050,
        targets = listOf(
            AppTarget(version = "13.9.0", versionCode = 130900, minSdk = 27),
            AppTarget(version = "13.9.6", versionCode = 130906, minSdk = 26),
        ),
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
            AppTarget(version = "2.32.0", versionCode = 23200, minSdk = 26),
            AppTarget(version = "2.33.0", versionCode = 23300, minSdk = 26),
            AppTarget(version = "2.33.1", versionCode = 23301, minSdk = 26),
            AppTarget(version = "2.34.1", versionCode = 23401, minSdk = 26),
        ),
    )

    val WALLMAA = Compatibility(
        name = "Wallmaa",
        packageName = "com.wallmaa.minimal.wallpapers",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x000000,
        signatures = setOf("2da8b29136b6eb1735c32d87d56722bbd374ad8781119914bd7443adb64b0b0d"),
        targets = listOf(AppTarget(version = "1.0.1", versionCode = 22, minSdk = 24)),
    )

    val WALP = Compatibility(
        name = "WalP",
        packageName = "com.stresscodes.wallp",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0xA346FC,
        signatures = setOf("dbb4adaa10cdf28ae38d36ffe3b11f5f6d30506c79f0e59e702a113b0e93a361"),
        targets = listOf(
            AppTarget(version = "10.2.1", versionCode = 168, minSdk = 32),
            AppTarget(version = "10.2.3", versionCode = 170, minSdk = 32),
        ),
    )

    val WATTPAD = Compatibility(
        name = "Wattpad",
        packageName = "wp.wattpad",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0xFF500A,
        signatures = setOf("012de45837d20561255a02bbc22334c1f681d01b366fe31bef418baf535903ef"),
        targets = listOf(AppTarget(version = "11.42.0", versionCode = 3836, minSdk = 24)),
    )

    val YANOSIK = Compatibility(
        name = "Yanosik",
        packageName = "pl.neptis.yanosik.mobi.android",
        apkFileType = ApkFileType.XAPK_REQUIRED,
        appIconColor = 0x00A99D,
        signatures = setOf(
            "5447ddeefa77b9897fd3755c225899a0cbfb61a959f04fc949870808aceb52e6",
        ),
        targets = listOf(AppTarget(version = "26.9.0", versionCode = 6001512, minSdk = 28)),
    )

    val YI_IOT = Compatibility(
        name = "Yi iot",
        packageName = "com.yunyi.smartcamera",
        apkFileType = ApkFileType.XAPK_REQUIRED,
        appIconColor = 0x38D880,
        targets = listOf(AppTarget(version = "5.1.8_20261021", versionCode = 3800, minSdk = 24)),
    )

    val YUKA = Compatibility(
        name = "Yuka",
        packageName = "io.yuka.android",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x6AC259,
        signatures = setOf(
            "a7e3c4a612680186937b02e7b89574d5fd62798b8cb668973992fa89f6198fcb",
        ),
        targets = listOf(AppTarget(version = "5.7", versionCode = 875, minSdk = 24)),
    )

    val USA_VPN = Compatibility(
        name = "USA VPN",
        packageName = "vpn.usa_tap2free",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x413C6B,
        signatures = setOf(
            "b52d663c4612475b293ca0ec4ca84e00da9c77d55a28cc655ba2dd4bf33ae9fb",
        ),
        targets = listOf(AppTarget(version = "1.168", versionCode = 168, minSdk = 24)),
    )

    val VPN_INDIA = Compatibility(
        name = "VPN India",
        packageName = "india.vpn_tap2free",
        apkFileType = ApkFileType.APKS_REQUIRED,
        appIconColor = 0x479C1E,
        signatures = setOf(
            "efa80db96fc21e9c6345584d3fab947c4cde2838937463d6fdd402c9458898c0",
        ),
        targets = listOf(AppTarget(version = "1.172", versionCode = 172, minSdk = 24)),
    )

    val GRAPH_MESSENGER = Compatibility(
        name = "Graph Messenger",
        packageName = "ir.ilmili.telegraph",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0xB6D790,
        signatures = setOf("e7d5ad736aeecd2fe237679e4deaf2a40bb32fc07837fe48778a5b41c0418862"),
        targets = listOf(AppTarget(version = "13.0.2.0", versionCode = 71751, minSdk = 23)),
    )
}
