/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.imo;

import java.util.concurrent.TimeUnit;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

@SuppressWarnings({ "unused", "deprecation" })
public final class AccountChooser {

    private static final String FRAGMENT_TAG = "hx_account_chooser";

    private static final int REQUEST_CODE = 0x6a11;

    private AccountChooser() {

    }

    public static AccountManagerFuture<Bundle> chooseAccount(AccountManager accountManager, String accountType,
            String authTokenType, String[] requiredFeatures, Bundle options, Activity activity,
            AccountManagerCallback<Bundle> callback, Handler handler) {
        ChooserFuture future = new ChooserFuture(callback, handler);
        String accountName = (options != null) ? options.getString(AccountManager.KEY_ACCOUNT_NAME) : null;
        Account selectedAccount = (accountName != null) ? new Account(accountName, accountType) : null;
        Intent intent = AccountManager.newChooseAccountIntent(selectedAccount, null, new String[] { accountType }, null,
                null, null, null);
        ResultFragment fragment = new ResultFragment();
        fragment.future = future;
        fragment.intent = intent;
        activity.getFragmentManager().beginTransaction().add(fragment, FRAGMENT_TAG).commitAllowingStateLoss();
        return future;
    }

    public static final class ResultFragment extends Fragment {

        private ChooserFuture future;

        private Intent intent;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (this.intent == null) {
                remove();
                return;
            }
            startActivityForResult(this.intent, REQUEST_CODE);
            this.intent = null;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode != REQUEST_CODE) {
                return;
            }
            if (this.future != null) {
                this.future.complete((resultCode == Activity.RESULT_OK && data != null) ? data.getExtras() : null);
            }
            remove();
        }

        private void remove() {
            getFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
        }

    }

    private static final class ChooserFuture implements AccountManagerFuture<Bundle> {

        private final AccountManagerCallback<Bundle> callback;

        private final Handler handler;

        private Bundle result;

        private boolean done;

        private boolean cancelled;

        ChooserFuture(AccountManagerCallback<Bundle> callback, Handler handler) {
            this.callback = callback;
            this.handler = (handler != null) ? handler : new Handler(Looper.getMainLooper());
        }

        synchronized void complete(Bundle result) {
            if (this.done) {
                return;
            }
            this.result = result;
            this.cancelled = (result == null);
            this.done = true;
            notifyAll();
            if (this.callback != null) {
                this.handler.post(() -> this.callback.run(this));
            }
        }

        @Override
        public synchronized boolean cancel(boolean mayInterruptIfRunning) {
            if (this.done) {
                return false;
            }
            this.cancelled = true;
            this.done = true;
            notifyAll();
            return true;
        }

        @Override
        public synchronized boolean isCancelled() {
            return this.cancelled;
        }

        @Override
        public synchronized boolean isDone() {
            return this.done;
        }

        @Override
        public Bundle getResult() throws OperationCanceledException {
            return getResult(0, TimeUnit.MILLISECONDS);
        }

        @Override
        public synchronized Bundle getResult(long timeout, TimeUnit unit) throws OperationCanceledException {
            long deadline = (timeout > 0) ? SystemClock.uptimeMillis() + unit.toMillis(timeout) : 0;
            while (!this.done) {
                long remaining = (deadline > 0) ? deadline - SystemClock.uptimeMillis() : 0;
                if (deadline > 0 && remaining <= 0) {
                    throw new OperationCanceledException("No account chosen within " + timeout + " " + unit);
                }
                try {
                    wait(remaining);
                } catch (InterruptedException ex) {
                    throw new OperationCanceledException(ex);
                }
            }
            if (this.cancelled) {
                throw new OperationCanceledException();
            }
            return this.result;
        }

    }

}
