/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.lingodeer.misc.pairip

import app.morphe.patcher.patch.classDefBy
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.pairip.replaceVirtualizedMethod
import com.android.tools.smali.dexlib2.AccessFlags

private const val APPLICATION_CLASS = "Lcom/lingo/app/LingoSkillApplication;"
private const val MAIN_ACTIVITY_CLASS = "Lcom/lingo/main/ui/MainComposeActivity;"
private const val HIDDEN_ACTIVITY_CLASS = "Landroidx/credentials/playservices/controllers/identityauth/HiddenActivity;"
private const val APPLICATION_REGISTER_COUNT = 13
private const val MAIN_ACTIVITY_REGISTER_COUNT = 8
private const val HIDDEN_ACTIVITY_REGISTER_COUNT = 13
private const val FINISHING_ACTIVITY_REGISTER_COUNT = 2
private const val RESULT_RECEIVER = "Landroid/os/ResultReceiver;"
private const val STRING_TYPE = "Ljava/lang/String;"
private const val PROCESS_INFO = "Landroid/app/ActivityManager${'$'}RunningAppProcessInfo;"
private const val BUILD_VERSION = "Landroid/os/Build${'$'}VERSION;"
private const val EXCEPTION_HANDLER = "Ljava/lang/Thread${'$'}UncaughtExceptionHandler;"

private val finishingActivityTypes = listOf(
    "Lcom/facebook/CustomTabMainActivity;",
    "Lcom/android/billingclient/api/ProxyBillingActivity;",
    "Lcom/android/billingclient/api/ProxyBillingActivityV2;",
    "Lcom/google/android/gms/auth/api/signin/internal/SignInHubActivity;",
    "Lcom/google/android/gms/common/api/GoogleApiActivity;",
    "Lcom/google/android/play/core/common/PlayCoreDialogWrapperActivity;",
)

private val referencedTypes = listOf(
    "Lim9;", "Li73;", "Lz79;", "Lm73;", "Ltpf;", "Lhgf;", "Ltre;", "Ljlb;", "Lwo3;", "Lje0;", "Ln56;",
    "Ldu9;", "Leu9;", "Law8;", "Ltf0;", "Ls1;", "Lyyf;", "Lrs4;", "Lri0;", "Lfe9;", "Lo31;",
    "Lu23;", "Lxcc;", "Leje;", "Lbp3;", "Lrr3;", "Lir3;", "Lvr3;", "Lxi7;", "Lfbg;",
)

private class CredentialFlowFailure(val type: String, val errorName: String, val sendFailure: String, val pendingFailure: String)

private val credentialFlowFailures = listOf(
    CredentialFlowFailure(
        "BEGIN_SIGN_IN",
        "GET_UNKNOWN",
        "During begin sign in, one tap ui intent sender failure: ",
        "internal error during the begin sign in operation",
    ),
    CredentialFlowFailure(
        "CREATE_PASSWORD",
        "CREATE_UNKNOWN",
        "During save password, found UI intent sender failure: ",
        "internal error during password creation",
    ),
    CredentialFlowFailure(
        "CREATE_PUBLIC_KEY_CREDENTIAL",
        "CREATE_UNKNOWN",
        "During public key credential, found IntentSender failure on public key creation: ",
        "internal error during public key credential creation",
    ),
    CredentialFlowFailure(
        "SIGN_IN_INTENT",
        "GET_UNKNOWN",
        "During get sign-in intent, one tap ui intent sender failure: ",
        "internal error during the sign-in intent operation",
    ),
)

private fun <T> List<T>.requireSingle(description: String): T {
    check(size == 1) { "Expected one $description in $HIDDEN_ACTIVITY_CLASS, found $size" }
    return single()
}

