package com.kj.repo.infra.perf.model;

public class RichTagBuilder {

    private final RichTag richTag;

    public RichTagBuilder(RichTag richTag) {
        super();
        this.richTag = richTag;
    }

    public RichTagBuilder setNamespace(String namespace) {
        this.richTag.setNamespace(namespace);
        return this;
    }

    public RichTagBuilder setTag(String tag) {
        this.richTag.setTag(tag);
        return this;
    }

    public RichTagBuilder addExtras(Object... extras) {
        for (Object obj : extras) {
            this.richTag.getExtras().add(obj);
        }
        return this;
    }

    public RichTag build() {
        return this.richTag;
    }

}
