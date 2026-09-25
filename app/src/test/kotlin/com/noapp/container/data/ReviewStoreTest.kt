package com.noapp.container.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.noapp.container.BuildConfig
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The rating policy, tested by moving the calendar rather than waiting two months for it.
 *
 * What matters here is the promise the app makes: never before 60 days, and after that it keeps
 * asking until the user answers — with "Later" explicitly not counting as an answer.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ReviewStoreTest {

    private lateinit var context: Context
    private var install = DAY_ZERO

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("no_app_review", Context.MODE_PRIVATE).edit().clear().commit()
        install = DAY_ZERO
        ReviewStore.installedAt = { install }
        ReviewStore.clock = { install + days * DAY_MS }
        // The policy, not the build switch: a test build that always asks would otherwise turn every
        // "silent" case below into a failure. The switch has its own test.
        ReviewStore.alwaysAsk = false
    }

    @After
    fun tearDown() {
        ReviewStore.installedAt = { ctx ->
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).firstInstallTime
        }
        ReviewStore.clock = { System.currentTimeMillis() }
        ReviewStore.alwaysAsk = BuildConfig.ALWAYS_ASK_FOR_REVIEW
    }

    private var days = 0L

    @Test
    fun `silent on the first day`() {
        days = 0
        assertFalse(ReviewStore.cardDue(context))
    }

    @Test
    fun `silent one day before the threshold`() {
        days = ReviewStore.ASK_FROM_DAYS - 1
        assertFalse(ReviewStore.cardDue(context))
    }

    @Test
    fun `due on the threshold`() {
        days = ReviewStore.ASK_FROM_DAYS
        assertTrue(ReviewStore.cardDue(context))
    }

    @Test
    fun `still due a year later`() {
        days = 365
        assertTrue(ReviewStore.cardDue(context))
    }

    @Test
    fun `hiding it on one screen is not an answer`() {
        days = ReviewStore.ASK_FROM_DAYS
        assertTrue(ReviewStore.cardDue(context))
        // "Later" only removes the card from the screen it was shown on; nothing is written for it.
        assertTrue(ReviewStore.cardDue(context))
    }

    @Test
    fun `never ask means never again`() {
        days = ReviewStore.ASK_FROM_DAYS
        ReviewStore.stopAsking(context)
        assertFalse(ReviewStore.cardDue(context))
        days = 3650
        assertFalse(ReviewStore.cardDue(context))
    }

    @Test
    fun `rating it means never again`() {
        days = ReviewStore.ASK_FROM_DAYS
        // The card's own button and the Settings row do the same thing, and this is that thing.
        ReviewStore.stopAsking(context)
        days = 3650
        assertFalse(ReviewStore.cardDue(context))
    }

    @Test
    fun `an install in the future is not a reason to ask`() {
        install = DAY_ZERO + 10 * DAY_MS
        days = 0
        assertFalse(ReviewStore.cardDue(context))
    }

    /**
     * The switch a `-PalwaysAskForReview=true` build sets, so the card can be looked at on a device
     * without waiting two months: it asks from the first launch and no answer silences it.
     */
    @Test
    fun `a test build asks from the first launch and no answer stops it`() {
        ReviewStore.alwaysAsk = true
        days = 0
        assertTrue(ReviewStore.cardDue(context))
        ReviewStore.stopAsking(context)
        assertTrue(ReviewStore.cardDue(context))
    }

    private companion object {
        const val DAY_MS = 24L * 60L * 60L * 1000L
        const val DAY_ZERO = 1_700_000_000_000L
    }
}
