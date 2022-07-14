package utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SetOfActions {
    String[] actions;
    int maxIndex;
    int size; // Size if the max index in currentStatus
    ArrayList<Integer> currentStatus; // Set of integer representing the index of the elements in the set


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
        size = -1;
    }

    public boolean increment() {
        //Initialize
        if (size == -1) {
            currentStatus.add(0);
            size = 0;
            return true;
        } else if (maxIndex==-1) {
            // ie. all sets are generated
            return false;
        } else {
            if (currentStatus.get(size) < maxIndex) {
                currentStatus.set(size, currentStatus.get(size)+1);
            } else {
                size += 1;
                maxIndex -= 1;
                currentStatus.add(0);
            }
            return true;
        }
    }

    public Set<String> getSet() {
        Set<String> returnSet = new HashSet<>();
        if(size!=-1) {
            for(int i=0; i< currentStatus.size(); i++) {
                returnSet.add(actions[currentStatus.get(i)]);
            }
        }
        return returnSet;
    }
}
