/**
 *  Battery Monitor
 *
 *  Copyright 2020, DarwinsDen.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */

definition(
    name: "Battery Monitor", namespace: "darwinsden", author: "Darwin",
    description: "Monitor your battery devices and display a dashboard tile with battery status summary information",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
    page name: "pageMain"
    page name: "pageSettings"
    page name: "pageProperties"
    page name: "pageNotifications"
    page name: "pageTileSettings"
    page name: "pageDevices"
}

def pageMain() {
    
    def pageProperties = [
        name: "pageMain",
        title: "Battery Monitor",
        nextPage: null,
        install: true,
        uninstall: true
    ]

    return dynamicPage(pageProperties) {
        initBatteryStateList()
        updateSortedBatteryData()
        section("Tile Display:") {
                paragraph "<div style='max-width: 400px'>${getTileStr()}</div>"
        }

section("Settings") {
            href "pageDevices", title: "Battery Devices", description: "Select your battery devices"
            href "pageSettings", title: "General Settings", description: "Manage general app settings"
            href "pageTileSettings", title: "Tile Display Settings", description: "Manage dashboard tile display"
            href "pageNotifications", title: "Notification Settings", description: "Manage notifications"
        }
    }
}

def pageDevices() {
    def inputBatteryDevices = [name: "batteryDevices", type: "capability.battery", title: "Monitor which battery devices?", multiple: true, required: false]

    def pageProperties = [
        name: "pageDevices",
        title: "Battery Devices",
        nextPage: null,
        install: false,
        uninstall: false
    ]
    
    return dynamicPage(pageProperties) {

       section('Select your battery devices') {
            input inputBatteryDevices
        }
    }
}

def pageSettings() {

    def pageProperties = [
        name: "pageSettings",
        title: "Battery Monitor - Settings",
        nextPage: null,
        install: false,
        uninstall: false
    ]
    
    return dynamicPage(pageProperties) {

        section("Battery Monitor Settings") {
            input "monitorEvents", "bool", title: "Subscribe to additional (non-battery) device events. By default, this app will subscribe to battery status events for " +
                "the selected battery devices for the purpose of providing device health status for notification and dashboard tile display. If this option is selected, " +
                "the app will also monitor for other non-battery events from the devices. " +
                "Turning this option on may decrease false positive battery/device status indications but " +
                "will add a small amount of additional hub resource overhead.",
                required: false, submitOnChange: true, defaultValue: false
            input "batteryInactivityDays", "number", title: "Number of days without status before a device is considered inactive for tile display and notification.", required: false, defaultValue : 5
        }
        
        section([title: "Other Options", mobileOnly: true]) {
            label title: "Assign a name for the app (optional)", required: false
        }
    }
}

def pageTileSettings() {

    def pageProperties = [
        name: "pageTileSettings",
        title: "Tile Display - Settings",
        nextPage: null,
        install: false,
        uninstall: false
    ]
    
    return dynamicPage(pageProperties) {

        section("Update battery tile status at this interval") {
            input "updateFrequency", "enum", title: "Update tile at this interval", required: false, defaultValue: "3 Hours", options: [
                [1: "10 Minutes"],
                [2: "30 Minutes"],
                [3: "1 Hour"],
                [4: "3 Hours"]
            ]
        }

        section("Tile Display layout settings") {
            input "devicesToDisplay", "number", title: "Number of battery devices to display on the tile [default: 10].", defaultValue: 10, required: false
            input "tileWidth", "number", title: "Tile width number of characters [default: 25].", defaultValue: 25, required: false
            input "tileFontSize", "number", title: "Tile font size in px  [default: 20].", defaultValue: 20, required: false
        }
    }
}


def pageNotifications() {

    def pageProperties = [
        name: "pageNotifications",
        title: "Notifications - Settings",
        nextPage: null,
        install: false,
        uninstall: false
    ]
    
    return dynamicPage(pageProperties) {

       section("Send battery level and device inactivity warnings. Notifications will be sent only if thresholds are exceeded " +
               "once a day at the specified time") {
            input "notifyTime", "time", title: "Time of day to check and send notifications", required: false, submitOnChange: true
            input "notifyOfLevel", "bool", title: "Enable battery % level notifications?", required: false, submitOnChange: true, defaultValue: false
            input "batteryThreshold", "number", title: "Battery % level for notification.", required: false, defaultValue: 50
            input "notifyOfInactivity", "bool", title: "Enable notifications if battery device has had no recent activity. " +
                "See General Settings for inactivity threshold values", required: false, submitOnChange: true, defaultValue: false
            input(name: "notifyDevices", type: "capability.notification", title: "Send to these notification devices", required: false, multiple: true, submitOnChange: true)
       }
    }
}

