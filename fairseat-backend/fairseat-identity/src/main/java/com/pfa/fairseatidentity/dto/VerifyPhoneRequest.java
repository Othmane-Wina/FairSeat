package com.pfa.fairseatidentity.dto;

// Java Record = classe immuable avec getters automatiques
// phone() remplace getPhone()
public record VerifyPhoneRequest(String phone) {}