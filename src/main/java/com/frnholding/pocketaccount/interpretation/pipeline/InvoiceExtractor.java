package com.frnholding.pocketaccount.interpretation.pipeline;

import com.frnholding.pocketaccount.interpretation.domain.InvoiceFields;

public interface InvoiceExtractor {
    InvoiceFields extract(InterpretedText text);
}
