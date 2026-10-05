/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.keepa.misc.accounts

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.util.inputStreamFromBundledResource
import org.w3c.dom.Element

private const val ACCOUNTS_ACTIVITY_CLASS = "app.hxreborn.extension.keepa.AccountsActivity"
private const val ACCOUNTS_ACTIVITY_THEME = "@style/AppTheme"
private const val BUNDLE = "assets/app/bundle.mjs"
private const val VENDOR = "assets/app/vendor.mjs"
private const val SETTINGS_CHUNK = "assets/app/src_app_features_settings_settings_component_ts.mjs"
private const val MANAGE_CHUNK = "assets/app/src_app_features_manage_manage_routes_ts.mjs"

private val multipleAccountsExtensionPatch = bytecodePatch {
    extendWith("extensions/extension.mpe")
}

@Suppress("unused")
val multipleAccountsPatch = resourcePatch(
    name = "Multiple accounts",
    description = "Signs in to several Keepa accounts at once and lists their price watches together. " +
        "Accounts are added and removed in Settings > Accounts.",
) {
    compatibleWith(AppCompatibilities.KEEPA)
    dependsOn(removePairipProtectionPatch, multipleAccountsExtensionPatch)

    execute {
        val runtime = inputStreamFromBundledResource("keepa", "accounts.js")
            ?.bufferedReader()?.use { it.readText() }
            ?: throw PatchException("Runtime resource keepa/accounts.js is unavailable")

        val bundle = get(BUNDLE)
        val settings = get(SETTINGS_CHUNK)
        val manage = get(MANAGE_CHUNK)
        requireRuntimeDependencies(runtime, bundle.readText() + get(VENDOR).readText())
        val editedBundle = applyNetworkEdits(bundle.readText(), runtime)
        val editedSettings = applySettingsEdits(settings.readText())
        val editedManage = applyManageEdits(manage.readText())
        bundle.writeText(editedBundle)
        settings.writeText(editedSettings)
        manage.writeText(editedManage)
    }

    finalize {
        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0) as Element
            val activity = document.createElement("activity")
            activity.setAttribute("android:name", ACCOUNTS_ACTIVITY_CLASS)
            activity.setAttribute("android:exported", "false")
            activity.setAttribute("android:theme", ACCOUNTS_ACTIVITY_THEME)
            application.appendChild(activity)
        }
    }
}

internal class Edit(val label: String, val anchor: String, val replacement: String)

internal class MinifiedSource(private var text: String, private val file: String) {

    private val bindings = mutableMapOf<String, String>()

    fun bind(name: String, value: String) {
        bindings[name] = value
    }

    fun value(name: String): String =
        bindings[name] ?: throw PatchException("$file: $name was never resolved")

    fun find(label: String, anchor: String): MatchResult {
        val names = mutableListOf<String>()
        val pattern = StringBuilder()
        var last = 0
        for (placeholder in PLACEHOLDER.findAll(anchor)) {
            pattern.append(Regex.escape(anchor.substring(last, placeholder.range.first)))
            val name = placeholder.groupValues[2]
            val bound = bindings[name]
            when {
                bound != null -> pattern.append(Regex.escape(bound))
                name in names -> pattern.append("\\k<").append(name).append('>')
                else -> {
                    names += name
                    val body = if (placeholder.groupValues[1] == "@") IDENTIFIER else NUMBER
                    pattern.append(NOT_AFTER_IDENTIFIER).append("(?<").append(name).append('>').append(body).append(')')
                }
            }
            last = placeholder.range.last + 1
        }
        pattern.append(Regex.escape(anchor.substring(last)))
        val matches = Regex(pattern.toString()).findAll(text).take(2).toList()
        if (matches.size != 1) {
            val found = if (matches.isEmpty()) "no match" else "more than one match"
            throw PatchException("$file: $label found $found for $anchor")
        }
        val match = matches.single()
        names.forEachIndexed { index, name -> bindings[name] = match.groupValues[index + 1] }
        return match
    }

