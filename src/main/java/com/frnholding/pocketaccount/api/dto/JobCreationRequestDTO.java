package com.frnholding.pocketaccount.api.dto;

public class JobCreationRequestDTO {
    private String pipeline;
    private boolean useOcr;
    private boolean useAi;
    private String languageHint;

    public JobCreationRequestDTO() {
    }

    public JobCreationRequestDTO(String pipeline, boolean useOcr, boolean useAi, String languageHint) {
        this.pipeline = pipeline;
        this.useOcr = useOcr;
        this.useAi = useAi;
        this.languageHint = languageHint;
    }

    public String getPipeline() {
        return pipeline;
    }

    public void setPipeline(String pipeline) {
        this.pipeline = pipeline;
    }

    public boolean isUseOcr() {
        return useOcr;
    }

    public void setUseOcr(boolean useOcr) {
        this.useOcr = useOcr;
    }

    public boolean isUseAi() {
        return useAi;
    }

    public void setUseAi(boolean useAi) {
        this.useAi = useAi;
    }

    public String getLanguageHint() {
        return languageHint;
    }

    public void setLanguageHint(String languageHint) {
        this.languageHint = languageHint;
    }
}