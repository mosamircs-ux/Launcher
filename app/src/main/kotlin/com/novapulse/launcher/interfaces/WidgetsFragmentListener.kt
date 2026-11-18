package com.novapulse.launcher.interfaces

import com.novapulse.launcher.models.AppWidget

interface WidgetsFragmentListener {
    fun onWidgetLongPressed(appWidget: AppWidget)
}
