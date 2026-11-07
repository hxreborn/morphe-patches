/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.devcheck.misc.pairip

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.checkCast
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal const val CPU_TIME_ACTIVITY = "Lflar2/devcheck/cputimes/CPUTimeActivity;"
internal const val SENSOR_ACTIVITY = "Lflar2/devcheck/sensors/SensorActivity;"

private const val STRING = "Ljava/lang/String;"

internal object SystemInfoTaskPostExecuteFingerprint : Fingerprint(
    name = "onPostExecute",
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = "this"),
        checkCast(SENSOR_ACTIVITY),
        opcode(Opcode.NEW_INSTANCE, MatchAfterImmediately()),
        checkCast(CPU_TIME_ACTIVITY),
    ),
)

internal object PreferenceBooleanFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Ljava/lang/Boolean;",
    parameters = listOf(STRING),
    filters = listOf(
        string("prefHardwareDB"),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = emptyList(),
            returnType = "Landroid/content/SharedPreferences;",
        ),
    ),
)

internal fun preferencePutBooleanFingerprint(preferencesClass: String) = Fingerprint(
    definingClass = preferencesClass,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf(STRING, "Z"),
    filters = listOf(
        methodCall(definingClass = "Ljava/lang/Boolean;", name = "toString"),
        methodCall(definingClass = "Landroid/content/SharedPreferences\$Editor;", name = "apply"),
    ),
)

internal object PathExistsFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = listOf(STRING),
    strings = listOf("prefRoot", "[ -e ", " ] && echo 1"),
)

internal object FrequencyLabelFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = STRING,
    parameters = listOf(STRING),
    strings = listOf("Unavailable", "EE", " MHz"),
)

internal object TimeInStatePathFingerprint : Fingerprint(
    definingClass = CPU_TIME_ACTIVITY,
    name = "onCreate",
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = "this", type = STRING),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = "this",
            type = STRING,
            location = MatchAfterImmediately(),
        ),
    ),
)

internal object TimeInStateConstructorFingerprint : Fingerprint(
    name = "<init>",
    parameters = listOf(STRING, "J", STRING, STRING, "I"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IPUT_OBJECT),
        fieldAccess(opcode = Opcode.IPUT_WIDE),
        fieldAccess(opcode = Opcode.IPUT_OBJECT),
        fieldAccess(opcode = Opcode.IPUT_OBJECT),
        fieldAccess(opcode = Opcode.IPUT),
    ),
    custom = { _, classDef -> "Ljava/lang/Comparable;" in classDef.interfaces },
)

internal object TimeInStateAdapterFingerprint : Fingerprint(
    name = "<init>",
    parameters = listOf(CPU_TIME_ACTIVITY, "Ljava/util/ArrayList;"),
)

internal fun timeInStateRowBindingFingerprint(adapterClass: String) = Fingerprint(
    definingClass = adapterClass,
    name = "getView",
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = STRING),
        methodCall(name = "setText", location = MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = STRING, location = MatchAfterWithin(1)),
        methodCall(name = "setText", location = MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = STRING, location = MatchAfterWithin(1)),
        methodCall(name = "setText", location = MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IGET, type = "I", location = MatchAfterWithin(1)),
        methodCall(name = "setProgress", location = MatchAfterImmediately()),
    ),
)

internal object SensorRangeFingerprint : Fingerprint(
    definingClass = SENSOR_ACTIVITY,
    returnType = STRING,
    parameters = listOf("Landroid/hardware/Sensor;", "Z"),
)

internal object SensorStepsFingerprint : Fingerprint(
    definingClass = SENSOR_ACTIVITY,
    name = "onSensorChanged",
    filters = listOf(
        opcode(Opcode.FLOAT_TO_INT),
        fieldAccess(opcode = Opcode.IPUT, definingClass = "this", type = "I", location = MatchAfterImmediately()),
    ),
)

internal fun infoRowTypeFingerprint(adapterClass: String) = Fingerprint(
    definingClass = adapterClass,
    returnType = "I",
    parameters = listOf("I"),
    filters = listOf(
        checkCast("L"),
        fieldAccess(opcode = Opcode.IGET, type = "I", location = MatchAfterImmediately()),
    ),
)

internal const val BATTERY_ACTIVITY = "Lflar2/devcheck/BatteryMonitor/BatteryActivity;"
internal const val BATTERY_MONITOR_SERVICE = "Lflar2/devcheck/BatteryMonitor/BatteryMonitorService;"
internal const val BLUETOOTH_ACTIVITY = "Lflar2/devcheck/tools/BluetoothActivity;"

