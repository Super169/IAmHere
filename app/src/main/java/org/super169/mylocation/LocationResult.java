package org.super169.mylocation;
import android.app.SharedElementCallback;
import android.location.Location;
/**
 * Created by makk on 25/08/2017.
 */

public class LocationResult {

    public enum ResultStatus {
        EMPTY,
        READY,
        UNEXPECTED_STATUS,
        UNEXPECTED_ERROR,
        NO_PERMISSION,
        NO_SERVICE,
        FAIL_GET_LOCATION
    }
    private ResultStatus mStatus;
    private Location mLocation;
    private String mMessage;

    public LocationResult() {
        mStatus = ResultStatus.EMPTY;
        mLocation = null;
        mMessage = "";
    }

    public void SetError(ResultStatus status, String message) {
        switch (status) {
            case READY:
                SetResult(ResultStatus.UNEXPECTED_STATUS, null, "Unexpected Ready; " + message);
            default:
                SetResult(status, null, message);
        }
    }

    public void SetLocation(Location location) {
        mStatus = ResultStatus.READY;
        mLocation = location;
        mMessage = "";
    }

    private void SetResult(ResultStatus status, Location location, String message) {
        mStatus = status;
        mLocation = location;
        mMessage = message;
    }

    public boolean IsReady() { return (mStatus == ResultStatus.READY); }
    public Location location() { return mLocation;}
    public ResultStatus status() {return mStatus; }
    public String message() { return mMessage; }
}
