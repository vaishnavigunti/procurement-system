package com.infosys.procurement_system.purchaseorder;

import com.infosys.procurement_system.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseOrderResponse create(@Valid @RequestBody PurchaseOrderRequest request) {
        return service.create(request);
    }

    @GetMapping
    public PageResponse<PurchaseOrderResponse> list(
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false) Long supplierId,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(status, supplierId, pageable);
    }

    @GetMapping("/{id}")
    public PurchaseOrderResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/issue")
    public PurchaseOrderResponse issue(@PathVariable Long id) {
        return service.issue(id);
    }

    @PostMapping("/{id}/confirm")
    public PurchaseOrderResponse confirm(@PathVariable Long id, @Valid @RequestBody PurchaseOrderConfirmRequest request) {
        return service.confirm(id, request);
    }

    @PostMapping("/{id}/receive")
    public PurchaseOrderResponse receive(@PathVariable Long id, @Valid @RequestBody PurchaseOrderReceiveRequest request) {
        return service.receive(id, request);
    }

    @PostMapping("/{id}/close")
    public PurchaseOrderResponse close(@PathVariable Long id, @Valid @RequestBody(required = false) PurchaseOrderCloseRequest request) {
        return service.close(id, request);
    }

    @PostMapping("/{id}/cancel")
    public PurchaseOrderResponse cancel(@PathVariable Long id, @Valid @RequestBody PurchaseOrderCancelRequest request) {
        return service.cancel(id, request);
    }
}
