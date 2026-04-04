package vn.edu.kma.product_service.service;

import vn.edu.kma.product_service.dto.request.CartonCreateRequest;
import vn.edu.kma.product_service.entity.Carton;

public interface CartonService {

    Carton createCarton(String palletId, CartonCreateRequest request, String tokenHeader);
}
