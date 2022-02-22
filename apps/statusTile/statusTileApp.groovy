/**
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions an limitations under the License.
 *
 */
definition(
    name: "Status Tile",
    namespace: "darwinsden", 
    author: "Darwin",
    description: "Create device status overview and history dashboard tiles.",
    category: "Convenience",
    iconUrl: dashIcon,
    iconX2Url: "")

preferences {
    page(name: "pageMain")
    page(name: "pageAddTile")
    page(name: "pageEditDevices")
    page(name: "pageEditTile")
    page(name: "pageEditLabel")
    page(name: "pageEditAttrStyle")
    page(name: "pageEditAttrFormat")
    page(name: "pageSelectEditTile")
    page(name: "pageSelectDevices")
    page(name: "pagePreferences")
}

import groovy.transform.Field

String version() {
    return "v0.1.01"
}

void deleteTile (Integer tileId) {
    logger ("Deleting tile: ${tileId}, number: ${tileId}", "debug")
    state.tileIdUsed[tileId-1] = false
    state.tileDeleted = true
    state.tileList.removeElement(tileId)
    clearTileData (tileId)
    deleteChildTileDevice (tileId)
}

Integer getNextTileId() {
    Integer nextTileId
    if (state.tileList == null) {
        state.tileIdUsed = []
        state.tileList = []
    }
    //Look for an unused tile Id
    if (state.tileIdUsed.size() > 0) {
      for (int i in 0 .. state.tileIdUsed.size() - 1) {
        if (!state.tileIdUsed[i]) {
            logger ("Re-using tile Id ${i + 1}","debug")
            nextTileId = i + 1
            break
        }
      }
    }
    if (!nextTileId) {
        nextTileId = state.tileIdUsed.size() + 1
    }
    return nextTileId
}
    
Integer addTile(Integer tileId = null) {
    if (tildId == null) {
        tileId = getNextTileId()
    }
    state.tileIdUsed[tileId - 1] = true
    logger ("Adding new tile with id: ${tileId}", "debug")
    state.tileList[state.tileList.size()] = tileId
    return tileId
}

void setTileData (Integer tileId, String param, def value) {
    if (tileId && param) {
        //log.debug "setting tile: ${tileId} param: '${param}' to '${value}'"
        if (!state."tile${tileId}Data") {
            log.debug "Adding new tile data state ${tileId} for param: '${param}' val: ${value}"
            state."tile${tileId}Data" = [:]
        }
        state."tile${tileId}Data"."${param}" = value 
    } else {
        log.warn "Invalid set for tile: ${tileId} param: '${param}' '${value}'"
    }  
}

void deleteTileData (Integer tileId, String param) {
    if (tileId && param) {
        //log.debug "removing tile: ${tileId} param: '${param}'"
        if (state."tile${tileId}Data") {
          //reqParams.remove('blah')
          state."tile${tileId}Data"=state."tile${tileId}Data".remove("${param}")
        }
     } else {
         log.warn "Invalid delete for tile: ${tileId} param: '${param}'"
    }  
}

def getTileData (Integer tileId, String param) {
    def val
    if (state."tile${tileId}Data" && tileId && param) {
       val = state."tile${tileId}Data"."${param}"
    } else {
       log.warn "Invalid get for tile: ${tileId} param: '${param}'"
    }
    return val
}

void clearTileData (Integer tileId) {
    try {
        Integer numEntries = getTileData(tileId, "numEntries") ?: 0
        for (Integer i in 1..numEntries) {    
          String devInput = "tile${tileId}Devices${i}"
          //app.updateSetting(devInput, [type: "capability", value: null]) 
          //  app.updateSetting(devInput, [type: "capability", value: ""]) 
            app.removeSetting("tile${tileId}Devices${i}")
       }
       state.remove("tile${tileId}Data".toString())
       state.remove("tile${tileId}History".toString())
       String tileString = "${tileId}"
       settings.each{key, val -> 
           //log.debug "checking ${key} ${val}"
           if (key.size() > 3 + tileString.size() && key.substring(0,4+tileString.size()) == "tile${tileId}") {
                //log.debug "clearing: ${key}"
                app.clearSetting(key)
                //app.removeSetting(key)
               
           }
       }
    } catch (e) {
                log.debug "Exception clearing tile data for tile ${tileId}: ${e}"
            }
}

def pageMain() {
    state.tileList?.each {
        createChildTileDevice (it)
    }
    state.thisState = "menuMain" 
    dynamicPage(name: "pageMain", title: "", install: true) {
        section {
            paragraph "<div style='margin-top: -10px'>"+dashboard("",0.8)+"</div>", width: 11
         
            input "btnRefresh", "button", title: refreshSymb, submitOnChange: true, width:1
            paragraph ""
            //href pageAddTileMenu, description: "", title: "<span style='font-size: 90%'>Add Tile</span>",submitOnChange: true, width:3
            String title = "<img src='${addIcon}' width='18' style='${imgFloat} margin: 0; padding: 0 10px 0 0'><span style='font-size: 95%'>Add Tile</span>"
            //title = "<span style='font-size: 95%'>Add Tile</span>"
            href pageAddTile, description: "", title: title, submitOnChange: true, width:3
            paragraph "", width: 1
            if (state.tileList?.size()) {
                href pageSelectEditTile, description: "", title: "<span style='font-size: 95%'>Edit Tile</span>",submitOnChange: true, width:3
                paragraph "", width: 1
                href pagePreferences, description: "", title: "<span style='font-size: 90%'>Preferences</span>",submitOnChange: true, width:3
                paragraph "", width: 1
            } else {
                href pagePreferences, description: "", title: "<span style='font-size: 90%'>Preferences</span>",submitOnChange: true, width:3
                paragraph "", width: 5
            }
        }
        footer()
    }
}

def pagePreferences() {
    if (state.lastUseParentDeviceSetting != useParentDevice()) {
        removeChildDevices(getChildDevices())
        state.tileList?.each {
            createChildTileDevice (it)
        } 
    }
    state.lastUseParentDeviceSetting = useParentDevice()
    dynamicPage(name: "pagePreferences", title: "Preferences", install: false, uninstall: false) {
        section() {
            input "logLevel", "enum", required: false, title: "Log level", defaultValue: "Info (default)", 
                options: ["none" : "No logging", "trace" : "Trace", "debug" : "Debug", "info" : "Info (default)", "warn" : "Warn", "error" : "Error"]
            label title: "Rename this app:", required: false
            //input "updateOnAnyEvent", "bool", required: false, title: "Update tile on all displayed device attribute events, regardless of whether state changed. "+
            //    "May result in increased hub load.", defaultValue: "false"
            input "filterDuplicates", "bool", required: false, 
                title: "Filter duplicate events (may improve dashboard performance if devices often send duplicate data)", defaultValue: "false"

            
        }
        section() {
            if (state.tileList?.size()) {
                if (useParentDevice()) {
                    paragraph "${warnSym} WARNING: Disabling this setting will delete all child tile devices and re-create them as non-parented devices"
                } else {
                    paragraph "${warnSym} WARNING: Enabling this setting to create a parent device will delete all tile devices and re-create them as parented devices"
                }
            }
            input "useParentDevice", "bool", required: false, title: "Create tile devices under a single parent device", defaultValue: "true", submitOnChange: true
            //settings.check4Change
        }
    }
}

    
String addTileMenuNextPage() {
    String nextPage
    if (state.thisState == "menuDevices") {
        nextPage = "pageEditTile"
    } else if (state.thisState == "menuTileType") {
        nextPage = null
    } 
    //log.debug "next page is: ${nextPage}"
    return nextPage
}
               
    
def pageAddTile() {  
    state.lastState = state.thisState ?: "menuTileType"
    switch (state.lastState) {
       case "menuMain" :
           state.thisState = "menuTileType"
           break
       case "menuTileType" :
           String tileType
           if (btn("HistoryTile")) {
               tileType = "History"
           } else if (btn("StatusTile")) {
               tileType = "Status"
           }
           if (tileType) {
               state.editTileId = addTile()
               //state.add Huh?
               setTileData (state.editTileId, "numEntries", 0)
               setTileData (state.editTileId, "attrFromDev", [])
               setAttrFromDev (state.editTileId, 1, false)
               state.thisState = "menuDevices"
               setTileData (state.editTileId, "tileType", tileType)  
               app.updateSetting("tile${state.editTileId}LineHeight", [type: "decimal", value:  1.2])
               app.updateSetting("tile${tileId}BorderRad", [type: "number", value:  10])
           }  
           break
       case "menuDevices" :
          initHistory(state.editTileId)
          initLastUpdateTime(state.editTileId)
          //if (!getTileDevice(state.editTileId)) {
              //createParentTileDevice() //make sure parent exists first
              //createChildTileDevice (state.editTileId)
          //}
          if (btn("EditTile")) {
              state.thisState = "editTile" 
          }
          break
       case "editTile" :
          if (btn("EditDevices")) {
              state.thisState = "menuDevices" 
          }
          break
       default:
           log.debug "Unexpected pageAddTile state: ${state.lastState}"
           break
    
    } 
        
    dynamicPage(name: "pageAddTile", title: "") {
        switch (state.thisState.toString()) {  
            case "menuTileType" :  
                 section { 
                    paragraph "Select Tile Type to add:"
                    input "btnStatusTile", "button", title: "Status Tile", submitOnChange: true, width:3
                    input "btnHistoryTile", "button", title: "History Tile", submitOnChange: true, width:3
                  }
                 break
            case "menuDevices" :
                log.debug "setting device tile label to: ${generateTileDeviceLabel(state.editTileId)}"
                setDeviceTileLabel (state.editTileId, generateTileDeviceLabel(state.editTileId))
                synch
                devicesMenu(state.editTileId)
                if (getTileData(state.editTileId, "numEntries")) {
                    section {
                        input "btnEditTile", "button", title: "Edit Tile", submitOnChange: true, width:3
                        String dashboard = "<div style='float:left; margin: -8px 14px 0 0;'>${dashboard(tileDeviceLabel(state.editTileId), 0.8, state.editTileId)}</div>"
                        paragraph  dashboard, width: 9
                    }
                }
                break
            case "editTile" :
            
                editTileMenu (state.editTileId) 
                break
                       
            default:
                 log.debug "unexpected State: ${state.thisState}" 
                 break
        }
    }
}
       
def pageSelectDevices(params) {  
    dynamicPage(name: "pageSelectDevices", title: "", install : false ) {
        devicesMenu(state.editTileId)
    }
}

def pageEditDevices(params) {
    //setTileData (params.tileId, "lastTileTitle",null) //editing devices, so clear our last generated title
    //setTileData (params.tileId, "lastTileIcon",null) 
    //log.debug "deleted lastTileTitle"
    //setTileData (tileId, "lastTileTitle", null)
    String attrInput = "tile${params.tileId}Attr${params.entry}"
    String devInput = "tile${params.tileId}Devices${params.entry}"
    if (btn("MoreAttr")) {
        setAttrFromDev(params.tileId, params.entry, true)
        app.updateSetting(devInput, [type: "capability", value: ""]) 
        app.updateSetting(attrInput, [type: "enum", value: ""]) 
    }  
    if (btn("LessAttr")) {
        setAttrFromDev(params.tileId, params.entry, false)
        app.updateSetting(devInput, [type: "capability", value: ""]) 
        app.updateSetting(attrInput, [type: "enum", value: ""]) 
    }  
    dynamicPage(name: "pageEditDevices", title: "") {
        if (btn("Delete")) {
            //app.updateSetting(devInput, [type: "capability", value: ""]) 
            app.removeSetting(devInput)
            app.updateSetting(attrInput, [type: "enum", value: ""]) 
            section {
                paragraph "Entry Deleted"
                subscribeDevices()
            }
        } else {
            if (getAttrFromDev(params.tileId, params.entry)) {
                getAttributesFromDevices(params.tileId, params.entry)
            } else {
                if (state.pageEditInit) {
                    state.pageEditInit = false
                } else {
                    if (settings[attrInput] != state.lastAttrInput) {
                       app.updateSetting(devInput, [type: "capability", value: ""]) 
                    }
                }
                state.lastAttrInput = settings[attrInput]
                getDevicesFromAttributes(params.tileId, params.entry)
            }
            section {
               input "btnDelete", "button", title: "Delete Entry", submitOnChange: true, width:3 
            }
        }
    }
}

