/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.musicolet.misc.pairip

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.RestoredMethod
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.pairip.restoreVirtualizedMethods
import app.morphe.patches.shared.misc.smali.addDefaultConstructor

private const val APP_PACKAGE = "Lin/krosbits/musicolet"
private const val DO_IN_BACKGROUND = "doInBackground"
private const val ON_RECEIVE = "onReceive"

private val RESTORED_METHODS = listOf(
    RestoredMethod(
        "La61;", DO_IN_BACKGROUND, 5,
        """
            iget-object p1, p0, La61;->a:Ljava/util/ArrayList;
            const/4 v0, 0x1
            iput-boolean v0, p0, La61;->c:Z
            const/4 v1, 0x0
            const/4 v2, 0x0
            :next
            invoke-virtual {p1}, Ljava/util/ArrayList;->isEmpty()Z
            move-result v0
            if-nez v0, :drained
            invoke-virtual {p1, v2}, Ljava/util/ArrayList;->remove(I)Ljava/lang/Object;
            move-result-object v0
            check-cast v0, Lz51;
            iput-object v0, p0, La61;->b:Lz51;
            invoke-interface {v0}, Ljava/lang/Runnable;->run()V
            iput-object v1, p0, La61;->b:Lz51;
            goto :next
            :drained
            iput-boolean v2, p0, La61;->c:Z
            return-object v1
        """,
    ),
    RestoredMethod(
        "Lon1;", DO_IN_BACKGROUND, 5,
        """
            iget-object p1, p0, Lon1;->c:Lqn1;
            invoke-virtual {p1}, Landroid/database/sqlite/SQLiteOpenHelper;->getWritableDatabase()Landroid/database/sqlite/SQLiteDatabase;
            move-result-object p1
            new-instance v0, Landroid/content/ContentValues;
            const/4 v1, 0x1
            invoke-direct {v0, v1}, Landroid/content/ContentValues;-><init>(I)V
            iget-wide v1, p0, Lon1;->a:J
            invoke-static {v1, v2}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;
            move-result-object v1
            const-string v2, "COL_LASTPOS"
            invoke-virtual {v0, v2, v1}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/Long;)V
            iget-object p0, p0, Lon1;->b:Ldm0;
            iget-object p0, p0, Ldm0;->c:Lkn1;
            iget-object p0, p0, Lkn1;->f:Lro;
            invoke-virtual {p0}, Ljava/lang/Object;->toString()Ljava/lang/String;
            move-result-object p0
            filled-new-array {p0}, [Ljava/lang/String;
            move-result-object p0
            const-string v1, "TABLE_SONGS"
            const-string v2, "COL_PATH=?"
            invoke-virtual {p1, v1, v0, v2, p0}, Landroid/database/sqlite/SQLiteDatabase;->update(Ljava/lang/String;Landroid/content/ContentValues;Ljava/lang/String;[Ljava/lang/String;)I
            const/4 p0, 0x0
            return-object p0
        """,
    ),
    RestoredMethod(
        "Lnn1;", DO_IN_BACKGROUND, 5,
        """
            iget-object p1, p0, Lnn1;->g:Lqn1;
            invoke-virtual {p1}, Landroid/database/sqlite/SQLiteOpenHelper;->getWritableDatabase()Landroid/database/sqlite/SQLiteDatabase;
            move-result-object p1
            new-instance v0, Landroid/content/ContentValues;
            const/4 v1, 0x5
            invoke-direct {v0, v1}, Landroid/content/ContentValues;-><init>(I)V
            iget-wide v1, p0, Lnn1;->a:J
            invoke-static {v1, v2}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;
            move-result-object v1
            const-string v2, "COL_LAST_PLAYED"
            invoke-virtual {v0, v2, v1}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/Long;)V
            iget v1, p0, Lnn1;->b:I
            invoke-static {v1}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
            move-result-object v1
            const-string v2, "COL_NUM_PLAYED"
            invoke-virtual {v0, v2, v1}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/Integer;)V
            iget v1, p0, Lnn1;->c:I
            invoke-static {v1}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
            move-result-object v1
            const-string v2, "COL_NUM_PLAYED_W"
            invoke-virtual {v0, v2, v1}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/Integer;)V
            iget v1, p0, Lnn1;->d:I
            invoke-static {v1}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
            move-result-object v1
            const-string v2, "COL_NUM_PLAYED_M"
            invoke-virtual {v0, v2, v1}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/Integer;)V
            iget v1, p0, Lnn1;->e:I
            invoke-static {v1}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
            move-result-object v1
            const-string v2, "COL_NUM_PLAYED_Y"
            invoke-virtual {v0, v2, v1}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/Integer;)V
            iget-object p0, p0, Lnn1;->f:Ldm0;
            iget-object p0, p0, Ldm0;->c:Lkn1;
            iget-object p0, p0, Lkn1;->f:Lro;
            invoke-virtual {p0}, Ljava/lang/Object;->toString()Ljava/lang/String;
            move-result-object p0
            filled-new-array {p0}, [Ljava/lang/String;
            move-result-object p0
            const-string v1, "TABLE_SONGS"
            const-string v2, "COL_PATH=?"
            invoke-virtual {p1, v1, v0, v2, p0}, Landroid/database/sqlite/SQLiteDatabase;->update(Ljava/lang/String;Landroid/content/ContentValues;Ljava/lang/String;[Ljava/lang/String;)I
            const/4 p0, 0x0
            return-object p0
        """,
    ),
    RestoredMethod(
        "Lht0;", DO_IN_BACKGROUND, 5,
        """
            iget p1, p0, Lht0;->a:I
            const/4 v0, 0x0
            if-eqz p1, :rescan
            const/4 v1, 0x1
            if-ne p1, v1, :done
            invoke-static {}, Ls71;->i()V
            goto :done
            :rescan
            sput-object p0, $APP_PACKAGE/MediaMountedReceiver;->b:Lht0;
            const-wide/16 v1, 0x1388
            invoke-static {v1, v2}, Ljava/lang/Thread;->sleep(J)V
            invoke-static {}, $APP_PACKAGE/MyApplication;->q()Z
            const-wide/16 v1, 0xfa0
            invoke-static {v1, v2}, Ljava/lang/Thread;->sleep(J)V
            invoke-static {}, $APP_PACKAGE/MyApplication;->q()Z
            sput-object v0, $APP_PACKAGE/MediaMountedReceiver;->b:Lht0;
            :done
            return-object v0
        """,
    ),
    RestoredMethod(
        "Ljm1;", DO_IN_BACKGROUND, 9,
        """
            sget-object v1, $APP_PACKAGE/MyApplication;->i:$APP_PACKAGE/MyApplication;
            invoke-virtual {v1}, Landroid/content/Context;->getApplicationContext()Landroid/content/Context;
            move-result-object v1
            invoke-static {v1}, Ldi0;->m(Landroid/content/Context;)Z
            move-result p1
            const/4 v0, 0x0
            if-nez p1, :supported
            return-object v0
            :supported
            new-instance p1, Landroid/content/Intent;
            iget-object v2, p0, Ljm1;->a:Ljava/lang/String;
            invoke-direct {p1, v2}, Landroid/content/Intent;-><init>(Ljava/lang/String;)V
            const-class v6, $APP_PACKAGE/MusicActivity;
            invoke-virtual {p1, v1, v6}, Landroid/content/Intent;->setClass(Landroid/content/Context;Ljava/lang/Class;)Landroid/content/Intent;
            iget-object v2, p0, Ljm1;->b:Ljava/lang/String;
            const-string v3, "extshjp_"
            invoke-virtual {p1, v3, v2}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;
            new-instance v4, Ljava/lang/StringBuilder;
            invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V
            invoke-virtual {p1}, Landroid/content/Intent;->getAction()Ljava/lang/String;
            move-result-object v2
            invoke-virtual {v4, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
            invoke-virtual {p1, v3}, Landroid/content/Intent;->getStringExtra(Ljava/lang/String;)Ljava/lang/String;
            move-result-object v2
            invoke-virtual {v4, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
            new-instance v2, Lhm1;
            invoke-direct {v2}, Lhm1;-><init>()V
            iput-object v1, v2, Lhm1;->a:Landroid/content/Context;
            invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
            move-result-object v3
            iput-object v3, v2, Lhm1;->b:Ljava/lang/String;
            const/4 v3, 0x1
            new-array v4, v3, [Landroid/content/Intent;
            const/4 v5, 0x0
            aput-object p1, v4, v5
            iput-object v4, v2, Lhm1;->c:[Landroid/content/Intent;
            new-instance p1, Landroid/content/ComponentName;
            invoke-direct {p1, v1, v6}, Landroid/content/ComponentName;-><init>(Landroid/content/Context;Ljava/lang/Class;)V
            iput-object p1, v2, Lhm1;->d:Landroid/content/ComponentName;
            iget-object p0, p0, Ljm1;->c:Ljava/lang/String;
            iput-object p0, v2, Lhm1;->e:Ljava/lang/CharSequence;
            invoke-virtual {v1}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;
            move-result-object p0
            invoke-virtual {v1}, Landroid/content/Context;->getApplicationInfo()Landroid/content/pm/ApplicationInfo;
            move-result-object p1
            iget p1, p1, Landroid/content/pm/ApplicationInfo;->icon:I
            invoke-virtual {p0, p1}, Landroid/content/res/Resources;->getDrawable(I)Landroid/graphics/drawable/Drawable;
            move-result-object p0
            sget-object p1, Landroid/graphics/Bitmap${'$'}Config;->ARGB_8888:Landroid/graphics/Bitmap${'$'}Config;
            const/16 v4, 0xa8
            invoke-static {v4, v4, p1}, Landroid/graphics/Bitmap;->createBitmap(IILandroid/graphics/Bitmap${'$'}Config;)Landroid/graphics/Bitmap;
            move-result-object p1
            invoke-virtual {p0, v5, v5, v4, v4}, Landroid/graphics/drawable/Drawable;->setBounds(IIII)V
            new-instance v4, Landroid/graphics/Canvas;
            invoke-direct {v4, p1}, Landroid/graphics/Canvas;-><init>(Landroid/graphics/Bitmap;)V
            invoke-virtual {p0, v4}, Landroid/graphics/drawable/Drawable;->draw(Landroid/graphics/Canvas;)V
            new-instance p0, Landroidx/core/graphics/drawable/IconCompat;
            invoke-direct {p0, v3}, Landroidx/core/graphics/drawable/IconCompat;-><init>(I)V
            iput-object p1, p0, Landroidx/core/graphics/drawable/IconCompat;->b:Ljava/lang/Object;
            iput-object p0, v2, Lhm1;->f:Landroidx/core/graphics/drawable/IconCompat;
            invoke-static {v1, v2}, Ldi0;->s(Landroid/content/Context;Lhm1;)V
            return-object v0
        """,
    ),
    RestoredMethod(
        "Lkb;", DO_IN_BACKGROUND, 12,
        """
            iget v0, p0, Lkb;->a:I
            if-nez v0, :clear
            iget-object v0, p0, Lkb;->e:Ljava/lang/Object;
            check-cast v0, Lk00;
            invoke-static {}, $APP_PACKAGE/MyApplication;->k()Z
            move-result v1
            if-nez v1, :backup_start
            const/4 v1, 0x1
            sput-boolean v1, $APP_PACKAGE/MyApplication;->R:Z
            sget-object v1, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
            return-object v1
            :backup_start
            iget-boolean v1, p0, Lkb;->c:Z
            invoke-static {v0, v1}, Lmb;->a(Lk00;Z)V
            invoke-static {}, Lmb;->j()V
            :backup_end
            .catch Ljava/lang/Throwable; {:backup_start .. :backup_end} :backup_failed
            const/4 v1, 0x1
            invoke-static {v1}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
            move-result-object v1
            return-object v1
            :backup_failed
            move-exception v1
            invoke-virtual {v1}, Ljava/lang/Throwable;->printStackTrace()V
            :delete_start
            invoke-virtual {v0}, Lk00;->e()Z
            :delete_end
            .catch Ljava/lang/Throwable; {:delete_start .. :delete_end} :backup_discarded
            :backup_discarded
            sget-object v1, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
            return-object v1
            :clear
            iget-object v0, p0, Lkb;->e:Ljava/lang/Object;
            check-cast v0, Ljava/util/List;
            const/4 v1, 0x0
            :clear_start
            const/4 v2, 0x0
            invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
            move-result-object v2
            sget-object v3, $APP_PACKAGE/MyApplication;->j:Lqn1;
            invoke-virtual {v3}, Landroid/database/sqlite/SQLiteOpenHelper;->getWritableDatabase()Landroid/database/sqlite/SQLiteDatabase;
            move-result-object v1
            invoke-virtual {v1}, Landroid/database/sqlite/SQLiteDatabase;->beginTransactionNonExclusive()V
            new-instance v3, Landroid/content/ContentValues;
            const/4 v4, 0x7
            invoke-direct {v3, v4}, Landroid/content/ContentValues;-><init>(I)V
            const-string v4, "COL_NUM_PLAYED"
            invoke-virtual {v3, v4, v2}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/Integer;)V
            const-string v4, "COL_NUM_PLAYED_W"
            invoke-virtual {v3, v4, v2}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/Integer;)V
            const-string v4, "COL_NUM_PLAYED_M"
            invoke-virtual {v3, v4, v2}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/Integer;)V
            const-string v4, "COL_NUM_PLAYED_Y"
            invoke-virtual {v3, v4, v2}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/Integer;)V
            const-string v4, "COL_LAST_PLAYED"
            invoke-virtual {v3, v4, v2}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/Integer;)V
            const-string v4, "COL_LASTPOS"
            invoke-virtual {v3, v4, v2}, Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/Integer;)V
            const/4 v4, 0x1
            new-array v4, v4, [Ljava/lang/String;
            sget-object v5, Lcom/google/android/exoplayer2/video/ZR/pVkdwM;->xiRminNIZti:Ljava/lang/String;
            invoke-interface {v0}, Ljava/util/List;->iterator()Ljava/util/Iterator;
            move-result-object v6
            :update_next
            invoke-interface {v6}, Ljava/util/Iterator;->hasNext()Z
            move-result v7
            if-eqz v7, :updated
            invoke-interface {v6}, Ljava/util/Iterator;->next()Ljava/lang/Object;
            move-result-object v7
            check-cast v7, Ldm0;
            iget-object v7, v7, Ldm0;->c:Lkn1;
            iget-object v7, v7, Lkn1;->f:Lro;
            invoke-static {v7}, Lwf0;->Q0(Ljava/lang/Object;)Ljava/lang/String;
            move-result-object v7
            const/4 v8, 0x0
            aput-object v7, v4, v8
            const-string v7, "TABLE_SONGS"
            invoke-virtual {v1, v7, v3, v5, v4}, Landroid/database/sqlite/SQLiteDatabase;->update(Ljava/lang/String;Landroid/content/ContentValues;Ljava/lang/String;[Ljava/lang/String;)I
            goto :update_next
            :updated
            invoke-virtual {v1}, Landroid/database/sqlite/SQLiteDatabase;->setTransactionSuccessful()V
            invoke-static {v0}, Ld51;->b(Ljava/util/List;)V
            invoke-interface {v0}, Ljava/util/List;->iterator()Ljava/util/Iterator;
            move-result-object v6
            :reset_next
            invoke-interface {v6}, Ljava/util/Iterator;->hasNext()Z
            move-result v7
            if-eqz v7, :reset_done
            invoke-interface {v6}, Ljava/util/Iterator;->next()Ljava/lang/Object;
            move-result-object v7
            check-cast v7, Ldm0;
            const/4 v8, 0x0
            iput v8, v7, Ldm0;->t:I
            iput v8, v7, Ldm0;->r:I
            iput v8, v7, Ldm0;->s:I
            iput v8, v7, Ldm0;->o:I
            const-wide/16 v8, 0x0
            iput-wide v8, v7, Ldm0;->m:J
            iput-wide v8, v7, Ldm0;->n:J
            goto :reset_next
            :reset_done
            invoke-virtual {v1}, Landroid/database/sqlite/SQLiteDatabase;->endTransaction()V
            :clear_end
            .catch Ljava/lang/Throwable; {:clear_start .. :clear_end} :clear_failed
            :cleared
            const v0, 0x7f12018a
            const/4 v1, 0x0
            invoke-static {v0, v1}, Lwf0;->O0(II)V
            const/4 v0, 0x0
            return-object v0
            :clear_failed
            move-exception v2
            invoke-virtual {v2}, Ljava/lang/Throwable;->printStackTrace()V
            if-eqz v1, :cleared
            :end_start
            invoke-virtual {v1}, Landroid/database/sqlite/SQLiteDatabase;->endTransaction()V
            :end_end
            .catch Ljava/lang/Throwable; {:end_start .. :end_end} :end_failed
            goto :cleared
            :end_failed
            move-exception v2
            invoke-virtual {v2}, Ljava/lang/Throwable;->printStackTrace()V
            goto :cleared
        """,
    ),
    RestoredMethod(
        "Lc51;", DO_IN_BACKGROUND, 2,
        """
            const/4 p0, 0x0
            return-object p0
        """,
    ),
    RestoredMethod(
        "Lez0;", DO_IN_BACKGROUND, 2,
        """
            new-instance p1, Ljava/util/HashSet;
            invoke-direct {p1}, Ljava/util/HashSet;-><init>()V
            iput-object p1, p0, Lez0;->c:Ljava/util/HashSet;
            return-object p1
        """,
    ),
    RestoredMethod(
        "$APP_PACKAGE/fh;", ON_RECEIVE, 6,
        """
            new-instance v0, Landroid/content/Intent;
            invoke-direct {v0, p2}, Landroid/content/Intent;-><init>(Landroid/content/Intent;)V
            sget-object v1, $APP_PACKAGE/MyApplication;->i:$APP_PACKAGE/MyApplication;
            invoke-virtual {v1}, Landroid/content/Context;->getApplicationContext()Landroid/content/Context;
            move-result-object v1
            const-string v2, "tclass"
            invoke-virtual {p2, v2}, Landroid/content/Intent;->getStringExtra(Ljava/lang/String;)Ljava/lang/String;
            move-result-object v2
            invoke-static {v2}, Ljava/lang/Class;->forName(Ljava/lang/String;)Ljava/lang/Class;
            move-result-object v2
            invoke-virtual {v0, v1, v2}, Landroid/content/Intent;->setClass(Landroid/content/Context;Ljava/lang/Class;)Landroid/content/Intent;
            const-string v1, "E_IA"
            const/4 v2, 0x0
            invoke-virtual {v0, v1, v2}, Landroid/content/Intent;->getBooleanExtra(Ljava/lang/String;Z)Z
            move-result v1
            if-nez v1, :activity
            invoke-static {p1, v0}, $APP_PACKAGE/fh;->a(Landroid/content/Context;Landroid/content/Intent;)V
            return-void
            :activity
            const v1, 0x50800000
            invoke-virtual {v0, v1}, Landroid/content/Intent;->addFlags(I)Landroid/content/Intent;
            invoke-virtual {p1, v0}, Landroid/content/Context;->startActivity(Landroid/content/Intent;)V
            return-void
        """,
    ),
    RestoredMethod(
        "$APP_PACKAGE/RemoteMediaControlReceiver;", ON_RECEIVE, 3,
        """
            invoke-static {p1, p2}, $APP_PACKAGE/RemoteMediaControlReceiver;->a(Landroid/content/Context;Landroid/content/Intent;)V
            return-void
        """,
    ),
    RestoredMethod(
        "$APP_PACKAGE/MediaMountedReceiver;", ON_RECEIVE, 3,
        """
            new-instance p0, Ljava/lang/StringBuilder;
            const-string p1, "MMR OR "
            invoke-direct {p0, p1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V
            invoke-virtual {p0, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;
            invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
            move-result-object p0
            invoke-static {p0}, Lv82;->y(Ljava/lang/String;)V
            invoke-static {}, $APP_PACKAGE/MediaMountedReceiver;->a()V
            return-void
        """,
    ),
)

val restoreVirtualizedMethodsPatch = bytecodePatch {
    compatibleWith(AppCompatibilities.MUSICOLET)

    dependsOn(removePairipVirtualizationPatch)

    execute {
        restoreVirtualizedMethods(RESTORED_METHODS)
        mutableClassDefBy("Lhm1;").addDefaultConstructor()
    }
}
