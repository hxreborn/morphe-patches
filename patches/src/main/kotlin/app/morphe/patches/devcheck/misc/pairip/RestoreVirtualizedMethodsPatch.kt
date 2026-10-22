/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.devcheck.misc.pairip

import app.morphe.patcher.Match
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.pairip.replaceVirtualizedBody
import app.morphe.patches.shared.misc.smali.addDefaultConstructor
import app.morphe.patches.shared.misc.smali.assembleDex
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.util.inputStreamFromBundledResource
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import java.io.ByteArrayInputStream
import java.util.function.Supplier

private const val RESTORED_CLASS = "LDevCheckRestored;"

private val RESTORED_SOURCES = listOf(
    "DevCheckRestored.smali",
    "DevCheckRestored\$BluetoothMessage.smali",
    "DevCheckRestored\$HardwareReload.smali",
)

private fun stub(name: String) = "Lstub/$name;"

private fun BytecodePatchContext.resolveStubs(): Pair<Map<String, String>, Map<MutableMethod, String>> {
    val stubs = mutableMapOf<String, String>()

    fun Match.reference(index: Int) =
        instructionMatches[index].getInstruction<ReferenceInstruction>().reference

    fun Match.field(index: Int) = reference(index) as FieldReference

    fun Match.type(index: Int) = (reference(index) as TypeReference).type

    fun member(owner: String, name: String, real: String) {
        stubs["${stub(owner)}->$name"] = real
    }

    fun field(owner: String, name: String, type: String, real: FieldReference) {
        check(real.type == type || type == "Ljava/lang/Object;") { "${descriptor(real)} is not $type" }
        member(owner, "$name:$type", descriptor(real))
    }

    fun field(owner: String, name: String, type: String, classDef: ClassDef) =
        field(owner, name, type, classDef.fields.single { it.type == type })

    val postExecute = SystemInfoTaskPostExecuteFingerprint.matchSingle()
    stubs[stub("CpuTimeSensorTask")] = postExecute.classDef.type
    field("CpuTimeSensorTask", "owner", "Ljava/lang/Object;", postExecute.field(0))

    stubs[stub("CpuTimeActivity")] = CPU_TIME_ACTIVITY
    field("CpuTimeActivity", "states", "Ljava/util/ArrayList;", classDefBy(CPU_TIME_ACTIVITY))
    field("CpuTimeActivity", "timeInStatePath", "Ljava/lang/String;", TimeInStatePathFingerprint.matchSingle().field(1))

    stubs[stub("SensorActivity")] = SENSOR_ACTIVITY
    field("SensorActivity", "sensor", "Landroid/hardware/Sensor;", classDefBy(SENSOR_ACTIVITY))
    field("SensorActivity", "steps", "I", SensorStepsFingerprint.matchSingle().field(1))
    member(
        "SensorActivity",
        "formatRange(Landroid/hardware/Sensor;Z)Ljava/lang/String;",
        descriptor(SensorRangeFingerprint.matchSingle().method),
    )

    val preferenceBoolean = PreferenceBooleanFingerprint.matchSingle()
    member(
        "EncryptedPrefs",
        "get()Landroid/content/SharedPreferences;",
        descriptor(preferenceBoolean.reference(1) as MethodReference),
    )
    member(
        "EncryptedPrefs",
        "putBoolean(Ljava/lang/String;Z)V",
        descriptor(preferencePutBooleanFingerprint(preferenceBoolean.classDef.type).matchSingle().method),
    )

    val pathExists = PathExistsFingerprint.matchSingle()
    member("DeviceFiles", "exists(Ljava/lang/String;)Z", descriptor(pathExists.method))
    member(
        "CpuFrequency",
        "label(Ljava/lang/String;)Ljava/lang/String;",
        descriptor(FrequencyLabelFingerprint.matchSingle().method),
    )

    val timeInState = TimeInStateConstructorFingerprint.matchSingle()
    stubs[stub("TimeInState")] = timeInState.classDef.type
    field("TimeInState", "name", "Ljava/lang/String;", timeInState.field(0))
    field("TimeInState", "duration", "Ljava/lang/String;", timeInState.field(2))
    field("TimeInState", "percent", "Ljava/lang/String;", timeInState.field(3))
    field("TimeInState", "progress", "I", timeInState.field(4))

    val rowBinding = timeInStateRowBindingFingerprint(TimeInStateAdapterFingerprint.matchSingle().classDef.type)
        .matchSingle()
    val rowFields = listOf(0, 2, 4, 6).map(rowBinding::field)
    val listRow = mutableClassDefBy(rowFields.map { it.definingClass }.distinct().single())
    stubs[stub("TimeInStateRow")] = listRow.type
    field("TimeInStateRow", "type", "B", listRow)
    field("TimeInStateRow", "title", "Ljava/lang/String;", rowFields[0])
    field("TimeInStateRow", "duration", "Ljava/lang/String;", rowFields[1])
    field("TimeInStateRow", "percent", "Ljava/lang/String;", rowFields[2])
    field("TimeInStateRow", "progress", "I", rowFields[3])
    listRow.addDefaultConstructor()

    val infoRow = infoRowTypeFingerprint(postExecute.type(2)).matchSingle().type(0)
    check(
        classDefBy(infoRow).methods.any {
            it.name == "<init>" && it.parameterTypes == listOf("I", "Ljava/lang/String;", "Ljava/lang/String;")
        },
    ) { "$infoRow has no (int, String, String) constructor" }
    stubs[stub("InfoRow")] = infoRow
    val signalRow = signalRowConstructorFingerprint(infoRow).matchSingle()
    field("InfoRow", "address", "Ljava/lang/String;", signalRow.field(2))
    field("InfoRow", "type", "I", signalRow.field(5))

    val activityResult = HardwareActivityResultFingerprint.matchSingle()
    val hardwareFragment = activityResult.classDef.type
    val viewModel = activityResult.field(2)
    stubs[stub("HardwareFragment")] = hardwareFragment
    stubs[stub("HardwareViewModel")] = viewModel.type
    member("HardwareFragment", "viewModel:${stub("HardwareViewModel")}", descriptor(viewModel))
    member(
        "HardwareFragment",
        "requireContext()Landroid/content/Context;",
        descriptor(activityResult.reference(0) as MethodReference),
    )
    member("HardwareViewModel", "reload()V", descriptor(activityResult.reference(3) as MethodReference))

    val nearbyScan = nearbyScanFingerprint(hardwareFragment).matchSingle()
    val nearbyRow = (nearbyScan.reference(4) as MethodReference).definingClass
    check(nearbyRow == infoRow) { "Nearby scan row class is $nearbyRow, expected $infoRow" }
    field("HardwareFragment", "nearbyRows", "Ljava/util/ArrayList;", nearbyScan.field(0))
    member("HardwareFragment", "getString(I)Ljava/lang/String;", descriptor(nearbyScan.reference(3) as MethodReference))
    member(
        "HardwareFragment",
        "requestPermissions([Ljava/lang/String;I)V",
        descriptor(bluetoothPermissionRequestFingerprint(hardwareFragment).matchSingle().reference(1) as MethodReference),
    )

    val nearbyAdapter = nearbyAdapterFingerprint(hardwareFragment).matchSingle().field(1)
    val adapterBase = classDefBy(nearbyAdapter.type).superclass!!
    stubs[stub("RowAdapter")] = nearbyAdapter.type
    member("HardwareFragment", "nearbyAdapter:${stub("RowAdapter")}", descriptor(nearbyAdapter))
    member("RowAdapter", "notifyDataSetChanged()V", descriptor(notifyDataSetChangedFingerprint(adapterBase).matchSingle().method))

    val hardwareReceiver = hardwareReceiverConstructorFingerprint(hardwareFragment).matchSingle()
    stubs[stub("HardwareReceiver")] = hardwareReceiver.classDef.type
    field("HardwareReceiver", "mode", "B", hardwareReceiver.field(0))
    member("HardwareReceiver", "fragment:${stub("HardwareFragment")}", descriptor(hardwareReceiver.field(1)))
    member(
        "HardwareFragment",
        "isAdded()Z",
        descriptor(
            hardwareReloadFingerprint(hardwareReceiver.classDef.type, hardwareFragment).matchSingle().reference(2)
                as MethodReference,
        ),
    )

    val batteryTask = BatteryTaskPostExecuteFingerprint.matchSingle()
    stubs[stub("BatteryActivityTask")] = batteryTask.classDef.type
    stubs[stub("BatteryActivity")] = BATTERY_ACTIVITY
    stubs[stub("BatteryMonitorService")] = BATTERY_MONITOR_SERVICE
    field("BatteryActivityTask", "mode", "B", batteryTask.field(0))
    member("BatteryActivityTask", "activity:${stub("BatteryActivity")}", descriptor(batteryTask.field(1)))
    field("BatteryActivity", "entries", "Ljava/util/ArrayList;", classDefBy(BATTERY_ACTIVITY))

    val bluetoothTask = BluetoothTaskPostExecuteFingerprint.matchSingle()
    stubs[stub("BluetoothTask")] = bluetoothTask.classDef.type
    stubs[stub("BluetoothActivity")] = BLUETOOTH_ACTIVITY
    member("BluetoothTask", "activity:${stub("BluetoothActivity")}", descriptor(bluetoothTask.field(0)))
    field("BluetoothTask", "delay", "I", bluetoothTask.classDef)
    field("BluetoothActivity", "emptyView", "Landroid/view/View;", bluetoothTask.field(1))
    field("BluetoothActivity", "emptyText", "Landroid/widget/TextView;", bluetoothTask.field(2))
    field("BluetoothActivity", "adapter", "Landroid/bluetooth/BluetoothAdapter;", classDefBy(BLUETOOTH_ACTIVITY))

    val publicIpTask = PublicIpTaskPreExecuteFingerprint.matchSingle()
    stubs[stub("PublicIpTask")] = publicIpTask.classDef.type
    field("PublicIpTask", "resultText", "Landroid/widget/TextView;", publicIpTask.field(1))

    val networkTask = NetworkTaskPostExecuteFingerprint.matchSingle()
    stubs[stub("NetworkDialogTask")] = networkTask.classDef.type
    field("NetworkDialogTask", "mobileConnected", "Z", networkTask.classDef)
    member("NetworkDialogTask", "list:Landroid/view/View;", descriptor(networkTask.field(1)))
    val networkTypeReader = NetworkTypeReaderFingerprint.matchSingle()
    stubs[stub("NetworkTypeReader")] = networkTypeReader.classDef.type
    member("NetworkTypeReader", "read()Ljava/lang/String;", descriptor(networkTypeReader.method))
    val networkFragment = classDefBy(networkTask.field(0).type)
    member(
        "DashboardFragment",
        "networkTypeReader:${stub("NetworkTypeReader")}",
        descriptor(networkFragment.fields.single { AccessFlags.STATIC.isSet(it.accessFlags) && it.type == networkTypeReader.classDef.type }),
    )

    val deviceInfoClass = pathExists.classDef.type
    member("DeviceNetwork", "ipAddress()Ljava/lang/String;", descriptor(ipAddressFingerprint(deviceInfoClass).matchSingle().method))
    member(
        "Telephony",
        "operator(Landroid/content/Context;)Ljava/lang/String;",
        descriptor(OperatorFingerprint.matchSingle().method),
    )
    member(
        "Telephony",
        "phoneType(Landroid/content/Context;Landroid/telephony/TelephonyManager;)Ljava/lang/String;",
        descriptor(PhoneTypeFingerprint.matchSingle().method),
    )
    member(
        "Telephony",
        "simState(Landroid/content/Context;I)Ljava/lang/String;",
        descriptor(simStateFingerprint(deviceInfoClass).matchSingle().method),
    )

    stubs[stub("BootReceiver")] = BOOT_RECEIVER
    stubs[stub("MonitorReceiver")] = MONITOR_RECEIVER
    stubs[stub("UpdateReceiver")] = UPDATE_RECEIVER
    stubs[stub("PauseResumeReceiver")] = PAUSE_RESUME_RECEIVER
    stubs[stub("MonitorActivity")] = MONITOR_ACTIVITY
    member(
        "BootReceiver",
        "startBatteryMonitor(Landroid/content/Context;)V",
        descriptor(BootBatteryMonitorFingerprint.matchSingle().method),
    )
    member(
        "BootReceiver",
        "startMonitorWindows(Landroid/content/Context;)V",
        descriptor(BootMonitorWindowsFingerprint.matchSingle().method),
    )

    val tempStart = TempMonitorStartFingerprint.matchSingle()
    val tempReceiver = classDefBy(tempStart.field(2).type)
    stubs[stub("TempMonitorWindow")] = TEMP_MONITOR_WINDOW
    stubs[stub("TempMonitorReceiver")] = tempReceiver.type
    field("TempMonitorReceiver", "mode", "B", tempReceiver)
    member(
        "TempMonitorReceiver",
        "window:${stub("TempMonitorWindow")}",
        descriptor(tempReceiver.fields.single { it.type == TEMP_MONITOR_WINDOW }),
    )
    field("TempMonitorWindow", "usesBatteryTemperature", "Z", tempStart.field(0))
    member("TempMonitorWindow", "batteryReceiver:Landroid/content/BroadcastReceiver;", descriptor(tempStart.field(2)))
    field("TempMonitorWindow", "handler", "Landroid/os/Handler;", tempStart.field(4))
    member("TempMonitorWindow", "poller:Ljava/lang/Runnable;", descriptor(tempStart.field(5)))
    field("TempMonitorWindow", "view", "Landroid/view/View;", classDefBy(TEMP_MONITOR_WINDOW))
    val showTemperature = TempMonitorShowTemperatureFingerprint.matchSingle()
    member("TempMonitorWindow", "showTemperature(I)V", descriptor(showTemperature.method))
    val appPrefs = showTemperature.field(1)
    stubs[stub("ProviderPrefs")] = appPrefs.type
    member("ProviderPrefs", "instance:${stub("ProviderPrefs")}", descriptor(appPrefs))
    member(
        "ProviderPrefs",
        "getBoolean(Ljava/lang/String;)Z",
        descriptor(showTemperature.reference(2) as MethodReference),
    )
    member("MonitorPause", "offsetView(Landroid/view/View;)V", descriptor(MonitorToggleFingerprint.matchSingle().method))

    val batteryStart = BatteryMonitorStartFingerprint.matchSingle()
    val batteryReceiver = classDefBy(batteryStart.field(1).type)
    val batteryViews = BatteryMonitorViewsFingerprint.matchSingle()
    stubs[stub("BatteryMonitorWindow")] = BATTERY_MONITOR_WINDOW
    stubs[stub("BatteryMonitorReceiver")] = batteryReceiver.type
    field("BatteryMonitorReceiver", "mode", "B", batteryReceiver)
    member(
        "BatteryMonitorReceiver",
        "window:${stub("BatteryMonitorWindow")}",
        descriptor(batteryReceiver.fields.single { it.type == BATTERY_MONITOR_WINDOW }),
    )
    member("BatteryMonitorWindow", "batteryReceiver:Landroid/content/BroadcastReceiver;", descriptor(batteryStart.field(1)))
    field("BatteryMonitorWindow", "levelText", "Landroid/widget/TextView;", batteryViews.field(1))
    field("BatteryMonitorWindow", "temperatureText", "Landroid/widget/TextView;", batteryViews.field(3))
    field("BatteryMonitorWindow", "view", "Landroid/view/View;", classDefBy(BATTERY_MONITOR_WINDOW))

    val shizukuReceiver = ShizukuBinderReceiverFingerprint.matchSingle().type(0)
    stubs[stub("ShizukuReceiver")] = shizukuReceiver
    stubs[stub("BinderContainer")] = BINDER_CONTAINER
    field("BinderContainer", "binder", "Landroid/os/IBinder;", classDefBy(BINDER_CONTAINER))
    val binderReceived = ShizukuBinderReceivedFingerprint.matchAll()
        .map { descriptor(it.reference(0) as MethodReference) }
        .distinct().single()
    member("Shizuku", "onBinderReceived(Landroid/os/IBinder;Ljava/lang/String;)V", binderReceived)

    val bind = RootServiceBindFingerprint.matchSingle()
    val rootServiceManager = bind.classDef
    stubs[stub("RootServiceManager")] = rootServiceManager.type
    member(
        "RootServiceManager",
        "bind(Landroid/content/Intent;Ljava/util/concurrent/Executor;Landroid/content/ServiceConnection;)Ljava/lang/Object;",
        descriptor(bind.method),
    )
    member("RootServiceManager", "daemon:${stub("RemoteProcess")}", descriptor(bind.field(1)))
    member("RootServiceManager", "remote:${stub("RemoteProcess")}", descriptor(bind.field(2)))
    field("RootServiceManager", "flags", "I", rootServiceManager.fields.single { it.type == "I" && !AccessFlags.STATIC.isSet(it.accessFlags) })
    field("RootServiceManager", "pendingTasks", "Ljava/util/ArrayList;", rootServiceManager)

    val connect = RootManagerConnectFingerprint.matchSingle()
    val rootManagerProxy = connect.classDef
    val rootManager = rootManagerProxy.interfaces.single()
    rootManagerProxy.addDefaultConstructor()
    stubs[stub("IRootServiceManagerProxy")] = rootManagerProxy.type
    stubs[stub("IRootServiceManager")] = rootManager
    member("IRootServiceManager", "connect(Landroid/os/IBinder;)V", "$rootManager->${connect.method.name}(Landroid/os/IBinder;)V")
    field("IRootServiceManagerProxy", "remote", "Landroid/os/IBinder;", rootManagerProxy)

    val remoteProcess = constructorFingerprint(rootServiceManager.type, rootManager).matchSingle()
    stubs[stub("RemoteProcess")] = remoteProcess.classDef.type
    member(
        "RemoteProcess",
        "<init>(${stub("RootServiceManager")}${stub("IRootServiceManager")})V",
        descriptor(remoteProcess.method),
    )

    val pendingBind = constructorFingerprint(
        rootServiceManager.type,
        "Landroid/content/Intent;",
        "Ljava/util/concurrent/Executor;",
        "Landroid/content/ServiceConnection;",
    ).matchSingle().classDef
    stubs[stub("BindTask")] = pendingBind.type
    listOf(
        "intent" to "Landroid/content/Intent;",
        "executor" to "Ljava/util/concurrent/Executor;",
        "connection" to "Landroid/content/ServiceConnection;",
    ).forEach { (name, type) -> field("BindTask", name, type, pendingBind) }

    val rootServiceReceiver = rootServiceReceiverConstructorFingerprint(rootServiceManager.type).matchSingle()
    stubs[stub("RootServiceReceiver")] = rootServiceReceiver.classDef.type
    field("RootServiceReceiver", "mode", "B", rootServiceReceiver.field(0))
    field("RootServiceReceiver", "owner", "Ljava/lang/Object;", rootServiceReceiver.field(1))
    field("RootServiceReceiver", "messenger", "Ljava/lang/Object;", rootServiceReceiver.field(3))

    fun receiver(type: String) = mutableClassDefBy(type).methods.single { it.name == "onReceive" }

    val virtualizedMethods = mapOf(
        postExecute to "cpuTimeSensorDoInBackground",
        batteryTask to "batteryActivityDoInBackground",
        bluetoothTask to "bluetoothDoInBackground",
        publicIpTask to "publicIpDoInBackground",
        networkTask to "networkDialogDoInBackground",
    ).mapKeys { (match, _) -> match.classDef.methods.single { it.name == "doInBackground" } } + mapOf(
        receiver(BOOT_RECEIVER) to "bootReceive",
        receiver(MONITOR_RECEIVER) to "monitorReceive",
        receiver(UPDATE_RECEIVER) to "updateReceive",
        receiver(PAUSE_RESUME_RECEIVER) to "pauseResumeReceive",
        receiver(tempReceiver.type) to "tempMonitorReceive",
        receiver(batteryReceiver.type) to "batteryMonitorReceive",
        receiver(shizukuReceiver) to "shizukuBinderReceive",
        receiver(rootServiceReceiver.classDef.type) to "rootServiceReceive",
        receiver(hardwareReceiver.classDef.type) to "hardwareReceive",
    )
    return stubs to virtualizedMethods
}

