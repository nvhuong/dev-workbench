package com.nvhuong.devworkbench.plugin.api.service;

import com.nvhuong.devworkbench.model.RequestImportDraft;
import com.nvhuong.devworkbench.model.RequestImportResult;

import java.util.List;

public interface RequestCollectionImportService {

    RequestImportResult importRequests(List<RequestImportDraft> requests);
}