def eventCheck(evt) {
    state.batteryData[evt.deviceId.toString()].lastUpdateTime = msToMins(now())
    //log.debug "received event $evt"
}

def notificationCheck() {
    updateBatteryStateData()
    runIn(5, checkNotifications)
}

def updateBatteryStateData() {
    batteryDevices.each {
        def batVal = it.currentValue("battery")?.toInteger()
        if (batVal) {
            state.batteryData[it.id.toString()].level = batVal
        }
        state.batteryData[it.id.toString()].name = it.displayName
    }
}

def checkNotifications() {
    if (notifyDevices) {
       String message = ""
       String newLine = "<br>"
       state.batteryData.each {key, value ->
           if (notifyOfLevel?.toBoolean()) {
               if (batteryThreshold && value.level <= batteryThreshold.toInteger()) {
                   message += value.name + " battery level is: ${value.level}%." + newLine
               }
           }
           if (notifyOfInactivity?.toBoolean()) {
               daysSinceLastReport = minsToDays(msToMins(now()) - value.lastUpdateTime)
               if (daysSinceLastReport >= (batteryInactivityDays?.toInteger() ?: 3)) {
                  message += value.name  + " has not updated its status in ${daysSinceLastReport} days." + newLine
               }
           }
        }
        if (message.length()) {
           notifyDevices.each {
               it.deviceNotification(message)
           }
        }
    }
}

def updateSortedBatteryData() {
    def battlistMap = []
    batteryDevices.each() {
        Integer batVal = -1
        Integer minutesSinceLastReport
        if (it.currentValue("battery") != null) {
            batVal = it.currentValue("battery").toInteger()
        }
        battlistMap += [
            [battery: batVal, lastUpdateTime: state.batteryData[it.deviceId.toString()].lastUpdateTime, name: "${it.displayName}", id: it.deviceId.toString()]
        ]
    }
    def battlistSorted = battlistMap.sort { a,b ->
        a.battery <=> b.battery ?: a.lastUpdateTime <=> b.lastUpdateTime ?: a.name <=> b.name
    }
    state.batteryListSorted = battlistSorted
}

def installed() {
    initialize()
}

def updated() {
    log.debug "updated"
    initialize()
}

def getBattDevice() {
    def deviceIdStr = null
    if (state.childDeviceId) {
        deviceIdStr = state.childDeviceId
    } else {
        def devices = getChildDevices()
        if (devices.size() > 0) {
            deviceIdStr = getChildDevices().first().getDeviceNetworkId()
            state.childDeviceId = deviceIdStr
        }
    }
    return getChildDevice(deviceIdStr)
}

