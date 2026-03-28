package com.example;

import com.example.glue.MemberAccess;

import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class DefaultSettings implements Settings {
    private Locale locale;
    private DecimalFormatSymbols decimalFormatSymbols;
    private MemberAccess memberAccess;

    @Override
    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    @Override
    public DecimalFormatSymbols getDecimalFormatSymbols() {
        return decimalFormatSymbols;
    }

    public void setDecimalFormatSymbols(DecimalFormatSymbols decimalFormatSymbols) {
        this.decimalFormatSymbols = decimalFormatSymbols;
    }

    @Override
    public MemberAccess getMemberAccess() {
        return memberAccess;
    }

    public void setMemberAccess(MemberAccess memberAccess) {
        this.memberAccess = memberAccess;
    }
}
