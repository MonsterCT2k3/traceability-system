package vn.edu.kma.product_service.service;

import vn.edu.kma.product_service.dto.response.MaterialCategoryCatalogResponse;

import java.util.List;

public interface MaterialCatalogService {

    List<MaterialCategoryCatalogResponse> getCatalog();

    /** Kiểm tra cặp (materialType = label loại, materialName = tên mục trong loại). */
    boolean isValidPair(String materialTypeLabel, String materialName);
}
