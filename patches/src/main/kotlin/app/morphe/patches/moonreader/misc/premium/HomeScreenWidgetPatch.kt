/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.moonreader.misc.premium

import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Document
import org.w3c.dom.Element

private const val COMPONENTS_PACKAGE = "com.flyersoft.components"
private const val APP_THEME = "@style/MRThemeSelector"
private const val WIDGET_OPTIONS_THEME = "hx_widget_options_theme"
private val TRANSLUCENT_WINDOW_ATTRIBUTES = mapOf(
    "android:windowIsTranslucent" to "true",
    "android:windowBackground" to "@android:color/transparent",
    "android:windowNoTitle" to "true",
    "android:backgroundDimEnabled" to "false",
)

internal val homeScreenWidgetPatch = resourcePatch {
    execute {
        document("res/values/styles.xml").use { document ->
            document.documentElement.appendChild(
                document.createElement("style").apply {
                    setAttribute("name", WIDGET_OPTIONS_THEME)
                    setAttribute("parent", APP_THEME)
                    TRANSLUCENT_WINDOW_ATTRIBUTES.forEach { (name, value) ->
                        appendChild(
                            document.createElement("item").apply {
                                setAttribute("name", name)
                                textContent = value
                            },
                        )
                    }
                },
            )
        }

        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0) as Element

            application.appendChild(
                document.component("receiver", "WidgetProvider", exported = true).apply {
                    appendChild(document.intentFilter("android.appwidget.action.APPWIDGET_UPDATE"))
                    appendChild(
                        document.createElement("meta-data").apply {
                            setAttribute("android:name", "android.appwidget.provider")
                            setAttribute("android:resource", "@xml/widget_provider")
                        },
                    )
                },
            )
            application.appendChild(
                document.component("service", "WidgetService", exported = false).apply {
                    setAttribute("android:permission", "android.permission.BIND_REMOTEVIEWS")
                },
            )
            application.appendChild(
                document.component("activity", "WidgetOptionActivity", exported = true).apply {
                    setAttribute("android:theme", "@style/$WIDGET_OPTIONS_THEME")
                    appendChild(document.intentFilter("android.appwidget.action.APPWIDGET_CONFIGURE"))
                },
            )
        }
    }
}

private fun Document.component(tag: String, className: String, exported: Boolean) =
    createElement(tag).apply {
        setAttribute("android:name", "$COMPONENTS_PACKAGE.$className")
        setAttribute("android:exported", exported.toString())
    }

private fun Document.intentFilter(action: String) =
    createElement("intent-filter").apply {
        appendChild(createElement("action").apply { setAttribute("android:name", action) })
    }