def pageEditLabel(params) {
//params: [tileId: tileId, device: dev.toString(), attribute: it], width: 2
     String custLabel = customLabel(params.tileId, params.device, params.attribute) ?: params.device
    //if (custLabel) {
    //    app.updateSetting("tile${tileId}Title", [type: "text", value: custLabel])
   // } else {
    //    app.updateSetting("tile${tileId}Title", [type: "text", value: custLabel])
        
    if (newLabel) {
        setCustomLabel (params.tileId, params.device, params.attribute, newLabel)
        //log.debug "setting custom label! ${newLabel}"
    }
    dynamicPage(name: "pageEditLabel", title: "") {
        section {
            input "newLabel", "text", title:"new label for: ${params.device} (attribute: ${params.attribute})", defaultValue: custLabel,submitOnChange: true
        }
    }
}

def pageEditAttrStyle(params) {
    List attrStyle = getTileData(params.tileId, "attrStyle") ?: attrStyleDefault
    log.debug "checking"
    if (btn("InsertBefore")) {
        log.debug "ib"
      state.styleEntryMode = "insertBefore"
    } else if (btn("InsertAfter"))  {
         log.debug "ia"
      state.styleEntryMode = "insertAfter"
    } else if (btn("Edit")) {
         log.debug "e"
      state.styleEntryMode = "edit"
    } else if (btn("Delete")) {
         log.debug "d"
      state.styleEntryMode = "delete"
    }
    
    dynamicPage(name: "pageEditAttrStyle", title: "") {
        section {    
                   paragraph params.entryString, width: 10
                   paragraph params.sampleCell, width: 2
        switch (state.styleEntryMode) {
            case "insertBefore":
            case "insertAfter":
            case "edit":
            
            def optns = []
            attributesInTile(params.tileId).each {
                optns << "${it}".capitalize()
            }  
            log.debug "${optns}"
             log.debug "${attributesInTile(params.tileId)}"
            //input attrInput, "enum", title: "Select attributes for ${settings[devInput]}:", options: optns, multiple: true, submitOnChange: true 

            
                   def attributes = attributesInTile(params.tileId)
                   if (attributes.size() > 1) {
                       // input "attribute" , "enum", title "attribute" options: attributesInTile(params.tileId), submitOnChange: true 
                       input "attribute" , "enum", title: "attribute", options: attributes, submitOnChange: true, width: 3
                   } else {
                       paragraph attributes[0], width: 3
                   }
                   input "operator", "text", title: "Operator", submitOnChange: true, width:2
                   input "comparitor", "text", title: "Comparitor", submitOnChange: true, width:2
                   input "style", "text", title: "Style", submitOnChange: true, width:2
                   break
            case "delete":
              paragraph "Tile Deleted"
               break
            default:
                     
                   input "btnInsertBefore", "button", title: "Insert Before", submitOnChange: true, width:2
                   input "btnInsertAfter", "button", title: "Insert After", submitOnChange: true, width:2
                   input "btnEdit", "button", title: "Edit", submitOnChange: true, width:2
                   input "btnDelete", "button", title: "Delete", submitOnChange: true, width:2
                    //input "style", "text", title:"new style for: ${params.styleIndex}", submitOnChange: true, width:4      
            }
        }   }
}


def pageEditAttrFormat(params) {
    Map attrFormat = getTileData(params.tileId, "attrFormat") ?: attrFormatDefault
    
    if (settings.attributeFormat) {
        attrFormat[params.attribute]=settings.attributeFormat
        setTileData(params.tileId, "attrFormat", attrFormat) 
        Map attrFormatMod = getTileData(params.tileId, "attrFormatMod")  ?: [:]
        attrFormatMod[params.attribute]=true
        setTileData(params.tileId, "attrFormatMod", attrFormatMod) 
    } else if (attrFormat[params.attribute]) {
        app.updateSetting "attributeFormat", [type: "text", value: attrFormat[params.attribute]]
    }
    dynamicPage(name: "pageEditAttrFormat", title: "") {
        section ("Sample formats: humidity: '%.0f%%', power: '%,.1f Watts', battery: '%.0f%%'"){    
            input "attributeFormat", "text", title:"new format for: ${params.attribute}", submitOnChange: true
        }
    }      
}

void setAttrFromDev(Integer tileId, Integer entry, Boolean val) {
    List attrFromDev = getTileData(tileId, "attrFromDev")
    if (tileId) {
        if (entry > attrFromDev.size()) {
            attrFromDev << val
        } else {
            attrFromDev[entry-1] = val
        }
        setTileData(tileId, "attrFromDev", attrFromDev )
    } else {
        log.warn "Can't set entry ${entry} ${val} for tile ${tileId} in ${attrFromDev}"
    }
}

Boolean getAttrFromDev (Integer tileId, Integer entry) {
    List attrFromDev = getTileData(tileId, "attrFromDev")
    return attrFromDev[entry-1] ?: false
}

List getAttrFromInput(Integer tileId, Integer entry) {
    List result
    attr = settings["tile${tileId}Attr${entry}"]
    if (attr instanceof List) {
        result = attr
    } else if (attr) {
        result = [attr]
    }
    return result
}

def getDevicesFromInput(Integer tileId, Integer entry) {
    return settings["tile${tileId}Devices${entry}"]
}

String customLabel(Integer tileId, String device, String attribute) {
   Integer numEntries = getTileData(tileId, "numEntries") ?: 0
   def labelData = getTileData(tileId, "customLabel")
   String label
   if (labelData) {
       def devLabel = labelData[device]
       if (devLabel) {
           label = devLabel[attribute.uncapitalize()]
       }     
    }
   return label
}     

void setCustomLabel(Integer tileId, String device, String attribute, String label) {
    Integer numEntries = getTileData(tileId, "numEntries") ?: 0
    def customLabel = getTileData(tileId, "customLabel")
    if (!customLabel) {
        customLabel = [:]
    }
    if (!customLabel[device]) {
        customLabel[device] = [:]
    }
    //log.debug "setting custom label: ${label} for ${device} ${attribute} ${tileId}"
    customLabel[device.toString()][attribute.uncapitalize()] = label
    setTileData(tileId, "customLabel", customLabel)
}
    
def displayCurrentDeviceSelections(Integer tileId) {
    section {
        Integer numEntries = getTileData(tileId, "numEntries") ?: 0
        for (Integer i in 1..numEntries) {    
             String devInput = "tile${tileId}Devices${i}"
             List attributes = getAttrFromInput(tileId, i)
             if (settings[devInput] ) {
                 String title
                 if (attributes) {
                     title = "${attributes} devices:"
                 } else {
                     title = "<span style='color:red;'>No capabilities selected for:</span>"
                 }
                 title = title + "\n<span style='color:blue;'>${settings[devInput]}</span>"
                 state.pageEditInit = true
                 href pageEditDevices, description: "", title: title, params: [entry : i, tileId: tileId]
             }
        }
  }
}

def getAttributesFromDevices(Integer tileId, Integer entry) {
    String devInput = "tile${tileId}Devices${entry}"
    String attrInput = "tile${tileId}Attr${entry}"
    section {     
        state.resubscribeDevices = true
        input devInput, "capability.*", title: "Select device to choose attributes", submitOnChange: true, width: 9
        input "btnLessAttr", "button", title: "Filter...", submitOnChange: true, width:3
        if (settings[devInput]) {
            def optns = []
            settings[devInput].supportedAttributes.each {
                optns << "${it}".capitalize()
            }  
            input attrInput, "enum", title: "Select attributes for ${settings[devInput]}:", options: optns, multiple: true, submitOnChange: true 
        }            
    }
}

def getDevicesFromAttributes(Integer tileId, Integer entry, String attribute = null) {
    section {
        // Get attributes, then devices
        String msg
        String attrInput = "tile${tileId}Attr${entry}"
        String devInput = "tile${tileId}Devices${entry}"
        if (entry > 1) {
            msg = "Select a capability to add additional devices:"
        } else {
            msg = "Select a capability to add devices:"
        }
        state.resubscribeDevices = true
        input attrInput, "enum", title: msg, options: attributesInLookup(), submitOnChange: true, width : 9
        input "btnMoreAttr", "button", title: "Or Select by device..", submitOnChange: true, width:3
        selection = getAttrFromInput (tileId, entry) 
        if (selection)  {
            String capability = "capability." + attrLookup[selection[0].uncapitalize()]
            input devInput, capability, title: "Select ${selection[0]} devices", multiple: true, submitOnChange: true
        }   
    }
}

String smInp (String inTxt) {
    return "<span style='font-size: 88%;line-height:1.1;margin:0;padding:0;'>${inTxt}</span>"
}
    
Boolean labelMatchesAnother (Integer tileId, String label) {
    Boolean matches = false
    //log.debug "checking for ${label}"
    //state.tileList?.each {
    for (i in 0..state.tileList.size()) {
        Integer id = state.tileList[i]                                 
        //log.debug "check ${tileId}: ${settings["tile${tileId}Label"]}"
        //if (settings["tile${tileId}Label"] == label) {
        if (tileId != id && getDeviceTileLabel (id) == label) {
            //log.debug "label ${label} matches tile: ${tileId} ${getDeviceTileLabel (tileId)}"
            matches = true
            break
        }
    } 
    //log.debug "matches: ${matches}"
    return matches
}

String generateTileDeviceLabel (Integer tileId) {
    String title = generateTileTitle(tileId)
    log.debug "title is: ${title}"
    Integer count = 1
    String countStr = ""
    String label
    for (i in 0..state.tileList.size()) { 
        log.debug "setting label..."
        label = "${title}${countStr} Tile"
        if (!labelMatchesAnother (tileId, label)) {
            break
        } else {
            count = count + 1
            countStr = " ${count}"   
        }
    }
    log.debug "returning the label: ${label}"
    return label
}
    
    
String tileDeviceLabel(Integer tileId) {
    String label = settings["tile${tileId}Label"]
    if (!label) {
        label = getDeviceTileLabel (tileId)
        synchTileLabel (tileId)
    }
    if (!label) {
        generateTileDeviceLabel(tileId)
        synchTileLabel (tileId)
    }
    return label
}

String generateTileTitle (Integer tileId) {  
    Integer checkEntries = (getTileData(tileId, "numEntries") ?: 0) + 1
    Boolean commonAttr = true
    String title
    String attr
    if (checkEntries && getAttrFromInput(tileId, 1)) {
        attr = getAttrFromInput(tileId, 1)[0]
        log.debug "attr: ${attr}"
        for (Integer i in 2..checkEntries) { 
           getAttrFromInput(tileId, i).each {
               log.debug "it is: ${it}"
               if (attr != it) {
                   commonAttr = false
               }
           }
       }
    }
    String tileType = getTileData(tileId, "tileType")
    if (attr && commonAttr) {
        title = "${camelCapSplit(attr)} ${tileType}"
    } else {
        title = "Device ${tileType}"
    }
    log.debug "title is: ${title}"
    return title
}

String generateTileIcon (Integer tileId) {  
    Integer checkEntries = (getTileData(tileId, "numEntries") ?: 0) + 1
    Boolean commonAttr = true
    String icon
    String attr
    if (checkEntries && getAttrFromInput(tileId, 1)) {
       attr = getAttrFromInput(tileId, 1)[0]
       for (Integer i in 2..checkEntries) { 
           getAttrFromInput(tileId, i).each {
               if (attr != it) {
                   commonAttr = false
               }
           }
       }
    }
    //log.debug "${attr}"
    if (commonAttr) {
        switch (attr) {
            case "Battery" :
                icon = "http://192.168.2.80/dashboard/battery-32.png"
                break
            case "Temperature" :
                icon = "https://git.io/Jzmkm"
                break
            case "Humidity" :
                icon = "https://git.io/Jzmk0"
                break
            default:
                icon = ""
                break
        }  
    } else {
        icon = ""
    }
    //log.debug "returning icon: ${icon} for tile: ${tileId}"
    return icon
}

