package ch.bissbert.arsenal;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightStateTest {
    @Test void impactIsExactlyOnce() {
        FlightState flight = new FlightState();
        assertTrue(flight.impact());
        assertFalse(flight.impact());
        assertEquals(0, flight.nextDistance(3));
    }
    @Test void distanceAndLifetimeDoNotExpire() {
        FlightState flight = new FlightState();
        for (int i = 0; i < 10_000; i++) assertEquals(3, flight.nextDistance(3));
        assertTrue(flight.impact());
    }
    @Test void cleanupPreventsLaterExplosion() {
        FlightState flight = new FlightState();
        flight.expire();
        assertFalse(flight.impact());
        assertEquals(0, flight.nextDistance(3));
    }
}
