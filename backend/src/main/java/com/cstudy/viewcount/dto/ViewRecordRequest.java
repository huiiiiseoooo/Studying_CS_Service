package com.cstudy.viewcount.dto;

import jakarta.validation.constraints.NotNull;

public record ViewRecordRequest(@NotNull Long mediumCategoryId) {
}