internal object BatteryTaskPostExecuteFingerprint : Fingerprint(
    name = "onPostExecute",
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_BYTE, definingClass = "this"),
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = "this", type = BATTERY_ACTIVITY),
    ),
)

internal object BluetoothTaskPostExecuteFingerprint : Fingerprint(
    name = "onPostExecute",
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = "this", type = BLUETOOTH_ACTIVITY),
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = BLUETOOTH_ACTIVITY, type = "Landroid/view/View;"),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = BLUETOOTH_ACTIVITY,
            type = "Landroid/widget/TextView;",
        ),
    ),
)

internal object PublicIpTaskPreExecuteFingerprint : Fingerprint(
    name = "onPreExecute",
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = "this",
            type = "Lflar2/devcheck/utils/SuccessLoadingView;",
        ),
        fieldAccess(opcode = Opcode.IPUT_OBJECT, definingClass = "this", type = "Landroid/widget/TextView;"),
    ),
)

internal object NetworkTaskPostExecuteFingerprint : Fingerprint(
    name = "onPostExecute",
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = "this"),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "this",
            type = "Landroidx/recyclerview/widget/RecyclerView;",
        ),
        methodCall(definingClass = "Landroid/app/Dialog;", name = "isShowing"),
    ),
    custom = { _, classDef -> classDef.fields.any { it.type == "Z" } },
)

internal object NetworkTypeReaderFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = STRING,
    parameters = emptyList(),
    strings = listOf("android.permission.READ_PHONE_STATE"),
    custom = { _, classDef -> classDef.fields.any { it.type == "Landroid/telephony/TelephonyManager;" } },
)

internal fun ipAddressFingerprint(deviceInfoClass: String) = Fingerprint(
    definingClass = deviceInfoClass,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = STRING,
    parameters = emptyList(),
    filters = listOf(
        methodCall(definingClass = "Ljava/net/NetworkInterface;", name = "getInetAddresses"),
        methodCall(definingClass = "Ljava/net/InetAddress;", name = "isLoopbackAddress"),
        methodCall(definingClass = "Ljava/net/InetAddress;", name = "getHostAddress"),
    ),
)

internal object OperatorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = STRING,
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf("telephony_subscription_service"),
    filters = listOf(methodCall(definingClass = "Landroid/telephony/TelephonyManager;", name = "getNetworkOperatorName")),
)

internal object PhoneTypeFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = STRING,
    parameters = listOf("Landroid/content/Context;", "Landroid/telephony/TelephonyManager;"),
    filters = listOf(methodCall(definingClass = "Landroid/telephony/TelephonyManager;", name = "getPhoneType")),
)

internal fun simStateFingerprint(deviceInfoClass: String) = Fingerprint(
    definingClass = deviceInfoClass,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = STRING,
    parameters = listOf("Landroid/content/Context;", "I"),
    filters = listOf(resourceLiteral(ResourceType.STRING, "no_sim")),
)

internal const val BOOT_RECEIVER = "Lflar2/devcheck/utils/BootReceiver;"
internal const val UPDATE_RECEIVER = "Lflar2/devcheck/utils/UpdateReceiver;"
internal const val MONITOR_RECEIVER = "Lflar2/devcheck/utils/MonitorReceiver;"
internal const val PAUSE_RESUME_RECEIVER = "Lflar2/devcheck/monitors/PauseResumeReceiver;"
internal const val MONITOR_ACTIVITY = "Lflar2/devcheck/MonitorActivity;"

internal object BootBatteryMonitorFingerprint : Fingerprint(
    definingClass = BOOT_RECEIVER,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
)

internal object BootMonitorWindowsFingerprint : Fingerprint(
    definingClass = BOOT_RECEIVER,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
)

internal const val TEMP_MONITOR_WINDOW = "Lflar2/devcheck/monitors/TempMonitorWindow;"
internal const val BATTERY_MONITOR_WINDOW = "Lflar2/devcheck/monitors/BatteryMonitorWindow;"
private const val BATTERY_CHANGED = "android.intent.action.BATTERY_CHANGED"

