package org.eclipse.cargotracker.ai;

import java.util.List;

public record CargoStatusDetails(
    String trackingId,
    String origin,
    String destination,
    String routingStatus,
    String transportStatus,
    String lastKnownLocation,
    String nextExpectedActivity,
    String eta,
    boolean misdirected,
    List<String> itinerary) {}
