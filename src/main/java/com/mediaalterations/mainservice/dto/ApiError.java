package com.mediaalterations.mainservice.dto;

import lombok.Builder;

@Builder
public record ApiError(
   int status,
   String errorMessage,
   String errorClass
) {}