private fun hiddenActivityOnCreate(activity: MutableClass): String {
    val receiver = activity.fields.filter { it.type == RESULT_RECEIVER }.requireSingle("ResultReceiver field")
    val waiting = activity.fields.filter { it.type == "Z" }.requireSingle("boolean field")
    val setupFailure = activity.methods.filter { method ->
        !AccessFlags.STATIC.isSet(method.accessFlags) && method.returnType == "V" &&
            method.parameterTypes.map(CharSequence::toString) == listOf(RESULT_RECEIVER, STRING_TYPE, STRING_TYPE)
    }.requireSingle("instance method taking (ResultReceiver, String, String)")
    val receiverField = "$HIDDEN_ACTIVITY_CLASS->${receiver.name}:$RESULT_RECEIVER"
    val waitingField = "$HIDDEN_ACTIVITY_CLASS->${waiting.name}:Z"
    val failureCall = "$HIDDEN_ACTIVITY_CLASS->${setupFailure.name}($RESULT_RECEIVER$STRING_TYPE$STRING_TYPE)V"

    fun failureBranches(label: String, failureMessage: (CredentialFlowFailure) -> String) =
        credentialFlowFailures.joinToString("\n") { failure ->
            """
                const-string v3, "${failure.type}"
                invoke-virtual {v3, v2}, $STRING_TYPE->equals(Ljava/lang/Object;)Z
                move-result v3
                if-eqz v3, :${label}_not_${failure.type}
                ${failureMessage(failure)}
                const-string v5, "${failure.errorName}"
                iget-object v3, p0, $receiverField
                invoke-virtual {p0, v3, v5, v4}, $failureCall
                :${label}_not_${failure.type}
            """
        }
    val sendFailures = failureBranches("send") { failure ->
        """
            new-instance v4, Ljava/lang/StringBuilder;
            invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V
            const-string v5, "${failure.sendFailure}"
            invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append($STRING_TYPE)Ljava/lang/StringBuilder;
            invoke-virtual {v4, v0}, Ljava/lang/StringBuilder;->append($STRING_TYPE)Ljava/lang/StringBuilder;
            invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()$STRING_TYPE
            move-result-object v4
        """
    }
    val pendingFailures = failureBranches("pending") { failure ->
        "const-string v4, \"${failure.pendingFailure}\""
    }

    return """
        invoke-super {p0, p1}, Landroid/app/Activity;->onCreate(Landroid/os/Bundle;)V
        const/4 v0, 0x0
        invoke-virtual {p0, v0, v0}, Landroid/app/Activity;->overridePendingTransition(II)V
        invoke-virtual {p0}, Landroid/app/Activity;->getIntent()Landroid/content/Intent;
        move-result-object v1
        const-string v2, "TYPE"
        invoke-virtual {v1, v2}, Landroid/content/Intent;->getStringExtra($STRING_TYPE)$STRING_TYPE
        move-result-object v2
        const-string v3, "RESULT_RECEIVER"
        invoke-virtual {v1, v3}, Landroid/content/Intent;->getParcelableExtra($STRING_TYPE)Landroid/os/Parcelable;
        move-result-object v3
        check-cast v3, $RESULT_RECEIVER
        iput-object v3, p0, $receiverField
        if-nez v3, :has_receiver
        invoke-virtual {p0}, Landroid/app/Activity;->finish()V
        :has_receiver
        if-eqz p1, :not_restored
        const-string v3, "androidx.credentials.playservices.AWAITING_RESULT"
        invoke-virtual {p1, v3, v0}, Landroid/os/Bundle;->getBoolean(${STRING_TYPE}Z)Z
        move-result v3
        iput-boolean v3, p0, $waitingField
        :not_restored
        iget-boolean v3, p0, $waitingField
        if-eqz v3, :start_flow
        return-void
        :start_flow
        if-nez v2, :has_type
        const-string v3, "HiddenActivity"
        const-string v4, "Activity handed an unsupported type"
        invoke-static {v3, v4}, Landroid/util/Log;->w($STRING_TYPE$STRING_TYPE)I
        invoke-virtual {p0}, Landroid/app/Activity;->finish()V
        return-void
        :has_type
        const-string v4, "EXTRA_FLOW_PENDING_INTENT"
        invoke-virtual {v1, v4}, Landroid/content/Intent;->getParcelableExtra($STRING_TYPE)Landroid/os/Parcelable;
        move-result-object v4
        check-cast v4, Landroid/app/PendingIntent;
        const-string v5, "ACTIVITY_REQUEST_CODE"
        const/4 v6, 0x1
        invoke-virtual {v1, v5, v6}, Landroid/content/Intent;->getIntExtra(${STRING_TYPE}I)I
        move-result v5
        if-eqz v4, :pending_missing
        :send_start
        const/4 v0, 0x1
        iput-boolean v0, p0, $waitingField
        invoke-virtual {v4}, Landroid/app/PendingIntent;->getIntentSender()Landroid/content/IntentSender;
        move-result-object v4
        move-object v3, p0
        const/4 v6, 0x0
        const/4 v7, 0x0
        const/4 v8, 0x0
        const/4 v9, 0x0
        const/4 v10, 0x0
        invoke-virtual/range {v3 .. v10}, Landroid/app/Activity;->startIntentSenderForResult(Landroid/content/IntentSender;ILandroid/content/Intent;IIILandroid/os/Bundle;)V
        :send_end
        .catch Landroid/content/IntentSender${'$'}SendIntentException; {:send_start .. :send_end} :send_failed
        return-void
        :send_failed
        move-exception v0
        invoke-virtual {v0}, Ljava/lang/Throwable;->getMessage()$STRING_TYPE
        move-result-object v0
        $sendFailures
        return-void
        :pending_missing
        $pendingFailures
        return-void
    """
}

