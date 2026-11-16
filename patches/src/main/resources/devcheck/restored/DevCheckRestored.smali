.class public final LDevCheckRestored;
.super Ljava/lang/Object;

.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        LDevCheckRestored$BluetoothMessage;,
        LDevCheckRestored$HardwareReload;
    }
.end annotation

.method private constructor <init>()V
    .registers 1

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static batteryActivityDoInBackground(Lstub/BatteryActivityTask;[Ljava/lang/Object;)Ljava/lang/Object;
    .registers 2

    iget-byte p1, p0, Lstub/BatteryActivityTask;->mode:B

    if-nez p1, :cond_0

    iget-object p0, p0, Lstub/BatteryActivityTask;->activity:Lstub/BatteryActivity;

    invoke-static {p0}, LDevCheckRestored;->restartBatteryMonitor(Lstub/BatteryActivity;)V

    goto :goto_0

    :cond_0
    const-wide/16 p0, 0x2ee

    invoke-static {p0, p1}, LDevCheckRestored;->sleep(J)V

    :goto_0
    sget-object p0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;

    return-object p0
.end method

.method public static batteryMonitorReceive(Lstub/BatteryMonitorReceiver;Landroid/content/Context;Landroid/content/Intent;)V
    .registers 9

    const-string p1, "\u00b0C"

    iget-object v0, p0, Lstub/BatteryMonitorReceiver;->window:Lstub/BatteryMonitorWindow;

    iget-byte p0, p0, Lstub/BatteryMonitorReceiver;->mode:B

    if-nez p0, :cond_1

    const-string p0, "level"

    const/4 v1, 0x0

    invoke-virtual {p2, p0, v1}, Landroid/content/Intent;->getIntExtra(Ljava/lang/String;I)I

    move-result p0

    const-string v2, "scale"

    const/4 v3, -0x1

    invoke-virtual {p2, v2, v3}, Landroid/content/Intent;->getIntExtra(Ljava/lang/String;I)I

    move-result v2

    const-string v3, "temperature"

    invoke-virtual {p2, v3, v1}, Landroid/content/Intent;->getIntExtra(Ljava/lang/String;I)I

    move-result p2

    div-int/lit8 p2, p2, 0xa

    iget-object v1, v0, Lstub/BatteryMonitorWindow;->levelText:Landroid/widget/TextView;

    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V

    int-to-float p0, p0

    const/high16 v4, 0x42c80000

    mul-float/2addr p0, v4

    int-to-float v2, v2

    div-float/2addr p0, v2

    float-to-int p0, p0

    invoke-virtual {v3, p0}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string p0, "%"

    invoke-virtual {v3, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-virtual {v1, p0}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    :try_start_0
    sget-object p0, Lstub/ProviderPrefs;->instance:Lstub/ProviderPrefs;

    const-string v1, "prefFahrenheit"

    invoke-virtual {p0, v1}, Lstub/ProviderPrefs;->getBoolean(Ljava/lang/String;)Z

    move-result p0

    if-eqz p0, :cond_0

    iget-object p0, v0, Lstub/BatteryMonitorWindow;->temperatureText:Landroid/widget/TextView;

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    int-to-double v2, p2

    const-wide v4, 0x3ffccccccccccccdL

    mul-double/2addr v2, v4

    const-wide/high16 v4, 0x4040000000000000L

    add-double/2addr v2, v4

    double-to-int v2, v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v2, "\u00b0F"

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {p0, v1}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    return-void

    :cond_0
    iget-object p0, v0, Lstub/BatteryMonitorWindow;->temperatureText:Landroid/widget/TextView;

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v1, p2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {p0, v1}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    return-void

    :catch_0
    iget-object p0, v0, Lstub/BatteryMonitorWindow;->temperatureText:Landroid/widget/TextView;

    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v0, p2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-virtual {p0, p1}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    return-void

    :cond_1
    invoke-virtual {p2}, Landroid/content/Intent;->getAction()Ljava/lang/String;

    move-result-object p0

    const-string p1, "android.intent.action.SCREEN_ON"

    invoke-virtual {p1, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p1

    if-eqz p1, :cond_2

    invoke-virtual {v0}, Lstub/BatteryMonitorWindow;->getApplicationContext()Landroid/content/Context;

    move-result-object p0

    iget-object p1, v0, Lstub/BatteryMonitorWindow;->batteryReceiver:Landroid/content/BroadcastReceiver;

    new-instance p2, Landroid/content/IntentFilter;

    const-string v0, "android.intent.action.BATTERY_CHANGED"

    invoke-direct {p2, v0}, Landroid/content/IntentFilter;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0, p1, p2}, Landroid/content/Context;->registerReceiver(Landroid/content/BroadcastReceiver;Landroid/content/IntentFilter;)Landroid/content/Intent;

    return-void

    :cond_2
    const-string p1, "android.intent.action.SCREEN_OFF"

    invoke-virtual {p1, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p1

    if-eqz p1, :cond_3

    invoke-virtual {v0}, Lstub/BatteryMonitorWindow;->getApplicationContext()Landroid/content/Context;

    move-result-object p0

    iget-object p1, v0, Lstub/BatteryMonitorWindow;->batteryReceiver:Landroid/content/BroadcastReceiver;

    invoke-virtual {p0, p1}, Landroid/content/Context;->unregisterReceiver(Landroid/content/BroadcastReceiver;)V

    return-void

    :cond_3
    const-string p1, "flar2.devcheck.ACTION_MONITOR_TOGGLE"

    invoke-virtual {p1, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p0

    if-eqz p0, :cond_4

    iget-object p0, v0, Lstub/BatteryMonitorWindow;->view:Landroid/view/View;

    invoke-static {p0}, Lstub/MonitorPause;->offsetView(Landroid/view/View;)V

    :cond_4
    return-void
.end method

.method public static bluetoothDoInBackground(Lstub/BluetoothTask;[Ljava/lang/Object;)Ljava/lang/Object;
    .registers 6

    iget-object p1, p0, Lstub/BluetoothTask;->activity:Lstub/BluetoothActivity;

    new-instance v0, Ljava/util/ArrayList;

    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    :try_start_0
    iget p0, p0, Lstub/BluetoothTask;->delay:I

    int-to-long v1, p0

    invoke-static {v1, v2}, Ljava/lang/Thread;->sleep(J)V
    :try_end_0
    .catch Ljava/lang/InterruptedException; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_0

    :catch_0
    move-exception p0

    invoke-virtual {p0}, Ljava/lang/InterruptedException;->printStackTrace()V

    :goto_0
    :try_start_1
    sget p0, Landroid/os/Build$VERSION;->SDK_INT:I

    const/16 v1, 0x1f

    if-lt p0, v1, :cond_0

    const-string p0, "android.permission.BLUETOOTH_SCAN"

    invoke-virtual {p1, p0}, Lstub/BluetoothActivity;->checkSelfPermission(Ljava/lang/String;)I

    move-result p0

    if-eqz p0, :cond_0

    new-instance p0, LDevCheckRestored$BluetoothMessage;

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    sget v2, Lstub/StringRes;->permission_denied:I

    invoke-virtual {p1, v2}, Lstub/BluetoothActivity;->getString(I)Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v2, "\nandroid.permission.BLUETOOTH_SCAN"

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-direct {p0, p1, v1}, LDevCheckRestored$BluetoothMessage;-><init>(Lstub/BluetoothActivity;Ljava/lang/String;)V

    invoke-virtual {p1, p0}, Lstub/BluetoothActivity;->runOnUiThread(Ljava/lang/Runnable;)V

    :cond_0
    iget-object p0, p1, Lstub/BluetoothActivity;->adapter:Landroid/bluetooth/BluetoothAdapter;

    invoke-virtual {p0}, Landroid/bluetooth/BluetoothAdapter;->getBondedDevices()Ljava/util/Set;

    move-result-object p0

    invoke-interface {p0}, Ljava/util/Set;->size()I

    move-result v1

    if-lez v1, :cond_1

    invoke-interface {p0}, Ljava/util/Set;->iterator()Ljava/util/Iterator;

    move-result-object p0

    :goto_1
    invoke-interface {p0}, Ljava/util/Iterator;->hasNext()Z

    move-result p1

    if-eqz p1, :cond_2

    invoke-interface {p0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Landroid/bluetooth/BluetoothDevice;

    new-instance v1, Lstub/InfoRow;

    invoke-virtual {p1}, Landroid/bluetooth/BluetoothDevice;->getName()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {p1}, Landroid/bluetooth/BluetoothDevice;->getAddress()Ljava/lang/String;

    move-result-object p1

    const/16 v3, 0x25

    invoke-direct {v1, v3, v2, p1}, Lstub/InfoRow;-><init>(ILjava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v0, v1}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    goto :goto_1

    :cond_1
    new-instance p0, LDevCheckRestored$BluetoothMessage;

    sget v1, Lstub/StringRes;->none:I

    invoke-virtual {p1, v1}, Lstub/BluetoothActivity;->getString(I)Ljava/lang/String;

    move-result-object v1

    invoke-direct {p0, p1, v1}, LDevCheckRestored$BluetoothMessage;-><init>(Lstub/BluetoothActivity;Ljava/lang/String;)V

    invoke-virtual {p1, p0}, Lstub/BluetoothActivity;->runOnUiThread(Ljava/lang/Runnable;)V
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_1

    goto :goto_2

    :catch_1
    move-exception p0

    invoke-virtual {p0}, Ljava/lang/Exception;->printStackTrace()V

    :cond_2
    :goto_2
    return-object v0
.end method

.method public static bootReceive(Lstub/BootReceiver;Landroid/content/Context;Landroid/content/Intent;)V
    .registers 4

    const-string v0, "android.intent.action.BOOT_COMPLETED"

    invoke-virtual {p2}, Landroid/content/Intent;->getAction()Ljava/lang/String;

    move-result-object p2

    invoke-virtual {v0, p2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p2

    if-nez p2, :cond_0

    goto :goto_1

    :cond_0
    const-string p2, "prefCPUTimeSaveOffsets"

    const/4 v0, 0x0

    invoke-static {p2, v0}, Lstub/EncryptedPrefs;->putBoolean(Ljava/lang/String;Z)V

    const-string p2, "prefBootReceiver"

    const/4 v0, 0x1

    invoke-static {p2, v0}, Lstub/EncryptedPrefs;->putBoolean(Ljava/lang/String;Z)V

    :try_start_0
    invoke-static {p1}, Lstub/BootReceiver;->startBatteryMonitor(Landroid/content/Context;)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_0

    :catch_0
    invoke-static {p1}, Lstub/BootReceiver;->startBatteryMonitor(Landroid/content/Context;)V

    :goto_0
    :try_start_1
    invoke-virtual {p0, p1}, Lstub/BootReceiver;->startMonitorWindows(Landroid/content/Context;)V
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_1

    goto :goto_1

    :catch_1
    invoke-virtual {p0, p1}, Lstub/BootReceiver;->startMonitorWindows(Landroid/content/Context;)V

    :goto_1
    return-void
.end method

.method public static cpuTimeSensorDoInBackground(Lstub/CpuTimeSensorTask;[Ljava/lang/Object;)Ljava/lang/Object;
    .registers 2

    iget-object p0, p0, Lstub/CpuTimeSensorTask;->owner:Ljava/lang/Object;

    instance-of p1, p0, Lstub/CpuTimeActivity;

    if-eqz p1, :cond_0

    check-cast p0, Lstub/CpuTimeActivity;

    invoke-static {p0}, LDevCheckRestored;->cpuTimes(Lstub/CpuTimeActivity;)Ljava/util/List;

    move-result-object p0

    return-object p0

    :cond_0
    check-cast p0, Lstub/SensorActivity;

    invoke-static {p0}, LDevCheckRestored;->sensorDetails(Lstub/SensorActivity;)Ljava/util/List;

    move-result-object p0

    return-object p0
.end method

.method private static cpuTimes(Lstub/CpuTimeActivity;)Ljava/util/List;
    .registers 10

    new-instance v0, Ljava/util/ArrayList;

    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    new-instance v1, Ljava/text/DecimalFormat;

    const-string v2, "0.0"

    invoke-direct {v1, v2}, Ljava/text/DecimalFormat;-><init>(Ljava/lang/String;)V

    invoke-static {p0}, LDevCheckRestored;->readTimeInState(Lstub/CpuTimeActivity;)V

    :try_start_0
    iget-object v2, p0, Lstub/CpuTimeActivity;->timeInStatePath:Ljava/lang/String;

    invoke-static {v2}, Lstub/DeviceFiles;->exists(Ljava/lang/String;)Z

    move-result v2

    if-eqz v2, :cond_0

    new-instance v2, Lstub/TimeInStateRow;

    invoke-direct {v2}, Lstub/TimeInStateRow;-><init>()V

    const/4 v3, 0x0

    iput-byte v3, v2, Lstub/TimeInStateRow;->type:B

    sget v3, Lstub/StringRes;->cpu_time_in_state:I

    invoke-virtual {p0, v3}, Lstub/CpuTimeActivity;->getString(I)Ljava/lang/String;

    move-result-object v3

    iput-object v3, v2, Lstub/TimeInStateRow;->title:Ljava/lang/String;

    invoke-virtual {v0, v2}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    iget-object v2, p0, Lstub/CpuTimeActivity;->states:Ljava/util/ArrayList;

    invoke-virtual {v2}, Ljava/util/ArrayList;->size()I

    move-result v3

    add-int/lit8 v3, v3, -0x1

    :goto_0
    if-ltz v3, :cond_1

    invoke-virtual {v2, v3}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Lstub/TimeInState;

    new-instance v5, Lstub/TimeInStateRow;

    invoke-direct {v5}, Lstub/TimeInStateRow;-><init>()V

    const/4 v6, 0x2

    iput-byte v6, v5, Lstub/TimeInStateRow;->type:B

    iget-object v6, v4, Lstub/TimeInState;->name:Ljava/lang/String;

    iput-object v6, v5, Lstub/TimeInStateRow;->title:Ljava/lang/String;

    iget-object v6, v4, Lstub/TimeInState;->duration:Ljava/lang/String;

    iput-object v6, v5, Lstub/TimeInStateRow;->duration:Ljava/lang/String;

    new-instance v6, Ljava/lang/StringBuilder;

    invoke-direct {v6}, Ljava/lang/StringBuilder;-><init>()V

    iget-object v7, v4, Lstub/TimeInState;->percent:Ljava/lang/String;

    invoke-static {v7}, Ljava/lang/Double;->parseDouble(Ljava/lang/String;)D

    move-result-wide v7

    invoke-virtual {v1, v7, v8}, Ljava/text/DecimalFormat;->format(D)Ljava/lang/String;

    move-result-object v7

    invoke-virtual {v6, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v7, "%"

    invoke-virtual {v6, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v6

    iput-object v6, v5, Lstub/TimeInStateRow;->percent:Ljava/lang/String;

    iget v4, v4, Lstub/TimeInState;->progress:I

    iput v4, v5, Lstub/TimeInStateRow;->progress:I

    invoke-virtual {v0, v5}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    add-int/lit8 v3, v3, -0x1

    goto :goto_0

    :cond_0
    invoke-static {p0}, LDevCheckRestored;->notAvailable(Lstub/CpuTimeActivity;)Lstub/TimeInStateRow;

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_1

    :catch_0
    move-exception v1

    invoke-virtual {v1}, Ljava/lang/Exception;->printStackTrace()V

    invoke-static {p0}, LDevCheckRestored;->notAvailable(Lstub/CpuTimeActivity;)Lstub/TimeInStateRow;

    move-result-object p0

    invoke-virtual {v0, p0}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    :cond_1
    :goto_1
    new-instance p0, Lstub/TimeInStateRow;

    invoke-direct {p0}, Lstub/TimeInStateRow;-><init>()V

    const/4 v1, 0x3

    iput-byte v1, p0, Lstub/TimeInStateRow;->type:B

    invoke-virtual {v0, p0}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    return-object v0
.end method

.method private static formatDuration(J)Ljava/lang/String;
    .registers 8

    const-wide/16 v0, 0xa

    mul-long/2addr p0, v0

    const-wide/32 v0, 0x36ee80

    div-long v0, p0, v0

    const-wide/32 v2, 0xea60

    div-long v2, p0, v2

    const-wide/16 v4, 0x3e8

    div-long/2addr p0, v4

    sget-object v4, Ljava/util/concurrent/TimeUnit;->MINUTES:Ljava/util/concurrent/TimeUnit;

    invoke-virtual {v4, v2, v3}, Ljava/util/concurrent/TimeUnit;->toSeconds(J)J

    move-result-wide v4

    sub-long/2addr p0, v4

    invoke-static {v0, v1}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;

    move-result-object v4

    sget-object v5, Ljava/util/concurrent/TimeUnit;->HOURS:Ljava/util/concurrent/TimeUnit;

    invoke-virtual {v5, v0, v1}, Ljava/util/concurrent/TimeUnit;->toMinutes(J)J

    move-result-wide v0

    sub-long/2addr v2, v0

    invoke-static {v2, v3}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;

    move-result-object v0

    invoke-static {p0, p1}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;

    move-result-object p0

    const/4 p1, 0x3

    new-array p1, p1, [Ljava/lang/Object;

    const/4 v1, 0x0

    aput-object v4, p1, v1

    const/4 v1, 0x1

    aput-object v0, p1, v1

    const/4 v0, 0x2

    aput-object p0, p1, v0

    const-string p0, "%02d:%02d:%02d"

    invoke-static {p0, p1}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;

    move-result-object p0

    return-object p0
.end method

.method public static hardwareReceive(Lstub/HardwareReceiver;Landroid/content/Context;Landroid/content/Intent;)V
    .registers 11

    iget-object p1, p0, Lstub/HardwareReceiver;->fragment:Lstub/HardwareFragment;

    iget-byte p0, p0, Lstub/HardwareReceiver;->mode:B

    if-eqz p0, :cond_0

    const-string p0, "android.hardware.usb.action.USB_DEVICE_ATTACHED"

    invoke-virtual {p2}, Landroid/content/Intent;->getAction()Ljava/lang/String;

    move-result-object p2

    invoke-virtual {p0, p2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p0

    if-eqz p0, :cond_4

    new-instance p0, Landroid/os/Handler;

    invoke-direct {p0}, Landroid/os/Handler;-><init>()V

    new-instance p2, LDevCheckRestored$HardwareReload;

    invoke-direct {p2, p1}, LDevCheckRestored$HardwareReload;-><init>(Lstub/HardwareFragment;)V

    const-wide/16 v0, 0x3e8

    invoke-virtual {p0, p2, v0, v1}, Landroid/os/Handler;->postDelayed(Ljava/lang/Runnable;J)Z

    return-void

    :cond_0
    const-string p0, "android.bluetooth.device.action.FOUND"

    invoke-virtual {p2}, Landroid/content/Intent;->getAction()Ljava/lang/String;

    move-result-object v0

    invoke-virtual {p0, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p0

    if-nez p0, :cond_1

    goto :goto_0

    :cond_1
    iget-object p0, p1, Lstub/HardwareFragment;->nearbyRows:Ljava/util/ArrayList;

    invoke-virtual {p0}, Ljava/util/ArrayList;->size()I

    move-result v0

    const/4 v1, 0x1

    if-ne v0, v1, :cond_2

    const/4 v0, 0x0

    invoke-virtual {p0, v0}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Lstub/InfoRow;

    iget v2, v2, Lstub/InfoRow;->type:I

    if-ne v2, v1, :cond_2

    invoke-virtual {p0, v0}, Ljava/util/ArrayList;->remove(I)Ljava/lang/Object;

    :cond_2
    const-string v0, "android.bluetooth.device.extra.DEVICE"

    invoke-virtual {p2, v0}, Landroid/content/Intent;->getParcelableExtra(Ljava/lang/String;)Landroid/os/Parcelable;

    move-result-object v0

    check-cast v0, Landroid/bluetooth/BluetoothDevice;

    invoke-virtual {v0}, Landroid/bluetooth/BluetoothDevice;->getAddress()Ljava/lang/String;

    move-result-object v4

    const-string v1, "android.bluetooth.device.extra.RSSI"

    const/16 v2, -0x8000

    invoke-virtual {p2, v1, v2}, Landroid/content/Intent;->getShortExtra(Ljava/lang/String;S)S

    move-result p2

    int-to-float p2, p2

    invoke-static {p2}, Ljava/lang/Math;->abs(F)F

    move-result v1

    const v2, 0x3fd55555

    mul-float/2addr v1, v2

    const/high16 v2, 0x43270000

    sub-float/2addr v2, v1

    float-to-int v6, v2

    invoke-virtual {p0}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v1

    :cond_3
    invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z

    move-result v2

    if-eqz v2, :cond_5

    invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Lstub/InfoRow;

    iget-object v2, v2, Lstub/InfoRow;->address:Ljava/lang/String;

    invoke-virtual {v4, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v2

    if-eqz v2, :cond_3

    :cond_4
    :goto_0
    return-void

    :cond_5
    sget v1, Landroid/os/Build$VERSION;->SDK_INT:I

    const/16 v2, 0x1f

    if-lt v1, v2, :cond_6

    invoke-virtual {p1}, Lstub/HardwareFragment;->requireContext()Landroid/content/Context;

    move-result-object v1

    const-string v2, "android.permission.BLUETOOTH_CONNECT"

    invoke-virtual {v1, v2}, Landroid/content/Context;->checkSelfPermission(Ljava/lang/String;)I

    move-result v1

    if-eqz v1, :cond_6

    filled-new-array {v2}, [Ljava/lang/String;

    move-result-object p0

    const/16 p2, 0x86

    invoke-virtual {p1, p0, p2}, Lstub/HardwareFragment;->requestPermissions([Ljava/lang/String;I)V

    goto :goto_1

    :cond_6
    invoke-virtual {v0}, Landroid/bluetooth/BluetoothDevice;->getName()Ljava/lang/String;

    move-result-object v0

    invoke-static {v0}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v1

    if-eqz v1, :cond_7

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "<"

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    sget v1, Lstub/StringRes;->unknown:I

    invoke-virtual {p1, v1}, Lstub/HardwareFragment;->getString(I)Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v1, ">"

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    :cond_7
    move-object v2, v0

    new-instance v1, Lstub/InfoRow;

    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v0, p2}, Ljava/lang/StringBuilder;->append(F)Ljava/lang/StringBuilder;

    const-string p2, "dBm"

    invoke-virtual {v0, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v5

    const/16 v7, 0x13

    const-string v3, ""

    invoke-direct/range {v1 .. v7}, Lstub/InfoRow;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II)V

    invoke-virtual {p0, v1}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    :goto_1
    iget-object p0, p1, Lstub/HardwareFragment;->nearbyAdapter:Lstub/RowAdapter;

    invoke-virtual {p0}, Lstub/RowAdapter;->notifyDataSetChanged()V

    return-void
.end method

.method public static monitorReceive(Lstub/MonitorReceiver;Landroid/content/Context;Landroid/content/Intent;)V
    .registers 3

    new-instance p0, Landroid/content/Intent;

    const-class p2, Lstub/MonitorActivity;

    invoke-direct {p0, p1, p2}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V

    const/high16 p2, 0x10000000

    invoke-virtual {p0, p2}, Landroid/content/Intent;->addFlags(I)Landroid/content/Intent;

    move-result-object p0

    invoke-virtual {p1, p0}, Landroid/content/Context;->startActivity(Landroid/content/Intent;)V

    return-void
.end method

.method private static notAvailable(Lstub/CpuTimeActivity;)Lstub/TimeInStateRow;
    .registers 3

    new-instance v0, Lstub/TimeInStateRow;

    invoke-direct {v0}, Lstub/TimeInStateRow;-><init>()V

    const/4 v1, 0x0

    iput-byte v1, v0, Lstub/TimeInStateRow;->type:B

    sget v1, Lstub/StringRes;->not_available:I

    invoke-virtual {p0, v1}, Lstub/CpuTimeActivity;->getString(I)Ljava/lang/String;

    move-result-object p0

    iput-object p0, v0, Lstub/TimeInStateRow;->title:Ljava/lang/String;

    return-object v0
.end method

.method public static pauseResumeReceive(Lstub/PauseResumeReceiver;Landroid/content/Context;Landroid/content/Intent;)V
    .registers 4

    const-string p0, "flar2.devcheck.ACTION_MASTER_MONITOR_TOGGLE"

    invoke-virtual {p2}, Landroid/content/Intent;->getAction()Ljava/lang/String;

    move-result-object p2

    invoke-virtual {p0, p2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p0

    if-nez p0, :cond_0

    return-void

    :cond_0
    const-string p0, "monitors"

    const/4 p2, 0x0

    invoke-virtual {p1, p0, p2}, Landroid/content/Context;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;

    move-result-object p0

    const-string v0, "prefMonitorPaused"

    invoke-interface {p0, v0, p2}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z

    move-result p2

    invoke-interface {p0}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences$Editor;

    move-result-object p0

    xor-int/lit8 p2, p2, 0x1

    invoke-interface {p0, v0, p2}, Landroid/content/SharedPreferences$Editor;->putBoolean(Ljava/lang/String;Z)Landroid/content/SharedPreferences$Editor;

    move-result-object p0

    invoke-interface {p0}, Landroid/content/SharedPreferences$Editor;->commit()Z

    new-instance p0, Landroid/content/Intent;

    const-string p2, "flar2.devcheck.ACTION_MONITOR_TOGGLE"

    invoke-direct {p0, p2}, Landroid/content/Intent;-><init>(Ljava/lang/String;)V

    const-string p2, "flar2.devcheck"

    invoke-virtual {p0, p2}, Landroid/content/Intent;->setPackage(Ljava/lang/String;)Landroid/content/Intent;

    invoke-virtual {p1, p0}, Landroid/content/Context;->sendBroadcast(Landroid/content/Intent;)V

    return-void
.end method

.method private static publicIp()Ljava/lang/String;
    .registers 4

    :try_start_0
    new-instance v0, Ljava/io/BufferedReader;

    new-instance v1, Ljava/io/InputStreamReader;

    new-instance v2, Ljava/net/URL;

    const-string v3, "https://api.ipify.org"

    invoke-direct {v2, v3}, Ljava/net/URL;-><init>(Ljava/lang/String;)V

    invoke-virtual {v2}, Ljava/net/URL;->openConnection()Ljava/net/URLConnection;

    move-result-object v2

    invoke-virtual {v2}, Ljava/net/URLConnection;->getInputStream()Ljava/io/InputStream;

    move-result-object v2

    invoke-direct {v1, v2}, Ljava/io/InputStreamReader;-><init>(Ljava/io/InputStream;)V

    invoke-direct {v0, v1}, Ljava/io/BufferedReader;-><init>(Ljava/io/Reader;)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    :try_start_1
    invoke-virtual {v0}, Ljava/io/BufferedReader;->readLine()Ljava/lang/String;

    move-result-object v1
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    :try_start_2
    invoke-virtual {v0}, Ljava/io/BufferedReader;->close()V
    :try_end_2
    .catch Ljava/lang/Exception; {:try_start_2 .. :try_end_2} :catch_0

    return-object v1

    :catchall_0
    move-exception v1

    :try_start_3
    invoke-virtual {v0}, Ljava/io/BufferedReader;->close()V
    :try_end_3
    .catchall {:try_start_3 .. :try_end_3} :catchall_1

    goto :goto_0

    :catchall_1
    move-exception v0

    :try_start_4
    invoke-virtual {v1, v0}, Ljava/lang/Throwable;->addSuppressed(Ljava/lang/Throwable;)V

    :goto_0
    throw v1
    :try_end_4
    .catch Ljava/lang/Exception; {:try_start_4 .. :try_end_4} :catch_0

    :catch_0
    const-string v0, ""

    return-object v0
.end method

.method public static publicIpDoInBackground(Lstub/PublicIpTask;[Ljava/lang/Object;)Ljava/lang/Object;
    .registers 4

    new-instance p1, Ljava/lang/StringBuilder;

    invoke-direct {p1}, Ljava/lang/StringBuilder;-><init>()V

    iget-object p0, p0, Lstub/PublicIpTask;->resultText:Landroid/widget/TextView;

    invoke-virtual {p0}, Landroid/widget/TextView;->getContext()Landroid/content/Context;

    move-result-object p0

    :try_start_0
    const-string v0, "\n"

    invoke-virtual {p1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-static {}, LDevCheckRestored;->publicIp()Ljava/lang/String;

    move-result-object v0

    invoke-virtual {p1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    return-object p0

    :catch_0
    :try_start_1
    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/String;->length()I

    move-result v0

    const/4 v1, 0x6

    if-le v0, v1, :cond_0

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    goto :goto_0

    :cond_0
    sget p1, Lstub/StringRes;->not_avail:I

    invoke-virtual {p0, p1}, Landroid/content/Context;->getString(I)Ljava/lang/String;

    move-result-object p0
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_1

    :goto_0
    return-object p0

    :catch_1
    sget p1, Lstub/StringRes;->not_avail:I

    invoke-virtual {p0, p1}, Landroid/content/Context;->getString(I)Ljava/lang/String;

    move-result-object p0

    return-object p0
.end method

.method private static readLines(Ljava/lang/String;)[Ljava/lang/String;
    .registers 4

    new-instance v0, Ljava/util/ArrayList;

    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    :try_start_0
    new-instance v1, Ljava/io/BufferedReader;

    new-instance v2, Ljava/io/FileReader;

    invoke-direct {v2, p0}, Ljava/io/FileReader;-><init>(Ljava/lang/String;)V

    const/16 p0, 0x200

    invoke-direct {v1, v2, p0}, Ljava/io/BufferedReader;-><init>(Ljava/io/Reader;I)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    :goto_0
    :try_start_1
    invoke-virtual {v1}, Ljava/io/BufferedReader;->readLine()Ljava/lang/String;

    move-result-object p0

    if-eqz p0, :cond_0

    invoke-virtual {v0, p0}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    goto :goto_0

    :cond_0
    :try_start_2
    invoke-virtual {v1}, Ljava/io/BufferedReader;->close()V
    :try_end_2
    .catch Ljava/lang/Exception; {:try_start_2 .. :try_end_2} :catch_0

    invoke-virtual {v0}, Ljava/util/ArrayList;->size()I

    move-result p0

    new-array p0, p0, [Ljava/lang/String;

    invoke-virtual {v0, p0}, Ljava/util/ArrayList;->toArray([Ljava/lang/Object;)[Ljava/lang/Object;

    move-result-object p0

    check-cast p0, [Ljava/lang/String;

    return-object p0

    :catchall_0
    move-exception p0

    :try_start_3
    invoke-virtual {v1}, Ljava/io/BufferedReader;->close()V
    :try_end_3
    .catchall {:try_start_3 .. :try_end_3} :catchall_1

    goto :goto_1

    :catchall_1
    move-exception v0

    :try_start_4
    invoke-virtual {p0, v0}, Ljava/lang/Throwable;->addSuppressed(Ljava/lang/Throwable;)V

    :goto_1
    throw p0
    :try_end_4
    .catch Ljava/lang/Exception; {:try_start_4 .. :try_end_4} :catch_0

    :catch_0
    const/4 p0, 0x0

    new-array p0, p0, [Ljava/lang/String;

    return-object p0
.end method

.method private static readTimeInState(Lstub/CpuTimeActivity;)V
    .registers 34

    move-object/from16 v1, p0

    const-string v2, "prefCPUTimeSaveOffsets"

    const-string v3, "prefCPUTimeDeepSleep"

    iget-object v0, v1, Lstub/CpuTimeActivity;->timeInStatePath:Ljava/lang/String;

    invoke-static {v0}, Lstub/DeviceFiles;->exists(Ljava/lang/String;)Z

    move-result v0

    if-nez v0, :cond_0

    goto/16 :goto_d

    :cond_0
    invoke-static {}, Lstub/EncryptedPrefs;->get()Landroid/content/SharedPreferences;

    move-result-object v4

    :try_start_0
    iget-object v0, v1, Lstub/CpuTimeActivity;->timeInStatePath:Ljava/lang/String;

    invoke-static {v0}, LDevCheckRestored;->readLines(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v5

    iget-object v6, v1, Lstub/CpuTimeActivity;->states:Ljava/util/ArrayList;

    invoke-virtual {v6}, Ljava/util/ArrayList;->clear()V

    array-length v0, v5
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_1

    const/4 v9, 0x0

    move v10, v9

    const-wide/16 v11, 0x0

    :goto_0
    const-string v13, "N/A"

    const-string v14, "freq"

    const-string v15, "\\s+"

    const/16 v16, 0x1

    if-ge v10, v0, :cond_3

    const-wide/16 v17, 0x0

    :try_start_1
    aget-object v7, v5, v10

    invoke-virtual {v7, v14}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v8

    if-nez v8, :cond_2

    invoke-virtual {v7, v13}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v8

    if-eqz v8, :cond_1

    goto :goto_1

    :cond_1
    invoke-virtual {v7, v15}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v7

    aget-object v7, v7, v16

    invoke-static {v7}, Ljava/lang/Long;->parseLong(Ljava/lang/String;)J

    move-result-wide v7

    long-to-double v7, v7

    add-double/2addr v11, v7

    cmpg-double v7, v11, v17

    if-gez v7, :cond_2

    move-wide/from16 v11, v17

    :cond_2
    :goto_1
    add-int/lit8 v10, v10, 0x1

    goto :goto_0

    :cond_3
    const-wide/16 v17, 0x0

    invoke-interface {v4, v3, v9}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z

    move-result v0

    const-wide/16 v7, 0x0

    if-nez v0, :cond_4

    invoke-static {}, Landroid/os/SystemClock;->elapsedRealtime()J

    move-result-wide v19

    invoke-static {}, Landroid/os/SystemClock;->uptimeMillis()J

    move-result-wide v21

    sub-long v19, v19, v21

    const-wide/16 v21, 0xa

    div-long v9, v19, v21

    invoke-static {v9, v10, v7, v8}, Ljava/lang/Math;->max(JJ)J

    move-result-wide v9

    long-to-double v7, v9

    add-double/2addr v11, v7

    const/4 v7, 0x0

    goto :goto_2

    :cond_4
    move v7, v9

    const-wide/16 v9, 0x0

    :goto_2
    invoke-interface {v4, v2, v7}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z

    move-result v0

    const/4 v8, 0x0

    if-eqz v0, :cond_9

    const-string v0, "prefCPUTimecluster2"

    invoke-interface {v4, v0, v7}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z

    move-result v0

    if-eqz v0, :cond_5

    const-string v0, "prefCPUTimeOffsetsBig"

    invoke-interface {v4, v0, v8}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    goto :goto_3

    :cond_5
    const-string v0, "prefCPUTimecluster3"

    const/4 v7, 0x0

    invoke-interface {v4, v0, v7}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z

    move-result v0

    if-eqz v0, :cond_6

    const-string v0, "prefCPUTimeOffsetsPerf"

    invoke-interface {v4, v0, v8}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    goto :goto_3

    :cond_6
    const-string v0, "prefCPUTimeOffsetsLittle"

    invoke-interface {v4, v0, v8}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    :goto_3
    new-instance v7, Ljava/util/ArrayList;

    const-string v8, ","

    invoke-virtual {v0, v8}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v0

    invoke-static {v0}, Ljava/util/Arrays;->asList([Ljava/lang/Object;)Ljava/util/List;

    move-result-object v0

    invoke-direct {v7, v0}, Ljava/util/ArrayList;-><init>(Ljava/util/Collection;)V

    move-wide/from16 v23, v9

    const/4 v0, 0x0

    const-wide/16 v8, 0x0

    :goto_4
    invoke-virtual {v7}, Ljava/util/ArrayList;->size()I

    move-result v10

    if-ge v0, v10, :cond_7

    invoke-virtual {v7, v0}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    move-result-object v10

    check-cast v10, Ljava/lang/String;

    invoke-static {v10}, Ljava/lang/Long;->parseLong(Ljava/lang/String;)J

    move-result-wide v25

    add-long v8, v8, v25

    add-int/lit8 v0, v0, 0x1

    goto :goto_4

    :cond_7
    long-to-double v8, v8

    sub-double/2addr v11, v8

    const/4 v8, 0x0

    invoke-interface {v4, v3, v8}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z

    move-result v0
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_1

    if-nez v0, :cond_8

    :try_start_2
    const-string v0, "prefCPUTimeDeepSleepOffset"

    const/4 v8, 0x0

    invoke-interface {v4, v0, v8}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    invoke-static {v0}, Ljava/lang/Long;->parseLong(Ljava/lang/String;)J

    move-result-wide v8
    :try_end_2
    .catch Ljava/lang/Exception; {:try_start_2 .. :try_end_2} :catch_0

    :goto_5
    move-wide/from16 v21, v11

    goto :goto_6

    :catch_0
    move-exception v0

    :try_start_3
    invoke-virtual {v0}, Ljava/lang/Exception;->printStackTrace()V

    const/4 v8, 0x0

    invoke-static {v2, v8}, Lstub/EncryptedPrefs;->putBoolean(Ljava/lang/String;Z)V

    const-wide/16 v8, 0x0

    goto :goto_5

    :goto_6
    long-to-double v10, v8

    sub-double v11, v21, v10

    move-object v10, v7

    move-wide/from16 v25, v8

    move-wide/from16 v7, v17

    invoke-static {v11, v12, v7, v8}, Ljava/lang/Math;->max(DD)D

    move-result-wide v11

    sub-long v7, v23, v25

    move-object/from16 v17, v10

    const-wide/16 v9, 0x0

    invoke-static {v7, v8, v9, v10}, Ljava/lang/Math;->max(JJ)J

    move-result-wide v7

    move-wide v9, v7

    move-object/from16 v0, v17

    goto :goto_7

    :cond_8
    move-object/from16 v17, v7

    move-wide/from16 v21, v11

    move-object/from16 v0, v17

    move-wide/from16 v9, v23

    goto :goto_7

    :cond_9
    move-wide/from16 v23, v9

    move-object v0, v8

    :goto_7
    double-to-long v7, v11

    new-instance v23, Lstub/TimeInState;

    move-object/from16 v17, v0

    sget v0, Lstub/StringRes;->total:I

    invoke-virtual {v1, v0}, Lstub/CpuTimeActivity;->getString(I)Ljava/lang/String;

    move-result-object v24

    invoke-static {v7, v8}, LDevCheckRestored;->formatDuration(J)Ljava/lang/String;

    move-result-object v27

    const-string v28, "100"

    const/16 v29, 0x64

    move-wide/from16 v25, v7

    invoke-direct/range {v23 .. v29}, Lstub/TimeInState;-><init>(Ljava/lang/String;JLjava/lang/String;Ljava/lang/String;I)V

    move-object/from16 v0, v23

    invoke-virtual {v6, v0}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    const/4 v7, 0x0

    invoke-interface {v4, v3, v7}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z

    move-result v0

    const-wide/high16 v21, 0x4024000000000000L

    if-nez v0, :cond_a

    const-wide v30, 0x408f400000000000L

    long-to-double v7, v9

    div-double/2addr v7, v11

    mul-double v7, v7, v30

    invoke-static {v7, v8}, Ljava/lang/Math;->round(D)J

    move-result-wide v7

    long-to-double v7, v7

    div-double v7, v7, v21

    move-wide/from16 v25, v9

    const-wide/high16 v9, 0x4059000000000000L

    invoke-static {v7, v8, v9, v10}, Ljava/lang/Math;->min(DD)D

    move-result-wide v7

    new-instance v23, Lstub/TimeInState;

    sget v0, Lstub/StringRes;->deep_sleep:I

    invoke-virtual {v1, v0}, Lstub/CpuTimeActivity;->getString(I)Ljava/lang/String;

    move-result-object v24

    invoke-static/range {v25 .. v26}, LDevCheckRestored;->formatDuration(J)Ljava/lang/String;

    move-result-object v27

    invoke-static {v7, v8}, Ljava/lang/Double;->toString(D)Ljava/lang/String;

    move-result-object v28

    invoke-static {v7, v8}, Ljava/lang/Math;->round(D)J

    move-result-wide v0

    long-to-int v0, v0

    move/from16 v29, v0

    invoke-direct/range {v23 .. v29}, Lstub/TimeInState;-><init>(Ljava/lang/String;JLjava/lang/String;Ljava/lang/String;I)V

    move-object/from16 v0, v23

    invoke-virtual {v6, v0}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    goto :goto_8

    :cond_a
    const-wide v30, 0x408f400000000000L

    :goto_8
    array-length v0, v5

    const/4 v1, 0x0

    const/4 v7, 0x0

    :goto_9
    if-ge v7, v0, :cond_10

    aget-object v3, v5, v7

    invoke-virtual {v3, v14}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v8

    if-nez v8, :cond_f

    invoke-virtual {v3, v13}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v8

    if-eqz v8, :cond_b

    goto/16 :goto_b

    :cond_b
    invoke-virtual {v3, v15}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v8

    const/4 v9, 0x0

    aget-object v8, v8, v9

    invoke-virtual {v3, v15}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v3

    aget-object v3, v3, v16

    invoke-static {v3}, Ljava/lang/Long;->parseLong(Ljava/lang/String;)J

    move-result-wide v23

    invoke-interface {v4, v2, v9}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z

    move-result v3

    if-eqz v3, :cond_c

    if-eqz v17, :cond_c

    move-object/from16 v3, v17

    invoke-virtual {v3, v1}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    move-result-object v9

    check-cast v9, Ljava/lang/String;

    invoke-static {v9}, Ljava/lang/Long;->parseLong(Ljava/lang/String;)J

    move-result-wide v9

    sub-long v23, v23, v9

    goto :goto_a

    :cond_c
    move-object/from16 v3, v17

    :goto_a
    move/from16 v17, v0

    move/from16 v18, v1

    move-wide/from16 v9, v23

    long-to-double v0, v9

    div-double/2addr v0, v11

    mul-double v0, v0, v30

    invoke-static {v0, v1}, Ljava/lang/Math;->round(D)J

    move-result-wide v0

    long-to-double v0, v0

    move-object/from16 v32, v2

    move-object/from16 p0, v3

    move-wide/from16 v2, v30

    invoke-static {v0, v1, v2, v3}, Ljava/lang/Math;->min(DD)D

    move-result-wide v0

    div-double v0, v0, v21

    const-wide/high16 v2, 0x4059000000000000L

    invoke-static {v0, v1, v2, v3}, Ljava/lang/Math;->min(DD)D

    move-result-wide v0

    const-wide/16 v19, 0x0

    cmp-long v23, v9, v19

    if-lez v23, :cond_d

    new-instance v23, Lstub/TimeInState;

    invoke-static {v8}, Lstub/CpuFrequency;->label(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v24

    invoke-static {v9, v10}, LDevCheckRestored;->formatDuration(J)Ljava/lang/String;

    move-result-object v27

    invoke-static {v0, v1}, Ljava/lang/Double;->toString(D)Ljava/lang/String;

    move-result-object v28

    invoke-static {v0, v1}, Ljava/lang/Math;->round(D)J

    move-result-wide v0

    long-to-int v0, v0

    move/from16 v29, v0

    move-wide/from16 v25, v9

    invoke-direct/range {v23 .. v29}, Lstub/TimeInState;-><init>(Ljava/lang/String;JLjava/lang/String;Ljava/lang/String;I)V

    move-object/from16 v0, v23

    invoke-virtual {v6, v0}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    :cond_d
    const-string v0, "prefCPUTimeSort"

    const/4 v8, 0x0

    invoke-interface {v4, v0, v8}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z

    move-result v0

    if-eqz v0, :cond_e

    invoke-static {v6}, Ljava/util/Collections;->sort(Ljava/util/List;)V
    :try_end_3
    .catch Ljava/lang/Exception; {:try_start_3 .. :try_end_3} :catch_1

    :cond_e
    add-int/lit8 v1, v18, 0x1

    goto :goto_c

    :cond_f
    :goto_b
    move/from16 v18, v1

    move-object/from16 v32, v2

    move-object/from16 p0, v17

    const-wide/high16 v2, 0x4059000000000000L

    const/4 v8, 0x0

    const-wide/16 v19, 0x0

    move/from16 v17, v0

    move/from16 v1, v18

    :goto_c
    add-int/lit8 v7, v7, 0x1

    move/from16 v0, v17

    move-object/from16 v2, v32

    const-wide v30, 0x408f400000000000L

    move-object/from16 v17, p0

    goto/16 :goto_9

    :catch_1
    move-exception v0

    invoke-virtual {v0}, Ljava/lang/Exception;->printStackTrace()V

    :cond_10
    :goto_d
    return-void
.end method

.method private static restartBatteryMonitor(Lstub/BatteryActivity;)V
    .registers 6

    iget-object v0, p0, Lstub/BatteryActivity;->entries:Ljava/util/ArrayList;

    invoke-virtual {v0}, Ljava/util/ArrayList;->size()I

    move-result v0

    const/4 v1, 0x2

    if-le v0, v1, :cond_0

    iget-object v0, p0, Lstub/BatteryActivity;->entries:Ljava/util/ArrayList;

    invoke-virtual {v0, v1}, Ljava/util/ArrayList;->remove(I)Ljava/lang/Object;

    :cond_0
    new-instance v0, Landroid/content/Intent;

    invoke-virtual {p0}, Lstub/BatteryActivity;->getApplicationContext()Landroid/content/Context;

    move-result-object v1

    const-class v2, Lstub/BatteryMonitorService;

    invoke-direct {v0, v1, v2}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V

    invoke-virtual {p0, v0}, Lstub/BatteryActivity;->stopService(Landroid/content/Intent;)Z

    const-wide/16 v0, 0x1f4

    invoke-static {v0, v1}, LDevCheckRestored;->sleep(J)V

    invoke-static {}, Lstub/EncryptedPrefs;->get()Landroid/content/SharedPreferences;

    move-result-object v2

    const-string v3, "prefBMEnable"

    const/4 v4, 0x0

    invoke-interface {v2, v3, v4}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z

    move-result v2

    if-eqz v2, :cond_2

    new-instance v2, Landroid/content/Intent;

    invoke-virtual {p0}, Lstub/BatteryActivity;->getApplicationContext()Landroid/content/Context;

    move-result-object v3

    const-class v4, Lstub/BatteryMonitorService;

    invoke-direct {v2, v3, v4}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V

    sget v3, Landroid/os/Build$VERSION;->SDK_INT:I

    const/16 v4, 0x1a

    if-lt v3, v4, :cond_1

    invoke-virtual {p0, v2}, Lstub/BatteryActivity;->startForegroundService(Landroid/content/Intent;)Landroid/content/ComponentName;

    goto :goto_0

    :cond_1
    invoke-virtual {p0, v2}, Lstub/BatteryActivity;->startService(Landroid/content/Intent;)Landroid/content/ComponentName;

    :cond_2
    :goto_0
    invoke-static {v0, v1}, LDevCheckRestored;->sleep(J)V

    return-void
.end method

.method public static rootServiceReceive(Lstub/RootServiceReceiver;Landroid/content/Context;Landroid/content/Intent;)V
    .registers 6

    iget-byte p1, p0, Lstub/RootServiceReceiver;->mode:B

    if-eqz p1, :cond_0

    goto/16 :goto_3

    :cond_0
    iget-object p1, p0, Lstub/RootServiceReceiver;->owner:Ljava/lang/Object;

    check-cast p1, Lstub/RootServiceManager;

    const-string v0, "extra.bundle"

    invoke-virtual {p2, v0}, Landroid/content/Intent;->getBundleExtra(Ljava/lang/String;)Landroid/os/Bundle;

    move-result-object v0

    if-nez v0, :cond_1

    goto :goto_3

    :cond_1
    const-string v1, "binder"

    invoke-virtual {v0, v1}, Landroid/os/Bundle;->getBinder(Ljava/lang/String;)Landroid/os/IBinder;

    move-result-object v0

    if-nez v0, :cond_2

    goto :goto_3

    :cond_2
    const-string v1, "com.topjohnwu.superuser.internal.IRootServiceManager"

    invoke-interface {v0, v1}, Landroid/os/IBinder;->queryLocalInterface(Ljava/lang/String;)Landroid/os/IInterface;

    move-result-object v1

    instance-of v2, v1, Lstub/IRootServiceManager;

    if-eqz v2, :cond_3

    check-cast v1, Lstub/IRootServiceManager;

    goto :goto_0

    :cond_3
    new-instance v1, Lstub/IRootServiceManagerProxy;

    invoke-direct {v1}, Lstub/IRootServiceManagerProxy;-><init>()V

    iput-object v0, v1, Lstub/IRootServiceManagerProxy;->remote:Landroid/os/IBinder;

    :goto_0
    :try_start_0
    iget-object p0, p0, Lstub/RootServiceReceiver;->messenger:Ljava/lang/Object;

    check-cast p0, Landroid/os/Messenger;

    invoke-virtual {p0}, Landroid/os/Messenger;->getBinder()Landroid/os/IBinder;

    move-result-object p0

    invoke-interface {v1, p0}, Lstub/IRootServiceManager;->connect(Landroid/os/IBinder;)V

    new-instance p0, Lstub/RemoteProcess;

    invoke-direct {p0, p1, v1}, Lstub/RemoteProcess;-><init>(Lstub/RootServiceManager;Lstub/IRootServiceManager;)V

    const-string v0, "extra.daemon"

    const/4 v1, 0x0

    invoke-virtual {p2, v0, v1}, Landroid/content/Intent;->getBooleanExtra(Ljava/lang/String;Z)Z

    move-result p2

    if-eqz p2, :cond_4

    iput-object p0, p1, Lstub/RootServiceManager;->daemon:Lstub/RemoteProcess;

    iget p0, p1, Lstub/RootServiceManager;->flags:I

    and-int/lit8 p0, p0, -0x3

    iput p0, p1, Lstub/RootServiceManager;->flags:I

    goto :goto_1

    :cond_4
    iput-object p0, p1, Lstub/RootServiceManager;->remote:Lstub/RemoteProcess;

    iget p0, p1, Lstub/RootServiceManager;->flags:I

    and-int/lit8 p0, p0, -0x2

    iput p0, p1, Lstub/RootServiceManager;->flags:I

    :goto_1
    iget-object p0, p1, Lstub/RootServiceManager;->pendingTasks:Ljava/util/ArrayList;

    invoke-virtual {p0}, Ljava/util/ArrayList;->size()I

    move-result p2

    add-int/lit8 p2, p2, -0x1

    :goto_2
    if-ltz p2, :cond_6

    invoke-virtual {p0, p2}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lstub/BindTask;

    iget-object v1, v0, Lstub/BindTask;->intent:Landroid/content/Intent;

    iget-object v2, v0, Lstub/BindTask;->executor:Ljava/util/concurrent/Executor;

    iget-object v0, v0, Lstub/BindTask;->connection:Landroid/content/ServiceConnection;

    invoke-virtual {p1, v1, v2, v0}, Lstub/RootServiceManager;->bind(Landroid/content/Intent;Ljava/util/concurrent/Executor;Landroid/content/ServiceConnection;)Ljava/lang/Object;

    move-result-object v0

    if-nez v0, :cond_5

    invoke-virtual {p0, p2}, Ljava/util/ArrayList;->remove(I)Ljava/lang/Object;
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    :cond_5
    add-int/lit8 p2, p2, -0x1

    goto :goto_2

    :cond_6
    :goto_3
    return-void

    :catch_0
    move-exception p0

    invoke-virtual {p0}, Ljava/lang/Exception;->printStackTrace()V

    return-void
.end method

.method private static sensorDetails(Lstub/SensorActivity;)Ljava/util/List;
    .registers 9

    new-instance v0, Ljava/util/ArrayList;

    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    iget-object v1, p0, Lstub/SensorActivity;->sensor:Landroid/hardware/Sensor;

    if-nez v1, :cond_0

    return-object v0

    :cond_0
    invoke-virtual {p0}, Lstub/SensorActivity;->getIntent()Landroid/content/Intent;

    move-result-object v2

    new-instance v3, Lstub/InfoRow;

    sget v4, Lstub/StringRes;->information:I

    invoke-virtual {p0, v4}, Lstub/SensorActivity;->getString(I)Ljava/lang/String;

    move-result-object v4

    const/4 v5, 0x0

    const/4 v6, 0x0

    invoke-direct {v3, v6, v4, v5}, Lstub/InfoRow;-><init>(ILjava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v0, v3}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    const-string v3, "sensorType"

    const/4 v4, 0x1

    invoke-virtual {v2, v3, v4}, Landroid/content/Intent;->getIntExtra(Ljava/lang/String;I)I

    move-result v3

    const/16 v5, 0x13

    if-ne v3, v5, :cond_2

    iget v3, p0, Lstub/SensorActivity;->steps:I

    if-nez v3, :cond_1

    sget v3, Lstub/StringRes;->not_avail:I

    invoke-virtual {p0, v3}, Lstub/SensorActivity;->getString(I)Ljava/lang/String;

    move-result-object v3

    goto :goto_0

    :cond_1
    iget v3, p0, Lstub/SensorActivity;->steps:I

    invoke-static {v3}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;

    move-result-object v3

    :goto_0
    new-instance v5, Lstub/InfoRow;

    sget v7, Lstub/StringRes;->total_steps:I

    invoke-virtual {p0, v7}, Lstub/SensorActivity;->getString(I)Ljava/lang/String;

    move-result-object v7

    invoke-direct {v5, v4, v7, v3}, Lstub/InfoRow;-><init>(ILjava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v0, v5}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    :cond_2
    new-instance v3, Lstub/InfoRow;

    sget v5, Lstub/StringRes;->manufacturer:I

    invoke-virtual {p0, v5}, Lstub/SensorActivity;->getString(I)Ljava/lang/String;

    move-result-object v5

    const-string v7, "sensorMfg"

    invoke-virtual {v2, v7}, Landroid/content/Intent;->getStringExtra(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v7

    invoke-direct {v3, v4, v5, v7}, Lstub/InfoRow;-><init>(ILjava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v0, v3}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    new-instance v3, Lstub/InfoRow;

    sget v5, Lstub/StringRes;->model:I

    invoke-virtual {p0, v5}, Lstub/SensorActivity;->getString(I)Ljava/lang/String;

    move-result-object v5

    const-string v7, "sensorModel"

    invoke-virtual {v2, v7}, Landroid/content/Intent;->getStringExtra(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v2

    invoke-direct {v3, v4, v5, v2}, Lstub/InfoRow;-><init>(ILjava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v0, v3}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    new-instance v2, Lstub/InfoRow;

    sget v3, Lstub/StringRes;->resolution:I

    invoke-virtual {p0, v3}, Lstub/SensorActivity;->getString(I)Ljava/lang/String;

    move-result-object v3

    invoke-static {v1, v6}, Lstub/SensorActivity;->formatRange(Landroid/hardware/Sensor;Z)Ljava/lang/String;

    move-result-object v5

    invoke-direct {v2, v4, v3, v5}, Lstub/InfoRow;-><init>(ILjava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v0, v2}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    new-instance v2, Lstub/InfoRow;

    sget v3, Lstub/StringRes;->max_range:I

    invoke-virtual {p0, v3}, Lstub/SensorActivity;->getString(I)Ljava/lang/String;

    move-result-object v3

    invoke-static {v1, v4}, Lstub/SensorActivity;->formatRange(Landroid/hardware/Sensor;Z)Ljava/lang/String;

    move-result-object v5

    invoke-direct {v2, v4, v3, v5}, Lstub/InfoRow;-><init>(ILjava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v0, v2}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    new-instance v3, Ljava/text/DecimalFormat;

    const-string v5, "0.000#######"

    invoke-direct {v3, v5}, Ljava/text/DecimalFormat;-><init>(Ljava/lang/String;)V

    invoke-virtual {v1}, Landroid/hardware/Sensor;->getPower()F

    move-result v5

    float-to-double v5, v5

    invoke-virtual {v3, v5, v6}, Ljava/text/DecimalFormat;->format(D)Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v3, " mA"

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    new-instance v3, Lstub/InfoRow;

    sget v5, Lstub/StringRes;->power:I

    invoke-virtual {p0, v5}, Lstub/SensorActivity;->getString(I)Ljava/lang/String;

    move-result-object v5

    invoke-direct {v3, v4, v5, v2}, Lstub/InfoRow;-><init>(ILjava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v0, v3}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    invoke-virtual {v1}, Landroid/hardware/Sensor;->isWakeUpSensor()Z

    move-result v1

    if-eqz v1, :cond_3

    sget v1, Lstub/StringRes;->yes:I

    goto :goto_1

    :cond_3
    sget v1, Lstub/StringRes;->no:I

    :goto_1
    invoke-virtual {p0, v1}, Lstub/SensorActivity;->getString(I)Ljava/lang/String;

    move-result-object v1

    new-instance v2, Lstub/InfoRow;

    sget v3, Lstub/StringRes;->wakeup_sensor:I

    invoke-virtual {p0, v3}, Lstub/SensorActivity;->getString(I)Ljava/lang/String;

    move-result-object p0

    invoke-direct {v2, v4, p0, v1}, Lstub/InfoRow;-><init>(ILjava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v0, v2}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    return-object v0
.end method

.method public static shizukuBinderReceive(Lstub/ShizukuReceiver;Landroid/content/Context;Landroid/content/Intent;)V
    .registers 3

    const-string p0, "moe.shizuku.privileged.api.intent.extra.BINDER"

    invoke-virtual {p2, p0}, Landroid/content/Intent;->getParcelableExtra(Ljava/lang/String;)Landroid/os/Parcelable;

    move-result-object p0

    check-cast p0, Lstub/BinderContainer;

    if-eqz p0, :cond_0

    iget-object p2, p0, Lstub/BinderContainer;->binder:Landroid/os/IBinder;

    if-eqz p2, :cond_0

    iget-object p0, p0, Lstub/BinderContainer;->binder:Landroid/os/IBinder;

    invoke-virtual {p1}, Landroid/content/Context;->getPackageName()Ljava/lang/String;

    move-result-object p1

    invoke-static {p0, p1}, Lstub/Shizuku;->onBinderReceived(Landroid/os/IBinder;Ljava/lang/String;)V

    :cond_0
    return-void
.end method

.method private static sleep(J)V
    .registers 2

    :try_start_0
    invoke-static {p0, p1}, Ljava/lang/Thread;->sleep(J)V
    :try_end_0
    .catch Ljava/lang/InterruptedException; {:try_start_0 .. :try_end_0} :catch_0

    return-void

    :catch_0
    invoke-static {}, Ljava/lang/Thread;->currentThread()Ljava/lang/Thread;

    move-result-object p0

    invoke-virtual {p0}, Ljava/lang/Thread;->interrupt()V

    return-void
.end method

.method public static tempMonitorReceive(Lstub/TempMonitorReceiver;Landroid/content/Context;Landroid/content/Intent;)V
    .registers 4

    iget-object p1, p0, Lstub/TempMonitorReceiver;->window:Lstub/TempMonitorWindow;

    iget-byte p0, p0, Lstub/TempMonitorReceiver;->mode:B

    if-nez p0, :cond_0

    const-string p0, "temperature"

    const/4 v0, 0x0

    invoke-virtual {p2, p0, v0}, Landroid/content/Intent;->getIntExtra(Ljava/lang/String;I)I

    move-result p0

    invoke-virtual {p1, p0}, Lstub/TempMonitorWindow;->showTemperature(I)V

    return-void

    :cond_0
    invoke-virtual {p2}, Landroid/content/Intent;->getAction()Ljava/lang/String;

    move-result-object p0

    const-string p2, "android.intent.action.SCREEN_ON"

    invoke-virtual {p2, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p2

    if-eqz p2, :cond_2

    iget-boolean p0, p1, Lstub/TempMonitorWindow;->usesBatteryTemperature:Z

    if-eqz p0, :cond_1

    invoke-virtual {p1}, Lstub/TempMonitorWindow;->getApplicationContext()Landroid/content/Context;

    move-result-object p0

    iget-object p1, p1, Lstub/TempMonitorWindow;->batteryReceiver:Landroid/content/BroadcastReceiver;

    new-instance p2, Landroid/content/IntentFilter;

    const-string v0, "android.intent.action.BATTERY_CHANGED"

    invoke-direct {p2, v0}, Landroid/content/IntentFilter;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0, p1, p2}, Landroid/content/Context;->registerReceiver(Landroid/content/BroadcastReceiver;Landroid/content/IntentFilter;)Landroid/content/Intent;

    return-void

    :cond_1
    iget-object p0, p1, Lstub/TempMonitorWindow;->handler:Landroid/os/Handler;

    iget-object p1, p1, Lstub/TempMonitorWindow;->poller:Ljava/lang/Runnable;

    invoke-virtual {p0, p1}, Landroid/os/Handler;->post(Ljava/lang/Runnable;)Z

    return-void

    :cond_2
    const-string p2, "android.intent.action.SCREEN_OFF"

    invoke-virtual {p2, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p2

    if-eqz p2, :cond_4

    iget-boolean p0, p1, Lstub/TempMonitorWindow;->usesBatteryTemperature:Z

    if-eqz p0, :cond_3

    invoke-virtual {p1}, Lstub/TempMonitorWindow;->getApplicationContext()Landroid/content/Context;

    move-result-object p0

    iget-object p1, p1, Lstub/TempMonitorWindow;->batteryReceiver:Landroid/content/BroadcastReceiver;

    invoke-virtual {p0, p1}, Landroid/content/Context;->unregisterReceiver(Landroid/content/BroadcastReceiver;)V

    return-void

    :cond_3
    iget-object p0, p1, Lstub/TempMonitorWindow;->handler:Landroid/os/Handler;

    iget-object p1, p1, Lstub/TempMonitorWindow;->poller:Ljava/lang/Runnable;

    invoke-virtual {p0, p1}, Landroid/os/Handler;->removeCallbacks(Ljava/lang/Runnable;)V

    return-void

    :cond_4
    const-string p2, "flar2.devcheck.ACTION_MONITOR_TOGGLE"

    invoke-virtual {p2, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p0

    if-eqz p0, :cond_5

    iget-object p0, p1, Lstub/TempMonitorWindow;->view:Landroid/view/View;

    invoke-static {p0}, Lstub/MonitorPause;->offsetView(Landroid/view/View;)V

    :cond_5
    return-void
.end method

.method public static updateReceive(Lstub/UpdateReceiver;Landroid/content/Context;Landroid/content/Intent;)V
    .registers 4

    if-eqz p2, :cond_3

    invoke-virtual {p2}, Landroid/content/Intent;->getAction()Ljava/lang/String;

    move-result-object p0

    if-eqz p0, :cond_3

    if-nez p1, :cond_0

    goto :goto_1

    :cond_0
    :try_start_0
    invoke-virtual {p2}, Landroid/content/Intent;->getAction()Ljava/lang/String;

    move-result-object p0

    const-string v0, "android.intent.action.MY_PACKAGE_REPLACED"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_2

    const-string v0, "android.intent.action.PACKAGE_REPLACED"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p0

    if-eqz p0, :cond_1

    invoke-virtual {p2}, Landroid/content/Intent;->getData()Landroid/net/Uri;

    move-result-object p0

    if-eqz p0, :cond_1

    invoke-virtual {p2}, Landroid/content/Intent;->getData()Landroid/net/Uri;

    move-result-object p0

    invoke-virtual {p0}, Landroid/net/Uri;->getSchemeSpecificPart()Ljava/lang/String;

    move-result-object p0

    invoke-virtual {p1}, Landroid/content/Context;->getPackageName()Ljava/lang/String;

    move-result-object p1

    invoke-virtual {p0, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p0

    if-eqz p0, :cond_1

    goto :goto_0

    :cond_1
    return-void

    :cond_2
    :goto_0
    const-string p0, "pbc"

    const/4 p1, 0x1

    invoke-static {p0, p1}, Lstub/EncryptedPrefs;->putBoolean(Ljava/lang/String;Z)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    return-void

    :catch_0
    move-exception p0

    invoke-virtual {p0}, Ljava/lang/Exception;->printStackTrace()V

    :cond_3
    :goto_1
    return-void
.end method
