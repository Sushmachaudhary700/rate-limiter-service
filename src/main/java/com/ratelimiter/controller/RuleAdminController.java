package com.ratelimiter.controller;

import com.ratelimiter.model.RateLimitRule;
import com.ratelimiter.repository.RateLimitRuleRepository;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rules")
public class RuleAdminController {

    private final RateLimitRuleRepository ruleRepository;

    public RuleAdminController(RateLimitRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @GetMapping
    @Operation(summary = "List all configured rate limit rules")
    public List<RateLimitRule> listAll() {
        return ruleRepository.findAll();
    }

    @PostMapping
    @Operation(summary = "Create a new rate limit rule for a client + resource")
    public ResponseEntity<RateLimitRule> create(@Valid @RequestBody RateLimitRule rule) {
        RateLimitRule saved = ruleRepository.save(rule);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing rate limit rule")
    public ResponseEntity<RateLimitRule> update(@PathVariable Long id, @Valid @RequestBody RateLimitRule updated) {
        return ruleRepository.findById(id)
                .map(existing -> {
                    updated.setId(existing.getId());
                    return ResponseEntity.ok(ruleRepository.save(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a rate limit rule")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!ruleRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        ruleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
