package vn.edu.kma.catalog_service.service;

import vn.edu.kma.catalog_service.dto.response.MaterialCategoryCatalogResponse;
import vn.edu.kma.catalog_service.dto.response.MaterialItemOptionResponse;

import java.util.List;

public interface MaterialCatalogService {

    List<MaterialCategoryCatalogResponse> getCatalog();

    List<MaterialCategoryCatalogResponse> getMyCatalog(String ownerId);

    MaterialCategoryCatalogResponse createCategory(String ownerId, String label);

    MaterialItemOptionResponse createItem(String ownerId, String categoryId, String name);

    /** Kiểm tra cặp (materialType = label loại, materialName = tên mục trong loại). */
    boolean isValidPair(String materialTypeLabel, String materialName);
}
