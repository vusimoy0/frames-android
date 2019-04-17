package com.checkout.sdk.request;

import com.checkout.sdk.billingdetails.NetworkBillingModel;

/**
 * The request model object for the card tokenisation request
 */
public class CardTokenisationRequest {

    private String number;
    private String name;
    private String expiryMonth;
    private String expiryYear;
    private String cvv;

    private NetworkBillingModel billingModel;

    public CardTokenisationRequest(String number, String name, String expiryMonth, String expiryYear, String cvv, NetworkBillingModel billingModel) {
        this.number = number;
        this.name = name;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.cvv = cvv;
        this.billingModel = billingModel;
    }

    public CardTokenisationRequest(String number, String name, String expiryMonth, String expiryYear, String cvv) {
        this.number = number;
        this.name = name;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.cvv = cvv;
    }
}
