package com.tamali_app_back.www.service;

import com.tamali_app_back.www.dto.BusinessActivityEntryDto;
import com.tamali_app_back.www.dto.SaleDto;
import com.tamali_app_back.www.dto.StockMovementDto;
import com.tamali_app_back.www.entity.User;
import com.tamali_app_back.www.enums.MovementType;
import com.tamali_app_back.www.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class BusinessActivityExportService {

    private final SaleService saleService;
    private final StockMovementService stockMovementService;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<BusinessActivityEntryDto> exportActivity(
            UUID businessId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        List<SaleDto> sales = saleService.exportSales(businessId, from, to);

        Map<UUID, User> usersById = userRepository.findAllById(
                        sales.stream()
                                .map(SaleDto::cashierId)
                                .distinct()
                                .toList()
                )
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<BusinessActivityEntryDto> saleEntries = sales.stream()
                .map(s -> {
                    User user = usersById.get(s.cashierId());
                    String displayName = user != null
                            ? String.format("%s %s", defaultString(user.getFirstname()), defaultString(user.getLastname())).trim()
                            : "Utilisateur";
                    return new BusinessActivityEntryDto(
                            "VENTE",
                            "Création de vente",
                            s.id(),
                            s.businessId(),
                            s.cashierId(),
                            displayName,
                            s.saleDate(),
                            "SYNCHRONISÉ"
                    );
                })
                .toList();

        List<StockMovementDto> movements = stockMovementService.exportByBusinessId(businessId, from, to);

        List<BusinessActivityEntryDto> movementEntries = movements.stream()
                .map(m -> new BusinessActivityEntryDto(
                        "MOUVEMENT_STOCK",
                        m.type() == MovementType.IN
                                ? "Entrée de stock"
                                : (m.type() == MovementType.OUT ? "Sortie de stock" : "Mouvement de stock (vente)"),
                        m.id(),
                        m.businessId(),
                        null,
                        null,
                        m.movementAt(),
                        "SYNCHRONISÉ"
                ))
                .toList();

        return Stream.concat(saleEntries.stream(), movementEntries.stream())
                .sorted(Comparator.comparing(BusinessActivityEntryDto::occurredAt))
                .toList();
    }

    private static String defaultString(String value) {
        return value != null ? value : "";
    }
}

