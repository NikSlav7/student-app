package com.example.ResourceServer.pdf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class MarksModeTest {

    // The frontend (MarksheetDialog.jsx) sends exactly these two names
    @Test
    void findsModeByTheNameTheFrontendSends() {
        assertThat(MarksMode.getModeByModeName("int")).isEqualTo(MarksMode.MODE_INTEGER);
        assertThat(MarksMode.getModeByModeName("double")).isEqualTo(MarksMode.MODE_DOUBLE);
    }

    @Test
    void ignoresUpperAndLowerCase() {
        assertThat(MarksMode.getModeByModeName("DOUBLE")).isEqualTo(MarksMode.MODE_DOUBLE);
        assertThat(MarksMode.getModeByModeName("Int")).isEqualTo(MarksMode.MODE_INTEGER);
    }

    // Anything unknown or missing falls back to integer mode instead of crashing
    @ParameterizedTest(name = "unknown mode \"{0}\" -> MODE_INTEGER")
    @NullAndEmptySource
    @ValueSource(strings = {"decimal", "fractional", "   ", "MODE_DOUBLE"})
    void fallsBackToIntegerModeForUnknownNames(String modeName) {
        assertThat(MarksMode.getModeByModeName(modeName)).isEqualTo(MarksMode.MODE_INTEGER);
    }
}
