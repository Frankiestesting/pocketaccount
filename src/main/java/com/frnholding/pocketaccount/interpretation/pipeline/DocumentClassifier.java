package com.frnholding.pocketaccount.interpretation.pipeline;

public interface DocumentClassifier {
    DocumentType classify(InterpretedText text, DocumentType hintedType);
}
