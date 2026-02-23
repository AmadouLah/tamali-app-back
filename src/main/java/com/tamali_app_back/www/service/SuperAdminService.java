package com.tamali_app_back.www.service;

import com.tamali_app_back.www.dto.*;
import com.tamali_app_back.www.entity.Business;
import com.tamali_app_back.www.repository.BusinessRepository;
import com.tamali_app_back.www.repository.SaleRepository;
import com.tamali_app_back.www.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SuperAdminService {

    private static final int RECENT_DAYS = 7;
    private static final int USAGE_STATS_DAYS = 30;
    private static final int INACTIVE_DAYS = 30;
    private static final int MOST_ACTIVE_LIMIT = 10;

    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final SaleRepository saleRepository;

    @Transactional(readOnly = true)
    public SuperAdminPlatformStatsDto getPlatformStats() {
        long totalBusinesses = businessRepository.count();
        long totalUsers = userRepository.count();
        long totalSalesCount = saleRepository.count();
        BigDecimal totalTransactionVolume = saleRepository.sumTotalAmount() != null ? saleRepository.sumTotalAmount() : BigDecimal.ZERO;
        long activeBusinessesToday = saleRepository.countDistinctBusinessIdsWithSaleToday();
        return new SuperAdminPlatformStatsDto(
                totalBusinesses, totalUsers, totalSalesCount, totalTransactionVolume, activeBusinessesToday);
    }

    @Transactional(readOnly = true)
    public SuperAdminRecentActivityDto getRecentActivity() {
        LocalDateTime since = LocalDateTime.now().minusDays(RECENT_DAYS);
        int newBusinessesCount = (int) businessRepository.countByCreatedAtAfter(since);
        int newUsersCount = (int) userRepository.countByCreatedAtAfter(since);

        LocalDateTime sinceActivity = LocalDateTime.now().minusDays(INACTIVE_DAYS);
        List<Object[]> salesByBusiness = saleRepository.countSalesByBusinessSince(sinceActivity);
        List<Object[]> lastSaleByBusiness = saleRepository.findLastSaleDateByBusinessId();

        Map<UUID, Long> saleCountByBusinessId = salesByBusiness.stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));
        Map<UUID, LocalDateTime> lastSaleByBusinessId = lastSaleByBusiness.stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> (LocalDateTime) row[1]));

        List<UUID> mostActiveIds = salesByBusiness.stream()
                .limit(MOST_ACTIVE_LIMIT)
                .map(row -> (UUID) row[0])
                .toList();
        Map<UUID, String> namesById = businessRepository.findAllById(mostActiveIds).stream()
                .collect(Collectors.toMap(Business::getId, Business::getName));
        List<BusinessActivitySummaryDto> mostActiveBusinesses = mostActiveIds.stream()
                .map(id -> new BusinessActivitySummaryDto(id, namesById.getOrDefault(id, ""), saleCountByBusinessId.getOrDefault(id, 0L)))
                .toList();

        LocalDateTime inactiveThreshold = LocalDateTime.now().minusDays(INACTIVE_DAYS);
        List<Business> allBusinesses = businessRepository.findAll();
        List<BusinessActivitySummaryDto> inactiveBusinesses = new ArrayList<>();
        for (Business b : allBusinesses) {
            LocalDateTime lastSale = lastSaleByBusinessId.get(b.getId());
            if (lastSale == null || lastSale.isBefore(inactiveThreshold)) {
                long days = lastSale == null
                        ? ChronoUnit.DAYS.between(b.getCreatedAt(), LocalDateTime.now())
                        : ChronoUnit.DAYS.between(lastSale, LocalDateTime.now());
                inactiveBusinesses.add(new BusinessActivitySummaryDto(b.getId(), b.getName(), days));
            }
        }

        return new SuperAdminRecentActivityDto(newBusinessesCount, newUsersCount, mostActiveBusinesses, inactiveBusinesses);
    }

    @Transactional(readOnly = true)
    public SuperAdminUsageStatsDto getUsageStats() {
        LocalDateTime since = LocalDateTime.now().minusDays(USAGE_STATS_DAYS);
        List<Object[]> rows = saleRepository.countSalesPerDaySince(since);
        List<SalesPerDayDto> salesPerDay = rows.stream()
                .map(row -> {
                    LocalDate d = toLocalDate(row[0]);
                    return d != null ? new SalesPerDayDto(d, ((Number) row[1]).longValue()) : null;
                })
                .filter(Objects::nonNull)
                .toList();

        String peakActivityLabel = "—";
        if (!salesPerDay.isEmpty()) {
            SalesPerDayDto peak = salesPerDay.stream().max(Comparator.comparingLong(SalesPerDayDto::count)).orElse(null);
            if (peak != null) {
                peakActivityLabel = peak.date().format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH)) + " (" + peak.count() + " ventes)";
            }
        }

        long totalBusinesses = businessRepository.count();
        long businessesWithSalesInPeriod = saleRepository.countSalesByBusinessSince(since).size();
        double usageRatePercent = totalBusinesses == 0 ? 0.0 : (100.0 * businessesWithSalesInPeriod / totalBusinesses);

        return new SuperAdminUsageStatsDto(salesPerDay, peakActivityLabel, Math.round(usageRatePercent * 10) / 10.0);
    }

    @Transactional(readOnly = true)
    public SuperAdminSystemMonitoringDto getSystemMonitoring() {
        String serverStatus = "UP";
        List<String> criticalErrors = List.of();
        List<String> syncFailures = List.of();
        List<String> emailOrWhatsAppFailures = List.of();
        return new SuperAdminSystemMonitoringDto(serverStatus, criticalErrors, syncFailures, emailOrWhatsAppFailures);
    }

    @Transactional(readOnly = true)
    public List<BusinessSummaryDto> getBusinessSummaries() {
        List<Business> businesses = businessRepository.findAll();
        List<Object[]> userCounts = userRepository.countUsersGroupByBusinessId();
        Map<UUID, Long> userCountByBusinessId = userCounts.stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));
        return businesses.stream()
                .map(b -> new BusinessSummaryDto(
                        b.getId(), b.getName(), b.getEmail(), b.isActive(),
                        userCountByBusinessId.getOrDefault(b.getId(), 0L).intValue(),
                        b.getCreatedAt()))
                .toList();
    }

    private static LocalDate toLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof java.sql.Date d) return d.toLocalDate();
        if (value instanceof LocalDate ld) return ld;
        return null;
    }
}
