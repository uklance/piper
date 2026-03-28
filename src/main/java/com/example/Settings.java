package com.example;

import com.example.glue.MemberAccess;

import java.text.DecimalFormatSymbols;
import java.util.Locale;

public interface Settings {
    Locale getLocale();

    DecimalFormatSymbols getDecimalFormatSymbols();

    MemberAccess getMemberAccess();
}
