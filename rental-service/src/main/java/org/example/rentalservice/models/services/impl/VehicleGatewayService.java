package org.example.rentalservice.models.services.impl;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.example.rentalservice.clients.VehicleClient;
import org.example.rentalservice.exceptions.VehicleNotFoundException;
import org.example.rentalservice.exceptions.VehicleServiceException;
import org.example.rentalservice.models.dto.responses.VehicleResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VehicleGatewayService {

    private final VehicleClient vehicleClient;

    public VehicleResponse getVehicleById(Long vehicleId) {
        try {
            return vehicleClient.getVehicleById(vehicleId);
        }catch (FeignException e) {
            if (e.status() == 404) {
                throw new VehicleNotFoundException(vehicleId);
            }
            throw new VehicleServiceException("Lỗi gọi Vehicle Service: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new VehicleServiceException("Lỗi Vehicle Service: " + e.getMessage(), e);
        }
    }
}
