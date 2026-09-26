package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.CiploxPhaseCalculator
import com.example.util.EyeMedTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("EyeMed", appName)
    }

    @Test
    fun `ciplox-d phase 1 calculation matches discharge summary`() {
        val phase1Date = LocalDate.parse("2026-09-28")
        val phase1 = CiploxPhaseCalculator.getPhaseForDate(phase1Date)
        assertNotNull(phase1)
        assertEquals(1, phase1?.phaseNumber)
        assertEquals(listOf("07:00", "10:00", "13:00", "16:00", "19:00", "22:00"), phase1?.defaultTimes)
    }

    @Test
    fun `ciplox-d phase 5 once daily matches discharge summary`() {
        val phase5Date = LocalDate.parse("2026-10-26")
        val phase5 = CiploxPhaseCalculator.getPhaseForDate(phase5Date)
        assertNotNull(phase5)
        assertEquals(5, phase5?.phaseNumber)
        assertEquals(listOf("19:00"), phase5?.defaultTimes)
    }

    @Test
    fun `dhaka timezone is UTC plus 6`() {
        val zone = EyeMedTime.DHAKA_ZONE
        assertEquals("Asia/Dhaka", zone.id)
    }
}
