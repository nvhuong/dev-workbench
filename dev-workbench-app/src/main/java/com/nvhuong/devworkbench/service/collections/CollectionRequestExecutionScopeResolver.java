package com.nvhuong.devworkbench.service.collections;

import com.nvhuong.devworkbench.collection.CollectionInheritance;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;
import com.nvhuong.devworkbench.service.variable.RequestExecutionContext;
import com.nvhuong.devworkbench.service.variable.RequestExecutionScope;
import lombok.experimental.UtilityClass;

import java.util.Optional;

@UtilityClass
public class CollectionRequestExecutionScopeResolver {

    private static final CollectionRequestRepository REQUEST_REPOSITORY =
            new ActiveCollectionRequestRepository();

    public Optional<RequestExecutionScope> resolveCurrentScope(HttpRequestItem request) {
        return request == null ? Optional.empty() : resolveCurrentScope(request.getId());
    }

    public Optional<RequestExecutionScope> resolveCurrentScope(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            return Optional.empty();
        }
        return REQUEST_REPOSITORY.findRequestContextById(requestId)
                .map(context -> CollectionInheritance.mergeGroupVariables(context.getGroupChain()))
                .map(RequestExecutionScope::fromVariables);
    }

    public boolean syncCurrentScope(HttpRequestItem request) {
        return resolveCurrentScope(request)
                .map(CollectionRequestExecutionScopeResolver::setCurrentScope)
                .orElse(false);
    }

    public boolean syncCurrentScope(String requestId) {
        return resolveCurrentScope(requestId)
                .map(CollectionRequestExecutionScopeResolver::setCurrentScope)
                .orElse(false);
    }

    public void syncCurrentScopeOrEmpty(String requestId) {
        RequestExecutionContext.setCurrentScope(resolveCurrentScope(requestId)
                .orElseGet(RequestExecutionScope::empty));
    }

    private boolean setCurrentScope(RequestExecutionScope scope) {
        RequestExecutionContext.setCurrentScope(scope);
        return true;
    }
}