internal object TempMonitorStartFingerprint : Fingerprint(
    definingClass = TEMP_MONITOR_WINDOW,
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_BOOLEAN, definingClass = "this"),
        string(BATTERY_CHANGED),
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = "this", location = MatchAfterWithin(1)),
        methodCall(name = "registerReceiver", location = MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = "this", type = "Landroid/os/Handler;"),
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = "this", location = MatchAfterImmediately()),
        methodCall(definingClass = "Landroid/os/Handler;", name = "post", location = MatchAfterImmediately()),
    ),
)

internal object TempMonitorShowTemperatureFingerprint : Fingerprint(
    definingClass = TEMP_MONITOR_WINDOW,
    returnType = "V",
    parameters = listOf("I"),
    filters = listOf(
        string("prefFahrenheit"),
        opcode(Opcode.SGET_OBJECT, MatchAfterImmediately()),
        methodCall(returnType = "Z", parameters = listOf(STRING), location = MatchAfterImmediately()),
    ),
)

internal object MonitorToggleFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/view/View;"),
    strings = listOf("prefMonitorPaused"),
    custom = { method, _ -> AccessFlags.STATIC.isSet(method.accessFlags) },
)

internal object BatteryMonitorStartFingerprint : Fingerprint(
    definingClass = BATTERY_MONITOR_WINDOW,
    filters = listOf(
        string(BATTERY_CHANGED),
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = "this", location = MatchAfterWithin(1)),
        methodCall(name = "registerReceiver", location = MatchAfterImmediately()),
    ),
)

internal object BatteryMonitorViewsFingerprint : Fingerprint(
    definingClass = BATTERY_MONITOR_WINDOW,
    filters = listOf(
        resourceLiteral(ResourceType.ID, "batt_level"),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = "this",
            type = "Landroid/widget/TextView;",
            location = MatchAfterWithin(6),
        ),
        resourceLiteral(ResourceType.ID, "batt_temp"),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = "this",
            type = "Landroid/widget/TextView;",
            location = MatchAfterWithin(6),
        ),
    ),
)

internal const val SHIZUKU_PROVIDER = "Lrikka/shizuku/ShizukuProvider;"
internal const val BINDER_CONTAINER = "Lmoe/shizuku/api/BinderContainer;"

internal object ShizukuBinderReceiverFingerprint : Fingerprint(
    definingClass = SHIZUKU_PROVIDER,
    filters = listOf(
        opcode(Opcode.NEW_INSTANCE),
        methodCall(
            definingClass = "Landroid/content/Context;",
            name = "registerReceiver",
            parameters = listOf("Landroid/content/BroadcastReceiver;", "Landroid/content/IntentFilter;"),
            returnType = "Landroid/content/Intent;",
        ),
    ),
)

internal object ShizukuBinderReceivedFingerprint : Fingerprint(
    definingClass = SHIZUKU_PROVIDER,
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf("Landroid/os/IBinder;", STRING),
            returnType = "V",
        ),
    ),
)

internal fun dashboardBatteryRegistrationFingerprint(receiverType: String) = Fingerprint(
    returnType = "V",
    strings = listOf(BATTERY_CHANGED),
    filters = listOf(
        methodCall(definingClass = "Landroid/content/Context;", name = "registerReceiver", returnType = "Landroid/content/Intent;"),
        fieldAccess(opcode = Opcode.SPUT_OBJECT, type = "Landroid/content/Intent;", location = MatchAfterWithin(2)),
    ),
    custom = { _, classDef -> classDef.fields.any { it.type == receiverType } },
)

private const val ROOT_SERVICE_MANAGER_DESCRIPTOR = "com.topjohnwu.superuser.internal.IRootServiceManager"

internal object RootServiceBindFingerprint : Fingerprint(
    parameters = listOf("Landroid/content/Intent;", "Ljava/util/concurrent/Executor;", "Landroid/content/ServiceConnection;"),
    filters = listOf(
        methodCall(definingClass = "Ljava/lang/Boolean;", name = "booleanValue"),
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = "this", location = MatchAfterWithin(2)),
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = "this", location = MatchAfterWithin(2)),
    ),
    custom = { _, classDef -> "Landroid/os/Handler\$Callback;" in classDef.interfaces },
)

internal object RootManagerConnectFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/os/IBinder;"),
    strings = listOf(ROOT_SERVICE_MANAGER_DESCRIPTOR),
    custom = { _, classDef -> classDef.fields.any { it.type == "Landroid/os/IBinder;" } },
)

