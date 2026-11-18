package com.nvhuong.devworkbench.service.collections;

import com.nvhuong.devworkbench.collection.CollectionInheritance;
import com.nvhuong.devworkbench.collection.model.CollectionRequestContext;
import com.nvhuong.devworkbench.collection.model.RequestGroup;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;
import com.nvhuong.devworkbench.service.variable.RequestExecutionContext;
import com.nvhuong.devworkbench.service.variable.RequestExecutionScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Collection Kế thừa các dịch vụ ứng dụng.
 *
 * <p>Chịu trách nhiệm về các yêu cầu báo chí ID Tìm chuỗi nhóm chính, làm mới phạm vi biến thực thi của yêu cầu này và đặt nó collection-core
 * Các quy tắc kế thừa được áp dụng cho bản nháp yêu cầu mới nhất.</p>
 *
 * @author nvhuong
 * @since 4.3.22
 */
@Slf4j
@RequiredArgsConstructor
public class InheritanceService {

    private final CollectionRequestRepository requestRepository;

    /**
     * Tạo một dịch vụ kế thừa (sử dụng triển khai mặc định)
     */
    public InheritanceService() {
        this(new ActiveCollectionRequestRepository());
    }

    /**
     * Áp dụng quy tắc kế thừa nhóm
     *
     * @param item Mục yêu cầu ban đầu (mới nhất, bao gồm cả mới nhất url/params）
     * @return Các mục yêu cầu kế thừa được áp dụng
     */
    public HttpRequestItem applyInheritance(HttpRequestItem item) {
        if (item == null) {
            return null;
        }

        try {
            List<RequestGroup> groupChain = findGroupChain(item.getId());
            refreshExecutionScope(groupChain);
            if (groupChain.isEmpty()) {
                log.trace("yêu cầu [{}] Không phải ở đây Collections Các nhóm trong cây hoặc không có nhóm cha, hãy sử dụng cấu hình ban đầu", item.getName());
                return item;
            }

            log.debug("cho yêu cầu [{}] Áp dụng kế thừa nhóm", item.getName());
            return CollectionInheritance.apply(item, groupChain);
        } catch (Exception e) {
            log.debug("Đã xảy ra ngoại lệ khi áp dụng tính kế thừa (cấu hình gốc sẽ được sử dụng): {}", e.getMessage());
            return item;
        }
    }

    private List<RequestGroup> findGroupChain(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            return List.of();
        }
        return requestRepository.findRequestContextById(requestId)
                .map(CollectionRequestContext::getGroupChain)
                .orElseGet(List::of);
    }

    private void refreshExecutionScope(List<RequestGroup> groupChain) {
        RequestExecutionContext.setCurrentScope(RequestExecutionScope.fromVariables(
                CollectionInheritance.mergeGroupVariables(groupChain)
        ));
    }
}
