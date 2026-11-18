package com.nvhuong.devworkbench.collection.model;

import com.nvhuong.devworkbench.request.model.HttpRequestItem;
import lombok.Value;

import java.util.List;

@Value
public class CollectionRequestContext {
    HttpRequestItem request;
    List<RequestGroup> groupChain;

    public CollectionRequestContext(HttpRequestItem request, List<RequestGroup> groupChain) {
        this.request = request;
        this.groupChain = groupChain == null ? List.of() : List.copyOf(groupChain);
    }

    public boolean hasRequest() {
        return request != null;
    }
}