def getTileStr() {
    //log.debug "updating tile string..."
    String tileString
    if (state.batteryListSorted) {
        final Integer maxLength = 1024
        final Integer minutesInDay = 1440
        def list = state.batteryListSorted
        Integer listSize = list.size()
        if (state.tileEntries && state.tileEntries.toInteger() < listSize) {
            listSize = state.tileEntries.toInteger()
        }
        String titleStr = "Battery Status"
        String titleInfo = " (${listSize} Lowest)"
        if (titleStr.length() + titleInfo.length() <= state.tileWidthChars.toInteger()) {
            titleStr += titleInfo
        }
        Integer fontSize = settings.tileFontSize?.toInteger() ?: 20
        String cssStyle =
            "<style>" +
            ".batmondiv1 {text-align:center;padding:4px;background-color:rgba(0,40,0,0.4);border-radius:10px;border:2px solid #006900;" + "font-size:${fontSize}px}" +
            ".batmondiv2 {line-height: 1.0}" +
            ".batmontab1 tr td:nth-child(odd) {text-align:right}" +
            ".batmontab1 tr td:nth-child(even) {text-align:left;padding-left:8px}" +
            "</style>"
        String tileStartStr = cssStyle + "<div class='batmondiv1'><b>&#128267${titleStr}</b><div class='batmondiv2'><table class='batmontab1'>"
        String tileEndStr = "</table></div></div>"
        String tileMidStr = ""

        for (i = 0; i < listSize; i++) {
            def blevel = list[i].battery.toInteger()
            String blevelStr = "--%"
            if (blevel > 0) {
                blevelStr = blevel.toString() + '%'
            }
            def ageDays = minsToDays(msToMins(now())-list[i].lastUpdateTime)
            String rowstyle = ""
            String ageStr = ""
            if (ageDays > (batteryInactivityDays?.toInteger() ?: 3)) {
                rowstyle = " style='color:#BEBEBE'"
                ageStr = " (${ageDays.toInteger()}"
                if (ageDays > 1) {
                    ageStr += " days)"
                } else {
                    ageStr += " day)"
                }
            }
            String battLevelStyle = ""
            if (blevel < 1) {
                battLevelStyle = " style='color:darkgray'"
            } else if (blevel < 50) {
                battLevelStyle = " style='color:red'"
            } else if (blevel < 75) {
                battLevelStyle = " style='color:orange'"
            }
            Integer nameLength = list[i].name.length() + ageStr.length()
            if (list[i].name.length() + ageStr.length() > state.tileWidthChars.toInteger()) {
                nameLength = state.tileWidthChars.toInteger() - ageStr.length()
            } else {
                nameLength = list[i].name.length()
            }
            String nameStr = list[i].name.substring(0, nameLength) + ageStr
            //log.debug nameStr + " " + (msToMins(now())-list[i].lastUpdateTime).toString()
            String lineStr = "<tr${rowstyle}>"
            lineStr += "<td${battLevelStyle}>${blevelStr}</td>"
            lineStr += "<td>${nameStr}</td>"
            lineStr += "</tr>"
            if (tileStartStr.length() + tileMidStr.length() + tileEndStr.length() + lineStr.length() < maxLength) {
                tileMidStr += lineStr
            } else {
                log.debug("limiting tile to ${maxLength} chars")
                break;
            }
        }
        tileString = tileStartStr + tileMidStr + tileEndStr
    } else {
        tileString = "Battery status not yet received"
    }
    return tileString
}

def setTile() {
    def battDevice = getBattDevice()
    if (battDevice) {
        String tileStr = getTileStr()
        //log.debug "size is ${tileStr.size()}"
        battDevice.sendEvent(name: "batteryTile", value: tileStr, displayed: true, isStateChange: true)
        //log.debug "updating tile..."
    } else {
        log.warn "Unable to update battery tile. Battery Tile device does not exist."
    }
}

private initBatteryDevice() {
    def battDevice = getBattDevice()
    if (!battDevice) {
        def device = addChildDevice("darwinsden", "Battery Status Monitor", "BatteryMonitor" + now().toString(), null,
            [name: "Battery Status Monitor", label: "Battery Status Monitor", completedSetup: true])
        log.debug "Created battery status monitor device"
    } else {
        battDevice.initialize()
    }
}

def initBatteryStateList() {
    state.tileEntries = devicesToDisplay?.toInteger() ?: 10
    state.tileWidthChars = tileWidth?.toInteger() ?: 25
    if (!state.batteryData) {
        state.batteryData = [:]
    }
    batteryDevices.each() {
        //state.batteryData[it.id.toString()].lastUpdateTime = msToMins(now()) - 96*60
        if (!state.batteryData[it.id.toString()]) {
            log.debug "Adding batteryData for: ${it.displayName}"
            state.batteryData[it.id.toString()] = [
                lastUpdateTime: msToMins(now()), level: -1, name: it.displayName
            ]
        }
    }
    def keysToRemove = []
    state.batteryData.each {key, value ->
        def itemFound = false
        batteryDevices.each() {
            if (it.id == key) {
                itemFound = true;
            }
        }
        if (!itemFound) {
            keysToRemove += key
        }
    }
    keysToRemove.each() {
        log.debug "Removing batteryData for: ${state.batteryData.remove(it).name}"
        state.batteryData.remove(it)
    }
}

