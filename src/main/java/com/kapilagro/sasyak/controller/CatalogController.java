package com.kapilagro.sasyak.controller;

import com.kapilagro.sasyak.model.CatalogItem;
import com.kapilagro.sasyak.model.CatalogRequest;
import com.kapilagro.sasyak.model.CatalogResponse;
import com.kapilagro.sasyak.model.User;
import com.kapilagro.sasyak.services.CatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@RestController
@RequestMapping("/api/admin/catalog")
@PreAuthorize("hasRole('ADMIN')")
public class CatalogController {


    // Helper method to get the current tenant ID from the authenticated user
    private UUID getCurrentUserTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        if (currentUser.getTenantId() == null) {
            throw new IllegalStateException("Admin user is not associated with a tenant");
        }
        return currentUser.getTenantId();
    }


    // get  current user id
    private int getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        return currentUser.getUserId();
    }



    @Autowired
    private CatalogService catalogService;


    @PostMapping("")
    public ResponseEntity<?> createCatalogItem(@RequestBody CatalogRequest catalogRequest) {
        try {
            UUID tenantId = getCurrentUserTenantId();
            int currentUserId = getCurrentUserId();

            CatalogResponse response = catalogService.createCatalog(catalogRequest, currentUserId, tenantId);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid input: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating catalog item: " + e.getMessage());
        }
    }

    @GetMapping("")
    public ResponseEntity<?> getCatalog() {
        try {
            UUID tenantId = getCurrentUserTenantId();

            List<CatalogResponse> catalogs = catalogService.getAllCatalogByTenant(tenantId);

            return ResponseEntity.ok(catalogs);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching catalog: " + e.getMessage());
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCatalog(@PathVariable("id") int id) {
        try {
            UUID tenantId = getCurrentUserTenantId();

            boolean deleted = catalogService.deleteCatalog(id, tenantId);

            if (deleted) {
                return ResponseEntity.ok("Catalog item deleted successfully");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Catalog item not found or does not belong to your tenant");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting catalog item: " + e.getMessage());
        }
    }

    @GetMapping("/{category}")
    public ResponseEntity<?> getCatalogByCategory(@PathVariable("category") String category) {
        try {
            UUID tenantId = getCurrentUserTenantId();

            List<CatalogItem> catalogItems = catalogService.getCatalogByCategory(category, tenantId);

            if (catalogItems.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No catalog items found for category: " + category);
            }

            return ResponseEntity.ok(catalogItems);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid category: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching catalog by category: " + e.getMessage());
        }
    }

// Additional helpful endpoints

    @GetMapping("/categories")
    public ResponseEntity<?> getDistinctCategories() {
        try {
            UUID tenantId = getCurrentUserTenantId();

            List<String> categories = catalogService.getDistinctCategories(tenantId);

            return ResponseEntity.ok(categories);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching categories: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCatalog(@PathVariable("id") int id, @RequestBody CatalogRequest catalogRequest) {
        try {
            UUID tenantId = getCurrentUserTenantId();

            Optional<CatalogResponse> updatedCatalog = catalogService.updateCatalog(id, catalogRequest, tenantId);

            if (updatedCatalog.isPresent()) {
                return ResponseEntity.ok(updatedCatalog.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Catalog item not found or does not belong to your tenant");
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid input: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating catalog item: " + e.getMessage());
        }
    }

    @GetMapping("/item/{id}")
    public ResponseEntity<?> getCatalogById(@PathVariable("id") int id) {
        try {
            UUID tenantId = getCurrentUserTenantId();

            Optional<CatalogResponse> catalog = catalogService.getCatalogById(id, tenantId);

            if (catalog.isPresent()) {
                return ResponseEntity.ok(catalog.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Catalog item not found or does not belong to your tenant");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching catalog item: " + e.getMessage());
        }
    }
}
