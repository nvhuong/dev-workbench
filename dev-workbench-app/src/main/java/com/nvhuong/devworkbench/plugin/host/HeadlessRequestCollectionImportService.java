package com.nvhuong.devworkbench.plugin.host;

import com.nvhuong.devworkbench.model.RequestImportDraft;
import com.nvhuong.devworkbench.model.RequestImportResult;
import com.nvhuong.devworkbench.plugin.api.service.RequestCollectionImportService;

import java.util.List;

public class HeadlessRequestCollectionImportService implements RequestCollectionImportService {

    @Override
    public RequestImportResult importRequests(List<RequestImportDraft> requests) {
        return RequestImportResult.unavailable();
    }
}
