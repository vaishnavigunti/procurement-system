package com.infosys.procurement_system.user;

import com.infosys.procurement_system.common.ActiveStatusRequest;
import com.infosys.procurement_system.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserCreateRequest request) {
        return userService.create(request);
    }

    @GetMapping
    public PageResponse<UserResponse> list(@RequestParam(required = false) Long roleId,
                                           @RequestParam(required = false) Long departmentId,
                                           @RequestParam(required = false) Boolean active,
                                           @RequestParam(required = false) String q,
                                           @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return userService.list(roleId, departmentId, active, q, pageable);
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return userService.get(id);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return userService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public UserResponse setStatus(@PathVariable Long id, @Valid @RequestBody ActiveStatusRequest request) {
        return userService.setActive(id, request.active());
    }
}
