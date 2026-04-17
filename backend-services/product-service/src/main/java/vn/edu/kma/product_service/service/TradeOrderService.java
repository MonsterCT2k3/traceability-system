package vn.edu.kma.product_service.service;

import vn.edu.kma.product_service.dto.request.AssignCarrierRequest;
import vn.edu.kma.product_service.dto.request.TradeOrderCreateRequest;
import vn.edu.kma.product_service.dto.response.TradeOrderResponse;

import java.util.List;

public interface TradeOrderService {

    TradeOrderResponse createOrder(TradeOrderCreateRequest request, String token);

    TradeOrderResponse getById(String orderId, String token);

    List<TradeOrderResponse> listAsBuyer(String token);

    List<TradeOrderResponse> listAsSeller(String token);

    List<TradeOrderResponse> listAsCarrier(String token);

    TradeOrderResponse accept(String orderId, String token);

    TradeOrderResponse reject(String orderId, String token);

    TradeOrderResponse cancel(String orderId, String token);

    TradeOrderResponse assignCarrier(String orderId, AssignCarrierRequest body, String token);

    /** VC xác nhận đã nhận hàng tại kho / điểm của người bán. */
    TradeOrderResponse confirmPickedUpFromSeller(String orderId, String token);

    TradeOrderResponse confirmDelivered(String orderId, String token);
}
