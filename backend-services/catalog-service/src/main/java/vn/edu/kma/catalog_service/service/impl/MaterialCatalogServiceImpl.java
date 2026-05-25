package vn.edu.kma.catalog_service.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.kma.catalog_service.dto.response.MaterialCategoryCatalogResponse;
import vn.edu.kma.catalog_service.dto.response.MaterialItemOptionResponse;
import vn.edu.kma.catalog_service.entity.MaterialCategory;
import vn.edu.kma.catalog_service.entity.MaterialItem;
import vn.edu.kma.catalog_service.repository.MaterialCategoryRepository;
import vn.edu.kma.catalog_service.repository.MaterialItemRepository;
import vn.edu.kma.catalog_service.service.MaterialCatalogService;
import vn.edu.kma.common.exception.AppException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaterialCatalogServiceImpl implements MaterialCatalogService {

    private final MaterialCategoryRepository categoryRepository;
    private final MaterialItemRepository itemRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MaterialCategoryCatalogResponse> getCatalog() {
        // Lấy danh mục chung (ownerId = null)
        List<MaterialCategory> categories = categoryRepository.findAllGlobalAndOwner(null);
        return categories.stream().map(c -> toResponseWithOwner(c, null)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaterialCategoryCatalogResponse> getMyCatalog(String ownerId) {
        // Lấy danh mục chung + riêng
        List<MaterialCategory> categories = categoryRepository.findAllGlobalAndOwner(ownerId);
        return categories.stream().map(c -> toResponseWithOwner(c, ownerId)).toList();
    }

    private MaterialCategoryCatalogResponse toResponseWithOwner(MaterialCategory c, String ownerId) {
        List<MaterialItem> items;
        if (ownerId == null) {
            // Chỉ lấy item chung (cách chữa cháy tạm: query owner = null)
            items = itemRepository.findAllGlobalAndOwnerByCategory(c.getId(), null);
        } else {
            items = itemRepository.findAllGlobalAndOwnerByCategory(c.getId(), ownerId);
        }
        
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
    @Transactional
    public MaterialCategoryCatalogResponse createCategory(String ownerId, String label) {
        // Tạo category riêng cho supplier
        MaterialCategory cat = MaterialCategory.builder()
                .code("CUSTOM_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .label(label)
                .ownerId(ownerId)
                .sortOrder(999) // Đẩy xuống cuối
                .active(true)
                .build();
        categoryRepository.save(cat);
        return toResponseWithOwner(cat, ownerId);
    }

    @Override
    @Transactional
    public MaterialItemOptionResponse createItem(String ownerId, String categoryId, String name) {
        MaterialCategory cat = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category không tồn tại"));
                
        MaterialItem item = MaterialItem.builder()
                .category(cat)
                .name(name)
                .ownerId(ownerId)
                .sortOrder(999)
                .active(true)
                .build();
        itemRepository.save(item);
        
        return MaterialItemOptionResponse.builder()
                .id(item.getId())
                .name(item.getName())
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
        
        // Vì có thể có Category trùng tên do các Supplier khác nhau tạo
        // nên ta tìm tất cả category khớp tên
        List<MaterialCategory> cats = categoryRepository.findAllByActiveTrueOrderBySortOrderAsc()
                .stream()
                .filter(c -> c.getLabel().equalsIgnoreCase(type))
                .toList();
                
        for (MaterialCategory cat : cats) {
            if (itemRepository.existsByCategory_IdAndNameAndActiveTrue(cat.getId(), name)) {
                return true;
            }
        }
        return false;
    }
}
