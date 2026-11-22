/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.watchit.misc.piracy

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.misc.smali.descriptor

internal val disablePiracyCheckPatch = patch {
    execute {
        val start = PiracyCheckerStartFingerprint.match()
        val (allowCallbackRead) = start.instructionMatches
        val allowCallbackField = allowCallbackRead.fieldReference
        val (allowCallbackCall) = allowVerificationCallbackFingerprint(allowCallbackField.type)
            .match(start.method, start.classDef).instructionMatches

        start.method.addInstructions(
            0,
            """
                move-object/from16 v0, p0
                iget-object v0, v0, ${descriptor(allowCallbackField)}
                if-eqz v0, :skip
                invoke-virtual { v0 }, ${descriptor(allowCallbackCall.methodReference)}
                :skip
                return-void
            """,
        )
    }
}
