/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.devcheck.misc.pairip

import app.morphe.patcher.InstructionFilter
import app.morphe.patcher.Match
import app.morphe.patcher.patch.classDefBy
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.misc.smali.addDefaultConstructor
import app.morphe.patches.shared.misc.smali.descriptor
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private fun stub(name: String) = "Lstub/$name;"

internal fun resolveRestoredMethodBindings(): Pair<Map<String, String>, Map<MutableMethod, String>> {
    val stubs = mutableMapOf<String, String>()

    fun Match.fieldMatchedBy(filter: InstructionFilter) =
        instructionMatches.single { it.filter === filter }.fieldReference

    fun member(owner: String, name: String, real: String) {
        stubs["${stub(owner)}->$name"] = real
    }

    fun field(owner: String, name: String, type: String, real: FieldReference) {
        check(real.type == type || type == "Ljava/lang/Object;") { "${descriptor(real)} is not $type" }
        member(owner, "$name:$type", descriptor(real))
    }

    fun field(owner: String, name: String, type: String, classDef: ClassDef) =
        field(owner, name, type, classDef.fields.single { it.type == type })

    val postExecute = SystemInfoTaskPostExecuteFingerprint.match()
    val (taskOwnerRead, _, newRowAdapter, _) = postExecute.instructionMatches
    stubs[stub("CpuTimeSensorTask")] = postExecute.classDef.type
    field("CpuTimeSensorTask", "owner", "Ljava/lang/Object;", taskOwnerRead.fieldReference)

    stubs[stub("CpuTimeActivity")] = CPU_TIME_ACTIVITY
    field("CpuTimeActivity", "states", "Ljava/util/ArrayList;", classDefBy(CPU_TIME_ACTIVITY))
    val (_, timeInStatePathRead) = TimeInStatePathFingerprint.match().instructionMatches
    field("CpuTimeActivity", "timeInStatePath", "Ljava/lang/String;", timeInStatePathRead.fieldReference)

    stubs[stub("SensorActivity")] = SENSOR_ACTIVITY
    field("SensorActivity", "sensor", "Landroid/hardware/Sensor;", classDefBy(SENSOR_ACTIVITY))
    val (_, stepsStore) = SensorStepsFingerprint.match().instructionMatches
    field("SensorActivity", "steps", "I", stepsStore.fieldReference)
    member(
        "SensorActivity",
        "formatRange(Landroid/hardware/Sensor;Z)Ljava/lang/String;",
        descriptor(SensorRangeFingerprint.method),
    )

    val preferenceBoolean = PreferenceBooleanFingerprint.match()
    val (_, getPreferencesCall) = preferenceBoolean.instructionMatches
    member(
        "EncryptedPrefs",
        "get()Landroid/content/SharedPreferences;",
        descriptor(getPreferencesCall.methodReference),
    )
    member(
        "EncryptedPrefs",
        "putBoolean(Ljava/lang/String;Z)V",
        descriptor(preferencePutBooleanFingerprint(preferenceBoolean.classDef.type).method),
    )

    val pathExists = PathExistsFingerprint.match()
    member("DeviceFiles", "exists(Ljava/lang/String;)Z", descriptor(pathExists.method))
    member(
        "CpuFrequency",
        "label(Ljava/lang/String;)Ljava/lang/String;",
        descriptor(FrequencyLabelFingerprint.method),
    )

    val timeInState = TimeInStateConstructorFingerprint.match()
    val (nameStore, _, durationStore, percentStore, progressStore) = timeInState.instructionMatches
    stubs[stub("TimeInState")] = timeInState.classDef.type
    field("TimeInState", "name", "Ljava/lang/String;", nameStore.fieldReference)
    field("TimeInState", "duration", "Ljava/lang/String;", durationStore.fieldReference)
    field("TimeInState", "percent", "Ljava/lang/String;", percentStore.fieldReference)
    field("TimeInState", "progress", "I", progressStore.fieldReference)

    val rowBinding = timeInStateRowBindingFingerprint(TimeInStateAdapterFingerprint.match().classDef.type)
        .match()
    val rowFields = listOf(
        timeInStateRowTitleRead,
        timeInStateRowDurationRead,
        timeInStateRowPercentRead,
        timeInStateRowProgressRead,
    ).map(rowBinding::fieldMatchedBy)
    val (rowTitle, rowDuration, rowPercent, rowProgress) = rowFields
    val listRow = mutableClassDefBy(rowFields.map { it.definingClass }.distinct().single())
    stubs[stub("TimeInStateRow")] = listRow.type
    field("TimeInStateRow", "type", "B", listRow)
    field("TimeInStateRow", "title", "Ljava/lang/String;", rowTitle)
    field("TimeInStateRow", "duration", "Ljava/lang/String;", rowDuration)
    field("TimeInStateRow", "percent", "Ljava/lang/String;", rowPercent)
    field("TimeInStateRow", "progress", "I", rowProgress)
    listRow.addDefaultConstructor()

    val rowAdapterType = newRowAdapter.instruction.getReference<TypeReference>()!!.type
    val (rowTypeCast, _) = infoRowTypeFingerprint(rowAdapterType).match().instructionMatches
    val infoRow = rowTypeCast.instruction.getReference<TypeReference>()!!.type
    check(
        classDefBy(infoRow).methods.any {
            it.name == "<init>" && it.parameterTypes == listOf("I", "Ljava/lang/String;", "Ljava/lang/String;")
        },
    ) { "$infoRow has no (int, String, String) constructor" }
    stubs[stub("InfoRow")] = infoRow
    val signalRow = signalRowConstructorFingerprint(infoRow).match()
    field("InfoRow", "address", "Ljava/lang/String;", signalRow.fieldMatchedBy(signalRowAddressStore))
    field("InfoRow", "type", "I", signalRow.fieldMatchedBy(signalRowTypeStore))

    val activityResult = HardwareActivityResultFingerprint.match()
    val (requireContextCall, _, viewModelRead, reloadCall) = activityResult.instructionMatches
    val hardwareFragment = activityResult.classDef.type
    val viewModel = viewModelRead.fieldReference
    stubs[stub("HardwareFragment")] = hardwareFragment
    stubs[stub("HardwareViewModel")] = viewModel.type
    member("HardwareFragment", "viewModel:${stub("HardwareViewModel")}", descriptor(viewModel))
    member(
        "HardwareFragment",
        "requireContext()Landroid/content/Context;",
        descriptor(requireContextCall.methodReference),
    )
    member("HardwareViewModel", "reload()V", descriptor(reloadCall.methodReference))

    val nearbyScan = nearbyScanFingerprint(hardwareFragment).match()
    val (nearbyRowsRead, _, _, getStringCall, rowConstructorCall) = nearbyScan.instructionMatches
    val nearbyRow = rowConstructorCall.methodReference.definingClass
    check(nearbyRow == infoRow) { "Nearby scan row class is $nearbyRow, expected $infoRow" }
    field("HardwareFragment", "nearbyRows", "Ljava/util/ArrayList;", nearbyRowsRead.fieldReference)
    member("HardwareFragment", "getString(I)Ljava/lang/String;", descriptor(getStringCall.methodReference))
    val (_, requestBluetoothPermissions) =
        bluetoothPermissionRequestFingerprint(hardwareFragment).match().instructionMatches
    member(
        "HardwareFragment",
        "requestPermissions([Ljava/lang/String;I)V",
        descriptor(requestBluetoothPermissions.methodReference),
    )

    val (_, nearbyAdapterStore, _, _) = nearbyAdapterFingerprint(hardwareFragment).match().instructionMatches
    val nearbyAdapter = nearbyAdapterStore.fieldReference
    val adapterBase = classDefBy(nearbyAdapter.type).superclass!!
    stubs[stub("RowAdapter")] = nearbyAdapter.type
    member("HardwareFragment", "nearbyAdapter:${stub("RowAdapter")}", descriptor(nearbyAdapter))
    member("RowAdapter", "notifyDataSetChanged()V", descriptor(notifyDataSetChangedFingerprint(adapterBase).method))

    val hardwareReceiver = hardwareReceiverConstructorFingerprint(hardwareFragment).match()
    val (hardwareReceiverModeStore, fragmentStore) = hardwareReceiver.instructionMatches
    stubs[stub("HardwareReceiver")] = hardwareReceiver.classDef.type
    field("HardwareReceiver", "mode", "B", hardwareReceiverModeStore.fieldReference)
    member("HardwareReceiver", "fragment:${stub("HardwareFragment")}", descriptor(fragmentStore.fieldReference))
    val (_, _, isAddedCall) =
        hardwareReloadFingerprint(hardwareReceiver.classDef.type, hardwareFragment).match().instructionMatches
    member("HardwareFragment", "isAdded()Z", descriptor(isAddedCall.methodReference))

    val batteryTask = BatteryTaskPostExecuteFingerprint.match()
    val (batteryModeRead, batteryActivityRead) = batteryTask.instructionMatches
    stubs[stub("BatteryActivityTask")] = batteryTask.classDef.type
    stubs[stub("BatteryActivity")] = BATTERY_ACTIVITY
    stubs[stub("BatteryMonitorService")] = BATTERY_MONITOR_SERVICE
    field("BatteryActivityTask", "mode", "B", batteryModeRead.fieldReference)
    member("BatteryActivityTask", "activity:${stub("BatteryActivity")}", descriptor(batteryActivityRead.fieldReference))
    field("BatteryActivity", "entries", "Ljava/util/ArrayList;", classDefBy(BATTERY_ACTIVITY))

    val bluetoothTask = BluetoothTaskPostExecuteFingerprint.match()
    val (bluetoothActivityRead, emptyViewRead, emptyTextRead) = bluetoothTask.instructionMatches
    stubs[stub("BluetoothTask")] = bluetoothTask.classDef.type
    stubs[stub("BluetoothActivity")] = BLUETOOTH_ACTIVITY
    member("BluetoothTask", "activity:${stub("BluetoothActivity")}", descriptor(bluetoothActivityRead.fieldReference))
    field("BluetoothTask", "delay", "I", bluetoothTask.classDef)
    field("BluetoothActivity", "emptyView", "Landroid/view/View;", emptyViewRead.fieldReference)
    field("BluetoothActivity", "emptyText", "Landroid/widget/TextView;", emptyTextRead.fieldReference)
    field("BluetoothActivity", "adapter", "Landroid/bluetooth/BluetoothAdapter;", classDefBy(BLUETOOTH_ACTIVITY))

    val publicIpTask = PublicIpTaskPreExecuteFingerprint.match()
    val (_, resultTextStore) = publicIpTask.instructionMatches
    stubs[stub("PublicIpTask")] = publicIpTask.classDef.type
    field("PublicIpTask", "resultText", "Landroid/widget/TextView;", resultTextStore.fieldReference)

    stubs[stub("BootReceiver")] = BOOT_RECEIVER
    stubs[stub("MonitorReceiver")] = MONITOR_RECEIVER
    stubs[stub("UpdateReceiver")] = UPDATE_RECEIVER
    stubs[stub("PauseResumeReceiver")] = PAUSE_RESUME_RECEIVER
    stubs[stub("MonitorActivity")] = MONITOR_ACTIVITY
    member(
        "BootReceiver",
        "startBatteryMonitor(Landroid/content/Context;)V",
        descriptor(BootBatteryMonitorFingerprint.method),
    )
    member(
        "BootReceiver",
        "startMonitorWindows(Landroid/content/Context;)V",
        descriptor(BootMonitorWindowsFingerprint.method),
    )

    val tempStart = TempMonitorStartFingerprint.match()
    val tempReceiverField = tempStart.fieldMatchedBy(temperatureBatteryReceiverRead)
    val tempReceiver = classDefBy(tempReceiverField.type)
    stubs[stub("TempMonitorWindow")] = TEMP_MONITOR_WINDOW
    stubs[stub("TempMonitorReceiver")] = tempReceiver.type
    field("TempMonitorReceiver", "mode", "B", tempReceiver)
    member(
        "TempMonitorReceiver",
        "window:${stub("TempMonitorWindow")}",
        descriptor(tempReceiver.fields.single { it.type == TEMP_MONITOR_WINDOW }),
    )
    field("TempMonitorWindow", "usesBatteryTemperature", "Z", tempStart.fieldMatchedBy(batteryTemperatureSettingRead))
    member("TempMonitorWindow", "batteryReceiver:Landroid/content/BroadcastReceiver;", descriptor(tempReceiverField))
    field("TempMonitorWindow", "handler", "Landroid/os/Handler;", tempStart.fieldMatchedBy(temperatureHandlerRead))
    member("TempMonitorWindow", "poller:Ljava/lang/Runnable;", descriptor(tempStart.fieldMatchedBy(temperaturePollerRead)))
    field("TempMonitorWindow", "view", "Landroid/view/View;", classDefBy(TEMP_MONITOR_WINDOW))
    val showTemperature = TempMonitorShowTemperatureFingerprint.match()
    val (_, providerPrefsRead, getBooleanCall) = showTemperature.instructionMatches
    member("TempMonitorWindow", "showTemperature(I)V", descriptor(showTemperature.method))
    val appPrefs = providerPrefsRead.fieldReference
    stubs[stub("ProviderPrefs")] = appPrefs.type
    member("ProviderPrefs", "instance:${stub("ProviderPrefs")}", descriptor(appPrefs))
    member(
        "ProviderPrefs",
        "getBoolean(Ljava/lang/String;)Z",
        descriptor(getBooleanCall.methodReference),
    )
    member("MonitorPause", "offsetView(Landroid/view/View;)V", descriptor(MonitorToggleFingerprint.method))

    val batteryStart = BatteryMonitorStartFingerprint.match()
    val (_, batteryReceiverRead, _) = batteryStart.instructionMatches
    val batteryReceiver = classDefBy(batteryReceiverRead.fieldReference.type)
    val batteryViews = BatteryMonitorViewsFingerprint.match()
    val (_, levelTextStore, _, temperatureTextStore) = batteryViews.instructionMatches
    stubs[stub("BatteryMonitorWindow")] = BATTERY_MONITOR_WINDOW
    stubs[stub("BatteryMonitorReceiver")] = batteryReceiver.type
    field("BatteryMonitorReceiver", "mode", "B", batteryReceiver)
    member(
        "BatteryMonitorReceiver",
        "window:${stub("BatteryMonitorWindow")}",
        descriptor(batteryReceiver.fields.single { it.type == BATTERY_MONITOR_WINDOW }),
    )
    member("BatteryMonitorWindow", "batteryReceiver:Landroid/content/BroadcastReceiver;", descriptor(batteryReceiverRead.fieldReference))
    field("BatteryMonitorWindow", "levelText", "Landroid/widget/TextView;", levelTextStore.fieldReference)
    field("BatteryMonitorWindow", "temperatureText", "Landroid/widget/TextView;", temperatureTextStore.fieldReference)
    field("BatteryMonitorWindow", "view", "Landroid/view/View;", classDefBy(BATTERY_MONITOR_WINDOW))

    val (newShizukuReceiver, _) = ShizukuBinderReceiverFingerprint.match().instructionMatches
    val shizukuReceiver = newShizukuReceiver.instruction.getReference<TypeReference>()!!.type
    stubs[stub("ShizukuReceiver")] = shizukuReceiver
    stubs[stub("BinderContainer")] = BINDER_CONTAINER
    field("BinderContainer", "binder", "Landroid/os/IBinder;", classDefBy(BINDER_CONTAINER))
    val binderReceived = ShizukuBinderReceivedFingerprint.matchAll()
        .map { descriptor(it.instructionMatches.single().methodReference) }
        .distinct().single()
    member("Shizuku", "onBinderReceived(Landroid/os/IBinder;Ljava/lang/String;)V", binderReceived)

    val bind = RootServiceBindFingerprint.match()
    val (_, daemonRead, remoteRead) = bind.instructionMatches
    val rootServiceManager = bind.classDef
    stubs[stub("RootServiceManager")] = rootServiceManager.type
    member(
        "RootServiceManager",
        "bind(Landroid/content/Intent;Ljava/util/concurrent/Executor;Landroid/content/ServiceConnection;)Ljava/lang/Object;",
        descriptor(bind.method),
    )
    member("RootServiceManager", "daemon:${stub("RemoteProcess")}", descriptor(daemonRead.fieldReference))
    member("RootServiceManager", "remote:${stub("RemoteProcess")}", descriptor(remoteRead.fieldReference))
    field("RootServiceManager", "flags", "I", rootServiceManager.fields.single { it.type == "I" && !AccessFlags.STATIC.isSet(it.accessFlags) })
    field("RootServiceManager", "pendingTasks", "Ljava/util/ArrayList;", rootServiceManager)

    val connect = RootManagerConnectFingerprint.match()
    val rootManagerProxy = connect.classDef
    val rootManager = rootManagerProxy.interfaces.single()
    rootManagerProxy.addDefaultConstructor()
    stubs[stub("IRootServiceManagerProxy")] = rootManagerProxy.type
    stubs[stub("IRootServiceManager")] = rootManager
    member("IRootServiceManager", "connect(Landroid/os/IBinder;)V", "$rootManager->${connect.method.name}(Landroid/os/IBinder;)V")
    field("IRootServiceManagerProxy", "remote", "Landroid/os/IBinder;", rootManagerProxy)

    val remoteProcess = constructorFingerprint(rootServiceManager.type, rootManager).match()
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
    ).match().classDef
    stubs[stub("BindTask")] = pendingBind.type
    listOf(
        "intent" to "Landroid/content/Intent;",
        "executor" to "Ljava/util/concurrent/Executor;",
        "connection" to "Landroid/content/ServiceConnection;",
    ).forEach { (name, type) -> field("BindTask", name, type, pendingBind) }

    val rootServiceReceiver = rootServiceReceiverConstructorFingerprint(rootServiceManager.type).match()
    val (rootReceiverModeStore, rootReceiverOwnerStore, _, messengerStore) = rootServiceReceiver.instructionMatches
    stubs[stub("RootServiceReceiver")] = rootServiceReceiver.classDef.type
    field("RootServiceReceiver", "mode", "B", rootReceiverModeStore.fieldReference)
    field("RootServiceReceiver", "owner", "Ljava/lang/Object;", rootReceiverOwnerStore.fieldReference)
    field("RootServiceReceiver", "messenger", "Ljava/lang/Object;", messengerStore.fieldReference)

    fun receiver(type: String) = mutableClassDefBy(type).methods.single { it.name == "onReceive" }

    val virtualizedMethods = mapOf(
        postExecute to "cpuTimeSensorDoInBackground",
        batteryTask to "batteryActivityDoInBackground",
        bluetoothTask to "bluetoothDoInBackground",
        publicIpTask to "publicIpDoInBackground",
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
