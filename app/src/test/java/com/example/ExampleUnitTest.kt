package com.example

import com.example.data.model.Dywan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testValidKmNumber() {
        assertTrue(Dywan.isValidKm("45657894"))
        assertTrue(Dywan.isValidKm("12345678"))
        assertFalse(Dywan.isValidKm("123"))
        assertFalse(Dywan.isValidKm("1234567"))
        assertFalse(Dywan.isValidKm("123456789"))
        assertFalse(Dywan.isValidKm("ABC45678"))
    }

    @Test
    fun testValidMiejsceFormat() {
        assertTrue(Dywan.isValidMiejsce("1A"))
        assertTrue(Dywan.isValidMiejsce("1B"))
        assertTrue(Dywan.isValidMiejsce("23A"))
        assertTrue(Dywan.isValidMiejsce("23B"))
        assertFalse(Dywan.isValidMiejsce("23C"))
        assertFalse(Dywan.isValidMiejsce("A23"))
        assertFalse(Dywan.isValidMiejsce("23"))
        assertFalse(Dywan.isValidMiejsce("23AB"))
    }

    @Test
    fun testDetectIdentifierType() {
        assertEquals(com.example.data.model.IdentifierType.LEROY_KM, Dywan.detectIdentifierType("45657894"))
        assertEquals(com.example.data.model.IdentifierType.LEROY_KM, Dywan.detectIdentifierType("12345678"))
        assertEquals(com.example.data.model.IdentifierType.EAN, Dywan.detectIdentifierType("5901234567890"))
        assertEquals(com.example.data.model.IdentifierType.EAN, Dywan.detectIdentifierType("05901234567890"))
        assertEquals(com.example.data.model.IdentifierType.UNKNOWN, Dywan.detectIdentifierType("tekst"))
        assertEquals(com.example.data.model.IdentifierType.UNKNOWN, Dywan.detectIdentifierType("123"))
    }
}
