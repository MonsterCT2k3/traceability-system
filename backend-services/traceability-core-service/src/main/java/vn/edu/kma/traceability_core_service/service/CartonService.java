package vn.edu.kma.traceability_core_service.service;

import vn.edu.kma.traceability_core_service.dto.request.CartonCreateRequest;
import vn.edu.kma.traceability_core_service.entity.Carton;

public interface CartonService {

    Carton createCarton(String palletId, CartonCreateRequest request, String tokenHeader);
}

