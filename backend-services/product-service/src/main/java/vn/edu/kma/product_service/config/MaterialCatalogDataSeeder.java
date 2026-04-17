package vn.edu.kma.product_service.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.kma.product_service.entity.MaterialCategory;
import vn.edu.kma.product_service.entity.MaterialItem;
import vn.edu.kma.product_service.repository.MaterialCategoryRepository;
import vn.edu.kma.product_service.repository.MaterialItemRepository;

import java.util.List;

/**
 * Seed danh mục nguyên liệu khi DB trống (mỗi loại ~5 tên).
 */
@Component
@Order(20)
@RequiredArgsConstructor
@Slf4j
public class MaterialCatalogDataSeeder implements ApplicationRunner {

    private final MaterialCategoryRepository categoryRepository;
    private final MaterialItemRepository itemRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (categoryRepository.count() > 0) {
            return;
        }
        log.info("Seeding material_categories / material_items...");

        record Cat(String code, String label, int order, List<String> names) {}

        List<Cat> data = List.of(
                new Cat("FRESH_MILK", "Sữa tươi", 10, List.of(
                        "Sữa tươi thanh trùng",
                        "Sữa tươi tiệt trùng (UHT)",
                        "Sữa tươi không đường",
                        "Sữa tươi có đường",
                        "Sữa tươi hữu cơ"
                )),
                new Cat("VEGETABLE", "Rau củ", 20, List.of(
                        "Cà rốt",
                        "Khoai tây",
                        "Bắp cải",
                        "Cà chua",
                        "Rau muống"
                )),
                new Cat("COFFEE", "Cà phê", 30, List.of(
                        "Cà phê hạt Arabica",
                        "Cà phê hạt Robusta",
                        "Cà phê hạt rang mộc",
                        "Cà phê nhân xanh",
                        "Cà phê bột pha phin"
                )),
                new Cat("OTHER", "Khác", 40, List.of(
                        "Nguyên liệu tổng hợp A",
                        "Nguyên liệu tổng hợp B",
                        "Nguyên liệu tổng hợp C",
                        "Nguyên liệu đặc thù",
                        "Nguyên liệu dự phòng"
                ))
        );

        for (Cat c : data) {
            MaterialCategory cat = categoryRepository.save(MaterialCategory.builder()
                    .code(c.code)
                    .label(c.label)
                    .sortOrder(c.order)
                    .active(true)
                    .build());
            int i = 0;
            for (String name : c.names) {
                itemRepository.save(MaterialItem.builder()
                        .category(cat)
                        .name(name)
                        .sortOrder(++i * 10)
                        .active(true)
                        .build());
            }
        }
        log.info("Material catalog seeded: {} categories", data.size());
    }
}
