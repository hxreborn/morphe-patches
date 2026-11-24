**Google Drive backup and restore** need **GmsCore support**. Without it, Google refuses Drive access to the patched
Viber, and the backup and restore error dialogs say "Google Drive backup and restore need the GmsCore support patch."

1. Install a microG build that uses the `app.revanced.android.gms` package and sign in to the Google account that
   holds the backup. [MicroG RE](https://github.com/MorpheApp/MicroG-RE/releases/latest), Morphe's own build, is
   recommended.
2. In Morphe Manager, turn on Settings → Patching → Expert mode, enable **GmsCore support** in Viber's patch list,
   then patch and install over the current copy. Manager keeps this selection for later repatches while expert mode
   is on.
3. In Viber, open Settings → Account → Viber Backup, tap the account row and pick the account that holds the
   backup.

Viber offers restore right after activation on the primary device, the phone registered with the Viber number.

Questions about Drive backup and restore go to [Q&A #1052](https://github.com/hxreborn/morphe-patches/discussions/1052).