    fun splice(edit: Edit) {
        val match = find(edit.label, edit.anchor)
        val resolved = PLACEHOLDER.replace(edit.replacement) { value(it.groupValues[2]) }
        text = text.replaceRange(match.range, resolved)
    }

    override fun toString() = text

    private companion object {
        val PLACEHOLDER = Regex("""([@#])\{([A-Za-z][A-Za-z0-9]*)}""")
        const val IDENTIFIER = """[\w$]+"""
        const val NUMBER = """\d+"""
        const val NOT_AFTER_IDENTIFIER = """(?<![\w$])"""
    }
}

internal fun requireRuntimeDependencies(runtime: String, shipped: String) {
    val modulePaths = Regex(""""(\./[^"]+\.(?:ts|js))"""").findAll(runtime).map { it.groupValues[1] }
        .filterNot { shipped.contains("\"$it\"(") }
    val exportNames = Regex("""(?:moduleExport|injectService|exportNamed)\([^,()]+,"(\w+)"\)""").findAll(runtime)
        .map { it.groupValues[1] }.filterNot { shipped.contains(":()=>$it") }
    val missing = (modulePaths + exportNames).toList()
    if (missing.isNotEmpty()) throw PatchException("accounts.js needs $missing, missing from this Keepa build")
}

internal fun constsCount(source: String, component: String): Int {
    val end = source.indexOf("],template:function ${component}_Template")
    val definition = source.indexOf("type:$component,")
    val start = if (definition < 0) -1 else source.indexOf("consts:[", definition)
    if (end < 0 || start < 0 || start > end) throw PatchException("$component consts array is missing")
    var depth = 0
    var count = 0
    var inString = false
    var index = start + "consts:".length
    while (index <= end) {
        val char = source[index]
        when {
            inString && char == '\\' -> index++
            inString && char == '"' -> inString = false
            inString -> Unit
            char == '"' -> inString = true
            char == '[' -> depth++
            char == ']' -> depth--
            char == ',' && depth == 1 -> count++
        }
        index++
    }
    return count + 1
}

private val webpackModuleKey = Regex(""""\./[^"]+"\([\w$,]*\)\{""")

internal fun applyNetworkEdits(bundle: String, runtime: String): String {
    val header = Regex(Regex.escape("\"${MultipleAccountsAnchors.NETWORK_MODULE}\"(")).find(bundle)
        ?: throw PatchException("bundle.mjs: module ${MultipleAccountsAnchors.NETWORK_MODULE} is missing")
    val end = webpackModuleKey.find(bundle, header.range.last + 1)?.range?.first
        ?: throw PatchException("bundle.mjs: could not find the end of the network service module")

    val module = MinifiedSource(bundle.substring(header.range.first, end), "bundle.mjs network module")
    module.bind("runtime", runtime)
    module.find("NetworkService constructor", MultipleAccountsAnchors.NETWORK_CONSTRUCTOR)
    MultipleAccountsAnchors.networkServiceEdits.forEach(module::splice)

    val source = MinifiedSource(bundle.substring(0, header.range.first) + module + bundle.substring(end), "bundle.mjs")
    MultipleAccountsAnchors.storageServiceEdits.forEach(source::splice)
    return source.toString()
}

internal fun applySettingsEdits(chunk: String): String {
    val source = MinifiedSource(chunk, "settings chunk")
    MultipleAccountsAnchors.settingsEdits.forEach(source::splice)
    return source.toString()
}

internal fun applyManageEdits(chunk: String): String {
    val source = MinifiedSource(chunk, "manage chunk")
    val first = constsCount(chunk, "ManageComponent")
    MultipleAccountsAnchors.MANAGE_CONST_NAMES.forEachIndexed { offset, name -> source.bind(name, (first + offset).toString()) }
    MultipleAccountsAnchors.manageEdits.forEach(source::splice)
    return source.toString()
}

internal object MultipleAccountsAnchors {

    const val NETWORK_MODULE = "./src/app/services/network/network.service.ts"
    const val NETWORK_CONSTRUCTOR = """class NetworkService{constructor(){this.errorService=(0,@{core}.@{inject})("""

    val networkServiceEdits = listOf(
        Edit(
            "network module header",
            """"$NETWORK_MODULE"(@{module},@{exports},@{require}){""",
            """"$NETWORK_MODULE"(@{module},@{exports},@{require}){const hxRequire=@{require};@{runtime}""",
        ),
        Edit(
            "NetworkService constructor end",
            """this.reconnect(!0);this.listenForTurnstileEvents()}""",
            """hxAccountsRuntime.attach(this,@{core}.@{inject},@{core});this.reconnect(!0);this.listenForTurnstileEvents()}""",
        ),
        Edit(
            "NetworkService.request",
            """}request(@{message}){""",
            """}request(@{message}){{const hxRouted=hxAccountsRuntime.route(this,@{message});if(hxRouted)return hxRouted}""",
        ),
        Edit(
            "NetworkService forced logout",
            """@{service}.flushAllRequests();@{service}.navigationService.navigate(["/login"],{queryParams:{type:"forceLogout"}})""",
            """if(hxAccountsRuntime.onForceLogout(@{service}))return;@{service}.flushAllRequests();@{service}.navigationService.navigate(["/login"],{queryParams:{type:"forceLogout"}})""",
        ),
        Edit(
            "NetworkService.handleAppResume",
            """handleAppResume(){""",
            """handleAppResume(){if(!this.hxSecondary)hxAccountsRuntime.applyPending();""",
        ),
    )

    val storageServiceEdits = listOf(
        Edit(
            "StorageService.updateWithServerData",
            """@{settings}=this.validateSettings(@{settings});this.setSettings(@{settings},""",
            """@{settings}=this.validateSettings(@{settings});globalThis.hxAccounts&&globalThis.hxAccounts.onServerData(@{settings});this.setSettings(@{settings},""",
        ),
        Edit(
            "StorageService.resetSettings",
            """resetSettings(){""",
            """resetSettings(){if(globalThis.hxAccounts&&globalThis.hxAccounts.onReset())return;""",
        ),
    )

    val settingsEdits = listOf(
        Edit(
            "deleteAccount row const",
            """@{deleteRow}=@{text}=>({name:"settings.deleteAccount",key:"delete",text:@{text}})""",
            """@{deleteRow}=@{text}=>({name:"settings.deleteAccount",key:"delete",text:@{text}}),hxAccountsRow=@{text}=>({name:"Accounts",key:"hxAccounts",textSkipTranslate:@{text}})""",
        ),
        Edit(
            "account section template",
            """function @{deleteRowTpl}_Template(@{p},@{q}){1&@{p}&&@{i}.@{elementContainer}(0)}function @{section}_Template(@{rf},@{ctx}){if(1&@{rf}){@{i}.@{elementStart}(0,"StackLayout",#{group});@{i}.@{template}(1,@{header}_Template,1,0,"ng-container",#{outlet});@{i}.@{elementStart}(2,"StackLayout",#{list});@{i}.@{template}(3,@{logoutRow}_Template,1,0,"ng-container",#{outlet});@{i}.@{element}(4,"StackLayout",#{divider});@{i}.@{template}(5,@{deleteRowTpl}_Template,1,0,"ng-container",#{outlet});@{i}.@{elementEnd}()()}if(2&@{rf}){const @{cmp}=@{i}.@{nextContext}(),@{headerRef}=@{i}.@{reference}(#{headerSlot}),@{rowRef}=@{i}.@{reference}(#{rowSlot});@{i}.@{advance}();@{i}.@{property}("ngTemplateOutlet",@{headerRef})("ngTemplateOutletContext",@{i}.@{pure0}(6,@{groupRow}));@{i}.@{advance}(2);@{i}.@{property}("ngTemplateOutlet",@{rowRef})("ngTemplateOutletContext",@{i}.@{pure1}(7,@{logoutConst},@{cmp}.settings.username+" ( "+(@{cmp}.settings.email||"-")+" )"));@{i}.@{advance}(2);@{i}.@{property}("ngTemplateOutlet",@{rowRef})("ngTemplateOutletContext",@{i}.@{pure1}(9,@{deleteRow},@{cmp}.allowDeletion?"settings.deleteAccountTap":"settings.deleteAccountInfo"))}}""",
            """function @{deleteRowTpl}_Template(@{p},@{q}){1&@{p}&&@{i}.@{elementContainer}(0)}function @{section}_Template(@{rf},@{ctx}){if(1&@{rf}){@{i}.@{elementStart}(0,"StackLayout",#{group});@{i}.@{template}(1,@{header}_Template,1,0,"ng-container",#{outlet});@{i}.@{elementStart}(2,"StackLayout",#{list});@{i}.@{template}(3,@{logoutRow}_Template,1,0,"ng-container",#{outlet});@{i}.@{element}(4,"StackLayout",#{divider});@{i}.@{template}(5,@{deleteRowTpl}_Template,1,0,"ng-container",#{outlet});@{i}.@{element}(6,"StackLayout",#{divider});@{i}.@{template}(7,@{deleteRowTpl}_Template,1,0,"ng-container",#{outlet});@{i}.@{elementEnd}()()}if(2&@{rf}){const @{cmp}=@{i}.@{nextContext}(),@{headerRef}=@{i}.@{reference}(#{headerSlot}),@{rowRef}=@{i}.@{reference}(#{rowSlot});@{i}.@{advance}();@{i}.@{property}("ngTemplateOutlet",@{headerRef})("ngTemplateOutletContext",@{i}.@{pure0}(8,@{groupRow}));@{i}.@{advance}(2);@{i}.@{property}("ngTemplateOutlet",@{rowRef})("ngTemplateOutletContext",@{i}.@{pure1}(9,@{logoutConst},@{cmp}.settings.username+" ( "+(@{cmp}.settings.email||"-")+" )"));@{i}.@{advance}(2);@{i}.@{property}("ngTemplateOutlet",@{rowRef})("ngTemplateOutletContext",@{i}.@{pure1}(11,@{deleteRow},@{cmp}.allowDeletion?"settings.deleteAccountTap":"settings.deleteAccountInfo"));@{i}.@{advance}(2);@{i}.@{property}("ngTemplateOutlet",@{rowRef})("ngTemplateOutletContext",@{i}.@{pure1}(13,hxAccountsRow,@{cmp}.hxAccountsSummary))}}""",
        ),
        Edit(
            "account section decls and vars",
            """@{i}.@{conditional}(4,@{section}_Template,6,11,"StackLayout",#{group})""",
            """@{i}.@{conditional}(4,@{section}_Template,8,15,"StackLayout",#{group})""",
        ),
        Edit(
            "logout tap case",
            """case"logout":this._goToLogout();break;""",
            """case"hxAccounts":globalThis.hxAccounts&&globalThis.hxAccounts.openAccounts();break;case"logout":this._goToLogout();break;""",
        ),
        Edit(
            "SettingsComponent constructor",
            """class SettingsComponent extends @{baseModule}.@{baseExport}{constructor(){super();""",
            """class SettingsComponent extends @{baseModule}.@{baseExport}{constructor(){super();globalThis.hxAccounts&&globalThis.hxAccounts.watchSettings(this);Object.defineProperty(this,"hxAccountsSummary",{configurable:!0,get:function(){return globalThis.hxAccounts?globalThis.hxAccounts.summary():""}});""",
        ),
    )

    val MANAGE_CONST_NAMES = listOf("hxTagRow", "hxTagIcon", "hxTagName", "hxFlagTag")

    val manageEdits = listOf(
        Edit(
            "product card grid rows",
            """["columns","auto, 80, *","rows","29, 29, 29"]""",
            """["columns","auto, 80, *","rows","29, 29, 29, auto"]""",
        ),
        Edit(
            "product card consts",
            """],template:function ManageComponent_Template""",
            """,["col","0","colSpan","3","row","3","columns","auto, *",1,"mt-1",3,"visibility"],["col","0","text","",1,"m-icon","text-xl","text-muted","mr-1"],["col","1","textWrap","true","verticalAlignment","center",1,"text-sm","text-muted",3,"text"],["col","1","row","2","textWrap","true","horizontalAlignment","center","verticalAlignment","top",1,"text-xs","text-muted",3,"text","visibility"]],template:function ManageComponent_Template""",
        ),
        Edit(
            "product card create block",
            """@{i}.@{element}(4,"Image",#{amazonLogo})(5,"Image",#{flag});@{i}.@{elementEnd}()();@{i}.@{elementStart}(6,"StackLayout",#{coupon});@{i}.@{element}(7,"Label",#{couponLabel});@{i}.@{pipe}(8,"translate");@{i}.@{element}(9,"app-product-value-coupon",#{couponValue});@{i}.@{elementEnd}();@{i}.@{elementStart}(10,"StackLayout",#{thumbs});@{i}.@{element}(11,"Label",#{thumbLabel});@{i}.@{elementEnd}()""",
            """@{i}.@{element}(4,"Image",#{amazonLogo})(5,"Image",#{flag});@{i}.@{elementEnd}()();@{i}.@{element}(6,"Label",#{hxFlagTag});@{i}.@{elementStart}(7,"GridLayout",#{hxTagRow});@{i}.@{element}(8,"Label",#{hxTagIcon})(9,"Label",#{hxTagName});@{i}.@{elementEnd}();@{i}.@{elementStart}(10,"StackLayout",#{coupon});@{i}.@{element}(11,"Label",#{couponLabel});@{i}.@{pipe}(12,"translate");@{i}.@{element}(13,"app-product-value-coupon",#{couponValue});@{i}.@{elementEnd}();@{i}.@{elementStart}(14,"StackLayout",#{thumbs});@{i}.@{element}(15,"Label",#{thumbLabel});@{i}.@{elementEnd}()""",
        ),
        Edit(
            "product card decls and vars",
            """ManageComponent_ng_template_4_Template,12,14""",
            """ManageComponent_ng_template_4_Template,16,18""",
        ),
        Edit(
            "product card flag binding",
            """@{cmp}.flagSrcMap.get(@{tracking}?@{tracking}.mainDomainId:@{cmp}.settings.domain));@{i}.@{advance}();@{i}.@{property}("visibility",""",
            """@{cmp}.flagSrcMap.get(@{tracking}?@{tracking}.mainDomainId:@{cmp}.settings.domain));{const hxTag=globalThis.hxAccounts?globalThis.hxAccounts.productTag(@{tracking}&&@{tracking}.asin):{};@{i}.@{advance}();@{i}.@{property}("text",hxTag.flag||"")("visibility",hxTag.flag?"visible":"collapsed");@{i}.@{advance}();@{i}.@{property}("visibility",hxTag.below?"visible":"collapsed");@{i}.@{advance}(2);@{i}.@{property}("text",hxTag.below||"")}@{i}.@{advance}();@{i}.@{property}("visibility",""",
        ),
        Edit(
            "product card coupon pipe",
            """@{i}.@{pipeBind1}(8,12,"notification.inclCoupon")""",
            """@{i}.@{pipeBind1}(12,16,"notification.inclCoupon")""",
        ),
    )
}
