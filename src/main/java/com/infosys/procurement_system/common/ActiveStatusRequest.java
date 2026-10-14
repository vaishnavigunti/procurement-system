package com.infosys.procurement_system.common;

import jakarta.validation.constraints.NotNull;

/** Body for PATCH .../status endpoints that simply activate or deactivate a record. */
public record ActiveStatusRequest(@NotNull Boolean active) {
}