def pageEditTile(params) {
    if (params?.tileId) {
        state.editTileId = params.tileId
    }
    dynamicPage(name: "pageEditTile", title: "") {
       
       editTileMenu (state.editTileId) 
    }
}

String camelCapSplit(String inString) {
    String result = ""
    for (String s : inString.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")) {
        if (result) {result = result + ' '}
        result = result + s
    }
    log.debug "camelSplit:${result}"
    return result
}

void synchTileLabel (Integer tileId) {
    if (state.lastInputLabel && settings["tile${tileId}Label"] && settings["tile${tileId}Label"] != state.lastInputLabel) {
        //user input new label has changed - ensure device label matches
        setDeviceTileLabel (tileId, settings["tile${tileId}Label"])
    } else {
        String label = getDeviceTileLabel (tileId)
        if (label && label != settings["tile${tileId}Label"]) {
            //ensure input label is current with device label
            //String inpLabel = settings["tile${tileId}Label"]
            app.updateSetting("tile${tileId}Label", [type: "text", value:  label])
        }
    }
    state.lastInputLabel = settings["tile${tileId}Label"]
}


void editTileMenu (Integer tileId) {
    synchTileLabel(tileId)
    if (btn("DeleteTile")) {
        deleteTile (tileId)
        section {
            paragraph "Tile Deleted"
        }
    } else {
        initLastUpdateTime(tileId)
        updateTile(tileId)
        section {
            String clipStr = ""
            String dashboard = "<div style='float:left; margin: -8px 14px 0 0; min-height: 102px'>${dashboard("", 0.8, tileId)}</div>"
            //  String dashboard = "<div style='float:left; margin: -8px 14px 0 0;'>${dashboard("", 1.0, tileId)}</div>"
            Integer totalChars = getTileData(tileId, "tileChars")
            Integer formatChars = getTileData(tileId, "tileFormatChars")
            Float fmtPercent = formatChars/totalChars * 100.0
            String charStr = "<u>Character counts</u>\nTotal: ${totalChars}\nFormat: ${formatChars} (${fmtPercent.round(1)}%)"
            if (getTileData(tileId, "tileType")== "Status") {
                if (getTileData(tileId, "clipped")) {
                    charStr = charStr + "\n${warnSym}<span style='color:orangered'>Clipped: ${getTileData(tileId, "clipped")}</span>"
                } else {
                    //Integer remainChar = (settings["maxTileChars"] ?: 1024) - (getTileData(tileId, "tileFormatChars") ?: 0)
                    Integer remainChar = maxTileSize(tileId) - totalChars
                    charStr = charStr + "\nRemaining: ${remainChar}"
                    
                }
            } else {
                charStr = charStr + "\nLines: ${getTileData(tileId,'histSize')}"
            } 
            
            String info = "<div style='float: left; font-size: 88%'><p style='padding:0; margin:-8px 0 5px 0'>${tileDeviceLabel(tileId)}</p>${charStr}</div>"
            paragraph  dashboard + info, width: 10
            input "refresh", "button", title : refreshSymb, width : 2, submitOnChange : true
            href pageSelectDevices, description: "", title: smInp("Edit Devices"), params: [tileId: tileId], width: 3
            paragraph "", width: 1

            input "btnDeleteTile", "button", title: "Delete Tile", textColor: "red", submitOnChange: true, width: 8
       
            //paragraph "tile size: ${getTileData(tileId, "tileChars")} ${clipStr}"
            input "tile${tileId}Label", "text", title: "Tile device label", defaultValue: tileDeviceLabel(tileId), submitOnChange : true, width: 3
            //input "tile${tileId}ContextColor", "bool", title: smInp("Context sensitive status color"), submitOnChange : true, defaultValue: true, width: 4
            input "tile${tileId}FontSize", "number", title: "Font size"+toolTip("units: px, default: 16"), defaultValue: 16, required: false, submitOnChange : true, width: 2
            input "tile${tileId}Padding", "number", title: "Padding"+toolTip("units: px, default: 16"), defaultValue: 6, submitOnChange : true, required: false, width: 2
           
            if (getTileData(tileId,"tileType") == "Status") {
                input "tile${tileId}ShowLastUpdate", "bool", title: "Show last update time", submitOnChange : true, defaultValue: false, width: 4
                if (settings["tile${tileId}ShowLastUpdate"] || getTileData(tileId,"tileType") == "History") {
                    input "tile${tileId}timeFormat", "text", title: smInp("time Format"), submitOnChange : true, defaultValue: "hh:mm a, dd MMM yyy", width: 3
                }
                input "tile${tileId}ColLayout", "bool", title: "Column layout", submitOnChange : true, defaultValue: true, width: 3
                //Boolean colLayout = settings["tile${tileId}ColLayout"] == null || settings["tile${tileId}ColLayout"]
                input "tile${tileId}AgedMode", "enum", title: smInp("Show aged indication if:"), 
                   options: ["off" : "Do not indicate aged", "attribute" :"Displayed attribute has aged","anyAttribute":"All attributes have aged"], 
                    submitOnChange : true, defaultValue: "All attributes have aged", width: 3
                
                if (settings["tile${tileId}AgedMode"] != "off") {
                    input "agedDays", "number", title: "Aged days"+toolTip("default: 3"), submitOnChange : true, defaultValue: 3, width: 2
                    input "tile${tileId}AgedIndicator", "enum", title: "Aged indicator", 
                       options: ["grayed" : "Grayed out text", "warn" : "Warning symbol", "grayAndWarn" : "Both grayed and warning symbol"], 
                       submitOnChange : true, defaultValue: "grayed", width: 3
                }
                input "tile${tileId}SortCol", "enum", title: "Sort column", 
                    options: ["none":"No Sorting","name":"Device Name", "value" : "Attribute Value", "time" :"Last Update Time"], submitOnChange : true, defaultValue: "Device Name", width: 3
                if (settings["tile${tileId}SortCol"] != "none") {
                    input "tile${tileId}SortInvert", "bool", title: "Invert sort order", submitOnChange : true, defaultValue: false, width: 3
                }

                if (settings["tile${tileId}ColLayout"]==false) {
                    input "tile${tileId}LineJustify", "enum", title: smInp("Line Justification"), 
                        options: ["left":"Left","center":"Center", "right" : "Right"], submitOnChange : true, defaultValue: "center", width: 3
                } else {
                   input "tile${tileId}ValJustify", "enum", title: smInp("Attribute Value Justification"), 
                        options: ["left":"Left","center":"Center", "right" : "Right"], submitOnChange : true, defaultValue: "Right", width: 3
                }     
            }

            input "tile${tileId}ShowDeviceName", "bool", title: "Show device name", submitOnChange : true, defaultValue: true, width: 3
            input "tile${tileId}ShowAttributeName", "bool", title: "Show attribute name", submitOnChange : true, defaultValue: false, width: 3
            input "tile${tileId}ShowTileTitle", "bool", title: "Show tile title", submitOnChange : true, defaultValue: true, width: 3
            if (settings["tile${tileId}ShowTileTitle"]==null || settings["tile${tileId}ShowTileTitle"]) {
                input "tile${tileId}Title", "text", title: "Tile title override"+toolTip("delete entry to show default"), submitOnChange : true, width: 3
                input "tile${tileId}TitlePerc", "number", title: "Title size"+toolTip("units: %, default: 110"), submitOnChange : true, defaultValue: 110, width: 3
            }
            input "tile${tileId}ShowTileIcon", "bool", title: "Show tile icon", submitOnChange : true, defaultValue: true, width: 3
            if (settings["tile${tileId}ShowTileIcon"]==null || settings["tile${tileId}ShowTileIcon"]) {
                input "tile${tileId}IconUrl", "text", title: "Icon URL override", submitOnChange : true, width: 3
            }
            input "tile${tileId}maxTileChars", "number", title: "Max tile characters"+toolTip("default: 1024"), submitOnChange : true, defaultValue: 1024, required: false, width: 3
            //input "maxTileChars", "number", title: smInp("Max Tile chars (default: 1024)"), submitOnChange : true, defaultValue: 1024, width: 3
            input "tile${tileId}MaxLines", "number", title: "Max tile lines"+toolTip("excluding title"), submitOnChange : true, width: 3
            input "tile${tileId}MaxDevChars", "number", title: "Max device name characters", submitOnChange : true, width: 3

            paragraph "", width: 7
            //paragraph "Tile St. Parameters here will decrease the available total tile characters available. Delete entries to turn off settings:"
            paragraph "<div style ='border: 1px solid gray; background-color: LightSteelBlue; padding: 10px;'>Extra formatting. Parameters here will decrease the available tile characters. Delete entries to turn off settings:</div>"
            //input "tile${tileId}Title", "text", title: "Tile title (set to single space to remove title line)", submitOnChange : true, width: 4
            input "tile${tileId}LineHeight", "decimal", title: "Line height"+toolTip("default 1.2"), submitOnChange : true, required: false, width: 3
            
            input "tile${tileId}BackgroundC", "text", title: "Background color"+toolTip("eg: 'blue', 'rgba(0,40,0,0.4)'"), submitOnChange : true, width: 3
            input "tile${tileId}FontColor", "text", title: "Font color" +toolTip("eg: 'red', '#0000ff'"), submitOnChange : true, width: 3
            input "tile${tileId}BorderC", "text", title: "Border color"+toolTip("eg: 'green', '#006900'"), submitOnChange : true, width: 3
            if (settings["tile${tileId}BorderC"]) {
                //input "tile${tileId}BorderRad", "number", title: smInp("Border radius px (default: 10)") + help, submitOnChange : true, width: 3
                 input "tile${tileId}BorderRad", "number", title: "Border Radius" + toolTip ("units: px, default: 10"), submitOnChange : true, width: 3
            }
           
       } 
        attrStyleSection(tileId)
        attrFormatSection(tileId)
        customLabelSection(tileId)
    }
}

String toolTip (String note) {
    //String cssStyle = "<style>.tooltip {position: relative;display: inline-block; border-bottom: 1px dotted black; padding: 0; margin:0; color: blue }" +
    //            ".tooltip .tooltiptext {width: 160px; visibility: hidden; color: blue; background-color: #FCF5E5; text-align: center;position: absolute;z-index: 1;}" +
    //            ".tooltip:hover .tooltiptext {visibility: visible;}</style>" 
    String cssStyle = "<style>.tooltip {position: relative;display: inline-block;padding: 0; margin:0; color: blue }" +
        ".tooltip .tooltiptext {white-space: nowrap ; visibility: hidden; color: blue; border: 1px solid darkgray;background-color: #FCF5E5; border-radius: 5px; " +
        "text-align: center;position: absolute; z-index: 1;}" +
                ".tooltip:hover .tooltiptext { bottom: 25px; left: -65px; padding: 0 5px; visibility: visible;}</style>" 
    //return cssStyle + "<span class='tooltip' ><sup style='text-decoration: underline'>(?)</sup><span class='tooltiptext'>${note}</span></span>"
    return " " + cssStyle + "<span class='tooltip' ><sup>(?)</sup><span class='tooltiptext'>${note}</span></span>"
}


String sampleValCell (Integer tileId, String attribute, def val) {
     //log.debug "uuu: ${tileId} ${attribute} ${val}"
     String sample = formatValue(tileId, attribute, val)
     String dashbTextStyle = "color:#fff;text-shadow: 1px 1px 4px #000;font-size: 14px;text-align: center;"
     return "<div style = 'padding: 3px; ${dashbTextStyle} background-color:dimgray'>${sample}</div>"
}

