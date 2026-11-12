package com.baoxian.web;

import com.baoxian.app.service.CustomerService;
import com.baoxian.app.service.ProductService;
import com.baoxian.common.R;
import com.baoxian.infra.entity.Customer;
import com.baoxian.infra.entity.InsuranceProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CustomerProductController {

    private final CustomerService customerService;
    private final ProductService productService;

    @GetMapping("/customers")
    public R<List<Customer>> customers(@RequestParam(required = false) String keyword) {
        return R.ok(customerService.list(keyword));
    }

    @PostMapping("/customers")
    public R<Customer> createCustomer(@RequestBody Customer customer) {
        return R.ok(customerService.create(customer));
    }

    @DeleteMapping("/customers/{id}")
    public R<Void> deleteCustomer(@PathVariable Long id) {
        customerService.delete(id);
        return R.ok();
    }

    @GetMapping("/products")
    public R<List<InsuranceProduct>> products() {
        return R.ok(productService.list());
    }

    @PostMapping("/products")
    public R<InsuranceProduct> createProduct(@RequestBody InsuranceProduct product) {
        return R.ok(productService.create(product));
    }

    @DeleteMapping("/products/{id}")
    public R<Void> deleteProduct(@PathVariable Long id) {
        productService.delete(id);
        return R.ok();
    }
}
