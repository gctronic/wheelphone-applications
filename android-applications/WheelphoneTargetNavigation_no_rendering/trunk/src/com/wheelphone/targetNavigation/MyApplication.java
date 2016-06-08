package com.wheelphone.targetNavigation;

import android.app.Application;
import org.acra.*;
import org.acra.annotation.*;

@ReportsCrashes(
		mailTo = "stefano@gctronic.com",
		customReportContent = { ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME, ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL, ReportField.CUSTOM_DATA, ReportField.AVAILABLE_MEM_SIZE, ReportField.STACK_TRACE, ReportField.LOGCAT },                
		mode = ReportingInteractionMode.TOAST,
		resToastText = R.string.crash_toast_text)

public class MyApplication extends Application {
	@Override
    public void onCreate() {
        super.onCreate();

        // The following line triggers the initialization of ACRA
        ACRA.init(this);
    }
}
