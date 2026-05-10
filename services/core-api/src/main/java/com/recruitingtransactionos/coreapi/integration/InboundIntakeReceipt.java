package com.recruitingtransactionos.coreapi.integration;

import java.util.UUID;

public record InboundIntakeReceipt(UUID sourceItemId, UUID informationPacketId) {}