def scheduleNotificationCheck() {
    try {
        String notificationTime
        if (notifyTime) {
            notificationTime = notifyTime
        } else {
           Integer zoneOffset = location.getTimeZone().rawOffset/10/60/60 
           String zoneOffsetStr = String.format("%04d", Math.abs(zoneOffset));  
           if (zoneOffset < 0) {
               zoneOffsetStr =  '-' + zoneOffsetStr 
           } else { 
               zoneOffsetStr = '+' + zoneOffsetStr 
           }
           notificationTime = "2020-08-11T14:00:00.000" + zoneOffsetStr //Set 2PM local as default
        }
        if (notifyOfLevel?.toBoolean() || notifyOfInactivity?.toBoolean()) {
            log.debug "Scheduling notification checks for: ${notificationTime}"
            schedule(notificationTime, notificationCheck)
        }
    } catch (Exception e) {
        log.error "Exception scheduling notifications: $e"
    }
}
    
def initialize() {
    log.trace "Initializing Battery Monitor"
    initBatteryDevice()
    initBatteryStateList()
    unsubscribe()
    unschedule()
    scheduleNotificationCheck()
    scheduleTileUpdates()
    subscribeDevices()
    updateTile()
}

def scheduleTileUpdates() {
    if (settings.updateFrequency != null && settings.updateFrequency != "") {
        switch (settings?.updateFrequency.toInteger()) {
            case 1:
                runEvery10Minutes(updateTile)
                break
            case 2:
                runEvery30Minutes(updateTile)
                break
            case 3:
                runEvery1Hour(updateTile)
                break
            case 4:
                runEvery3Hours(updateTile)
                break
            default:
                log.debug "No regular check frequency chosen."
                break
        }
    } else {
        runEvery3Hours(updateTile)
    }
}

def updateTile() {
    runIn(5, updateSortedBatteryData)
    runIn(10, setTile)
}

def subscribeDevices() {
    subscribe(batteryDevices, "battery", eventCheck, [filterEvents: false])
    if (monitorEvents) {
        batteryDevices.each {bat -> 
            bat.capabilities.each {cap ->
                 cap.attributes.each {attr ->
                      //log.debug "attr is $attr.name"
                      //subscribe to some common attributes for status check...
                      if (attr.name.matches("contact|temperature|switch|motion|water|shock|lock|smoke|alarm|presence|pushed|battery")) {
                           //log.debug "subscribing to device: ${bat} attribute ${attr.name}"
                           subscribe(bat, attr.name, eventCheck, [filterEvents: false])
                      }
                 }
            }
        }
    }
}

Long msToMins (value) {
    return (value/60000).toLong()
}  

Long minsToDays (value) {
    return (value/1440).toLong()
}  

private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

def uninstalled() {
    removeChildDevices(getChildDevices())
}

def refresh(child) {
    updateTile()
}
        initBatteryStateList()
        updateSortedBatteryData()
        section("Tile Display:") {
                paragraph "<div style='max-width: 400px'>${getTileStr()}</div>"
        }

section("Settings") {
            href "pageDevices", title: "Battery Devices", description: "Select your battery devices"
            href "pageSettings", title: "General Settings", description: "Manage general app settings"
            href "pageTileSettings", title: "Tile Display Settings", description: "Manage dashboard tile display"
            href "pageNotifications", title: "Notification Settings", description: "Manage notifications"
        }
    }
}

def pageDevices() {
    def inputBatteryDevices = [name: "batteryDevices", type: "capability.battery", title: "Monitor which battery devices?", multiple: true, required: false]

    def pageProperties = [
        name: "pageDevices",
        title: "Battery Devices",
        nextPage: null,
        install: false,
        uninstall: false
    ]
    
    return dynamicPage(pageProperties) {

       section('Select your battery devices') {
            input inputBatteryDevices
        }
    }
}

def pageSettings() {

    def pageProperties = [
        name: "pageSettings",
        title: "Battery Monitor - Settings",
        nextPage: null,
        install: false,
        uninstall: false
    ]
    
    return dynamicPage(pageProperties) {

        section("Battery Monitor Settings") {
            input "monitorEvents", "bool", title: "Subscribe to additional (non-battery) device events. By default, this app will subscribe to battery status events for " +
                "the selected battery devices for the purpose of providing device health status for notification and dashboard tile display. If this option is selected, " +
                "the app will also monitor for other non-battery events from the devices. " +
                "Turning this on may decrease false positive battery/device status indications but " +
                "will add a small amount of additional hub resource overhead.",
                required: false, submitOnChange: true, defaultValue: false
            input "batteryInactivityDays", "number", title: "Number of days without status before a device is considered inactive for tile display and notification.", required: false, defaultValue : 5
        }
        
        section([title: "Other Options", mobileOnly: true]) {
            label title: "Assign a name for the app (optional)", required: false
        }
    }
}

