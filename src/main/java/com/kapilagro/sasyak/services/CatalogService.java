package com.kapilagro.sasyak.services;

import com.kapilagro.sasyak.model.Catalog;
import com.kapilagro.sasyak.model.CatalogItem;
import com.kapilagro.sasyak.model.CatalogRequest;
import com.kapilagro.sasyak.model.CatalogResponse;
import com.kapilagro.sasyak.repository.CatalogRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CatalogService {

    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);

    @Autowired
    private CatalogRepo catalogRepo;

    /**
     * Create a new catalog item
     */
    @Transactional
    public CatalogResponse createCatalog(CatalogRequest request, int createdBy, UUID tenantId) {
        log.debug("Creating catalog item: category={}, value={}, tenantId={}",
                request.getCategory(), request.getValue(), tenantId);

        try {
            // Validate input
            validateCatalogRequest(request);

            // Create catalog entity
            Catalog catalog = Catalog.builder()
                    .category(request.getCategory().trim())
                    .value(request.getValue().trim())
                    .details(request.getDetails() != null ? request.getDetails().trim() : null)
                    .tenantId(tenantId)
                    .createdBy(createdBy)
                    .createdAt(LocalDateTime.now())
                    .build();

            // Save to database
            int catalogId = catalogRepo.save(catalog);
            catalog.setId(catalogId);

            log.debug("Catalog item created successfully: id={}, category={}", catalogId, request.getCategory());

            // Return response
            return CatalogResponse.builder()
                    .id(catalogId)
                    .category(catalog.getCategory())
                    .value(catalog.getValue())
                    .details(catalog.getDetails())
                    .build();

        } catch (Exception e) {
            log.error("Error creating catalog item: category={}, error={}", request.getCategory(), e.getMessage(), e);
            throw new RuntimeException("Failed to create catalog item: " + e.getMessage(), e);
        }
    }

    /**
     * Get all catalog items for a tenant
     */
    public List<CatalogResponse> getAllCatalogByTenant(UUID tenantId) {
        log.debug("Fetching all catalog items for tenantId: {}", tenantId);

        try {
            List<Catalog> catalogs = catalogRepo.findByTenantId(tenantId);

            return catalogs.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching catalog items for tenantId: {}, error: {}", tenantId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch catalog items: " + e.getMessage(), e);
        }
    }

    /**
     * Get catalog items by category
     */
    public List<CatalogItem> getCatalogByCategory(String category, UUID tenantId) {
        log.debug("Fetching catalog items for category: {}, tenantId: {}", category, tenantId);

        try {
            // Validate category
            if (category == null || category.trim().isEmpty()) {
                throw new IllegalArgumentException("Category cannot be null or empty");
            }

            List<CatalogItem> catalogItems = catalogRepo.findByTenantIdAndCategory(tenantId, category.trim());

            log.debug("Found {} catalog items for category: {}", catalogItems.size(), category);
            return catalogItems;

        } catch (Exception e) {
            log.error("Error fetching catalog items for category: {}, tenantId: {}, error: {}",
                    category, tenantId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch catalog items by category: " + e.getMessage(), e);
        }
    }

    /**
     * Delete catalog item by ID
     */
    @Transactional
    public boolean deleteCatalog(int catalogId, UUID tenantId) {
        log.debug("Deleting catalog item: id={}, tenantId={}", catalogId, tenantId);

        try {
            // Check if catalog exists
            Optional<Catalog> catalogOpt = catalogRepo.findByIdAndTenantId(catalogId, tenantId);
            if (!catalogOpt.isPresent()) {
                log.warn("Catalog item not found: id={}, tenantId={}", catalogId, tenantId);
                return false;
            }

            // Delete the catalog item
            boolean deleted = catalogRepo.deleteByIdAndTenantId(catalogId, tenantId);

            if (deleted) {
                log.debug("Catalog item deleted successfully: id={}", catalogId);
            } else {
                log.warn("Failed to delete catalog item: id={}", catalogId);
            }

            return deleted;

        } catch (Exception e) {
            log.error("Error deleting catalog item: id={}, tenantId={}, error={}",
                    catalogId, tenantId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete catalog item: " + e.getMessage(), e);
        }
    }

    /**
     * Get catalog item by ID
     */
    public Optional<CatalogResponse> getCatalogById(int catalogId, UUID tenantId) {
        log.debug("Fetching catalog item by id: {}, tenantId: {}", catalogId, tenantId);

        try {
            Optional<Catalog> catalogOpt = catalogRepo.findByIdAndTenantId(catalogId, tenantId);

            return catalogOpt.map(this::mapToResponse);

        } catch (Exception e) {
            log.error("Error fetching catalog item by id: {}, tenantId: {}, error: {}",
                    catalogId, tenantId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch catalog item: " + e.getMessage(), e);
        }
    }

    /**
     * Update catalog item
     */
    @Transactional
    public Optional<CatalogResponse> updateCatalog(int catalogId, CatalogRequest request, UUID tenantId) {
        log.debug("Updating catalog item: id={}, tenantId={}", catalogId, tenantId);

        try {
            // Validate input
            validateCatalogRequest(request);

            // Check if catalog exists
            Optional<Catalog> catalogOpt = catalogRepo.findByIdAndTenantId(catalogId, tenantId);
            if (!catalogOpt.isPresent()) {
                log.warn("Catalog item not found for update: id={}, tenantId={}", catalogId, tenantId);
                return Optional.empty();
            }

            Catalog catalog = catalogOpt.get();

            // Update fields
            catalog.setCategory(request.getCategory().trim());
            catalog.setValue(request.getValue().trim());
            catalog.setDetails(request.getDetails() != null ? request.getDetails().trim() : null);
            catalog.setUpdatedAt(LocalDateTime.now());

            // Update in database
            boolean updated = catalogRepo.update(catalog);

            if (updated) {
                log.debug("Catalog item updated successfully: id={}", catalogId);
                return Optional.of(mapToResponse(catalog));
            } else {
                log.warn("Failed to update catalog item: id={}", catalogId);
                return Optional.empty();
            }

        } catch (Exception e) {
            log.error("Error updating catalog item: id={}, tenantId={}, error={}",
                    catalogId, tenantId, e.getMessage(), e);
            throw new RuntimeException("Failed to update catalog item: " + e.getMessage(), e);
        }
    }

    /**
     * Get distinct categories for a tenant
     */
    public List<String> getDistinctCategories(UUID tenantId) {
        log.debug("Fetching distinct categories for tenantId: {}", tenantId);

        try {
            return catalogRepo.findDistinctCategoriesByTenantId(tenantId);
        } catch (Exception e) {
            log.error("Error fetching distinct categories for tenantId: {}, error: {}", tenantId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch categories: " + e.getMessage(), e);
        }
    }

    /**
     * Get catalog count for a tenant
     */
    public int getCatalogCount(UUID tenantId) {
        try {
            return catalogRepo.countByTenantId(tenantId);
        } catch (Exception e) {
            log.error("Error getting catalog count for tenantId: {}, error: {}", tenantId, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Get catalog count by category for a tenant
     */
    public int getCatalogCountByCategory(UUID tenantId, String category) {
        try {
            return catalogRepo.countByTenantIdAndCategory(tenantId, category);
        } catch (Exception e) {
            log.error("Error getting catalog count by category for tenantId: {}, category: {}, error: {}",
                    tenantId, category, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Helper method to validate catalog request
     */
    private void validateCatalogRequest(CatalogRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Catalog request cannot be null");
        }

        if (request.getCategory() == null || request.getCategory().trim().isEmpty()) {
            throw new IllegalArgumentException("Category cannot be null or empty");
        }

        if (request.getValue() == null || request.getValue().trim().isEmpty()) {
            throw new IllegalArgumentException("Value cannot be null or empty");
        }

        // Additional validation rules
        if (request.getCategory().length() > 100) {
            throw new IllegalArgumentException("Category cannot exceed 100 characters");
        }

        if (request.getValue().length() > 255) {
            throw new IllegalArgumentException("Value cannot exceed 255 characters");
        }

        if (request.getDetails() != null && request.getDetails().length() > 1000) {
            throw new IllegalArgumentException("Details cannot exceed 1000 characters");
        }
    }

    /**
     * Helper method to map Catalog entity to CatalogResponse
     */
    private CatalogResponse mapToResponse(Catalog catalog) {
        return CatalogResponse.builder()
                .id(catalog.getId())
                .category(catalog.getCategory())
                .value(catalog.getValue())
                .details(catalog.getDetails())
                .build();
    }
}