def attrStyleSection(Integer tileId) {
    List attrStyle = getTileData(tileId, "attrStyle") ?: attrStyleDefault
    def maxChars =[]
    attrStyle.each {
        Integer i = 0
        it.each{ 
            String a = "${it}"
            if (!maxChars[i] || a.length() + 3 > maxChars[i]) {
                maxChars[i] = a.length() + 3
            }
            i++
        }
    }
    
    section(hideable: true, hidden: true, "Attribute Style (color, font, etc)") {
        paragraph "<font style='font-size:12px; font-style: italic'>See <u><a href='https://www.w3schools.com/html/html_styles.asp' target='_blank'>html style attribute documentation</a></u></font>"        

        Integer i = 0
        List attributesInTile = attributesInTile(tileId)
        attrStyle.each {
            
            if (attributesInTile.contains(it[0].capitalize())) {
                //String attr = it[0]
                String row = ""
                Integer j = 0
                it.each {
                    Float width = (maxChars[j])*100.0/(maxChars[0]+maxChars[1]+maxChars[2]+maxChars[3])
                    String tdStyle = "width='${width}%' style='text-align: center; border:1px solid darkgray'"
                    row = row + "<td ${tdStyle}>${it}</td>"
                    j=j+1
                }
                String entryString = "<div style='margin-left: 16px'><table style='width: 100%; float:left; border-collapse: collapse'><tr>${row}</tr></table></div>"
                def val = genSampleVal(tileId, i)
                //log.debug "genning sampleCell for: ${tileId} ${it[0]} ${val} entire it ${it} attr: ${attr}"
                String sampleCell = sampleValCell(tileId, it[0], val)
                //state.remove("styleEntryMode")
                state.styleEntryMode = null
                href pageEditAttrStyle, description: "", title: smInp("Insert/Edit"), params: [tileId: tileId, styleIndex: i, entryString: entryString, sampleCell: sampleCell ], width: 3
                paragraph entryString, width: 7
                paragraph sampleCell, width: 2    
            }
            i=i+1 
        }
    }
}

Boolean deviceHasAttribute (def device, String attribute) {
    Boolean hasAttr = false
      device.supportedAttributes.each {
         //log.debug "Supported Attribute: ${att.name}" 
           if (it.name == attribute) {
              hasAttr = true
           }
      }
    return hasAttr
}

def attrFormatSection(Integer tileId){
    Map attrFormat = getTileData(tileId, "attrFormat") ?: attrFormatDefault
    Map attrFormatMod = getTileData(tileId, "attrFormatMod") 
    app.clearSetting("attributeFormat")
    section(hideable: true, hidden: true, "Attribute Format(units, decimal places, etc)") {
        paragraph "<font style='font-size:12px; font-style: italic'>See <u><a href='https://www.w3schools.com/php/func_string_sprintf.asp' target='_blank'>sprintf format argument documentation</a></u></font>"        
        attributesInTile(tileId).each {
            String attr = it.uncapitalize()
            String format = attrFormat[attr] ?: ""
            if (attrFormatMod && attrFormatMod[attr]) {
                format ="<span style='color:mediumblue'><strong>${format}</strong></span>" 
            }
            href pageEditAttrFormat, description: "", title: smInp("Edit"), params: [tileId: tileId, attribute: attr], width: 2
            String tdStyle = "width=50%' style='text-align: center; border:1px solid darkgray'"
            String pStyle = "style='padding: 0 3px; margin: 0'"
            String row = "<td ${tdStyle}>${attr}</td><td ${tdStyle}>${format}</td>" 
            paragraph "<div style='margin-left: 16px'><table style='width: 100%; float:left; border-collapse: collapse'><tr>${row}</tr></table></div>", width: 8
            Integer numEntries = getTileData(tileId, "numEntries") ?: 0
            def sampleVal
            for (Integer i in 1..numEntries) {    
               getDevicesFromInput(tileId, i).each {
                   if (deviceHasAttribute(it,attr)) {
                       sampleVal = it.currentValue(attr)
                   }
               }
            }
            paragraph sampleValCell(tileId, attr, sampleVal), width: 2
        }
    }
}

       
def  customLabelSection(Integer tileId) {
      section(hideable: true, hidden: true, "Custom Labels (override device name)") {
           Integer numEntries = getTileData(tileId, "numEntries") ?: 0
          //setCustomLabel(tileId, "Weather", "Humidity", "Yoda")
           for (Integer j in 1..numEntries) {    
               getDevicesFromInput(tileId, j).each {
                   def dev = it
                   getAttrFromInput(tileId, j).each {
                       String label = customLabel(tileId, dev.toString(), it)
                       //log.debug "label is: ${label} for ${dev} ${it}"
                       String addEdit
                       if (label) {
                          addEdit = "Edit"
                           label="<span style='color:mediumblue'><strong>${label}</strong></span>"
                       } else {
                           label = "--"
                           addEdit = "Add"
                       }
                   app.updateSetting("newLabel", [type: "text", value: ""])
                   href pageEditLabel, description: "", title: smInp(addEdit), params: [tileId: tileId, device: dev.toString(), attribute: it], width: 2
                   String row = ""
                   String tdStyle = "width=50%' style='text-align: left; border:1px solid darkgray'"
                   String pStyle = "style='padding: 0 3px; margin: 0'"
                   row = row + "<td ${tdStyle}>${dev}</td><td ${tdStyle}>${it}</td>" 
                    paragraph "<div style='margin-left: 16px'><table style='width: 100%; float:left; border-collapse: collapse'><tr>${row}</tr></table></div>", width: 6
                    //paragraph sampleDiv, width: 1
                       paragraph label, width: 4
                  } 
               }
          }
       }
}
   
def pageSelectEditTile() {

    state.lastState = state.thisState ?: "menuTileType"
    if (state.lastState == "menuMain") {
        if (state?.tileList.size() == 1) {
            state.thisState = "menuEditTile"
            state.editTileId = state.tileList[0]
        } else {
            state.thisState = "menuSelectTile"
        }
    }
    state.tileList.each() {
       if (btn("Edit${it}")) {
            state.thisState = "menuEditTile"
            state.editTileId = it
       }  
     }
    dynamicPage(name: "pageSelectEditTile", title: "") {
        switch (state.thisState) {
            case "menuSelectTile" :
                selectTileMenu()
                break
            case "menuEditTile" :
               editTileMenu (state.editTileId)
               break
            default:
               log.debug "Unexpected pageSelectEditTile state: ${state.lastState}"
               break
        }      
    }
}

void selectTileMenu() {
    section ("Select Tile to Edit") {
       heightSortedTiles().each() {
           Integer tileId = it.key
           String name = "<div style='width: 100px; float:left; padding: 4px; margin: 14px 24px 0 24px;font-size:95%'>${tileDeviceLabel(tileId)}</div>"
           String dashboard = "<div style='margin: -10px 0 12px 0'>${dashboard(tileDeviceLabel(tileId), 0.6, tileId)}</div>"
           input "btnEdit${tileId}", "button", title: "Edit", submitOnChange: true, width: 1
           paragraph name + dashboard, width: 11
       }
    }
}
    
void devicesMenu(Integer tileId) {
    setTileData (tileId, "lastTileTitle",null) //editing devices, so clear our last generated title
    setTileData (tileId, "lastTileIcon",null) 
    
    Integer numEntries = getTileData(tileId,"numEntries") ?: 0
    String nextDevInput = "tile${tileId}Devices${numEntries + 1}"
    String nextAttrInput = "tile${tileId}Attr${numEntries + 1}"
    Boolean newEntryComplete
    if (getAttrFromDev(tileId,numEntries + 1)) {
        newEntryComplete = settings[nextAttrInput]
    } else {
        newEntryComplete = settings[nextDevInput]
    }
    if (newEntryComplete ) {
        numEntries = numEntries + 1
        setTileData(tileId,"numEntries",numEntries)
        setAttrFromDev(tileId, numEntries + 1, false)
        

        state.resubscribeDevices = true
    }  
    if (numEntries) {
        section {
             displayCurrentDeviceSelections(tileId)
        }
    }
    if (state.resubscribeDevices) {
        log.debug "resubscribing to devices"
        subscribeDevices()
        state.resubscribeDevices = false
    }
    if (btn("MoreAttr")) {
        setAttrFromDev(tileId, numEntries + 1, true)
        app.updateSetting(nextDevInput, [type: "capability", value: ""]) 
        app.updateSetting(nextAttrInput, [type: "enum", value: ""]) 
    }  
    if (btn("LessAttr")) {
        setAttrFromDev(tileId, numEntries + 1, false)
        app.updateSetting(nextDevInput, [type: "capability", value: ""]) 
        app.updateSetting(nextAttrInput, [type: "enum", value: ""]) 
    }  
    if (getAttrFromDev(tileId, numEntries + 1)) {
        getAttributesFromDevices(tileId, numEntries + 1)
    } else {
        getDevicesFromAttributes(tileId, numEntries + 1)
    }

    /*
    section {
        paragraph ""
        if (numEntries) {
            input "btnDone", "button", title: "Done", submitOnChange: true, width:2
        } else {
            btnCancel(2)
       }

    } 
*/
}

def footer() {
    section {
            String ddMsg = "For more information, questions, or to provide feedback, please visit: <a href='${ddUrl}'>${ddUrl}</a>"
            String ddDiv = "<div style='display:inline-block;margin-right: 20px'>" + "<a href='${ddUrl}'><img src='${ddLogoHubitat}' height='27'></a></div>"
            String ppDiv = "<div style='display:inline-block'>" + "<a href='https://www.paypal.com/paypalme/darwinsden'><img src='${ppBtn}'></a></div>" 
            String freeMsg = "<span style = 'font-size: 90%'>This is free software. Donations are very much appreciated, but are not required or expected.</span>"
            //paragraph "<div style='text-align:center'>" + freeMsg + "</div>"
            //paragraph "<div style='text-align:center'>" + ddDiv + ppDiv + "</div>" 
            paragraph "<div style='text-align:center; margin-bottom: 4px'>" + freeMsg + "</div>" + "<div style='text-align:center'>" + ddDiv + ppDiv + "</div>" 
    }
}

Boolean btn(String name) {
    Boolean result = false
    if (state.buttonHit == "btn${name}") {
        //log.debug "btn processed: ${name}"
        state.buttonHit = null
        result = true
    }
    return result
}

void appButtonHandler(btn) {
    //log.debug "Button hit: ${btn}"
    state.buttonHit = btn
}

List attributesInLookup() {
    List attr = []
    attrLookup.each{
        key, value -> attr << "${key}".capitalize()
    }
    return attr
}

List attributesInTile(Integer tileId) {
   List attr = []
   Integer numEntries = getTileData(tileId,"numEntries") ?: 0
   for (Integer i in 1..numEntries) {    
       getAttrFromInput(tileId, i).each {
           attr << it
       }
    }  
    return attr
}

String formatText(String text, String beginTag, String endTag) {
    String output
    if (!hubIsSt()) {
        output = beginTag + text + endTag
    } else {
        output = text
    }
}

Long maxTileSize(Integer tileId) {
    return settings["tile${tileId}maxTileChars"] ?: 1024
}

String sampleTile(Integer tileId, Float scale, String name, Integer spacing = 14) {
    String tileStyle = "background-color:hsla(0,0%,50.2%,.5);border-radius: 3px;"
    String pName = ""
    //String tileType = getTileData(tileId, "tileType")
    String tileText
    Float fontSize = settings["tile${tileId}FontSize"] ? settings["tile${tileId}FontSize"]*scale : 16*scale
    Float padding = settings["tile${tileId}Padding"] != null ? settings["tile${tileId}Padding"]*scale : 6*scale
    //Long maxSize = settings["tile${tileId}maxTileChars"] ?: 1024
    //log.debug "${tileId} fontsize: ${fontSize}"
    tileText = getTileContents(tileId, fontSize, padding.toInteger())
    if (name) {
       pName = "<p style='margin:0;padding-left:6px;font-size:70%'>${name}</p>"
    } 
    //String container = "<div style='white-space:nowrap;line-height:1.2;font-weight:normal;padding: 12px 8px 8px 8px;${tileStyle}'>" +
    String container = "<div style='white-space:nowrap;font-weight:normal;padding: 12px 8px 8px 8px;${tileStyle}'>" +
        "${tileText}${pName}</div>"
    String tile = "<div style='float:left;margin:${spacing}px 0 0 ${spacing}px;'>"+container+"</div>"
    return tile
}

