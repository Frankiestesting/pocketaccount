package com.frnholding.pocketaccount.interpretation.pipeline;

import com.frnholding.pocketaccount.interpretation.domain.StatementTransaction;

import java.util.List;

public interface StatementExtractor {
    List<StatementTransaction> extract(InterpretedText text);
}
