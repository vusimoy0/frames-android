package com.checkout.sdk.billingdetails

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import com.checkout.sdk.R
import com.checkout.sdk.input.CountryInput
import com.checkout.sdk.input.PhoneInput
import com.checkout.sdk.store.DataStore
import com.checkout.sdk.store.InMemoryStore
import com.checkout.sdk.utils.PhoneUtils
import kotlinx.android.synthetic.main.billing_details.view.*
import java.util.*

/**
 * The controller of the billing details view page
 *
 *
 * This class handles interaction with the custom inputs in the billing details form.
 * The state of the view is handled here, so are action like focus changes, full form
 * validation, listeners, persistence over orientation.
 */
class BillingDetailsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    /**
     * The callback is used to communicate with the country input
     *
     *
     * The custom [CountryInput] takes care of populating the values in the spinner
     * and will trigger this callback when the user selects a new option. State is update
     * accordingly. Moreover, the phone prefix is added bade on the country selected.
     */
    private val mCountryListener = CountryInput.CountryListener { country, prefix ->
        if (country != "") {
            dataStore.customerCountry = country
        }
        if (prefix != "") {
            dataStore.customerPhonePrefix = prefix
        }
        phone_input.setText(prefix + " " + dataStore.customerPhone)
        phone_input.requestFocus()
        phone_input.performClick()
        phone_input.setSelection(phone_input!!.text.length)
    }

    /**
     * The callback is used to communicate with the phone input
     *
     *
     * The custom [PhoneInput] takes care takes care of the validation and it uses a callback
     * to indicate this controller if there is any error or if the error state needs to
     * be cleared. State is also updates based on the outcome of the input.
     */
    private val mPhoneListener = object : PhoneInput.PhoneListener {
        override fun onPhoneInputFinish(phone: String) {
            dataStore
                .customerPhone = phone.replace(dataStore.customerPhonePrefix, "")
                .replace("\\D".toRegex(), "")
        }

        override fun clearPhoneError() {
            phone_input_layout.error = null
            phone_input_layout.isErrorEnabled = false
        }
    }

    private var mListener: BillingDetailsView.Listener? = null
    private val dataStore: DataStore = DataStore.Factory.get()

    /**
     * Used to indicate the validity of the billing details from
     *
     *
     * The method will check if the inputs are valid.
     * This method will also populate the field error accordingly
     *
     * @return boolean abut form validity
     */
    private val isValidForm: Boolean
        get() {
            var result = true

            val inMemoryStore = InMemoryStore.Factory.get()
            if (!inMemoryStore.customerName.isValid()) {
                result = false
            }

            // TODO: validate inMemoryStore.billingAddress

            if (country_input.selectedItemPosition == 0) {
                (country_input.selectedView as TextView).error =
                        resources.getString(R.string.error_country)
                result = false
            }

            if (phone_input.length() < 3) {
                phone_input_layout.error = resources.getString(R.string.error_phone)
                result = false
            }

            return result
        }

    /**
     * The callback used to indicate is the billing details were completed
     *
     *
     * After the user completes their details and the form is valid this callback will
     * be used to communicate to the parent that teh focus needs to change
     */
    interface Listener {
        fun onBillingCompleted()

        fun onBillingCanceled()
    }

    init {
        inflate(this.context, R.layout.billing_details, this)
        orientation = VERTICAL
        isFocusableInTouchMode = true
        my_toolbar.setNavigationOnClickListener {
            if (dataStore.lastBillingValidState != null) {
                dataStore.customerName = dataStore.lastCustomerNameState!!
                dataStore.customerAddress1 = dataStore.lastBillingValidState!!.addressOne.value
                dataStore.customerAddress2 = dataStore.lastBillingValidState!!.addressTwo.value
                dataStore.customerZipcode = dataStore.lastBillingValidState!!.postcode.value
                dataStore.customerCountry = dataStore.lastBillingValidState!!.country
                dataStore.customerCity = dataStore.lastBillingValidState!!.city.value
                dataStore.customerState = dataStore.lastBillingValidState!!.state.value
                dataStore.customerPhonePrefix = dataStore.lastBillingValidState!!.phone.countryCode
                dataStore.customerPhone = dataStore.lastBillingValidState!!.phone.number
                repopulateFields()
                mListener?.onBillingCompleted()
            } else {
                mListener?.onBillingCanceled()
            }
        }
        country_input.setCountryListener(mCountryListener)
        phone_input.setPhoneListener(mPhoneListener)
        repopulateFields()
        clear_button.setOnClickListener {
            name_input.reset()
            if (dataStore.defaultCountry != null) {
                country_input.setSelection(
                    (country_input.adapter as ArrayAdapter<String>)
                        .getPosition(dataStore.defaultCountry!!.displayCountry)
                )
                dataStore.customerCountry = dataStore.defaultCountry!!.country
                dataStore.customerPhonePrefix = PhoneUtils.getPrefix(
                    dataStore.defaultCountry!!.country)
            } else {
                country_input.setSelection(0)
            }
            (country_input.selectedView as TextView).error = null
            if (dataStore.defaultCountry != null) {
                phone_input.setText(PhoneUtils.getPrefix(dataStore.defaultCountry!!.country) + " ")
            } else {
                phone_input.setText("")
            }
            phone_input_layout.error = null
            phone_input_layout.isErrorEnabled = false
            dataStore.cleanBillingData()
            mListener?.onBillingCanceled()
            dataStore.isBillingCompleted = false
        }
        done_button.setOnClickListener {
            if (isValidForm) {
                // TODO: Use InMemoryStore instead
//                    dataStore.isBillingCompleted = true
//                    dataStore.lastCustomerNameState = dataStore.customerName
//                    dataStore.lastBillingValidState = BillingModel(
//                        dataStore.customerAddress1,
//                        dataStore.customerAddress2,
//                        dataStore.customerZipcode,
//                        dataStore.customerCountry,
//                        dataStore.customerCity,
//                        dataStore.customerState,
//                        PhoneModel(
//                            dataStore.customerPhonePrefix,
//                            dataStore.customerPhone
//                        )
//                    )
//                    mListener?.onBillingCompleted()
            }
        }
        requestFocus()
        if (dataStore.phoneLabel != null) {
            phone_input_layout.hint = dataStore.phoneLabel
        }
    }

    /**
     * Used to restore state on orientation changes
     *
     *
     * The method will repopulate all the card input fields with the latest state they were in
     * if the device orientation changes, and therefore avoiding the text inputs to be cleared.
     */
    private fun repopulateFields() {
        // Repopulate country
        val locale = Locale.getAvailableLocales()
        var country: String

        for (loc in locale) {
            country = loc.displayCountry
            if (loc.country == dataStore.customerCountry) {
                country_input.setSelection(
                    (country_input.adapter as ArrayAdapter<String>).getPosition(country)
                )
            }
        }

        // Repopulate phone
        phone_input.setText(dataStore.customerPhone)
    }

    /**
     * Used to clear the text and state of the fields
     */
    fun resetFields() {
        name_input.reset()
        address_one_input.reset()
        address_two_input.reset()
        city_input.reset()
        state_input.reset()

        // Repopulate country
        if (dataStore.defaultCountry != null) {
            country_input.setSelection(
                (country_input.adapter as ArrayAdapter<String>)
                    .getPosition(dataStore.defaultCountry!!.displayCountry)
            )
            dataStore.customerCountry = dataStore.defaultCountry!!.country
            dataStore.customerPhonePrefix = PhoneUtils.getPrefix(
                dataStore.defaultCountry!!.country
            )
        } else {
            country_input.setSelection(0)
        }

        if (dataStore.defaultBillingDetails != null &&
            dataStore.defaultCountry != null &&
            dataStore.customerPhone != null
        ) {
            phone_input.setText(
                PhoneUtils.getPrefix(dataStore.defaultCountry!!.country) +
                        " " + dataStore.customerPhone
            )
            phone_input_layout.error = null
            phone_input_layout.isErrorEnabled = false
        } else {
            // Reset phone prefix
            if (dataStore.defaultCountry != null) {
                phone_input.setText(PhoneUtils.getPrefix(dataStore.defaultCountry!!.country) + " ")
            } else {
                phone_input.setText("")
            }
            (country_input.selectedView as TextView).error = null
            phone_input_layout.error = null
            phone_input_layout.isErrorEnabled = false
        }
    }

    // Move to previous view on back button pressed
    override fun dispatchKeyEventPreIme(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) {
            return false
        }
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            // Prevent back button to trigger the mListener is any is focused
            if (mListener != null &&
                !address_one_input.hasFocus() &&
                !name_input.hasFocus() &&
                !address_two_input.hasFocus() &&
                !city_input.hasFocus() &&
                !state_input.hasFocus() &&
                !zipcode_input.hasFocus() &&
                !phone_input.hasFocus()
            ) {
                if (dataStore.lastBillingValidState != null) {
                    dataStore.customerName = dataStore.lastCustomerNameState!!
                    dataStore.customerAddress1 = dataStore.lastBillingValidState!!.addressOne.value
                    dataStore.customerAddress2 = dataStore.lastBillingValidState!!.addressTwo.value
                    dataStore.customerZipcode = dataStore.lastBillingValidState!!.postcode.value
                    dataStore.customerCountry = dataStore.lastBillingValidState!!.country
                    dataStore.customerCity = dataStore.lastBillingValidState!!.city.value
                    dataStore.customerState = dataStore.lastBillingValidState!!.state.value
                    dataStore.customerPhonePrefix =
                            dataStore.lastBillingValidState!!.phone.countryCode
                    dataStore.customerPhone = dataStore.lastBillingValidState!!.phone.number
                    repopulateFields()
                    mListener?.onBillingCompleted()
                } else {
                    mListener?.onBillingCanceled()
                }
                return true
            } else {
                requestFocus()
                return false
            }
        }

        return super.dispatchKeyEventPreIme(event)
    }

    /**
     * Used to set the callback listener for when the card details page is requested
     */
    fun setGoToCardDetailsListener(listener: BillingDetailsView.Listener) {
        mListener = listener
    }
}
