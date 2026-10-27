/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.download

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.bereal.misc.settings.BEREAL_EXTENSION_PACKAGE
import app.morphe.patches.bereal.misc.settings.instructionMatch
import app.morphe.patches.bereal.misc.settings.settingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.setExtensionIsPatchIncluded
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val DOWNLOAD_ENABLED = "${BEREAL_EXTENSION_PACKAGE}download/FriendsPostDownload;->enabled()Z"

private const val LAST_FOUR_BIT_REGISTER = 15

@Suppress("unused")
val saveFriendsBeRealsPatch = bytecodePatch(
    name = "Save friends' BeReals",
    description = "Adds Download to the 3-dot menu of friends' BeReals in the feed and on the RealMoji screen.",
) {
    compatibleWith(AppCompatibilities.BEREAL)
    dependsOn(settingsPatch)

    execute {
        setExtensionIsPatchIncluded("${BEREAL_EXTENSION_PACKAGE}download/FriendsPostDownload;")

        val feedMenu = FeedPostMenuFingerprint.matchSingle()
        val method = feedMenu.method
        val resolveText = feedMenu.instructionMatch(Opcode.INVOKE_INTERFACE).getInstruction<ReferenceInstruction>()
            .getReference<MethodReference>()!!
        val downloadType = feedMenu.instructionMatch(Opcode.SGET_OBJECT).getInstruction<ReferenceInstruction>()
            .getReference<FieldReference>()!!
        val menuItemInit = feedMenu.instructionMatch(Opcode.INVOKE_DIRECT_RANGE).getInstruction<ReferenceInstruction>()
            .getReference<MethodReference>()!!
        val friendItems = feedFriendMenuItemsFingerprint(feedMenu.classDef.type).matchOrNull(method)
            ?: throw PatchException("No friend menu items in ${method.definingClass}")
        val friendItem = friendItems.instructionMatch(Opcode.INVOKE_DIRECT_RANGE)
            .getInstruction<RegisterRangeInstruction>()
        val listResult = friendItems.instructionMatches.last()
        val listIndex = listResult.index
        val list = listResult.getInstruction<OneRegisterInstruction>().registerA

        val item = friendItem.startRegister
        val postId = item + 1
        val title = item + 2
        val icon = item + 3
        val flag = item + 4
        val type = item + 5
        if (type > LAST_FOUR_BIT_REGISTER || list > LAST_FOUR_BIT_REGISTER || list in item..type) {
            throw PatchException("Feed menu registers v$item..v$type and list v$list do not fit")
        }
        val textProvider = classDefBy(method.definingClass).fields.single { it.type == resolveText.definingClass }
        val initSignature = "${menuItemInit.definingClass}-><init>(${menuItemInit.parameterTypes.joinToString("")})V"

        method.addInstructionsWithLabels(
            listIndex + 1,
            """
                invoke-static { }, $DOWNLOAD_ENABLED
                move-result v$item
                if-eqz v$item, :skip_download
                move-object/from16 v$title, p0
                iget-object v$title, v$title, ${method.definingClass}->${textProvider.name}:${textProvider.type}
                const v$icon, ${resourceId(ResourceType.STRING, "sharing_service_download")}
                invoke-interface/range { v$title .. v$icon }, $resolveText
                move-result-object v$title
                const v$icon, ${resourceId(ResourceType.DRAWABLE, "download_cloud_and_arrow")}
                invoke-static/range { v$icon .. v$icon }, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
                move-result-object v$icon
                const/4 v$flag, 0x0
                sget-object v$type, $downloadType
                new-instance v$item, ${menuItemInit.definingClass}
                invoke-direct/range { v$item .. v$type }, $initSignature
                invoke-virtual { v$list, v$flag, v$item }, Ljava/util/ArrayList;->add(ILjava/lang/Object;)V
            """,
            ExternalLabel("skip_download", method.getInstruction(listIndex + 1)),
        )

        val reactionsMenu = ReactionsPostMenuFingerprint.matchSingle()
        val reactions = reactionsMenu.method
        val ownerCheck = reactionsMenu.instructionMatch(Opcode.IF_EQZ).index
        val ownerBranch = reactions.getInstruction<BuilderOffsetInstruction>(ownerCheck)
        val ownerPassed = reactions.getInstruction(reactionsMenu.instructionMatch(Opcode.IF_NEZ).index)
        val notOwner = ownerBranch.target.location.instruction
        val isOwner = (ownerBranch as OneRegisterInstruction).registerA
        val enabled = reactions.getFreeRegisterProvider(ownerCheck, 1).getFreeRegister()
        reactions.replaceInstruction(ownerCheck, "invoke-static { }, $DOWNLOAD_ENABLED")
        reactions.addInstructionsWithLabels(
            ownerCheck + 1,
            """
                move-result v$enabled
                if-nez v$enabled, :owner_passed
                if-eqz v$isOwner, :not_owner
            """,
            ExternalLabel("owner_passed", ownerPassed),
            ExternalLabel("not_owner", notOwner!!),
        )
    }
}
