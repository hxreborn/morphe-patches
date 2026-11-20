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
package app.hxreborn.extension.viber;

import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28, shadows = ChatPaymentsTest.EnabledSettings.class,
        instrumentedPackages = "app.hxreborn.extension.viber")
public class ChatPaymentsTest {

    private enum Attachment {

        RAKUTEN_BANK, VIBER_PAY, GALLERY, CAMERA, FILE

    }

    @After
    public void resetSetting() {
        EnabledSettings.enabled = true;
    }

    @Test
    public void retainOrdinaryAttachmentsInOrder() {
        List<Attachment> original = Arrays.asList(Attachment.RAKUTEN_BANK, Attachment.GALLERY, Attachment.VIBER_PAY,
                Attachment.CAMERA, Attachment.FILE);
        Assert.assertEquals(Arrays.asList(Attachment.GALLERY, Attachment.CAMERA, Attachment.FILE),
                ChatPayments.filterAttachments(original, Attachment.RAKUTEN_BANK, Attachment.VIBER_PAY));
        Assert.assertEquals(5, original.size());
        Assert.assertFalse(ChatPayments.showSendMoney(true));
    }

    @Test
    public void preserveHostBehaviorWhenDisabled() {
        EnabledSettings.enabled = false;
        List<Attachment> original = Arrays.asList(Attachment.VIBER_PAY, Attachment.FILE);
        Assert.assertSame(original,
                ChatPayments.filterAttachments(original, Attachment.RAKUTEN_BANK, Attachment.VIBER_PAY));
        Assert.assertTrue(ChatPayments.showSendMoney(true));
        Assert.assertFalse(ChatPayments.showSendMoney(false));
    }

    @Test
    public void retainEqualNamedItemsAndNullInput() {
        Assert.assertNull(ChatPayments.filterAttachments(null, Attachment.RAKUTEN_BANK, Attachment.VIBER_PAY));
        List<Object> original = Arrays.asList("VIBER_PAY", null, Attachment.FILE);
        Assert.assertEquals(original,
                ChatPayments.filterAttachments(original, Attachment.RAKUTEN_BANK, Attachment.VIBER_PAY));
    }

    @Implements(value = PatchSettings.class, isInAndroidSdk = false)
    public static class EnabledSettings {

        static boolean enabled = true;

        @Implementation
        public static boolean removeChatPayments() {
            return enabled;
        }

    }

}
