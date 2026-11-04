package app.hxreborn.extension.patreon;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

public final class FeatureFlagsTest {

    @Test
    public void unpatchedBundleForcesNoFlagOff() {
        assertFalse(FeatureFlags.isForcedOff(Flag.SOCIAL_PROOF_ENABLED));
    }

    private enum Flag {

        SOCIAL_PROOF_ENABLED

    }

}
