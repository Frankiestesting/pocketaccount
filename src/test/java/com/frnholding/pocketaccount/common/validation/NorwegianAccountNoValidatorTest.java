package com.frnholding.pocketaccount.common.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class NorwegianAccountNoValidatorTest {

    private final NorwegianAccountNoValidator validator = new NorwegianAccountNoValidator();

    @Test
    void acceptsValidNorwegianAccountNumber() {
        assertThat(validator.isValid("22600712547", null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "52010718236",
            "45213104148",
            "25936168798",
            "42757397492",
            "37119693293",
            "62944894786",
            "74020926752",
            "84237756494",
            "73755430142",
            "60160941459",
            "85948785531",
            "64071102880",
            "36781020596",
            "44170147097",
            "98946785547",
            "64602074895",
            "88646959297",
            "39003909615",
            "35842896205",
            "09726418338",
            "54895603360",
            "25390729394",
            "79724596423",
            "25861378740",
            "75659297507",
            "09188718261",
            "07680767097",
            "18742753596",
            "46208146955",
            "44636584255",
            "25234740582",
            "92263246987",
            "78824624312",
            "94005582320",
            "14128315994",
            "33467526702",
            "73863357435",
            "95829604494",
            "89184598389",
            "76128297398",
            "79903907849",
            "35579806247",
            "41608371328",
            "64904590523",
            "49582242541",
            "98856536009",
            "30000584246",
            "57405674035",
            "18420023932",
            "08575469176"
    })
    void acceptsKnownValidAccountNumbers(String accountNo) {
        assertThat(validator.isValid(accountNo, null))
                .withFailMessage("Failed accountNo: %s", accountNo)
                .isTrue();
    }

    @Test
    void rejectsInvalidControlDigit() {
        assertThat(validator.isValid("22600712548", null)).isFalse();
    }
}
