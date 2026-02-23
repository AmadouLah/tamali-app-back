package com.tamali_app_back.www.dto;

import java.time.LocalDate;

public record SalesPerDayDto(LocalDate date, long count) {}