val restoreLifecyclePatch = patch {
    compatibleWith(AppCompatibilities.LINGODEER)

    dependsOn(removePairipVirtualizationPatch)

    execute {
        referencedTypes.forEach { classDefBy(it) }

        mutableClassDefBy(APPLICATION_CLASS).replaceVirtualizedMethod(
            "onCreate",
            APPLICATION_REGISTER_COUNT,
            """
                invoke-super {p0}, Landroid/app/Application;->onCreate()V
                const/4 v2, 0x0
                sget v0, $BUILD_VERSION->SDK_INT:I
                const/16 v1, 0x1c
                if-lt v0, v1, :legacy_name
                invoke-static {}, Landroid/app/Application;->getProcessName()Ljava/lang/String;
                move-result-object v0
                goto :have_name
                :legacy_name
                const-string v0, "activity"
                invoke-virtual {p0, v0}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;
                move-result-object v0
                instance-of v1, v0, Landroid/app/ActivityManager;
                if-eqz v1, :no_name
                check-cast v0, Landroid/app/ActivityManager;
                invoke-virtual {v0}, Landroid/app/ActivityManager;->getRunningAppProcesses()Ljava/util/List;
                move-result-object v0
                if-eqz v0, :no_name
                invoke-interface {v0}, Ljava/util/List;->iterator()Ljava/util/Iterator;
                move-result-object v0
                :next_process
                invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z
                move-result v1
                if-eqz v1, :no_name
                invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;
                move-result-object v1
                check-cast v1, $PROCESS_INFO
                iget v3, v1, $PROCESS_INFO->pid:I
                invoke-static {}, Landroid/os/Process;->myPid()I
                move-result v4
                if-ne v3, v4, :next_process
                iget-object v0, v1, $PROCESS_INFO->processName:Ljava/lang/String;
                goto :have_name
                :no_name
                move-object v0, v2
                :have_name
                if-eqz v0, :main_process
                invoke-virtual {p0}, Landroid/content/Context;->getPackageName()Ljava/lang/String;
                move-result-object v1
                invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v0
                if-eqz v0, :done
                :main_process
                new-instance v0, Lim9;
                const/4 v1, 0x3
                invoke-direct {v0, p0, v1}, Lim9;-><init>(Ljava/lang/Object;B)V
                sget-object v3, Li73;->M:Li73;
                monitor-enter v3
                :try_start
                new-instance v4, Lz79;
                invoke-direct {v4}, Lz79;-><init>()V
                sget-object v5, Li73;->N:Lm73;
                if-nez v5, :already_started
                iget-object v5, v4, Lz79;->a:Lm73;
                sput-object v5, Li73;->N:Lm73;
                invoke-virtual {v0, v4}, Lim9;->invoke(Ljava/lang/Object;)Ljava/lang/Object;
                iget-object v0, v4, Lz79;->a:Lm73;
                invoke-virtual {v0}, Lm73;->w()V
                :try_end
                .catchall {:try_start .. :try_end} :unlock
                monitor-exit v3
                goto :koin_started
                :already_started
                :throw_start
                new-instance v0, Lorg/koin/core/error/KoinApplicationAlreadyStartedException;
                const-string v1, "A Koin Application has already been started"
                invoke-direct {v0, v1}, Ljava/lang/Exception;-><init>(Ljava/lang/String;)V
                throw v0
                :throw_end
                .catchall {:throw_start .. :throw_end} :unlock
                :unlock
                move-exception v0
                monitor-exit v3
                throw v0
                :koin_started
                iget-object v6, p0, $APPLICATION_CLASS->a:Lwo3;
                iget-object v0, v4, Lz79;->a:Lm73;
                iget-object v0, v0, Lm73;->d:Ljava/lang/Object;
                check-cast v0, Ltpf;
                iget-object v0, v0, Ltpf;->e:Ljava/lang/Object;
                check-cast v0, Lhgf;
                const-class v1, Lje0;
                invoke-static {v1}, Ltre;->a(Ljava/lang/Class;)Lu23;
                move-result-object v1
                invoke-virtual {v0, v1, v2, v2}, Lhgf;->e(Lu23;Lxcc;Leje;)Ljava/lang/Object;
                move-result-object v5
                check-cast v5, Lje0;
                const/4 v9, 0x3
                const/4 v8, 0x0
                new-instance v7, Ldu9;
                invoke-direct {v7, p0, v2, v8}, Ldu9;-><init>(Lcom/lingo/app/LingoSkillApplication;Lbp3;B)V
                invoke-static {v6, v2, v2, v7, v9}, Ljlb;->g0(Lrr3;Lir3;Lvr3;Lxi7;I)Lfbg;
                new-instance v7, Leu9;
                invoke-direct {v7, v5, p0, v2, v8}, Leu9;-><init>(Lje0;Lcom/lingo/app/LingoSkillApplication;Lbp3;B)V
                invoke-static {v6, v2, v2, v7, v9}, Ljlb;->g0(Lrr3;Lir3;Lvr3;Lxi7;I)Lfbg;
                new-instance v7, Leu9;
                const/4 v8, 0x1
                invoke-direct {v7, v5, p0, v2, v8}, Leu9;-><init>(Lje0;Lcom/lingo/app/LingoSkillApplication;Lbp3;B)V
                invoke-static {v6, v2, v2, v7, v9}, Ljlb;->g0(Lrr3;Lir3;Lvr3;Lxi7;I)Lfbg;
                iget-object v0, v4, Lz79;->a:Lm73;
                iget-object v0, v0, Lm73;->d:Ljava/lang/Object;
                check-cast v0, Ltpf;
                iget-object v0, v0, Ltpf;->e:Ljava/lang/Object;
                check-cast v0, Lhgf;
                const-class v1, Ln56;
                invoke-static {v1}, Ltre;->a(Ljava/lang/Class;)Lu23;
                move-result-object v1
                invoke-virtual {v0, v1, v2, v2}, Lhgf;->e(Lu23;Lxcc;Leje;)Ljava/lang/Object;
                move-result-object v10
                sget v0, $BUILD_VERSION->SDK_INT:I
                const/16 v1, 0x1f
                if-lt v0, v1, :no_night_mode
                const-class v0, Landroid/app/UiModeManager;
                invoke-virtual {p0, v0}, Landroid/content/Context;->getSystemService(Ljava/lang/Class;)Ljava/lang/Object;
                move-result-object v0
                check-cast v0, Landroid/app/UiModeManager;
                if-eqz v0, :no_night_mode
                new-instance v7, Law8;
                const/16 v8, 0x9
                invoke-direct {v7, v8, v2, v10, v0}, Law8;-><init>(BLbp3;Ljava/lang/Object;Ljava/lang/Object;)V
                invoke-static {v6, v2, v2, v7, v9}, Ljlb;->g0(Lrr3;Lir3;Lvr3;Lxi7;I)Lfbg;
                :no_night_mode
                new-instance v0, Ltf0;
                const/4 v8, 0x0
                invoke-direct {v0, p0, v10, v8}, Ltf0;-><init>(Ljava/lang/Object;Ljava/lang/Object;B)V
                invoke-static {}, Ljava/lang/Thread;->getDefaultUncaughtExceptionHandler()$EXCEPTION_HANDLER
                move-result-object v1
                if-eqz v1, :keep_handler
                new-instance v7, Lrs4;
                const/4 v8, 0x1
                invoke-direct {v7, v1, v8}, Lrs4;-><init>(${EXCEPTION_HANDLER}B)V
                invoke-static {v7}, Ljava/lang/Thread;->setDefaultUncaughtExceptionHandler($EXCEPTION_HANDLER)V
                :keep_handler
                new-instance v1, Ls1;
                const/16 v7, 0x8
                invoke-direct {v1, v0, v7}, Ls1;-><init>(Ljava/lang/Object;B)V
                new-instance v0, Lyyf;
                invoke-direct {v0, v1}, Lyyf;-><init>(Ls1;)V
                invoke-virtual {v0}, Ljava/lang/Thread;->start()V
                :done
                return-void
            """,
        )

        mutableClassDefBy(MAIN_ACTIVITY_CLASS).replaceVirtualizedMethod(
            "onCreate",
            MAIN_ACTIVITY_REGISTER_COUNT,
            """
                const/4 v0, 0x0
                if-eqz p1, :no_state
                const-string v1, "state_course_practice_session"
                invoke-virtual {p1, v1}, Landroid/os/BaseBundle;->getString(Ljava/lang/String;)Ljava/lang/String;
                move-result-object v1
                goto :have_session
                :no_state
                move-object v1, v0
                :have_session
                iput-object v1, p0, $MAIN_ACTIVITY_CLASS->S:Ljava/lang/String;
                iget-object v2, p0, $MAIN_ACTIVITY_CLASS->Q:Lfe9;
                invoke-interface {v2}, Lfe9;->getValue()Ljava/lang/Object;
                move-result-object v2
                check-cast v2, Lri0;
                if-eqz v1, :session_restored
                iget-object v3, v2, Lri0;->d:Lm73;
                invoke-virtual {v2}, Lri0;->c()Ljava/lang/String;
                move-result-object v4
                iget-object v5, v3, Lm73;->b:Ljava/lang/Object;
                if-nez v5, :session_restored
                invoke-static {v4, v1}, Ljava/util/Objects;->equals(Ljava/lang/Object;Ljava/lang/Object;)Z
                move-result v4
                if-nez v4, :session_restored
                iput-object v1, v3, Lm73;->f:Ljava/lang/Object;
                :session_restored
                const/16 v2, 0xd
                if-eqz p1, :from_intent
                const-string v1, "state_force_relogin_host"
                invoke-virtual {p1, v1}, Landroid/os/BaseBundle;->getBoolean(Ljava/lang/String;)Z
                move-result v1
                goto :have_relogin
                :from_intent
                invoke-virtual {p0}, Landroid/app/Activity;->getIntent()Landroid/content/Intent;
                move-result-object v1
                const-string v3, "extra_app_login_source"
                const/high16 v4, -0x80000000
                invoke-virtual {v1, v3, v4}, Landroid/content/Intent;->getIntExtra(Ljava/lang/String;I)I
                move-result v1
                if-ne v1, v2, :not_relogin
                const/4 v1, 0x1
                goto :have_relogin
                :not_relogin
                const/4 v1, 0x0
                :have_relogin
                iput-boolean v1, p0, $MAIN_ACTIVITY_CLASS->M:Z
                if-eqz p1, :uid_from_intent
                const-string v1, "state_force_relogin_expected_uid"
                invoke-virtual {p1, v1}, Landroid/os/BaseBundle;->getString(Ljava/lang/String;)Ljava/lang/String;
                move-result-object v1
                if-nez v1, :have_uid
                :uid_from_intent
                invoke-virtual {p0}, Landroid/app/Activity;->getIntent()Landroid/content/Intent;
                move-result-object v1
                const-string v2, "extra_force_relogin_expected_uid"
                invoke-virtual {v1, v2}, Landroid/content/Intent;->getStringExtra(Ljava/lang/String;)Ljava/lang/String;
                move-result-object v1
                :have_uid
                iput-object v1, p0, $MAIN_ACTIVITY_CLASS->N:Ljava/lang/String;
                invoke-super {p0, p1}, Lo31;->onCreate(Landroid/os/Bundle;)V
                return-void
            """,
        )

        val hiddenActivity = mutableClassDefBy(HIDDEN_ACTIVITY_CLASS)
        hiddenActivity.replaceVirtualizedMethod(
            "onCreate",
            HIDDEN_ACTIVITY_REGISTER_COUNT,
            hiddenActivityOnCreate(hiddenActivity),
        )

        finishingActivityTypes.forEach { type ->
            val activity = mutableClassDefBy(type)
            activity.replaceVirtualizedMethod(
                "onCreate",
                FINISHING_ACTIVITY_REGISTER_COUNT,
                """
                    invoke-super {p0, p1}, ${activity.superclass}->onCreate(Landroid/os/Bundle;)V
                    invoke-virtual {p0}, Landroid/app/Activity;->finish()V
                    return-void
                """,
            )
        }
    }
}