def heightSortedTiles() {
    def tileHeights = [:]
    state.tileList.each() {
        //refresh all tiles to get proper tile length
        //Float fontSize = settings["tile${it}FontSize"] ? settings["tile${it}FontSize"]*scale : 16*scale
        //getTileContents(it, fontSize, 1)
        tileHeights[it]=getTileData(it, "heightPx").toFloat()
        //log.debug "height: ${it} is ${getTileData(it, 'heightPx').toFloat()}"
    }
    //sort based on tile height for better fit/flow on app dashboard
    log.debug "${tileHeights}"
    return tileHeights.sort { a, b -> a.value <=> b.value }
}

String dashboard(String title, Float scale, Integer tileId = null) {
    Integer dashbPadding = 0
    //String dashbTextStyle = "color:#fff;text-shadow: 1px 1px 4px #000;font-size: 14px;text-align: center;line-height:1.0;"
     String dashbTextStyle = "color:#fff;text-shadow: 1px 1px 4px #000;font-size: 14px;text-align: center;"
    //String dashbTextStyle = "color:#fff;text-shadow: 1px 1px 4px #000;text-align: center;line-height:1.0;"
    String dashbStyle = "background-color:black;border-radius: 3px;"
    String tile = ""
    String minWidth = ""
    if (tileId) {
        //tile = sampleTile(tileId,scale, title, 0)
        tile = sampleTile(tileId,scale, title, 0)
    } else {
        Integer numTiles = state.tileList?.size() ?: 0
        if (numTiles) {
            dashbPadding = 15
            def tileLength=[:]
            state.tileList.each() {
                //refresh all tiles to get proper tile length
                Float fontSize = settings["tile${it}FontSize"] ? settings["tile${it}FontSize"]*scale : 16*scale
               
                //Long maxSize = maxTileChars ?: 1024
                getTileContents(it, fontSize, 1)
                //tileLength[it]=getTileData(it, "heightPx")
            }
            heightSortedTiles().each() {
               tile = tile + sampleTile(it.key, scale, tileDeviceLabel(it.key))
               //tile = tile + sampleTile(it, scale, tileDeviceLabel(it))
            } 
        }
    }
    String header = ""
    if (!tileId) { //aggregate dashboard
        String imgTag = "<img style= 'float: left; margin: 12px' src='data:image/png;base64,${statusIcon}' />"
        minWidth= "min-width: 160px;"
        header = "<div style= 'overflow: hidden'>${imgTag}" + "<p style='text-align: left; margin:14px 0 0 0;line-height:0.9;font-size: 16px'>Status Tile\n<span style='font-size: 70%'>${app.version()}</span></p></div>"
        //header = "<div style= 'overflow: hidden'>${imgTag}" + "<p style='text-align: left; margin:14px 0 0 0;line-height:0.9;'>Status Tile\n<span style='font-size: 70%'>${app.version()}</span></p></div>"

    } 
    String dashboard = "<div style='margin:0;padding:0 ${dashbPadding}px ${dashbPadding}px 0;${minWidth}${dashbStyle}${dashbTextStyle}display:inline-block'>"+
       header + "${tile}</div>"
   return dashboard
}

def pageAdvanced() {
    dynamicPage(name: "pageAdvanced", title: "Advanced Options") {
        section {
            input "logAged", "bool", title: "Log when a sensor has not reported in the last 24 hours", defaultValue: true
            input "debugOutput", "bool", title: "Enable debug logging", defaultValue: false
        }
    }
}

def installed() {
    initialize()
}

def updated() {
    initialize()
}


String historyDevice() {
    "SummarizerHistory_${app.id}"
}

String statusDevice() {
    "SummarizerStatus_${app.id}"
}

void subscribeEvents (Integer tileId) {
    Integer numEntries = getTileData(tileId, "numEntries") ?: 0
    for (Integer i in 1..numEntries) {    
        getDevicesFromInput(tileId, i).each {
            def dev = it
            getAttrFromInput(tileId, i).each {
                 subscribe(dev, "${it}".uncapitalize(), deviceCallback)
            }
        }
    }
}

/*
void unsubscribeDevices() {
    state.tileList?.each {
        Integer tileId = it
        List entries = getTileData(tileId,"entries")
        Integer index = 0
        entries.each {
            index++
            settings["tile${tileId}Devices${index}"].each {
                log.debug "unsiscribing ${index}"
                unsubscribe(it)
            }
        }
    }
}
*/
    
void subscribeDevices () {
    unsubscribe()
    cleanLastUpdateTime()
    state.lastUpdateValue = [:]
    state.tileList?.each {
        createChildTileDevice (it)
        subscribeEvents (it)
    }
}

Boolean useParentDevice() {
    return settings.useParentDevice == null || settings.useParentDevice
}

def getTileDeviceNoParent (Integer tileId) {
    String dni = getTileDeviceDniNoParent (tileId)
    return getChildDevice (dni)
}

String getTileDeviceDniNoParent (Integer tileId) {
    return "StatusTile${app.id}_${tileId}"
}

def getParentDevice() {
   device = getChildDevice("StatusTile${app.id}")
   return device
}

private createParentDevice() {
    def parent = getParentDevice()
    if (!parent) {
        String dni = "StatusTile${app.id}"
        log.debug "creating Parent device dni: ${dni}"
        try {
            parent = addChildDevice("darwinsden", "Status Tile Parent", dni, null, 
                 [name: "Status Tile Parent", label: "Status Tile Parent", completedSetup: true ])
            parent.initialize()
        } catch (e) {
           log.debug "Unable to add parent tile device: ${e}"
        }
        
    }
    return parent
}

void createTileDeviceNoParent (Integer tileId) {
    if (!getTileDeviceNoParent(tileId)) {
       String dni= getTileDeviceDniNoParent(tileId)
       String namespace = "darwinsden"
       String name = "Status Tile ${tileId}"
       String typeName = "Tile Device"
       String label = tileDeviceLabel(tileId)
       log.debug "Adding new tile device for tile: ${tileId} - typeName: ${typeName} dni: ${dni}"
       try {
           device = addChildDevice(namespace, typeName, dni, [label: label, name: label])
       } catch (e) {
           log.debug "Unable to add device for tile id: ${tileId} ${typeName}: ${e}"
       }
     }
}

void createChildTileDevice(Integer tileId) {
    if (useParentDevice()) {
        createChildTileDeviceForParent (tileId)
    } else {
        createTileDeviceNoParent (tileId)
    }
    updateTile(tileId)
}

void createChildTileDeviceForParent (Integer tileId) {
    String id = "${tileId}"
    def parent = getParentDevice()
    if (!parent) {
        parent = createParentDevice()
    }
    if (!parent?.getChildDev(id)) {
       log.debug "Adding new tile device for tile: ${tileId}"
       try {
           String label = tileDeviceLabel(tileId)
           parent.createChildTile (id, label)
       } catch (e) {
           log.debug "Unable to add child device under parent for tile id: ${tileId} ${typeName}: ${e}"
       }
     }
}

void deleteChildTileDevice (Integer tileId) {
    if (useParentDevice()) {
        def parent = getParentDevice()
        String id = "${tileId}"
        parent.deleteChildDev (id)
    } else {
        String dni= getTileDeviceDniNoParent(tileId)
        deleteChildDevice(dni)
    }
}

void setDeviceTileLabel (Integer tileId, String label) {
    if (useParentDevice()) {
        def parent = getParentDevice()
        String id = "${tileId}"
        parent.setChildLabel(id, label)
    } else {
        def device = getTileDeviceNoParent (tileId)
        if (device) {
            device.setDisplayName(label)
         }
    }
}

String getDeviceTileLabel (Integer tileId) {
    if (useParentDevice()) {
        def parent = getParentDevice()
        if (parent) {
            String id = "${tileId}"
            label = parent.getChildLabel(id)
        }
    } else {
        def device = getTileDeviceNoParent (tileId)
        if (device) {
            label = device.label
        }
    }
    return label
}

void updateTileString(Integer tileId, String tileText) {
    if (useParentDevice()) {
        def parent = getParentDevice()
        if (parent) {
            String id = "${tileId}"
            parent.updateTile(id, tileText)
        }
    } else {
        def device = getTileDeviceNoParent (tileId)
        if (device) {
           device?.sendEvent(name: "tile", value: tileText, displayed: true, isStateChange: true)
        } 
     }
}


def initialize() {
    subscribeDevices()
    state.tileList?.each {
        createChildTileDevice (it)
    }
   
    refresh()
}

void cleanLastUpdateTime() {
    def lastUpdateTimeNew = [:]
    state.tileList?.each {
        Integer tileId = it
        Integer numEntries = getTileData(tileId, "numEntries") ?: 0
        for (Integer i in 1..numEntries) {    
            getDevicesFromInput(tileId, i).each {
                def dni = it.getDeviceNetworkId()
                getAttrFromInput(tileId, i).each {
                    def attribute = "${it}".uncapitalize()
                    Long unixTime = getLastUpdateTime (dni, attribute)
                    if (unixTime) {
                        if (lastUpdateTimeNew[dni] == null) {
                            lastUpdateTimeNew[dni] = [:]
                        }
                        lastUpdateTimeNew[dni][attribute] = unixTime
                    }
                }
            }
        }
    }
    state.lastUpdateTime = lastUpdateTimeNew
}

