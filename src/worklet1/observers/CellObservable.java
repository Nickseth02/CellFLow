lpackage worklet1.observers;

import java.util.ArrayList;
import java.util.List;

public class CellObservable {

    private final List<CellObserver> observers =
            new ArrayList<>();

    public void addObserver(CellObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void removeObserver(CellObserver observer) {
        observers.remove(observer);
    }

    public void notifyObservers(String reference) {
        for (CellObserver observer :
                List.copyOf(observers)) {

            observer.onCellChanged(reference);
        }
    }
}