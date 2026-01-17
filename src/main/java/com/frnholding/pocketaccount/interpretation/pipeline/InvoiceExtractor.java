package com.frnholding.pocketaccount.interpretation.pipeline;

import com.frnholding.pocketaccount.interpretation.domain.InvoiceFieldsDTO;

public interface InvoiceExtractor {
    InvoiceFieldsDTO extract(InterpretedText text);
}
