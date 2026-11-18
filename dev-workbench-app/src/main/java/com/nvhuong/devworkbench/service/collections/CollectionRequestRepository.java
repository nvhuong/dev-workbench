package com.nvhuong.devworkbench.service.collections;

import com.nvhuong.devworkbench.collection.model.CollectionDocument;
import com.nvhuong.devworkbench.collection.model.CollectionRequestContext;

import java.util.Optional;

public interface CollectionRequestRepository {

    Optional<CollectionRequestContext> findRequestContextById(String requestId);

    Optional<CollectionDocument> getDocument();
}
