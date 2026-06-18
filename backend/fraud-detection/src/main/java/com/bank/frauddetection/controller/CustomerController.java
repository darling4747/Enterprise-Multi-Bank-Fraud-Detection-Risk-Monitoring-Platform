package com.bank.frauddetection.controller;

import com.bank.frauddetection.dto.customer.CustomerCreateRequest;
import com.bank.frauddetection.dto.customer.CustomerResponse;
import com.bank.frauddetection.enums.CustomerStatus;
import com.bank.frauddetection.service.CustomerService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public List<CustomerResponse> list(Principal principal) {
        return customerService.list(principal.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse create(@Valid @RequestBody CustomerCreateRequest request, Principal principal) {
        return customerService.create(request, principal.getName());
    }

    @PatchMapping("/{id}/status")
    public CustomerResponse updateStatus(@PathVariable Long id, @RequestParam CustomerStatus status, Principal principal) {
        return customerService.updateStatus(id, status, principal.getName());
    }
}