internal fun rootServiceReceiverConstructorFingerprint(managerClass: String) = Fingerprint(
    name = "<init>",
    parameters = listOf(managerClass),
    filters = listOf(
        fieldAccess(opcode = Opcode.IPUT_BYTE, definingClass = "this", type = "B"),
        fieldAccess(opcode = Opcode.IPUT_OBJECT, definingClass = "this"),
        methodCall(definingClass = "Landroid/os/Messenger;", name = "<init>"),
        fieldAccess(opcode = Opcode.IPUT_OBJECT, definingClass = "this", location = MatchAfterImmediately()),
    ),
)

internal fun constructorFingerprint(vararg parameters: String) = Fingerprint(
    name = "<init>",
    parameters = parameters.toList(),
)

private const val CONTEXT = "Landroid/content/Context;"
private const val RECYCLER_VIEW = "Landroidx/recyclerview/widget/RecyclerView;"

internal object HardwareActivityResultFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("I", "I", "Landroid/content/Intent;"),
    filters = listOf(
        methodCall(returnType = CONTEXT, parameters = emptyList()),
        methodCall(definingClass = "Landroid/content/ContentResolver;", name = "takePersistableUriPermission"),
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = "this", location = MatchAfterImmediately()),
        methodCall(returnType = "V", parameters = emptyList(), location = MatchAfterImmediately()),
    ),
)

internal fun nearbyScanFingerprint(fragmentClass: String) = Fingerprint(
    definingClass = fragmentClass,
    returnType = "V",
    parameters = listOf("I"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = "this", type = "Ljava/util/ArrayList;"),
        methodCall(definingClass = "Ljava/util/ArrayList;", name = "clear", location = MatchAfterImmediately()),
        resourceLiteral(ResourceType.STRING, "scanning"),
        methodCall(returnType = STRING, parameters = listOf("I"), location = MatchAfterImmediately()),
        methodCall(name = "<init>", parameters = listOf("I", STRING, STRING)),
        methodCall(definingClass = "Landroid/content/IntentFilter;", name = "<init>", parameters = listOf(STRING)),
    ),
)

internal fun bluetoothPermissionRequestFingerprint(fragmentClass: String) = Fingerprint(
    definingClass = fragmentClass,
    filters = listOf(
        string("android.permission.BLUETOOTH_CONNECT"),
        methodCall(returnType = "V", parameters = listOf("[$STRING", "I")),
    ),
)

internal fun nearbyAdapterFingerprint(fragmentClass: String) = Fingerprint(
    filters = listOf(
        methodCall(name = "<init>", parameters = listOf(CONTEXT, "Ljava/util/List;")),
        fieldAccess(opcode = Opcode.IPUT_OBJECT, definingClass = fragmentClass, location = MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = RECYCLER_VIEW, location = MatchAfterImmediately()),
        methodCall(definingClass = RECYCLER_VIEW, name = "setAdapter", location = MatchAfterImmediately()),
    ),
)

internal fun hardwareReloadFingerprint(receiverClass: String, fragmentClass: String) = Fingerprint(
    name = "run",
    filters = listOf(
        checkCast(receiverClass),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = receiverClass,
            type = fragmentClass,
            location = MatchAfterImmediately(),
        ),
        methodCall(returnType = "Z", parameters = emptyList(), location = MatchAfterImmediately()),
    ),
)

internal fun signalRowConstructorFingerprint(rowClass: String) = Fingerprint(
    definingClass = rowClass,
    name = "<init>",
    parameters = listOf(STRING, STRING, STRING, STRING, "I", "I"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IPUT_OBJECT),
        fieldAccess(opcode = Opcode.IPUT_OBJECT),
        fieldAccess(opcode = Opcode.IPUT_OBJECT),
        fieldAccess(opcode = Opcode.IPUT_OBJECT),
        fieldAccess(opcode = Opcode.IPUT),
        fieldAccess(opcode = Opcode.IPUT),
    ),
)

internal fun hardwareReceiverConstructorFingerprint(fragmentClass: String) = Fingerprint(
    name = "<init>",
    parameters = listOf(fragmentClass, "B"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IPUT_BYTE, definingClass = "this", type = "B"),
        fieldAccess(opcode = Opcode.IPUT_OBJECT, definingClass = "this", type = fragmentClass),
    ),
    custom = { _, classDef -> classDef.superclass == "Landroid/content/BroadcastReceiver;" },
)

internal fun notifyDataSetChangedFingerprint(adapterClass: String) = Fingerprint(
    definingClass = adapterClass,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = emptyList(),
)
