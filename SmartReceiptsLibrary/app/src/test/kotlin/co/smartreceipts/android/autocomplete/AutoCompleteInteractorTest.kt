package co.smartreceipts.android.autocomplete

import co.smartreceipts.android.persistence.database.controllers.TableController
import co.smartreceipts.android.settings.UserPreferenceManager
import co.smartreceipts.android.settings.catalog.UserPreference
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.junit.Assert.*

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AutoCompleteInteractorTest {

    // Class under test
    private lateinit var interactor: AutoCompleteInteractor<Any>

    @Mock
    private lateinit var provider: AutoCompletionProvider<Any>

    @Mock
    private lateinit var resultsChecker: AutoCompleteResultsChecker<Any>

    @Mock
    private lateinit var userPreferenceManager: UserPreferenceManager

    @Mock
    private lateinit var autoCompleteField: AutoCompleteField

    @Mock
    private lateinit var tableController: TableController<Any>

    @Mock
    private lateinit var matchingResult1: Any

    @Mock
    private lateinit var matchingResult2: Any

    @Mock
    private lateinit var nonMatchingResult: Any

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        whenever(provider.tableController).thenReturn(tableController)
        whenever(tableController.get()).thenReturn(Single.just(listOf(matchingResult1, matchingResult2, nonMatchingResult)))
        whenever(userPreferenceManager.get(UserPreference.Receipts.EnableAutoCompleteSuggestions)).thenReturn(true)
        whenever(resultsChecker.matchesInput(any(), eq(autoCompleteField), eq(matchingResult1))).thenReturn(true)
        whenever(resultsChecker.matchesInput(any(), eq(autoCompleteField), eq(matchingResult2))).thenReturn(true)
        whenever(resultsChecker.matchesInput(any(), eq(autoCompleteField), eq(nonMatchingResult))).thenReturn(false)
        interactor = AutoCompleteInteractor(provider, resultsChecker, userPreferenceManager, Schedulers.trampoline())
    }

    @Test
    fun getAutoCompleteResultsWhenPreferenceIsDisabled() {
        whenever(userPreferenceManager.get(UserPreference.Receipts.EnableAutoCompleteSuggestions)).thenReturn(false)
        interactor.getAutoCompleteResults(autoCompleteField, "T")
                .test()
                .assertNoValues()
                .assertNoErrors()
                .assertComplete()
    }

    @Test
    fun getAutoCompleteResultsWhenNoCharactersAreTyped() {
        interactor.getAutoCompleteResults(autoCompleteField, "")
                .test()
                .assertNoValues()
                .assertNoErrors()
                .assertComplete()
    }

    @Test
    fun getAutoCompleteResultsWhenMultipleCharactersAreTyped() {
        interactor.getAutoCompleteResults(autoCompleteField, "Test")
                .test()
                .assertNoValues()
                .assertNoErrors()
                .assertComplete()
    }

    @Test
    fun getAutoCompleteResultsWhenASingleCharacterIsTypedAndResultsHaveDifferentNames() {
        whenever(resultsChecker.getValue(autoCompleteField, matchingResult1)).thenReturn("Test")
        whenever(resultsChecker.getValue(autoCompleteField, matchingResult2)).thenReturn("Test2")
        interactor.getAutoCompleteResults(autoCompleteField, "T")
                .test()
                .assertValues(listOf(AutoCompleteResult("Test", matchingResult1), AutoCompleteResult("Test2", matchingResult2)))
                .assertNoErrors()
                .assertComplete()
    }

    @Test
    fun getAutoCompleteResultsWhenASingleCharacterIsTypedButResultsHaveTheSameNames() {
        whenever(resultsChecker.getValue(autoCompleteField, matchingResult1)).thenReturn("Test")
        whenever(resultsChecker.getValue(autoCompleteField, matchingResult2)).thenReturn("Test")
        interactor.getAutoCompleteResults(autoCompleteField, "T")
                .test()
                .assertValues(listOf(AutoCompleteResult("Test", matchingResult1)))
                .assertNoErrors()
                .assertComplete()
    }
}