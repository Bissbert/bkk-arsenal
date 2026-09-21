package ch.bissbert.arsenal;

/** One-shot terminal gate shared by impact and every cleanup path. */
final class FlightState {
    private boolean finished;

    double nextDistance(double speed) {
        return finished ? 0 : speed;
    }

    boolean impact() {
        if (finished) return false;
        finished = true;
        return true;
    }

    void expire() { finished = true; }
}
