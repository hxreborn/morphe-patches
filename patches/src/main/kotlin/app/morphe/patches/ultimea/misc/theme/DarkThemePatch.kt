/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.theme

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.dart.dartAotLibrary
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.png.PngImage
import app.morphe.patches.ultimea.misc.settings.nativeMethods

private const val SECONDARY_INK = 0x00b8bcc2
private const val AVATAR_CHANNEL_MASK = 255
private const val AVATAR_CHANNEL_MAX = 255
private const val AVATAR_ALPHA_MASK = -0x1000000
private const val AVATAR_SOURCE_INK = 194
private const val AVATAR_SOURCE_RANGE = AVATAR_CHANNEL_MAX - AVATAR_SOURCE_INK
private const val AVATAR_BACKGROUND_CHANNEL = 36
private const val AVATAR_BACKGROUND_BLUE = 42
private const val AVATAR_INK_RED = 184
private const val AVATAR_INK_GREEN = 188
private const val AVATAR_INK_BLUE = 194
private const val AVATAR_RED_SHIFT = 16
private const val AVATAR_GREEN_SHIFT = 8

@Suppress("unused")
val darkThemePatch = resourcePatch(
    name = "Dark theme",
    description = "Adds dark and AMOLED themes with light text and controls.",
) {
    compatibleWith(AppCompatibilities.ULTIMEA)
    availability(requireArm64)
    dependsOn(themeSettingsPatch)

    execute {
        val library = dartAotLibrary()
        val image = ThemeElfImage(library.readBytes())
        val stock = ThemeElfImage(nativeMethods.bytes)
        val matches = ThemeInstructions(stock.bytes, stock.codeStart, stock.codeEnd).match()
        ThemeHooks.apply(image, matches)
        library.writeBytes(image.bytes)
        val flutter = get("lib/arm64-v8a/libflutter.so")
        val engine = flutter.readBytes()
        FlutterImageDecoder.apply(engine)
        flutter.writeBytes(engine)
        val icons = get("assets/flutter_assets/assets/icons")
        icons.resolve("login_logo_dark.svg").copyTo(icons.resolve("login_logo.svg"), overwrite = true)
        for (name in listOf("device_nomsgchat", "msg_center_noready", "msg_center_ready")) {
            icons.resolve("${name}_night.svg").copyTo(icons.resolve("$name.svg"), overwrite = true)
        }
        for (name in listOf("default_user_icon.svg", "default_new_userIcon.svg")) {
            val userIcon = icons.resolve(name)
            userIcon.writeText(userIcon.readText().replace("#E9E9E9", "#24242A").replace("fill=\"white\"", "fill=\"#B8BCC2\""))
        }
        for (name in listOf("ble_play_help", "eq_help", "jerry_can_help", "device_usb_icon", "right_into", "right_arrow", "device_arrow_connected")) {
            val icon = icons.resolve("$name.svg")
            if (icon.exists()) icon.writeText(icon.readText().replace("#646464", "#B8BCC2").replace("#161616", "#B8BCC2").replace("#E9E9E9", "#24242A").replace("stroke=\"black\"", "stroke=\"#B8BCC2\""))
        }
        val guide = icons.resolve("guide.svg")
        guide.writeText(guide.readText().replace("\"white\"", "\"#24242A\"").replace("#AAB0B5", "#B8BCC2"))
        for (name in listOf("hdmi", "usb", "optical", "aux", "bluetooth")) {
            val iconFile = get("assets/flutter_assets/assets/images/device_input_${name}_n.png")
            val icon = PngImage.read(iconFile)
            for (index in icon.argb.indices) icon.argb[index] = (icon.argb[index] and AVATAR_ALPHA_MASK) or SECONDARY_INK
            icon.write(iconFile)
        }
        for (name in listOf("store_icon", "store_icon_select", "home_icon", "home_icon_select", "mine_icon", "mine_icon_select")) {
            val icon = get("assets/flutter_assets/assets/images/$name.svg")
            icon.writeText(icon.readText().replace("#646464", "#B8BCC2").replace("#541597", "#C499FF")
                .replace("\"white\"", "\"#24242A\""))
        }
        document("res/values/colors.xml").use { document ->
            val colors = document.getElementsByTagName("color")
            for (index in 0 until colors.length) {
                val color = colors.item(index) as org.w3c.dom.Element
                if (color.getAttribute("name") == "splash_bg_color") color.textContent = "#ff000000"
            }
        }
        val avatarFile = get("assets/flutter_assets/assets/images/mine_user_icon.png")
        val avatar = PngImage.read(avatarFile)
        for (index in avatar.argb.indices) {
            val pixel = avatar.argb[index]
            val shade = pixel and AVATAR_CHANNEL_MASK
            val foreground = (AVATAR_CHANNEL_MAX - shade).coerceIn(0, AVATAR_SOURCE_RANGE)
            fun channel(background: Int, ink: Int) = background + (ink - background) * foreground / AVATAR_SOURCE_RANGE
            avatar.argb[index] = (pixel and AVATAR_ALPHA_MASK) or
                (channel(AVATAR_BACKGROUND_CHANNEL, AVATAR_INK_RED) shl AVATAR_RED_SHIFT) or
                (channel(AVATAR_BACKGROUND_CHANNEL, AVATAR_INK_GREEN) shl AVATAR_GREEN_SHIFT) or
                channel(AVATAR_BACKGROUND_BLUE, AVATAR_INK_BLUE)
        }
        avatar.write(avatarFile)
        val images = get("assets/flutter_assets/assets/images/home")
        images.resolve("home_add_device_dark.png").copyTo(images.resolve("home_add_device.png"), overwrite = true)
    }
}
