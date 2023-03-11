package com.example.modumessenger.Global;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ActivityStack {

    private static int sLastActivityNumber = 0;
    private static ActivityStack mThis;
    private ArrayList<ActivityRef> mActivityAliveList;

    private class ActivityRef {
        WeakReference<Activity> ref = new WeakReference<Activity>(null);
        boolean isResume = false;
        String aliveID = "";
        int taskID = 0;

    }

    private ActivityStack() {
        mActivityAliveList = new ArrayList<ActivityRef>();
    }

    public static ActivityStack getInstance() {
        if(mThis == null) {
            mThis = new ActivityStack();
        }
        return mThis;
    }

    public Integer[] getAliveTaskIDs() {
        cleanGarbageActivities();
        cleanGarbageActivities();
        Set<Integer> taskSet = new HashSet<Integer>();
        for(ActivityRef ref : mActivityAliveList) {
            taskSet.add(ref.taskID);
        }
        Integer[] aliveTaskIds =  taskSet.toArray(new Integer[0]);
        return aliveTaskIds;
    }

    public Integer[] getAliveTaskIDByBaseActvity(Class<?> baseActivityClass) {
        cleanGarbageActivities();
        ArrayList<ActivityRef> subList = subList(baseActivityClass);
        Set<Integer> taskSet = new HashSet<Integer>();
        for(ActivityRef ref : subList) {
            taskSet.add(ref.taskID);
        }
        Integer[] aliveTaskIds =  taskSet.toArray(new Integer[0]);
        return aliveTaskIds;
    }

    public String[] getAliveIDsInTask(int taskID) {
        cleanGarbageActivities();
        ArrayList<ActivityRef> subList = subList(taskID);
        String[] aliveIDs = new String[size()];
        for(int i = 0, n = subList.size(); i < n; ++i) {
            aliveIDs[i] =  subList.get(i).aliveID;
        }
        return aliveIDs;
    }

    public String[] getAliveIDs() {
        cleanGarbageActivities();
        String[] aliveIDs = new String[size()];
        for(int i = 0, n = size(); i < n; ++i) {
            aliveIDs[i] =  mActivityAliveList.get(i).aliveID;
        }
        return aliveIDs;
    }

    public String getAliveID(Activity activity) {
        ActivityRef ref = getActivityRef(activity);
        if(ref != null) return ref.aliveID;
        else return "";
    }

    public Activity getTopActivityInTask(int taskID) {
        cleanGarbageActivities();
        ArrayList<ActivityRef> subList = subList(taskID);
        int size =  subList.size();
        if(size > 0) {
            return subList.get(size - 1).ref.get();
        }
        return null;
    }

    public Activity getBaseActivityInTask(int taskID) {
        cleanGarbageActivities();
        ArrayList<ActivityRef> subList = subList(taskID);
        if(subList.size() > 0) {
            return subList.get(0).ref.get();
        }
        return null;

    }

    public Activity getActivity(String aliveID) {
        cleanGarbageActivities();
        return getActivityRef(aliveID).ref.get();
    }

    public boolean isRunning() {
        return size() > 0;
    }

    public boolean isTaskRunning(int task) {
        cleanGarbageActivities();
        return subList(task).size() > 0;
    }

    public int sizeInTask(int task) {
        cleanGarbageActivities();
        return subList(task).size();
    }

    public int size() {
        cleanGarbageActivities();
        return mActivityAliveList.size();
    }

    public boolean isForeground() {
        Activity activity = getForegroundActivity();
        return activity != null;
    }

    public boolean isForeground(int taskId) {
        Activity activity = getForegroundActivity();
        return activity != null && activity.getTaskId() == taskId;
    }

    public Activity getForegroundActivity() {
        cleanGarbageActivities();
        ActivityRef ref = getResumeActivityRef();
        if(ref != null && ref.ref != null) {
            return ref.ref.get();
        }
        return null;
    }

    public boolean regOnResumeState(Activity activity) {
        cleanGarbageActivities();
        ActivityRef ref = getActivityRef(activity);
        if(ref != null) {
            ref.isResume = true;
            return true;
        }
        return false;
    }

    public boolean regOnPauseState(Activity activity) {
        cleanGarbageActivities();
        ActivityRef ref = getActivityRef(activity);
        if(ref != null) {
            ref.isResume = false;
            return true;
        }
        return false;
    }

    public final void  regOnCreateState(Activity activity) {
        cleanGarbageActivities();
        addActivityRef(activity);
    }

    public final boolean regOnDestroyState(Activity activity) {
        return removeActivityRef(activity);
    }

    public String toString() {
        cleanGarbageActivities();
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("[");
        Integer[] taskIDs  =  getAliveTaskIDs();
        for(Integer taskID : taskIDs) {
            strBuilder.append("{\"taskId\":").append(taskID).append(",\"activityStack\":[");
            ArrayList<ActivityRef> subList = subList(taskID);
            for(ActivityRef ref : subList) {
                strBuilder.append("\"").append(ref.aliveID).append("\",");
            }
            strBuilder.append("]},");
        }
        strBuilder.append("]");
        return strBuilder.toString();
    }

    private ActivityRef createActivityRef(Activity activity) {
        ActivityRef activityRef = new ActivityRef();
        activityRef.ref = new WeakReference<Activity>(activity);
        activityRef.aliveID =  makeAliveID(activity);
        activityRef.taskID =  activity.getTaskId();
        return activityRef;
    }

    private String makeAliveID(Activity activity) {
        return sLastActivityNumber++ + ":" + activity.getClass().getName();
    }

    private void addActivityRef(Activity activity) {
        ActivityRef activityRef = createActivityRef(activity);
        mActivityAliveList.add(activityRef);
    }

    private boolean removeActivityRef(Activity activity) {
        Iterator<ActivityRef> iter =  mActivityAliveList.iterator();
        while(iter.hasNext()) {
            ActivityRef iterItem = iter.next();
            Activity itemActivity = iterItem.ref.get();
            if(iterItem.ref.get() != null && activity == itemActivity) {
                iter.remove();
                return true;
            }
        }
        return false;
    }

    private ActivityRef getActivityRef(Activity activity) {
        Iterator<ActivityRef> iter =  mActivityAliveList.iterator();
        while(iter.hasNext()) {
            ActivityRef iterItem = iter.next();
            Activity activityItem =  iterItem.ref.get();
            if(activityItem != null && activityItem == activity) {
                return iterItem;
            }
        }
        return null;
    }

    private ActivityRef getActivityRef(String aliveID) {
        Iterator<ActivityRef> iter =  mActivityAliveList.iterator();
        while(iter.hasNext()) {
            ActivityRef iterItem = iter.next();
            if(iterItem.aliveID.equals(aliveID)) {
                return iterItem;
            }
        }
        return null;
    }

    private ActivityRef getResumeActivityRef() {
        Iterator<ActivityRef> iter =  mActivityAliveList.iterator();
        while(iter.hasNext()) {
            ActivityRef iterItem = iter.next();
            Activity activityItem =  iterItem.ref.get();
            if(activityItem != null && iterItem.isResume) {
                return iterItem;
            }
        }
        return null;
    }

    private ArrayList<ActivityRef> subList(int taskID) {
        ArrayList<ActivityRef> subList = new ArrayList<ActivityRef>();
        Iterator<ActivityRef> iter =  mActivityAliveList.iterator();
        while(iter.hasNext()) {
            ActivityRef iterItem = iter.next();
            if(iterItem.taskID == taskID) {
                subList.add(iterItem);
            }
        }
        return subList;
    }

    private ArrayList<ActivityRef> subList(Class<?> activityClass) {
        ArrayList<ActivityRef> subList = new ArrayList<ActivityRef>();
        Iterator<ActivityRef> iter =  mActivityAliveList.iterator();
        while(iter.hasNext()) {
            ActivityRef iterItem = iter.next();
            if(iterItem.ref.get() != null && iterItem.ref.get().getClass().equals(activityClass)) {
                subList.add(iterItem);
            }
        }
        return subList;
    }

    @SuppressLint("NewApi")
    private void cleanGarbageActivities() {
        Iterator<ActivityRef> iter =  mActivityAliveList.iterator();
        while(iter.hasNext()) {
            ActivityRef iterItem = iter.next();
            if(iterItem.ref.get() == null || (android.os.Build.VERSION.SDK_INT >= 17 && iterItem.ref.get().isDestroyed())) {
                iter.remove();
                Log.i("remove", "destroyed : " + iterItem.aliveID);
            }
        }
    }
}
