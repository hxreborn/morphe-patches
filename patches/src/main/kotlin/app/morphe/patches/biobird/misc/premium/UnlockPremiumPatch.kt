/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.biobird.misc.premium

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.dart.DART_AOT_LIBRARY
import app.morphe.patches.shared.misc.dart.dartAotLibrary
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.replaceMasked

internal val ACTIVE_SUBSCRIPTION_PROVIDER_PROLOGUE =
    (
        "fd79bfa9fd030faaef6100d1501f40f9ff0110eb09040054403740f900cc4cf970" +
        "2340f91f00106b81000054625f4091427041f936160c94705f4091107641f9be0b" +
        "40f9fec100a9e00100f9642f40f96fd2fe9701f05ff8217c4cd3f00300aae00301" +
        "aae10310aa1edc3fd1be7a7ef8c0033fd61f00166b"
    ).hexToByteArray()

internal val ACTIVE_SUBSCRIPTION_PROVIDER_PROLOGUE_MASK =
    (
        "ffffffffffffffffffffffffff03c0ffffffffff1f0000ffff03c0ffff03c0ff1f" +
        "00c0ffffffffffffffffffff03c0ffff03c0ff000000fcff03c0ffff03c0ffffff" +
        "ffffffffffffffffffffff03c0ff000000fcffffffffffffffffffffffffffffff" +
        "ffffffffffff03c0ffffffffffffffffffffffffff"
    ).hexToByteArray()

internal val CURRENT_ENTITLEMENT_PROVIDER_PROLOGUE =
    (
        "fd79bfa9fd030faaef6100d1501f40f9ff0110eb69030054403740f900cc4cf970" +
        "2340f91f00106b81000054625f4091427041f9f9ad0c94705f4091107641f9be0b" +
        "40f9fec100a9e00100f9642f40f9326aff9701f05ff8217c4cd3f00300aae00301" +
        "aae10310aa1edc3fd1be7a7ef8c0033fd6ef031daa"
    ).hexToByteArray()

internal val CURRENT_ENTITLEMENT_PROVIDER_PROLOGUE_MASK =
    (
        "ffffffffffffffffffffffffffffffffffffffff1f0000ffff03c0ffff03c0ff1f" +
        "00c0ffffffffffffffffffff03c0ffff03c0ff000000fcff03c0ffff03c0ffffff" +
        "ffffffffffffffffffffff03c0ff000000fcffffffffffffffffffffffffffffff" +
        "ffffffffffff03c0ffffffffffffffffffffffffff"
    ).hexToByteArray()

internal const val STACK_LIMIT_LOAD_OFFSET = 12

internal class DartBuild(stackLimitLoad: String, pcbEntitlementLoad: String) {
    val stackLimitLoad = stackLimitLoad.hexToByteArray()
    val pcbEntitlementLoad = pcbEntitlementLoad.hexToByteArray()
    val returnPcbEntitlement = this.pcbEntitlementLoad + "c0035fd6".hexToByteArray()
}

internal val DART_BUILDS = listOf(
    DartBuild(stackLimitLoad = "501f40f9", pcbEntitlementLoad = "606b409100f444f9"),
    DartBuild(stackLimitLoad = "502740f9", pcbEntitlementLoad = "606f409100fc41f9"),
)

internal val ENTITLEMENT_FETCH_SEQUENCE =
    ("e10303aa6203409142a441f900000094a00358f801f041b821801c8ba28358f800000094" +
        "e10300aaa18317f800000094").hexToByteArray()

internal val ENTITLEMENT_FETCH_SEQUENCE_MASK =
    ("ffffffffff03c0ffff03c0ff000000fcffffffffffffffffffffffffffffffff000000fc" +
        "ffffffffffffffff000000fc").hexToByteArray()

internal const val FETCH_USER_ENTITLEMENT_CALL_OFFSET = 28

internal val RETURN_TRUE = "c0820091c0035fd6".hexToByteArray()

internal val RETURN_FALSE = "c0c20091c0035fd6".hexToByteArray()

internal val HAS_PASS_PROVIDER =
    (
        "fd79bfa9fd030faaefc100d1502740f9ff0110eba9090054403f40f900244ef950" +
        "4b40f91f00106b81000054624f409142e043f928df1294704f409110e643f9be0b" +
        "40f9fec100a9e00100f9642340f9b94ffc97704f409110ea43f9e04100a9647744" +
        "f9e7e6ff97a0831ff81f00166ba1000054c0c20091ef031daafd79c1a8c0035fd6"
    ).hexToByteArray()

