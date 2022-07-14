package utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SetOfActions {
    String[] actions;
    int maxIndex;
    int size; // Size if the max index in currentStatus
    ArrayList<Integer> currentStatus; // Set of integer representing the index of the elements in the set
    ArrayList<Integer> maxValues; // Max value for each index: eg. [9, 8, 7, ..., 1]
    ArrayList<Boolean> isMaxValued;

    /**
     * Create a set of actions with base set inputActions
     *
     * @param inputActions Set of actions
     */
    public SetOfActions(Set<String> inputActions) {
        actions = inputActions.toArray(new String[0]);
        maxIndex = inputActions.size() - 1;
        //currentSet = new HashSet<>();
        currentStatus = new ArrayList<Integer>();
        size = -2;
        maxValues = new ArrayList<>();
        isMaxValued = new ArrayList<>();
        for (int i = 0; i < inputActions.size(); i++) {
            maxValues.add(inputActions.size() - i - 1);
            isMaxValued.add(false);
        }
    }

    private int getLowerIndexForNonMax() {
        for (int i = 0; i < isMaxValued.size(); i++) {
            if (!isMaxValued.get(i)) {
                return i;
            }
        }
        return isMaxValued.size();
    }

    public boolean increment() {
        //Initialize
        if (size == -2) { // empty set is the first to be found after an increment
            size = -1;
            return true;
        } else if (size == -1) {
            currentStatus.add(0);
            size = 0;
            return true;
        } else {
            // Find the lower-index non-max value
            int lowerIndexForNonMax = this.getLowerIndexForNonMax();
            if (lowerIndexForNonMax == isMaxValued.size()) {
                // ie. all sets are generated
                return false;
            }

            // This index is incremented, the ones before are "reset" while guaranteeing an increasing
            // Step 1: this index
            int newValue;
            if (lowerIndexForNonMax >= currentStatus.size()) { // It might never be ">"
                newValue = 0;
                currentStatus.add(newValue);
            } else {
                newValue = currentStatus.get(lowerIndexForNonMax) + 1;
                currentStatus.set(lowerIndexForNonMax, newValue);
            }
            if (newValue == maxValues.get(lowerIndexForNonMax)) {
                // If the new value is its max, update the table
                isMaxValued.set(lowerIndexForNonMax, true);
            }
            // Step 2: deal with the following units
            int newValueUnit = newValue;
            int readingIndex;
            for (int i = 0; i < lowerIndexForNonMax; i++) {
                newValueUnit++;
                readingIndex = lowerIndexForNonMax - i - 1;
                currentStatus.set(readingIndex, newValueUnit);
                //Check for max
                isMaxValued.set(readingIndex, maxValues.get(readingIndex) == newValueUnit);
            }


/*            if (currentStatus.get(size) < maxIndex) {
                currentStatus.set(size, currentStatus.get(size)+1);
            } else {
                size += 1;
                maxIndex -= 1;
                currentStatus.add(0);
            }*/
            return true;
        }
    }

    public Set<String> getSet() {
        Set<String> returnSet = new HashSet<>();
        if (currentStatus.size() > 0) {
            for (int i = 0; i < currentStatus.size(); i++) {
                returnSet.add(actions[currentStatus.get(i)]);
            }
        }
        return returnSet;
    }

    public int getSize() {
        return currentStatus.size(); // Return the number of elements
    }
}
