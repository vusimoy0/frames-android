package com.checkout.sdk.yearinput

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import com.checkout.sdk.R
import com.checkout.sdk.architecture.MvpView
import com.checkout.sdk.architecture.PresenterStore
import com.checkout.sdk.store.InMemoryStore

/**
 * A custom Spinner with handling of card expiration year input
 */
class YearInput(internal var mContext: Context, attrs: AttributeSet? = null) :
    android.support.v7.widget.AppCompatSpinner(mContext, attrs),
    MvpView<YearInputUiState> {

    private val store = InMemoryStore.Factory.get()
    private lateinit var presenter: YearInputPresenter

    override fun onStateUpdated(uiState: YearInputUiState) {
        if (adapter == null) {
            val dataAdapter = ArrayAdapter(
                mContext,
                android.R.layout.simple_spinner_item, uiState.years
            )
            dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            adapter = dataAdapter
        }
        if (uiState.position != selectedItemPosition) {
            setSelection(uiState.position)
        }
        if (selectedView != null && selectedView is TextView) {
            if (uiState.showError) {
                (selectedView as TextView).error = context.getString(R.string.error_expiration_date)
            } else {
                (selectedView as TextView).error = null
            }
        }
    }

    /**
     * The UI initialisation
     * <p>
     * Used to initialise element as well as setting up appropriate listeners
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // Create/get and start the presenter
        presenter = PresenterStore.getOrCreateDefault(YearInputPresenter::class.java)
        presenter.start(this)

        onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                performClick()
                val imm =
                    mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
            }
        }

        onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val useCaseBuilder = YearSelectedUseCase.Builder(store, position)
                presenter.yearSelected(useCaseBuilder)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }

        // Remove extra padding left
        setPadding(0, this.paddingTop, this.paddingRight, this.paddingBottom)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        presenter.stop()
    }

    /**
     * Resets the values for the MonthInput view
     */
    fun reset() {
        val yearResetUseCase = YearResetUseCase(store)
        presenter.reset(yearResetUseCase)
    }

    fun showError(show: Boolean) {
        presenter.showError(show)
    }
}