internal val HAS_PASS_PROVIDER_MASK =
    (
        "ffffffffffffffffff0380ffff03c0ffffffffff1f0000ffff03c0ffff03c0ffff" +
        "03c0ffffffffff1f0000ffff0380ffff03c0ff000000fcff0380ffff03c0ffff03" +
        "c0ffffffffffff03c0ffff03c0ff000000fcff0380ffff03c0ffffffffffff03c0" +
        "ff000000fcffffffffffffffff1f0000ffff0380ffffffffffffffffffffffffff"
    ).hexToByteArray()

internal val CONTENT_LOCKED_PROVIDER =
    (
        "fd79bfa9fd030faaef8100d1502740f9ff0110eb490a0054403f40f900204ef950" +
        "4b40f91f00106b81000054624f4091426442f987e01294701747f9be0f40f9fec1" +
        "00a9e00100f9642340f91951fc971f00166ba1000054c0c20091ef031daafd79c1" +
        "a8c0035fd6403f40f9002c4ef9504b40f91f00106b81000054624f409142bc43f9" +
        "73e01294700b4cf9be0f40f9fec100a9e00100f9642340f90551fc97a0002037c0" +
        "c20091ef031daafd79c1a8c0035fd6"
    ).hexToByteArray()

internal val CONTENT_LOCKED_PROVIDER_MASK =
    (
        "ffffffffffffffffff0380ffff03c0ffffffffff1f0000ffff03c0ffff03c0ffff" +
        "03c0ffffffffff1f0000ffff0380ffff03c0ff000000fcff03c0ffff03c0ffffff" +
        "ffffff03c0ffff03c0ff000000fcffffffff1f0000ffff0380ffffffffffffffff" +
        "ffffffffffff03c0ffff03c0ffff03c0ffffffffff1f0000ffff0380ffff03c0ff" +
        "000000fcff03c0ffff03c0ffffffffffff03c0ffff03c0ff000000fcffffffffff" +
        "0380ffffffffffffffffffffffffff"
    ).hexToByteArray()

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks all chapters and previous-year papers. " +
        "Signing in requires GmsCore support.",
) {
    compatibleWith(AppCompatibilities.BIOBIRD)
    availability(requireArm64)
    dependsOn(removePairipProtectionPatch)

    execute {
        val library = dartAotLibrary()

        if (library.replaceMasked(HAS_PASS_PROVIDER, HAS_PASS_PROVIDER_MASK, mapOf(0 to RETURN_TRUE))) {
            if (!library.replaceMasked(
                    CONTENT_LOCKED_PROVIDER,
                    CONTENT_LOCKED_PROVIDER_MASK,
                    mapOf(0 to RETURN_FALSE),
                )
            ) {
                throw PatchException("Could not find the content locked provider in $DART_AOT_LIBRARY")
            }
            return@execute
        }

        if (!library.replaceMasked(
                ACTIVE_SUBSCRIPTION_PROVIDER_PROLOGUE,
                ACTIVE_SUBSCRIPTION_PROVIDER_PROLOGUE_MASK,
                mapOf(0 to RETURN_TRUE),
            )
        ) {
            throw PatchException("Could not find the active subscription provider in $DART_AOT_LIBRARY")
        }

        val build = DART_BUILDS.firstOrNull { build ->
            val prologue = CURRENT_ENTITLEMENT_PROVIDER_PROLOGUE.copyOf()
            build.stackLimitLoad.copyInto(prologue, STACK_LIMIT_LOAD_OFFSET)
            library.replaceMasked(
                prologue,
                CURRENT_ENTITLEMENT_PROVIDER_PROLOGUE_MASK,
                mapOf(0 to build.returnPcbEntitlement),
            )
        } ?: throw PatchException("Could not find the current entitlement provider in $DART_AOT_LIBRARY")

        val callReplacement = mapOf(FETCH_USER_ENTITLEMENT_CALL_OFFSET to build.pcbEntitlementLoad)
        if (!library.replaceMasked(ENTITLEMENT_FETCH_SEQUENCE, ENTITLEMENT_FETCH_SEQUENCE_MASK, callReplacement)) {
            throw PatchException("Could not find the fetchUserEntitlement call in $DART_AOT_LIBRARY")
        }
    }
}
