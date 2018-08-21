package co.smartreceipts.android.autocomplete.distance

import co.smartreceipts.android.autocomplete.AutoCompleteField
import co.smartreceipts.android.model.Distance
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.*
import org.junit.Before

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DistanceAutoCompleteResultsCheckerTest {

    companion object {
        private const val LOCATION = "location"
    }

    private val resultsChecker: DistanceAutoCompleteResultsChecker = DistanceAutoCompleteResultsChecker()

    @Mock
    private lateinit var distance: Distance

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        whenever(distance.location).thenReturn(LOCATION)
    }

    @Test
    fun matchesInput() {
        assertTrue(resultsChecker.matchesInput("l", DistanceAutoCompleteField.Location, distance))
        assertTrue(resultsChecker.matchesInput("lo", DistanceAutoCompleteField.Location, distance))
        assertTrue(resultsChecker.matchesInput("loc", DistanceAutoCompleteField.Location, distance))
        assertTrue(resultsChecker.matchesInput("loca", DistanceAutoCompleteField.Location, distance))
        assertTrue(resultsChecker.matchesInput("locat", DistanceAutoCompleteField.Location, distance))
        assertTrue(resultsChecker.matchesInput("locati", DistanceAutoCompleteField.Location, distance))
        assertTrue(resultsChecker.matchesInput("locatio", DistanceAutoCompleteField.Location, distance))
        assertTrue(resultsChecker.matchesInput("location", DistanceAutoCompleteField.Location, distance))

        assertFalse(resultsChecker.matchesInput("comment", DistanceAutoCompleteField.Location, distance))
        assertFalse(resultsChecker.matchesInput("name", DistanceAutoCompleteField.Location, distance))
        assertFalse(resultsChecker.matchesInput("location", mock(AutoCompleteField::class.java), distance))
    }

    @Test
    fun getValue() {
        assertEquals(LOCATION, resultsChecker.getValue(DistanceAutoCompleteField.Location, distance))
    }

    @Test(expected = IllegalArgumentException::class)
    fun getValueForUnknownField() {
        resultsChecker.getValue(mock(AutoCompleteField::class.java), distance)
    }
}