private val stringResourceRead = Regex("""sget ([vp]\d+), Lstub/StringRes;->(\w+):I""")

private lateinit var restoredSmali: List<String>

private lateinit var virtualizedMethods: Map<MutableMethod, String>

private val resolveRestoredMethodsPatch = bytecodePatch {
    execute {
        val (stubs, methods) = resolveStubs()
        val replacements = stubs.entries.sortedByDescending { it.key.length }
        restoredSmali = RESTORED_SOURCES.map { name ->
            val template = inputStreamFromBundledResource("devcheck/restored", name)!!.use {
                it.readBytes().decodeToString()
            }
            val smali = replacements
                .fold(template) { smali, (stub, real) -> smali.replace(stub, real) }
                .replace(stringResourceRead) { read ->
                    "const ${read.groupValues[1]}, ${resourceId(ResourceType.STRING, read.groupValues[2])}"
                }
            check("Lstub/" !in smali) { "Unresolved stub reference in $name" }
            smali
        }
        virtualizedMethods = methods
    }
}

internal val restoreVirtualizedMethodsPatch = bytecodePatch {
    dependsOn(removePairipVirtualizationPatch, resolveRestoredMethodsPatch)

    extendWith(Supplier { ByteArrayInputStream(assembleDex(restoredSmali)) })

    execute {
        virtualizedMethods.forEach { (method, restoredName) ->
            val parameters = method.definingClass + method.parameterTypes.joinToString("")
            val call = "invoke-static/range {p0 .. p${method.parameterTypes.size}}, " +
                "$RESTORED_CLASS->$restoredName($parameters)${method.returnType}"
            val returnSmali = if (method.returnType == "V") "return-void" else "move-result-object v0\nreturn-object v0"
            method.replaceVirtualizedBody("$call\n$returnSmali")
        }
    }
}