def pageTileSettings() {

    def pageProperties = [
        name: "pageTileSettings",
        title: "Tile Display - Settings",
        nextPage: null,
        install: false,
        uninstall: false
    ]
    
    return dynamicPage(pageProperties) {

        section("Update battery tile status at this interval") {
            input "updateFrequency", "enum", title: "Update tile at this interval", required: false, defaultValue: "3 Hours", options: [
                [1: "10 Minutes"],
                [2: "30 Minutes"],
                [3: "1 Hour"],
                [4: "3 Hours"]
            ]
        }

        section("Tile Display layout settings") {
            input "devicesToDisplay", "number", title: "Number of battery devices to display on the tile [default: 10].", defaultValue: 10, required: false
            input "tileWidth", "number", title: "Tile width number of characters [default: 25].", defaultValue: 25, required: false
            input "tileFontSize", "number", title: "Tile font size in px  [default: 20].", defaultValue: 20, required: false
        }
    }
}


def pageNotifications() {

    def pageProperties = [
        name: "pageNotifications",
        title: "Notifications - Settings",
        nextPage: null,
        install: false,
        uninstall: false
    ]
    
    return dynamicPage(pageProperties) {

       section("Send battery level and device inactivity warnings. Notifications will be sent only if thresholds are exceeded " +
               "once a day at the specified time") {
            input "notifyTime", "time", title: "Time of day to check and send notifications", required: false, submitOnChange: true
            input "notifyOfLevel", "bool", title: "Enable battery % level notifications?", required: false, submitOnChange: true, defaultValue: false
            input "batteryThreshold", "number", title: "Battery % level for notification.", required: false, defaultValue: 50
            input "notifyOfInactivity", "bool", title: "Enable notifications if battery device has had no recent activity. " +
                "See General Settings for inactivity threshold values", required: false, submitOnChange: true, defaultValue: false
            input(name: "notifyDevices", type: "capability.notification", title: "Send to these notification devices", required: false, multiple: true, submitOnChange: true)
       }
    }
}

def eventCheck(evt) {
    state.batteryData[evt.deviceId.toString()].lastUpdateTime = msToMins(now())
}

def notificationCheck() {
    updateBatteryStateData()
    runIn(5, checkNotifications)
}

def updateBatteryStateData() {
    batteryDevices.each {
        def batVal = it.currentValue("battery")?.toInteger()
        if (batVal) {
            state.batteryData[it.id.toString()].level = batVal
        }
        state.batteryData[it.id.toString()].name = it.displayName
    }
}

def checkNotifications() {
    if (notifyDevices) {
       String message = ""
       String newLine = "<br>"
       state.batteryData.each {key, value ->
           if (notifyOfLevel?.toBoolean()) {
               if (batteryThreshold && value.level <= batteryThreshold.toInteger()) {
                   message += value.name + " battery level is: ${value.level}%." + newLine
               }
           }
           if (notifyOfInactivity?.toBoolean()) {
               daysSinceLastReport = minsToDays(msToMins(now()) - value.lastUpdateTime)
               if (daysSinceLastReport >= (batteryInactivityDays?.toInteger() ?: 3)) {
                  message += value.name  + " has not updated its status in ${daysSinceLastReport} days." + newLine
               }
           }
        }
        if (message.length()) {
           notifyDevices.each {
               it.deviceNotification(message)
           }
        }
    }
}

def updateSortedBatteryData() {
    def battlistMap = []
    batteryDevices.each() {
        Integer batVal = -1
        Integer minutesSinceLastReport
        if (it.currentValue("battery") != null) {
            batVal = it.currentValue("battery").toInteger()
        }
        battlistMap += [
            [battery: batVal, lastUpdateTime: state.batteryData[it.deviceId.toString()].lastUpdateTime, name: "${it.displayName}", id: it.deviceId.toString()]
        ]
    }
    def battlistSorted = battlistMap.sort { a,b ->
        a.battery <=> b.battery ?: a.lastUpdateTime <=> b.lastUpdateTime ?: a.name <=> b.name
    }
    state.batteryListSorted = battlistSorted
}

