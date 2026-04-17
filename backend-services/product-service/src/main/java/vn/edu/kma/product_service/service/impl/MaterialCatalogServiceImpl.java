package vn.edu.kma.product_service.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.kma.product_service.dto.response.MaterialCategoryCatalogResponse;
import vn.edu.kma.product_service.dto.response.MaterialItemOptionResponse;
import vn.edu.kma.product_service.entity.MaterialCategory;
import vn.edu.kma.product_service.entity.MaterialItem;
import vn.edu.kma.product_service.repository.MaterialCategoryRepository;
import vn.edu.kma.product_service.repository.MaterialItemRepository;
import vn.edu.kma.product_service.service.MaterialCatalogService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialCatalogServiceImpl implements MaterialCatalogService {

    private final MaterialCategoryRepository categoryRepository;
    private final MaterialItemRepository itemRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MaterialCategoryCatalogResponse> getCatalog() {
        List<MaterialCategory> categories = categoryRepository.findAllByActiveTrueOrderBySortOrderAsc();
        return categories.stream().map(this::toResponse).toList();
    }

    private MaterialCategoryCatalogResponse toResponse(MaterialCategory c) {
        List<MaterialItem> items = itemRepository.findAllByCategory_IdAndActiveTrueOrderBySortOrderAsc(c.getId());
        List<MaterialItemOptionResponse> itemDtos = items.stream()
                .map(i -> MaterialItemOptionResponse.builder().id(i.getId()).name(i.getName()).build())
                .toList();
        return MaterialCategoryCatalogResponse.builder()
                .id(c.getId())
                .code(c.getCode())
                .label(c.getLabel())
                .items(itemDtos)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isValidPair(String materialTypeLabel, String materialName) {
        if (materialTypeLabel == null || materialTypeLabel.isBlank() || materialName == null || materialName.isBlank()) {
            return false;
        }
        String type = materialTypeLabel.trim();
        String name = materialName.trim();
        return categoryRepository.findByLabelAndActiveTrue(type)
                .map(cat -> itemRepository.existsByCategory_IdAndNameAndActiveTrue(cat.getId(), name))
                .orElse(false);
    }
}
