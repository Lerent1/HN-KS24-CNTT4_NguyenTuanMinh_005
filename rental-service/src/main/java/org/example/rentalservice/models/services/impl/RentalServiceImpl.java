package org.example.rentalservice.models.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.rentalservice.models.constants.RentalStatus;
import org.example.rentalservice.models.dto.requests.CreateRentalDetailRequest;
import org.example.rentalservice.models.dto.requests.CreateRentalRequest;
import org.example.rentalservice.models.dto.responses.RentalDetailResponse;
import org.example.rentalservice.models.dto.responses.RentalResponse;
import org.example.rentalservice.models.dto.responses.VehicleResponse;
import org.example.rentalservice.models.entities.Rental;
import org.example.rentalservice.models.entities.RentalDetail;
import org.example.rentalservice.models.repositories.RentalDetailRepository;
import org.example.rentalservice.models.repositories.RentalRepository;
import org.example.rentalservice.models.services.RentalService;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RentalServiceImpl implements RentalService {

        private final RentalRepository rentalRepository;
        private final RentalDetailRepository rentalDetailRepository;
        private final VehicleGatewayService vehicleGatewayService;
        private final KafkaProductService kafkaProductService;

        @Override
        public RentalResponse createRental(CreateRentalRequest request) {
                double total = 0.0;
                List<ResolvedItem> resolvedItems = request.items().stream().map(this::resolveVehicle).toList();

                total = resolvedItems.stream()
                        .mapToDouble(item -> item.vehicle().pricePerDay() * item.request().rentalDays())
                        .sum();

                Rental rental = rentalRepository.save(Rental.builder()
                        .customerName(request.customerName().trim())
                        .customerEmail(request.customerEmail().trim())
                        .totalAmount(total)
                        .status(RentalStatus.PENDING)
                        .build());

                List<RentalDetail> details = resolvedItems.stream()
                        .map(item -> RentalDetail.builder()
                                .rental(rental)
                                .vehicleId(item.vehicle().vehicleId())
                                .rentalDays(item.request().rentalDays())
                                .pricePerDay(item.vehicle().pricePerDay())
                                .build())
                        .toList();

                List<RentalDetail> savedDetails = rentalDetailRepository.saveAll(details);
                List<RentalDetailResponse> itemResponses = getRentalDetailResponses(savedDetails, resolvedItems);

                kafkaProductService.sendRentalCreated(rental.getCustomerEmail());

                return new RentalResponse(
                        rental.getId(),
                        rental.getCustomerName(),
                        rental.getCustomerEmail(),
                        rental.getTotalAmount(),
                        rental.getStatus(),
                        itemResponses);
        }

        private static @NonNull List<RentalDetailResponse> getRentalDetailResponses(List<RentalDetail> savedDetails,
                                                                                    List<ResolvedItem> resolvedItems) {
                List<RentalDetailResponse> itemResponses = new ArrayList<>(savedDetails.size());

                for (int index = 0; index < savedDetails.size(); index++) {
                        RentalDetail detail = savedDetails.get(index);
                        VehicleResponse vehicle = resolvedItems.get(index).vehicle();
                        itemResponses.add(new RentalDetailResponse(
                                detail.getId(),
                                detail.getVehicleId(),
                                vehicle.vehicleName(),
                                vehicle.vehicleType(),
                                detail.getRentalDays(),
                                detail.getPricePerDay(),
                                detail.getPricePerDay() * detail.getRentalDays()));
                }
                return itemResponses;
        }

        private ResolvedItem resolveVehicle(CreateRentalDetailRequest item) {
                VehicleResponse vehicle = vehicleGatewayService.getVehicleById(item.vehicleId());
                return new ResolvedItem(item, vehicle);
        }

        private record ResolvedItem(CreateRentalDetailRequest request, VehicleResponse vehicle) {
        }
}

