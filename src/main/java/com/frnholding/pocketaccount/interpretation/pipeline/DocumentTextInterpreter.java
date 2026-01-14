package com.frnholding.pocketaccount.interpretation.pipeline;

import java.util.UUID;

public interface DocumentTextInterpreter {
    InterpretedText extract(UUID documentId);
}