def installed() {
    initialize()
}

def updated() {
    log.debug "updated"
    initialize()
}

def getBattDevice() {
    def deviceIdStr = null
    if (state.childDeviceId) {
        deviceIdStr = state.childDeviceId
    } else {
        def devices = getChildDevices()
        if (devices.size() > 0) {
            deviceIdStr = getChildDevices().first().getDeviceNetworkId()
            state.childDeviceId = deviceIdStr
        }
    }
    return getChildDevice(deviceIdStr)
}

def getTileStr() {
    //log.debug "updating tile string..."
    String tileString
    if (state.batteryListSorted) {
        final Integer maxLength = 1024
        final Integer minutesInDay = 1440
        def list = state.batteryListSorted
        Integer listSize = list.size()
        if (state.tileEntries && state.tileEntries.toInteger() < listSize) {
            listSize = state.tileEntries.toInteger()
        }
        String titleStr = "Battery Status"
        String titleInfo = " (${listSize} Lowest)"
        if (titleStr.length() + titleInfo.length() <= state.tileWidthChars.toInteger()) {
            titleStr += titleInfo
        }
        Integer fontSize = settings.tileFontSize?.toInteger() ?: 20
        String cssStyle =
            "<style>" +
            ".batmondiv1 {text-align:center;padding:4px;background-color:rgba(0,40,0,0.4);border-radius:10px;border:2px solid #006900;" + "font-size:${fontSize}px}" +
            ".batmondiv2 {line-height: 1.0}" +
            ".batmontab1 tr td:nth-child(odd) {text-align:right}" +
            ".batmontab1 tr td:nth-child(even) {text-align:left;padding-left:8px}" +
            "</style>"
        String tileStartStr = cssStyle + "<div class='batmondiv1'><b>&#128267${titleStr}</b><div class='batmondiv2'><table class='batmontab1'>"
        String tileEndStr = "</table></div></div>"
        String tileMidStr = ""

        for (i = 0; i < listSize; i++) {
            def blevel = list[i].battery.toInteger()
            String blevelStr = "--%"
            if (blevel > 0) {
                blevelStr = blevel.toString() + '%'
            }
            def ageDays = minsToDays(msToMins(now())-list[i].lastUpdateTime)
            String rowstyle = ""
            String ageStr = ""
            if (ageDays > (batteryInactivityDays?.toInteger() ?: 3)) {
                rowstyle = " style='color:#BEBEBE'"
                ageStr = " (${ageDays.toInteger()}"
                if (ageDays > 1) {
                    ageStr += " days)"
                } else {
                    ageStr += " day)"
                }
            }
            String battLevelStyle = ""
            if (blevel < 1) {
                battLevelStyle = " style='color:darkgray'"
            } else if (blevel < 50) {
                battLevelStyle = " style='color:red'"
            } else if (blevel < 75) {
                battLevelStyle = " style='color:orange'"
            }
            Integer nameLength = list[i].name.length() + ageStr.length()
            if (list[i].name.length() + ageStr.length() > state.tileWidthChars.toInteger()) {
                nameLength = state.tileWidthChars.toInteger() - ageStr.length()
            } else {
                nameLength = list[i].name.length()
            }
            String nameStr = list[i].name.substring(0, nameLength) + ageStr
            //log.debug nameStr + " " + (msToMins(now())-list[i].lastUpdateTime).toString()
            String lineStr = "<tr${rowstyle}>"
            lineStr += "<td${battLevelStyle}>${blevelStr}</td>"
            lineStr += "<td>${nameStr}</td>"
            lineStr += "</tr>"
            if (tileStartStr.length() + tileMidStr.length() + tileEndStr.length() + lineStr.length() < maxLength) {
                tileMidStr += lineStr
            } else {
                log.debug("limiting tile to ${maxLength} chars")
                break;
            }
        }
        tileString = tileStartStr + tileMidStr + tileEndStr
    } else {
        tileString = "Battery status not yet received"
    }
    return tileString
}

def setTile() {
    def battDevice = getBattDevice()
    if (battDevice) {
        String tileStr = getTileStr()
        //log.debug "size is ${tileStr.size()}"
        battDevice.sendEvent(name: "batteryTile", value: tileStr, displayed: true, isStateChange: true)
        //log.debug "updating tile..."
    } else {
        log.warn "Unable to update battery tile. Battery Tile device does not exist."
    }
}