def uninstalled() {
    removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

String formatDate(Integer tileId, Long unixTime) {
    def dateObject = new Date(unixTime)
    String time = ""
    if (unixTime) {
       String timeformat = settings["tile${tileId}timeFormat"] ?: "hh:mm a, dd MMM yyy"
       try {
            time = dateObject.format(timeformat)
        } catch (Exception e) {
            log.warn "Error: ${e} applying time format ${timeformat} for tile: ${tileId}"
        }
    }
    return "${time}"   
}

String getDivFormat(Integer tileId, Float fontSize) {
    //String title
    String titleStr = ""
    String imgStr = ""
    String lineHeightStr = ""
    if (settings["tile${tileId}LineHeight"]) {
        lineHeightStr = "line-height:${settings["tile${tileId}LineHeight"]};"
    }
    //Float lineHeight = settings["tile${tileId}LineHeight"]?.toFloat() ?: 1.2
    //String divStyle = "${lineHeightStr}font-size:${fontSize}px;"
    String divStyle = "${lineHeightStr}font-size:${fontSize}px;"
    //String divStyle = "line-height:${lineHeight};font-size:${fontSize}px;"
    String title =""
    if (settings["tile${tileId}ShowTileTitle"] ==null || settings["tile${tileId}ShowTileTitle"]) {
        if (settings["tile${tileId}Title"]) {
            title = settings["tile${tileId}Title"]
        } else {
            //check for a previous auto-generated title
            title = getTileData (tileId, "lastTileTitle")
            if (!title) {
                title = generateTileTitle(tileId) 
                setTileData (tileId, "lastTileTitle", title)
            }
        }  
    }
    String icon =""
     if (settings["tile${tileId}ShowTileIcon"] ==null || settings["tile${tileId}ShowTileIcon"]) {
        if (settings["tile${tileId}IconUrl"]) {
            icon = settings["tile${tileId}IconUrl"]
        } else {
            //check for a previous auto-generated icon
            icon = getTileData (tileId, "lastTileIcon")
            if (!icon) {
                icon = generateTileIcon(tileId) 
                setTileData (tileId, "lastTileIcon", icon)
            }
        }  
    }
    /*
    if (settings["tile${tileId}ShowTileIcon"] ==null || settings["tile${tileId}ShowTileIcon"]) {
        icon = settings["tile${tileId}IconUrl"]
    }
    */
    if (title || icon) {
        if (title) {
            Float titlePerc = settings["tile${tileId}TitlePerc"]?.toFloat() ?: 110
            //Float fontPerc = titleFontSz/fontSize * 100.0
            titleStr = "<div style='font-size:${titlePerc}%;margin:3px 8px;float:left'><b>${title}</b></div>"
        }
        if (icon) {
            imgStr = "<img src='${icon}' style='height:${(fontSize*1.1).toInteger()+7}px' align='left'>"
        } 
        titleStr = "<div style='display:inline-block'>${imgStr}${titleStr}</div>"
    }
    if (settings["tile${tileId}BorderC"]) {
        String radStr = ""
        if (settings["tile${tileId}BorderRad"]) {
            radStr = "border-radius:${settings["tile${tileId}BorderRad"]}px;"
        }
        divStyle=divStyle + "padding:3px;${radStr}border:2px solid ${settings["tile${tileId}BorderC"]};"
    }
    if (settings["tile${tileId}BackgroundC"]) {
        divStyle=divStyle + "background-color:${settings["tile${tileId}BackgroundC"]};"
    }
    if (settings["tile${tileId}FontColor"]) {
        divStyle=divStyle + "color:${settings["tile${tileId}FontColor"]};"
    }
    if (divStyle) {
        divStyle=" style='${divStyle}'"
    }
    return "<div" + divStyle + ">" + titleStr
}

Float msToDays (value) {
    return value/1000/60/60/24
}  

String formatAged(Integer tileId, String name, Float numAgeDays)  {
    String ageStr = " (" + numAgeDays.toInteger().toString()
    if (numAgeDays > 1) {
        ageStr += " days)"
    } else {
        ageStr += " day)"
    }
    //if (settings["tile${tileId}AgedIndicator"] == "warn" || settings["tile${tileId}agedIndicator"] == "grayedAndWarn" ) {
    //    name = name + ' ' + warnSym
    //}
    Integer nameLength = name.length() + ageStr.length() 
    //log.debug "a ${settings["tile${tileId}MaxDevChars"]} b ${nameLength}"
    if (settings["tile${tileId}MaxDevChars"] && nameLength > settings["tile${tileId}MaxDevChars"]) {
         nameLength = settings["tile${tileId}MaxDevChars"].toInteger() - ageStr.length()
        //log.debug "1 ${nameLength}"
    } else {
         nameLength = name.length()
        //log.debug "2 ${nameLength}"
    }
    if (nameLength < 0 ) {
        nameLength = 0
    }
    String nameStr = name.substring(0, nameLength) + ageStr
    //log.debug "rtunning: ${nameStr}"
    return nameStr + ' ' + warnSym
}

Long lastDeviceUpdateTime (Integer tileId, def dni) {
    Long lastActiveTime
    Integer numEntries = getTileData(tileId, "numEntries") ?: 0
    for (Integer i in 1..numEntries) {  
        Boolean devMatch
        getDevicesFromInput(tileId, i).each {
            if (it.getDeviceNetworkId() == dni) {
                //lastActiveTime = it.lastActivity
                lastActiveTime = it.getLastActivity().getTime()
            }
        }
    }
    //log.debug "returning last ${lastActiveTime} for ${dni}" 
    return lastActiveTime
}



/*
Long ageDays (Integer tileId, String dni, Long lastAttrTime) { 
    //Boolean aged
    Long ageInDays = 0

   if (settings["tile${tileId}AgedMode"] == null || settings["tile${tileId}AgedMode"] =="anyAttribute") {
        ageInDays =  msToDays(now()-lastDeviceUpdateTime(tileId, dni))
   } else if (settings["tile${tileId}AgedMode"] == "attribute") {
        ageInDays = msToDays(now().toLong()-lastAttrTime)
    }
    return ageInDays
}
*/

String addToRow (String row, String toAdd, Boolean colLayout, Integer whiteSpaceSize) {
    String newRow = row
    if (colLayout) {
        newRow = newRow + "<td>${toAdd}</td>"
    } else {
        String whiteSpace = ' ' * whiteSpaceSize
        newRow = newRow + toAdd + whiteSpace
    }
    return newRow
}

String getStatusString(Integer tileId, Float fontSize, Integer padding) {
    //log.debug "Getting status for: ${tileId} ${tileLabel(tileId)}"
    List status = updateStatus(tileId)
    String cssStyle
    String tableClass="c${tileId}"
    
    Boolean colLayout = settings["tile${tileId}ColLayout"] == null || settings["tile${tileId}ColLayout"]
    Boolean showDev = settings["tile${tileId}ShowDeviceName"] == null || settings["tile${tileId}ShowDeviceName"]
    Boolean showAttr = settings["tile${tileId}ShowAttributeName"] ?: false
    Integer attrColumn = 1
    if (showDev) attrColumn++
    if (showAttr) attrColumn++
    if (!colLayout) {
         String justify = settings["tile${tileId}LineJustify"] ?: "center"
         cssStyle = "<style>.${tableClass} td{white-space:pre;text-align:${justify};padding:0}</style>" 
    } else {
         String justify = settings["tile${tileId}ValJustify"] ?: "right"
         cssStyle = "<style>.${tableClass} td:nth-child(${attrColumn}){text-align:${justify}}.${tableClass} td{text-align:left;padding:0 ${padding}px}</style>" 
        // cssStyle = "<style>.${tableClass} td:nth-child(${attrColumn}){text-align:${justify}}.${tableClass} td{text-align:left}</style>" 
         
    }
    Integer whiteSpaceSize = (1.5+(padding*padding/fontSize/2.0)).toInteger() //whitespace pseudo-padding
    String tileString = cssStyle + getDivFormat(tileId, fontSize)
    String endTag = "</table></div>"
    Integer maxWidth = 0
    Integer clipped = 0
    //Float heightPx = 0
    Integer numRows = 0
    String contentStr = ""
    //Float lineHeight = settings["tile${tileId}LineHeight"]?.toFloat() ?: 1.2
   
    //  endTag = "</table>" + endTag
    tileString = tileString + "<table class='${tableClass}'align='center'>" //center ensures table itself is centered left-right
    
    if (status) {
        status.each {
            String row
            String content = ""
            String devName = customLabel(tileId, it.name, it.attribute) ?: it.name
            String attrName = ""
            Long numAgeDays = 0

            if (settings["tile${tileId}AgedMode"] == null || settings["tile${tileId}AgedMode"] =="anyAttribute") {
                numAgeDays =  msToDays(now()-lastDeviceUpdateTime(tileId, it.dni))
            } else if (settings["tile${tileId}AgedMode"] == "attribute") {
                numAgeDays = msToDays(now().toLong()-it.time)
            }
            Integer agedDayIndicator = settings["tile${tileId}AgedDays"] ?: 3
            Boolean isAged = numAgeDays >= agedDayIndicator 
            //log.debug "aaa: ${tileId} ${it.attribute} ${it.value} ${isAged}"
            String val = formatValue(tileId, it.attribute, it.value, isAged)
            String trStr
            if (isAged) {
                if (settings["tile${tileId}AgedIndicator"] == null || settings["tile${tileId}AgedIndicator"] == "grayed" ||
                   settings["tile${tileId}AgedIndicator"] == "grayAndWarn") {
                   trStr = "<tr style='color:#BEBEBE'>"
                } else {
                    trStr = "<tr>"
                }
                devName = formatAged(tileId, devName, numAgeDays) 
            } else {
               trStr = "<tr>"
               if (settings["tile${tileId}MaxDevChars"] != null && devName.length() > settings["tile${tileId}MaxDevChars"]) {
                  devName = devName.substring(0, settings["tile${tileId}MaxDevChars"].toInteger())
               }
            }
            String timeDate = ""
            if (settings["tile${tileId}ShowLastUpdate"]) {
                timeDate = formatDate(tileId, it.time)
            }
            if (colLayout) {    
                row = "${trStr}"
            } else {
                row = "${trStr}<td>" + ' ' * whiteSpaceSize 
            }
            
            if (showDev) {
                row = addToRow (row, devName, colLayout, whiteSpaceSize)
                content = content + devName
            }
            if (showAttr) {
                row=addToRow (row,it.attribute, colLayout, whiteSpaceSize)
                content = content + it.attribute
            }
            //Always show Attribute Value
            row=addToRow (row, val, colLayout, whiteSpaceSize)
            content =content + val
            
            if (timeDate) {
                row=addToRow (row, timeDate, colLayout, whiteSpaceSize)
                content = content + timeDate
            }
            
            if (colLayout) {    
                row = row + "</tr>"
            } else {
                row = row + "</td></tr>"
            }
            if (tileString.size() + row.size() + endTag.size() <= maxTileSize(tileId)) {
                tileString = tileString + row
                contentStr = contentStr + content
                //heightPx = heightPx + fontSize * lineHeight/1.25
                numRows = numRows + 1               
            } else {
                clipped = clipped + tileString.size() + row.size() + endTag.size() - maxTileSize(tileId)
            }
            if (row.size() > maxWidth) {
                maxWidth=row.size()
            }
        }
        
    }  else {
        //tileString = tileString + "<tr><td>No devices available</td></tr>"
    }
    tileString = tileString + endTag
    setTileData (tileId, "tileChars", tileString.size())
    setTileData (tileId, "tileFormatChars", tileString.size() - contentStr.size())
    setTileData (tileId, "clipped", clipped)
  
    Float heightPx = calcTileHeight(numRows, fontSize.toFloat(), settings["tile${tileId}LineHeight"]?.toFloat(), settings["tile${tileId}Title"], 
                                    settings["tile${tileId}IconUrl"], settings["tile${tileId}BorderC"])
    // Float heightPx = calcTileHeight(numRows, fontSize.toFloat(), null, null, null, null)
    setTileData (tileId, "heightPx", heightPx)
    return tileString
}

String getTileContents(Integer tileId, Float fontSize, def padding) {
    String tileType = getTileData(tileId, "tileType")
    if (tileType == "Status") {
        tileText = getStatusString(tileId,fontSize, padding.toInteger())
    } else {
        tileText = getHistoryString(tileId,fontSize, padding.toInteger())
    }
}

String getHistoryString(Integer tileId, def fontSize, def padding) {
    //log.debug "Getting history for: ${tileId} ${tileLabel(tileId)}"
    //cssStyle = "<style>.${tableClass} td{white-space:pre;text-align:${justify};padding:0}</style>" 
    //tileString = tileString + "<table class='${tableClass}'align='center'>" //center ensures table itself is centered left-right
    String tableClass="c${tileId}"
    //String cssStyle = "<style>.${tableClass} td:nth-child(2){text-align:right}td{text-align:left;padding:0 ${padding}px;font-size:${fontSize}px}</style>"
    String cssStyle = "<style>.${tableClass} td:nth-child(2){text-align:right}.${tableClass} td{text-align:left;padding:0 ${padding}px}</style>"
    //log.debug "hist ${tileId} ${cssStyle}"
    String startTag = cssStyle + getDivFormat(tileId, fontSize) + "<table class='${tableClass}'align='center'>"
    Boolean showDev = settings["tile${tileId}ShowDeviceName"] == null || settings["tile${tileId}ShowDeviceName"]
    Boolean showAttr = settings["tile${tileId}ShowAttributeName"] ?: false
    String histString = "" 
    String endTag = "</table></div>"

    Integer histTableSize = 0
    Integer maxLines = settings["tile${tileId}MaxLines"] ?: 20
    //Float heightPx = 0
    Integer numRows = 0
    List histTable = []
    Boolean colLayout = settings["tile${tileId}ColLayout"] == null || settings["tile${tileId}ColLayout"]
    Integer whiteSpaceSize = (1.5+(padding*padding/fontSize/2.0)).toInteger() //whitespace pseudo-padding
    String contentStr = ""
    //Float lineHeight = settings["tile${tileId}LineHeight"]?.toFloat() ?: 1.2

    if (state."tile${tileId}History") {
        state."tile${tileId}History".each {
             ///log.debug "ccc: ${tileId} ${it.attr} ${it.value}"
            String content = ""
            //String row 
            String value = formatValue(tileId, it.attr, it.value)
            String timeDate = formatDate(tileId, it.unixTime)
            String devName = customLabel(tileId, it.name, it.attr) ?: it.name
            if (settings["tile${tileId}MaxDevChars"] != null && devName.length() > settings["tile${tileId}MaxDevChars"]) {
               devName = devName.substring(0, settings["tile${tileId}MaxDevChars"].toInteger())
            }

            //String rowAdd = "<tr><td>${devName}</td><td>${value}</td><td>${timeDate}</td></tr>"
            String row = "<tr>"
            if (showDev) {
                row = addToRow (row, devName, colLayout, whiteSpaceSize)
                content = content + devName
            }
            if (showAttr) {
                row=addToRow (row, it.attr, colLayout, whiteSpaceSize)
                content = content + it.attr
            }
            row=addToRow (row, value, colLayout, whiteSpaceSize)
            content =content + value
            if (timeDate) {
                row=addToRow (row, timeDate, colLayout, whiteSpaceSize)
                content = content + timeDate
            }
            row = row + "</tr>"
            if (startTag.size() + histString.size() + row.size() + endTag.size() <= maxTileSize(tileId) && histTable.size() < maxLines) {
                histTable << row
                histString = histString + row
                //heightPx = heightPx + fontSize * lineHeight/1.25
                numRows = numRows + 1
                contentStr = contentStr+content
            }
        }
        
         
        if (settings["tile${tileId}SortInvert"]) {
           histString = ""
           histTable = histTable.reverse().each {
               histString = histString + it
           }   
        } 
    } else {
        histString = "<tr><td>No history available</td></tr>"
        numRows = 1
    }
    /*
    heightPx = numRows*fontSize * lineHeight/1.25 + 4
        if (numRows > 1) {
             heightPx =  heightPx + 4
        }
    */
    String tileString = startTag + histString + endTag 
    setTileData (tileId, "tileChars", tileString.size())
    setTileData (tileId, "tileFormatChars", tileString.size() - contentStr.size())
    setTileData (tileId, "histSize", histTable.size())
    /*
    if (settings["tile${tileId}Title"] != " " || settings["tile${tileId}IconUrl"] ) {
         heightPx = heightPx + fontSize* lineHeight/1.25 + 10
    }
    if (settings["tile${tileId}BorderC"]) {
         heightPx = heightPx + 10 //verified
    }
*/
    Float heightPx = calcTileHeight (numRows, fontSize, settings["tile${tileId}LineHeight"]?.toFloat(), settings["tile${tileId}Title"], settings["tile${tileId}IconUrl"], settings["tile${tileId}BorderC"])
    setTileData (tileId, "heightPx", heightPx)
    return tileString
}

Float calcTileHeight (Integer numRows, Float fontSize, Float lineHeightSetting, String title, String icon, String borderC) {
     //Float heightPx = numRows*fontSize + numRows * fontSize* lineHeight/1.15
     Float lineHeight = lineHeightSetting ?: 1.4
     Float heightPx = numRows*fontSize * lineHeight

     if (numRows > 1) {
         heightPx =  heightPx + 6
     }
     if (title != " " || icon ) {
         //heightPx = heightPx + fontSize* lineHeight/1.15 + 10
         heightPx = heightPx + fontSize * lineHeight + 12
     }
     if (borderC) {
         heightPx = heightPx + 10 //verified
     }
    return heightPx 
}
    
List sortStatus (Integer tileId, List status) {
    List sorted 
    switch (settings["tile${tileId}SortCol"]) {
    case "value" :
           sorted = status.sort {a,b ->
               if (stringValSort) {
                   a.value.toString() <=> b.value.toString() ?: a.time <=> b.time ?: a.name <=> b.name
               } else {
                   a.value <=> b.value ?: a.time <=> b.time ?: a.name <=> b.name
               }
            }
            break
    case "name" :
    case null :
            //log.debug "status: ${status}"
            sorted = status?.sort {a,b ->
               if (stringValSort) {
                   a.name <=> b.name ?: a.time <=> b.time ?: a.value.toString() <=> b.value.toString()
               } else {
                   a.name <=> b.name ?: a.time <=> b.time ?: a.value <=> b.value
               }
            }
            break
    case "time" :
            sorted = status.sort {a,b ->
                 a.time <=> b.time   
            }
           
            break
        case "none" :
    default: 
        sorted = status
        break
        //no sort
    }
    if (settings["tile${tileId}SortInvert"]) {
        sorted = sorted.reverse()
    }
    return sorted
}
    
    
List updateStatus(Integer tileId) {
    //log.debug "Updating status for: ${tileId} ${tileLabel(tileId)}"
    List status = []
    Integer numEntries = getTileData(tileId, "numEntries") ?: 0
    Boolean stringValSort = false
    for (Integer i in 1..numEntries) {    
        getDevicesFromInput(tileId, i).each {
            def dev = it
            getAttrFromInput(tileId, i).each {
                Map entryData = [name: dev.toString(), 
                                 value: dev.currentValue(it.uncapitalize()), 
                                 attribute : it, 
                                 time: getLastUpdateTime(dev.getDeviceNetworkId(),it.uncapitalize()),
                                 dni: dev.getDeviceNetworkId()
                                ]
                status << entryData
                //log.debug "string val sort for ${it} ${entryData.value instanceof Collection}"
                if (entryData.value instanceof Collection || entryData.value instanceof String || entryData.value instanceof GString ) {
                    stringValSort = true
                }
            }
        }
    }
    status = sortStatus (tileId, status) 
    /* 
    switch (settings["tile${tileId}SortCol"]) {
        case "value" :
           status = status.sort {a,b ->
               if (stringValSort) {
                   a.value.toString() <=> b.value.toString() ?: a.time <=> b.time ?: a.name <=> b.name
               } else {
                   a.value <=> b.value ?: a.time <=> b.time ?: a.name <=> b.name
               }
            }
            break
        case "name" :
        case null :
            //log.debug "status: ${status}"
            status = status?.sort {a,b ->
               if (stringValSort) {
                   a.name <=> b.name ?: a.time <=> b.time ?: a.value.toString() <=> b.value.toString()
               } else {
                   a.name <=> b.name ?: a.time <=> b.time ?: a.value <=> b.value
               }
            }
            break
        case "time" :
            status = status.sort {a,b ->
                 a.time <=> b.time   
            }
           
            break
        case "none" :
        default: 
            break
            //no sort
     }
    if (settings["tile${tileId}SortInvert"]) {
        status = status.reverse()
    }
   */
    if (settings["tile${tileId}MaxLines"]) {
        status = status.take(settings["tile${tileId}MaxLines"].toInteger())
    }
    return status
}

void updateTile(Integer tileId) {
    //Long maxSize = maxTileChars ?: 1024
    Float fontSize = settings["tile${tileId}FontSize"] ? settings["tile${tileId}FontSize"] : 16
    Float padding = settings["tile${tileId}Padding"] != null ? settings["tile${tileId}Padding"] : 6
    String tileText = getTileContents(tileId, fontSize, padding.toInteger())

    updateTileString(tileId, tileText)
}

def genSampleVal (Integer tileId, Integer formatterEntry) {
    List attrStyle = getTileData(tileId, "attrStyle") ?: attrStyleDefault
    try {
      String operator = attrStyle[formatterEntry][1]
      def val = attrStyle[formatterEntry][2]
      if (operator == "<") {
        val--
      } else if (operator == ">") {
        val++
      }
       // log.debug "sample for ${tileId} opeartor: ${operator} is ${val} preval: ${attrStyle[formatterEntry][2]} formatter: ${attrStyle[formatterEntry]} entire attrStyle: ${attrStyle}"
        //log.debug "${attrStyle}"
       // log.debug "sample for ${tileId}: i: ${formatterEntry} operator: ${operator} is ${val}"
      return val
    } catch (Exception e) {
        val = "ERROR: ${e}"
        log.debug "Error: ${e} generating sample Val for entry ${formatterEntry}"
    }
}

String formatValStyle (Integer tileId, String attribute, def value) {
    List attrStyle = getTileData(tileId, "attrStyle") ?: attrStyleDefault
    String style = ""
    attrStyle.each {
        try {
            if (it[0] == attribute.uncapitalize() && value != null) {
                def checkVal 
                def val
                Boolean match
                if ("${value}".isNumber() && "${it[2]}".isNumber()) {
                     checkVal = it[2].toFloat()
                     val = value.toFloat()
                } else {
                    checkVal = it[2].toString()
                    val=value.toString()
                } 
                switch (it[1]) {
                    case "=":
                    case "==":
                        match = val == checkVal
                        break
                    case ">":
                        match = val > checkVal
                        break
                    case ">=":
                        match = val >= checkVal
                        break
                    case "<":
                       match = val < checkVal
                       break
                    case "<=":
                       match = val <= checkVal
                       break
                    default:
                       match = false
                       break
               }
               if (match) {
                  style = it[3]
               }
            }
       } catch (Exception e) {
            log.warn "Status Tile style format error: '${e}' applying style for tile: ${tileId}. Attribute: ${attribute}. Value: ${value}. Style: ${it}"
            style="error"
       }
    }
    return style
}
 
String formatValNum (Integer tileId, String attribute, def value) {
    String result = "${value}"
    Boolean match
    Map attrFormat = getTileData(tileId, "attrFormat") ?: attrFormatDefault
    attrFormat.each {
        try {
            if (it.key == attribute.uncapitalize() && value != null) {
                result = sprintf(it.value, value.toFloat())
                //if (it.key == "temperature") {
                //    result = result + " °${getTemperatureScale()}"
                //}
            }
        } catch (Exception e) {
            log.warn "Status Tile number format error: ${e} applying sprint style for tile: ${tileId}, value: ${value}, attribute: ${it.key} ${attribute}, style: ${it.value}"
            //Extra debug - delete...
            //List status = updateStatus(tileId)
            //log.debug "${status}"
            result = result+"⚠️"
        }
    }
    return result

}
    
String formatValue(Integer tileId, String attribute, def value, Boolean aged=null) {
    // log.debug "bbb: ${tileId} ${attribute} ${value} ${aged}"
    String result = formatValNum (tileId, attribute, value)
    String style = formatValStyle(tileId, attribute, value) 
    if (style == "error") {
        result = result + warnSym
    } else if (style && !aged) {
        result = "<span style='${style}'>${result}</span>"
    }
    return result
    //return formatValColor(tileId, attribute, valNum) 
    //return formatValColor(tileId, attribute, valNum) 
}

Boolean tileSubscribesToEvent(Integer tileId, def dni, String attribute) {
    Boolean matched = false
    Integer numEntries = getTileData(tileId, "numEntries") ?: 0
    for (Integer i in 1..numEntries) {  
        Boolean devMatch
        getDevicesFromInput(tileId, i).each {
            if (it.getDeviceNetworkId() == dni) {
                devMatch = true
            }
        }
        if (devMatch) {
             getAttrFromInput(tileId, i).each {
                 if (it?.uncapitalize() == attribute) {
                     matched = true
                     //log.debug "${dni} ${it} matched for tile: ${tileId}!"
                 }
             }
         }
    }
    return matched
}


void setLastUpdateTime (def dni, String attribute, Long unixTime) {
    //logDebug "Setting last update time: dni: ${dni} attr: ${attribute} time: ${unixTime}"
    if (state.lastUpdateTime == null) {
        state.lastUpdateTime = [:]
    }
    if (state.lastUpdateTime[dni] == null) {
        state.lastUpdateTime[dni] = [:]
    }
    state.lastUpdateTime[dni][attribute] = unixTime
}

Long getLastUpdateTime (def dni, String attribute) {
    Long unixTime = 0
    if (state.lastUpdateTime) {
       if (state.lastUpdateTime[dni]) {
           if (state.lastUpdateTime[dni][attribute]) {
                unixTime = state.lastUpdateTime[dni][attribute]
           }
       }
    }
    return unixTime
}

void setLastUpdateValue (def dni, String attribute, def value) {
    //logDebug "Setting last update value: dni: ${dni} attr: ${attribute} value: ${value}"
    if (state.lastUpdateValue == null) {
        state.lastUpdateValue = [:]
    }
    if (state.lastUpdateValue[dni] == null) {
        state.lastUpdateValue[dni] = [:]
    }
    state.lastUpdateValue[dni][attribute] = value
}

def getLastUpdateValue (def dni, String attribute) {
    def value
    if (state.lastUpdateValue) {
       if (state.lastUpdateValue[dni]) {
           if (state.lastUpdateValue[dni][attribute]) {
                value = state.lastUpdateValue[dni][attribute]
           }
       }
    }
    return value
}


void addHistoryEvent (Integer tileId, evt) {
    Integer listSize = 20
    def newEvent = [unixTime: evt.unixTime, value: evt.value, name: evt.displayName, attr: evt.name]
    //logDebug "Adding history event to tile ${tileId}: ${newEvent}"
    //log.debug "Adding history event to tile ${tileId}: ${newEvent}"
    Integer index = 0
    Boolean added = false
    state."tile${tileId}History".each {
        if (evt.unixTime > it.unixTime & !added) {
            state."tile${tileId}History" = state."tile${tileId}History".plus(index, newEvent)
            added = true
        }
        index++
    }
    if (!added) {
        state."tile${tileId}History" << newEvent
    } 
    state."tile${tileId}History" = state."tile${tileId}History".take(listSize)
}

void initLastUpdateTime(Integer tileId) {
    Integer numEntries = getTileData(tileId, "numEntries") ?: 0
    for (Integer i in 1..numEntries) {  
        //if an attribute was not found in device history, set initialize last update time to current time, so that it doesn't appear as aged 
        getDevicesFromInput(tileId, i).each {
            def dev = it
            getAttrFromInput(tileId, i).each {
                if (!getLastUpdateTime (dev.getDeviceNetworkId(), it.uncapitalize())) {
                    setLastUpdateTime (dev.getDeviceNetworkId(), it.uncapitalize(), now())
                }
            }
        }
    }
}

void initHistory(Integer tileId) {
    if (getTileData(tileId, "tileType") == "History") {
        state."tile${tileId}History" = []
    }
    Integer numEntries = getTileData(tileId, "numEntries") ?: 0
    for (Integer i in 1..numEntries) {  
        //Set last update time for attributes based on device history
        getDevicesFromInput(tileId, i).each {
            it.events().each {
                 if (tileSubscribesToEvent (tileId, it.device.getDeviceNetworkId(), it.name)) {
                     if (getTileData(tileId, "tileType") == "History") {
                         addHistoryEvent(tileId, it)
                     }
                     if (it.unixTime > getLastUpdateTime (it.device.getDeviceNetworkId(), it.name.uncapitalize())) {
                         setLastUpdateTime (it.device.getDeviceNetworkId(), it.name, it.unixTime)
                     }
                 }
             }
        }
    }
}

def deviceCallback(evt) {
    //logDebug "Sensor event: name: ${evt.name} val: ${evt.value} time: ${evt.date} name: ${evt.displayName}"   
    Boolean updateEvent = false
    if (settings.filterDuplicates) {
        if (getLastUpdateValue (evt.device.getDeviceNetworkId(), evt.name) != evt.value) {
            setLastUpdateValue (evt.device.getDeviceNetworkId(), evt.name, evt.value)
            updateEvent = true
        }
    } else {
        updateEvent = true
    } 
    if (updateEvent) {
        state.tileList.each() {
            if (tileSubscribesToEvent (it, evt.device.getDeviceNetworkId(), evt.name)) {
                if (getTileData(it, "tileType") == "History") {
                    addHistoryEvent(it, evt)
                }
                updateTile(it)
            }
        }
    }
    setLastUpdateTime (evt.device.getDeviceNetworkId(), evt.name, evt.unixTime)
}

void logDebug(msg) {
    if (debugOutput) {
        log.debug msg
    }
}
    
def refresh(child) {
     state.tileList.each() {
         updateTile(it)
     }
}

void logger (String message, String msgLevel="debug") {
    Integer prefLevelInt = settings.logLevel ? logLevels[settings.logLevel] : 4
    Integer msgLevelInt = logLevels[msgLevel]
    if (msgLevelInt >= prefLevelInt && prefLevelInt) {
        log."${msgLevel}" message
    } else if (!msgLevelInt) {
        log.info "${message} logged with invalid level: ${msgLevel}"
    }
}

def hrefMenuPage(String page, String titleStr, String descStr, String image, params, state = null) {
        String imgFloat = ""
        String imgElement = ""
        if (descStr) {
            imgFloat = "float: left;"
        } //Center title} if no description
        if (image) {
            imgElement = "<img src='${image}' width='40' style='${imgFloat} width: 40px; padding: 0 16px 0 0'>"
        }
        String titleDiv = imgElement + titleStr
        String descDiv = "<div style='float :left; width: 90%'>" + descStr + "</div>"
        href page, description: descDiv, title: titleDiv, required: false, params: params, state: state
}
// Constants
@Field static final Map logLevels = ["none":0, "trace":1,"debug":2,"info":3, "warn":4,"error":5]
@Field static final Map attrLookup = ["acceleration":"accelerationSensor","battery":"battery","contact":"contactSensor","carbonMonoxide":"carbonMonoxideDetector",
                                      "energy":"energyMeter",
                                      "humidity":"relativeHumidityMeasurement","lock":"lock","motion":"motionSensor","power":"powerMeter",
                                      "presence":"presenceSensor","smoke":"smokeDetector","switch":"switch","temperature":"temperatureMeasurement","water":"waterSensor"]                             
@Field static final Map attrFormatDefault = 
    ["battery":     "%.0f%%",
     "current":     "%.0f A",
     "energy":      "%,.0f kWh",
     "frequency":   "%.0f Hz",
     "held":        "held-%s",
     "hue":         "hue-%.0f",
     "humidity":    "%.0f%%",
     "level":       "lvl-%.0f",
     "illuminance": "%.0f lx",
     "power":       "%,.0f W",
     "pushed":      "push-%s",
     "released":    "release-%s",
     "saturation":  "sat-%.0f",
     "temperature": "%.1f\u00B0", 
     "voltage":     "%.0f V"]

@Field static final List attrStyleDefault = 
    [["battery",     "<",        75,   "color:orange"                     ],
     ["battery",     "<",        50,   "color:orangered;font-weight:bold" ],
     ["battery",     ">",        99,   "color:lime"                       ],
     ["contact",     "=",    "open",   "color:lime"                       ],
     ["motion",      "=",  "active",   "color:lime"                       ],
     ["temperature", ">=",        90,   "color:orange"                    ],
     ["temperature", "<=",        32,   "color:blue"                      ],
     ["temperature", ">=",       100,   "color:orangered"                 ],
     ["water",       "=",      "wet",   "color:blue;font-weight:bold"     ]]

// Icons
@Field static final String statusIcon = "iVBORw0KGgoAAAANSUhEUgAAABwAAAAcCAIAAAD9b0jDAAAABGdBTUEAALGPC/xhBQAAACBjSFJNAAB6JgAAgIQAAPoAAACA6AAAdTAAAOpgAAA6mAAAF3C" +
            "culE8AAAAhGVYSWZNTQAqAAAACAAGAQYAAwAAAAEAAgAAARIAAwAAAAEAAQAAARoABQAAAAEAAABWARsABQAAAAEAAABeASgAAwAAAAEAAgAAh2kABAAAAAEAAABmAAAAAAAAAEg" +
            "AAAABAAAASAAAAAEAAqACAAQAAAABAAAAHKADAAQAAAABAAAAHAAAAAD3nbWwAAAACXBIWXMAAAsTAAALEwEAmpwYAAAC4mlUWHRYTUw6Y29tLmFkb2JlLnhtcAAAAAAAPHg6eG1w" +
            "bWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0az0iWE1QIENvcmUgNS40LjAiPgogICA8cmRmOlJERiB4bWxuczpyZGY9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkvM" +
            "DIvMjItcmRmLXN5bnRheC1ucyMiPgogICAgICA8cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0iIgogICAgICAgICAgICB4bWxuczp0aWZmPSJodHRwOi8vbnMuYWRvYmUuY29tL3Rp" +
            "ZmYvMS4wLyIKICAgICAgICAgICAgeG1sbnM6ZXhpZj0iaHR0cDovL25zLmFkb2JlLmNvbS9leGlmLzEuMC8iPgogICAgICAgICA8dGlmZjpSZXNvbHV0aW9uVW5pdD4yPC90aWZmOl" +
            "Jlc29sdXRpb25Vbml0PgogICAgICAgICA8dGlmZjpPcmllbnRhdGlvbj4xPC90aWZmOk9yaWVudGF0aW9uPgogICAgICAgICA8dGlmZjpDb21wcmVzc2lvbj4xPC90aWZmOkNvbXBy" +
            "ZXNzaW9uPgogICAgICAgICA8dGlmZjpQaG90b21ldHJpY0ludGVycHJldGF0aW9uPjI8L3RpZmY6UGhvdG9tZXRyaWNJbnRlcnByZXRhdGlvbj4KICAgICAgICAgPGV4aWY6UGl4ZWxZR" +
            "GltZW5zaW9uPjQwPC9leGlmOlBpeGVsWURpbWVuc2lvbj4KICAgICAgICAgPGV4aWY6Q29sb3JTcGFjZT4xPC9leGlmOkNvbG9yU3BhY2U+CiAgICAgICAgIDxleGlmOlBpeGVsWERpbW" +
            "Vuc2lvbj40MDwvZXhpZjpQaXhlbFhEaW1lbnNpb24+CiAgICAgIDwvcmRmOkRlc2NyaXB0aW9uPgogICA8L3JkZjpSREY+CjwveDp4bXBtZXRhPgrQreb/AAAAx0lEQVRIDdWVPQ6EIBB" +
            "GZcMZKCk8BK0lIXAEvZyeQCo6GhIKwiE8grF3140HmJlEC6hfHvPN8NN577+4NY6jcw7DfjrKuowYnCbFGC+mHSlHJrqxlJIxBuwsTbrvewgBrKOdnr5SKa2nQgitNdhTmlQpNc8zKKXF" + 
            "Bw/TvR9NCtbYmpQvy1JKweSqtfZ9jyFpjLX2+UcaWUI70+fDMEgpMblijBjsz+B/02maXhnU89cUabzSNzT9nPN5nmA0xti2bcdxrOsKwj/Oqfp8ii6SGgAAAABJRU5ErkJggg=="
@Field static final String ddLogoHubitat = "https://darwinsden.com/download/ddlogo-for-hubitat-pwManagerV3-png"
@Field static final String ddLogoSt = "https://darwinsden.com/download/ddlogo-for-st-pwManagerV3-png"
@Field static final String cogIcon = "https://rawgit.com/DarwinsDen/SmartThingsPublic/master/resources/icons/cogD40.png"
@Field static final String ppBtn = "https://www.paypalobjects.com/en_US/i/btn/btn_donate_SM.gif"
@Field static final String dashIcon = "https://rawgit.com/DarwinsDen/SmartThingsPublic/master/resources/icons/dashboard40.png"
@Field static final String refreshSymb = "<span style = 'font-size: 140%; padding: 0; margin: 0; line-height: 0.2; font-weight: bold;'>&#10227;</span>"
@Field static final String gearSymb = "<span style = 'font-size: 180%; padding: 0; margin: 0; line-height: 0.2; font-weight: bold;'>&#9881;</span>"
@Field static final String addIcon = "https://rawgit.com/DarwinsDen/SmartThingsPublic/master/resources/icons/add40.png"
@Field static final String warnSym = "&#9888;&#65039;"
