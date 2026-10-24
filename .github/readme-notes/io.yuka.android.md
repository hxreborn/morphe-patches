**Sign-in token** needs an App Check token from Yuka installed from Google Play on a rooted phone. Yuka uses
the token only to sign in. You need a new one only if you sign out or clear Yuka's data. A root mount install
signs in without a token.

1. On the rooted phone, open Yuka, go to the sign-in screen, enter any email address and tap **Continue**. You
   don't need to enter a password.
2. In [Termux](https://f-droid.org/packages/com.termux), run this command and allow root. It prints one long
   line starting with `eyJ`.

   ```sh
   su --mount-master -c "grep -o 'eyJ[A-Za-z0-9_.-]*' /data/data/io.yuka.android/shared_prefs/com.google.firebase.appcheck.store.*.xml"
   ```

   `No such file or directory` means Yuka hasn't fetched a token yet. Repeat step 1, then run the command again.

   With [Termux:API](https://f-droid.org/packages/com.termux.api) and `pkg install termux-api`, add
   `| termux-clipboard-set` to copy the token straight to the clipboard.

3. Paste the line into the **App Check token** option in Morphe Manager, then patch, install and sign in on
   the same day.