private initBatteryDevice() {
    def battDevice = getBattDevice()
    if (!battDevice) {
        def device = addChildDevice("darwinsden", "Battery Status Monitor", "BatteryMonitor" + now().toString(), null,
            [name: "Battery Status Monitor", label: "Battery Status Monitor", completedSetup: true])
        log.debug "Created battery status monitor device"
    } else {
        battDevice.initialize()
    }
}

def initBatteryStateList() {
    state.tileEntries = devicesToDisplay?.toInteger() ?: 10
    state.tileWidthChars = tileWidth?.toInteger() ?: 25
    if (!state.batteryData) {
        state.batteryData = [:]
    }
    batteryDevices.each() {
        //state.batteryData[it.id.toString()].lastUpdateTime = msToMins(now()) - 96*60
        if (!state.batteryData[it.id.toString()]) {
            log.debug "Adding batteryData for: ${it.displayName}"
            state.batteryData[it.id.toString()] = [
                lastUpdateTime: msToMins(now()), level: -1, name: it.displayName
            ]
        }
    }
    def keysToRemove = []
    state.batteryData.each {key, value ->
        def itemFound = false
        batteryDevices.each() {
            if (it.id == key) {
                itemFound = true;
            }
        }
        if (!itemFound) {
            keysToRemove += key
        }
    }
    keysToRemove.each() {
        log.debug "Removing batteryData for: ${state.batteryData.remove(it).name}"
        state.batteryData.remove(it)
    }
}

def scheduleNotificationCheck() {
    try {
        String notificationTime
        if (notifyTime) {
            notificationTime = notifyTime
        } else {
           Integer zoneOffset = location.getTimeZone().rawOffset/10/60/60 
           String zoneOffsetStr = String.format("%04d", Math.abs(zoneOffset));  
           if (zoneOffset < 0) {
               zoneOffsetStr =  '-' + zoneOffsetStr 
           } else { 
               zoneOffsetStr = '+' + zoneOffsetStr 
           }
           notificationTime = "2020-08-11T14:00:00.000" + zoneOffsetStr //Set 2PM local as default
        }
        if (notifyOfLevel?.toBoolean() || notifyOfInactivity?.toBoolean()) {
            log.debug "Scheduling notification checks for: ${notificationTime}"
            schedule(notificationTime, notificationCheck)
        }
    } catch (Exception e) {
        log.error "Exception scheduling notifications: $e"
    }
}
    
def initialize() {
    log.trace "Initializing Battery Monitor"
    initBatteryDevice()
    initBatteryStateList()
    unsubscribe()
    unschedule()
    scheduleNotificationCheck()
    scheduleTileUpdates()
    subscribeDevices()
    updateTile()
}

def scheduleTileUpdates() {
    if (settings.updateFrequency != null && settings.updateFrequency != "") {
        switch (settings?.updateFrequency.toInteger()) {
            case 1:
                runEvery10Minutes(updateTile)
                break
            case 2:
                runEvery30Minutes(updateTile)
                break
            case 3:
                runEvery1Hour(updateTile)
                break
            case 4:
                runEvery3Hours(updateTile)
                break
            default:
                log.debug "No regular check frequency chosen."
                break
        }
    } else {
        runEvery3Hours(updateTile)
    }
}

def updateTile() {
    runIn(5, updateSortedBatteryData)
    runIn(10, setTile)
}

def subscribeDevices() {
    subscribe(batteryDevices, "battery", eventCheck, [filterEvents: false])
    if (monitorEvents) {
        batteryDevices.each {bat -> 
            bat.capabilities.each {cap ->
                 cap.attributes.each {attr ->
                      //subscribe to some common attributes for status check...
                      if (attr.name.matches("contact|temperature|switch|motion|water|shock|lock|smoke|alarm|presence")) {
                           //log.debug "subscribing to device: ${bat} attribute ${attr.name}"
                           subscribe(bat, attr.name, eventCheck, [filterEvents: false])
                      }
                 }
            }
        }
    }
}

Long msToMins (value) {
    return (value/60000).toLong()
}  

Long minsToDays (value) {
    return (value/1440).toLong()
}  

private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

def uninstalled() {
    removeChildDevices(getChildDevices())
}

def refresh(child) {
    updateTile()
}
