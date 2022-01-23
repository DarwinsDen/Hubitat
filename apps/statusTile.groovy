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
    description: "Create device status overview and history dashboard tiles. Create virtual average devices. Set up summary status information alerts",
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

def version() {
    return "v0.1.01.20210620"
}

void deleteTile (Integer tileId) {
    logger ("Deleting tile: ${tileId}, number: ${tileId}", "debug")
    state.tileIdUsed[tileId-1] = false
    state.tileDeleted = true
    state.tileList.removeElement(tileId)
    clearTileData (tileId)
    String dni= getTileDeviceDni(tileId)
    deleteChildDevice(dni)
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

def setTileData (Integer tileId, String param, def value) {
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
          app.updateSetting(devInput, [type: "capability", value: ""]) 
       }
       state.remove("tile${tileId}Data".toString())
       state.remove("tile${tileId}History".toString())
       String tileString = "${tileId}"
       settings.each{key, val -> 
           //log.debug "checking ${key} ${val}"
           if (key.size() > 3 + tileString.size() && key.substring(0,4+tileString.size()) == "tile${tileId}") {
                log.debug "clearing: ${key}"
                app.clearSetting(key)
           }
       }
    } catch (e) {
                log.debug "Exception clearing tile data for tile ${tileId}: ${e}"
            }
}

@Field static final def menuOrder = ["menuTyleType"       : [prev: "menuMain",     next: "menuDevices"],
                                     "menuDevices"        : [prev: "menuTileType", next: "menuEditTile"],
                                     "menuSelectEditTile" : [prev: "menuMain",     next: "menuEditTile"],
                                     "menuEditTile"       : [prev: "menuMain",     next: "menuMain"]]

    /* 
String nextState (String state) {
    if (menuOrder[state].next) {
        newState = menuOrder[state].next
    } else {
        log.warn "No menuOrder next state defined for ${state}"
    }
}

String previousState (String state) {
    if (menuOrder[state].prev) {
        newState = menuOrder[state].prev
    } else {
        log.warn "No menuOrder prev state defined for ${state}"
    }
}
*/
        
//def btnCancel(Integer width=null) {
//   input "btnCancel", "button", title: "Cancel", width : width
//}
//
//def btnBack(Integer width=null) {
//   input "btnBack", "button", title: "Back", width : width
//}

def pageMain() {
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
               state.add
               setTileData (state.editTileId, "numEntries", 0)
               setTileData (state.editTileId, "attrFromDev", [])
               setAttrFromDev (state.editTileId, 1, false)
               state.thisState = "menuDevices"
               setTileData (state.editTileId, "tileType", tileType)  
           }  
           break
       case "menuDevices" :
          initHistory(state.editTileId)
          initLastUpdateTime(state.editTileId)
          if (!getTileDevice(state.editTileId)) {
              createTileDevice (state.editTileId)
          }
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
                devicesMenu(state.editTileId)
                if (getTileData(state.editTileId, "numEntries")) {
                    section {
                        input "btnEditTile", "button", title: "Edit Tile Format", submitOnChange: true, width:3
                        String dashboard = "<div style='float:left; margin: -8px 14px 0 0;'>${dashboard(tileName(state.editTileId), 0.8, state.editTileId)}</div>"
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
    def attrSelection 
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
            app.updateSetting(devInput, [type: "capability", value: ""]) 
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
        
    if (label) {
        setCustomLabel (params.tileId, params.device, params.attribute, label)
        log.debug "setting custom label! ${label}"
    }
    dynamicPage(name: "pageEditLabel", title: "") {
        section {
           
            input "label", "text", title:"new label for: ${params.device} (attribute: ${params.attribute})", defaultValue: custLabel,submitOnChange: true
            
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
    
    if (format) {
        attrFormat[params.attribute]=format
        setTileData(params.tileId, "attrFormat", attrFormat) 
        Map attrFormatMod = getTileData(params.tileId, "attrFormatMod")  ?: [:]
        attrFormatMod[params.attribute]=true
        setTileData(params.tileId, "attrFormatMod", attrFormatMod) 
    }
    dynamicPage(name: "pageEditAttrFormat", title: "") {
        section {    
            input "format", "text", title:"new format for: ${params.attribute}", submitOnChange: true, width:4      
        }
    }      
}

/*
def pageEditAttrFormat(params) {
    Integer numEntries = getTileData(params.tileId, "numEntries") ?: 0
    List attributes = []
    for (Integer i in 1..numEntries) {    
        getAttrFromInput(params.tileId, i).each {
            attributes << it
        }
    }
    Map formatEntries = getTileData(params.tileId, "formatData") ?: attrFormat  
    if (format) {
        formatEntries[params.attribute]=format
        setTileData(params.tileId, "formatData", formatEntries) 
        log.debug "${formatEntries}"
        log.debug "setting custom format! ${format}"
    //} else {
    //     app.updateSetting("format", [type: "text", value: formatEntries[params.attribute]])
    }
    dynamicPage(name: "pageEditAttrFormat", title: "") {
        section {    
            input "format", "text", title:"new format for: ${params.attribute}", submitOnChange: true, width:4      
        }
    }

      
}
*/

/*
def pageAddAttrFormat(params) {
    //Just starting with copy from edit...
    Integer numEntries = getTileData(params.tileId, "numEntries") ?: 0
    List attributes = []
    for (Integer i in 1..numEntries) {    
        getAttrFromInput(params.tileId, i).each {
            attributes << it
        }
    }
    if (format) {
        Map formatEntries = getTileData(params.tileId, "formatData") ?: attrFormat 
        formatEntries[params.attribute]=format
        setTileData(params.tileId, "formatData", formatEntries) 
        log.debug "${formatEntries}"
        log.debug "setting custom format! ${format}"
    //} else {
    //     app.updateSetting("format", [type: "text", value: formatEntries[params.attribute]])
    }
    dynamicPage(name: "pageAddAttrFormat", title: "") {
        section {    
            input "attribute", "enum", title:"attribute", options: attributes, submitOnChange: true, width:4    
            
            if (attribute) {
                input format, "text", title:"Enter format", submitOnChange: true, width:4   
            }
                
            
        }
    }

      
}
*/

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
    log.debug "setting custom label: ${label} for ${device} ${attribute} ${tileId}"
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
        input "btnMoreAttr", "button", title: "More Capabilities..", submitOnChange: true, width:3
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
    
String tileName(Integer tileId) {
    if (settings["tile${tileId}Name"]) {
        name = settings["tile${tileId}Name"]
    } else {
        name = "Status Tile ${tileId}"
        if (getTileData(tileId, 'tileType') == "History") {
            name = name + " - History"
        }
    }
    return name
}

//String tileTitle (Integer tileId) { 
//    if (settings["tile${tileId}Title"]) {
//        name = settings["tile${tileId}Title"]
//    } else {
//        name = determineTileTitle(tileId)
//    }
//    return name
//}

String determineTileTitle (Integer tileId) {   
    Integer checkEntries = (getTileData(tileId, "numEntries") ?: 0) + 1
    Boolean commonAttr = true
    String title
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
    String tileType = getTileData(tileId, "tileType")
    //log.debug "numEntries: ${numEntries} attr: ${attr} commonAttr: ${commonAttr}"
    if (attr && commonAttr) {
        title = "${camelCapSplit(attr)} ${tileType}"
    } else {
        title = "Device ${tileType}"
    }
    return title
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
        result = result + ' ' + s
    }
    return result
}

/*
void initTileName (Integer tileId) {   
    Integer checkEntries = (getTileData(tileId, "numEntries") ?: 0) + 1
    Boolean commonAttr = true
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
    String tileType = getTileData(tileId, "tileType")
    //log.debug "numEntries: ${numEntries} attr: ${attr} commonAttr: ${commonAttr}"
    if (attr && commonAttr) {
        app.updateSetting("tile${tileId}Title", [type: "text", value: "${camelCapSplit(attr)} ${tileType}"]) 
    } else {
        app.updateSetting("tile${tileId}Title", [type: "text", value: "Device ${tileType}"]) 
    }
}
*/

void editTileMenu (Integer tileId) {
    //listCustomLabels(tileId)
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
            String dashboard = "<div style='float:left; margin: -8px 14px 0 0;'>${dashboard("", 0.8, tileId)}</div>"
            //  String dashboard = "<div style='float:left; margin: -8px 14px 0 0;'>${dashboard("", 1.0, tileId)}</div>"
            Integer totalChars = getTileData(tileId, "tileChars")
            Integer formatChars = getTileData(tileId, "tileFormatChars")
            Float fmtPercent = formatChars/totalChars * 100.0
            String charStr = "\nTotal Chars: ${totalChars}\nFormat: ${formatChars} (${fmtPercent.round(1)}%)"
            if (getTileData(tileId, "tileType")== "Status") {
                if (getTileData(tileId, "clipped")) {
                    charStr = charStr + "\n<span style='color:orangered'>Clipped: ${getTileData(tileId, "clipped")}</span>"
                } else {
                    //Integer remainChar = (settings["maxTileChars"] ?: 1024) - (getTileData(tileId, "tileFormatChars") ?: 0)
                    Integer remainChar = (settings["maxTileChars"] ?: 1024) - totalChars
                    charStr = charStr + "\nRemaining: ${remainChar}"
                    
                }
            } else {
                charStr = charStr + "\nLines: ${getTileData(tileId,'histSize')}"
            } 
            
            String info = "<div style='font-size: 88%'>${tileName(tileId)}${charStr}</div>"
           // paragraph  dashboard + info, width: 10
            paragraph  dashboard + info, width: 10
            input "refresh", "button", title : refreshSymb, width : 2, submitOnChange : true
       
            //paragraph "tile size: ${getTileData(tileId, "tileChars")} ${clipStr}"
            input "tile${tileId}Name", "text", title: "Tile name", defaultValue: tileName(tileId), submitOnChange : true, width: 3
            input "tile${tileId}SortInvert", "bool", title: smInp("Invert sort order"), submitOnChange : true, defaultValue: false, width: 2
            //input "tile${tileId}ContextColor", "bool", title: smInp("Context sensitive status color"), submitOnChange : true, defaultValue: true, width: 4
            input "tile${tileId}FontSize", "number", title: smInp("Font size (px)"), defaultValue: 16, required: false, submitOnChange : true, width: 2
            input "tile${tileId}Padding", "number", title: smInp("Padding (px)"), defaultValue: 6, submitOnChange : true, required: false, width: 2
            
            //input "tile${tileId}ShortTime", "bool", title: smInp("Shorten time/date cols"), submitOnChange : true, defaultValue: false, width: 3
            if (getTileData(tileId,"tileType") == "Status") {
                input "tile${tileId}ShowLastUpdate", "bool", title: "Show last update time", submitOnChange : true, defaultValue: false, width: 4
            }
            if (settings["tile${tileId}ShowLastUpdate"] || getTileData(tileId,"tileType") == "History") {
                input "tile${tileId}timeFormat", "text", title: smInp("time Format"), submitOnChange : true, defaultValue: "hh:mm a, dd MMM yyy", width: 3
                
                //input "tile${tileId}timeFormat", "text", title: smInp("time Format"), submitOnChange : true, defaultValue: "hh:mm a zzz", width: 3
                //input "tile${tileId}dateFormat", "text", title: smInp("date Format"), submitOnChange : true, defaultValue: "MMM dd, yyyy", width: 3
            }
            input "tile${tileId}ColLayout", "bool", title: smInp("Column layout"), submitOnChange : true, defaultValue: true, width: 3
            Boolean colLayout = settings["tile${tileId}ColLayout"] == null || settings["tile${tileId}ColLayout"]
            //input "tile${tileId}IndicateAged", "bool", title: "Indicate aged devices", submitOnChange : true, defaultValue: true, width: 4

            input "tile${tileId}ShowDeviceName", "bool", title: "Show Device Name", submitOnChange : true, defaultValue: true, width: 3
            input "tile${tileId}ShowAttributeName", "bool", title: "Show Attribute Name", submitOnChange : true, defaultValue: false, width: 3
            input "tile${tileId}ShowCustomLabel", "bool", title: "Show Custom Label", submitOnChange : true, defaultValue: false, width: 3
            input "tile${tileId}AgedMode", "enum", title: smInp("Aged display mode"), 
               options: ["off" : "Do not indicate aged", "attribute" :"consider only attribute displayed","anyAttribute":"consider any attribute"], submitOnChange : true, defaultValue: "anyAttribute", width: 3
 
            input "tile${tileId}SortCol", "enum", title: smInp("Status Tile Sort Column"), 
                options: ["none":"No Sorting","name":"Device Name", "value" : "Attribute Value", "time" :"Last Update Time"], submitOnChange : true, defaultValue: "Device Name", width: 3
            if (settings["tile${tileId}ColLayout"]==false) {
                input "tile${tileId}LineJustify", "enum", title: smInp("Line Justification"), 
                    options: ["left":"Left","center":"Center", "right" : "Right"], submitOnChange : true, defaultValue: "center", width: 3
            } else {
               input "tile${tileId}ValJustify", "enum", title: smInp("Attribute Value Justification"), 
                    options: ["left":"Left","center":"Center", "right" : "Right"], submitOnChange : true, defaultValue: "right", width: 3
            }         
            //input "tile${tileId}ValJustify", "enum", title: smInp(justifyLabel), 
            //    options: ["left":"Left","center":"Center", "right" : "Right"], submitOnChange : true, defaultValue: justifyDefault, width: 3
            input "maxTileChars", "number", title: smInp("Max Tile chars (default: 1024)"), submitOnChange : true, defaultValue: 1024, width: 3
            href pageSelectDevices, description: "", title: smInp("Edit Devices"), params: [tileId: tileId], width: 3
            //input "btnEditDevices", "button", title: "Edit Devices", submitOnChange: true, width: 3
            //input "btnDone", "button", title: "Done", submitOnChange: true, width: 10
            paragraph "", width: 7
            input "btnDeleteTile", "button", title: "Delete Tile", textColor: "red", submitOnChange: true, width: 2
            //paragraph "Tile St. Parameters here will decrease the available total tile characters available. Delete entries to turn off settings:"
            paragraph "<div style ='border: 1px solid gray; background-color: LightSteelBlue; padding: 10px;'>Tile formatting. Parameters here will decrease the available total tile characters. Delete entries to turn off settings::</div>"
            input "tile${tileId}Title", "text", title: "Tile title (set to single space to remove title line)", submitOnChange : true, width: 4
            input "tile${tileId}IconUrl", "text", title: "Tile Icon URL", submitOnChange : true, width: 4
            input "tile${tileId}BackgroundC", "text", title: "Background color eg: blue, rgba(0,40,0,0.4) ", submitOnChange : true, width: 4
            input "tile${tileId}FontColor", "text", title: "font color eg: red, #0000ff", submitOnChange : true, width: 4
            input "tile${tileId}BorderC", "text", title: "Border color eg: green, #006900", submitOnChange : true, width: 4
            input "tile${tileId}MaxLines", "number", title: "Max entry lines to display", submitOnChange : true, width: 4
            input "tile${tileId}MaxDevChars", "number", title: "Max device name characters", submitOnChange : true, width: 4
       } 
        attrStyleSection(tileId)
        attrFormatSection(tileId)
        customLabelSection(tileId)
    }
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
      section(hideable: true, hidden: true, "Custom Labels") {
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
                    app.updateSetting("label", [type: "text", value: ""])
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
   
           /*
            href pageEditI, description: "", title: smInp("Edit"), params: [tileId: i], width: 2
            String row = ""
            Integer j = 0
            //def val = genSampleVal(i)
            //String sample = formatValue(tileId, it[0], val)
            //String sampleDiv = "<div style = 'padding: 3px; text-align:center; background-color:dimgray'>${sample}</div>"
            //it.each {
                //Float width = (maxChars[j])*100.0/(maxChars[0]+maxChars[1]+maxChars[2]+maxChars[3])
                String tdStyle = "width=50%' style='text-align: left; border:1px solid darkgray'"
                String pStyle = "style='padding: 0 3px; margin: 0'"
                //String pStyle = "style='font-size: 95%; padding: 0 3px; margin: 0'"
                //row = row + "<td ${tdStyle}><p ${pStyle}>${formatter[i][j]}</p></td>" 
                row = row + "<td ${tdStyle}>${it.key}</td><td ${tdStyle}>${it.value}</td>" 
                //j++
            //}
            paragraph "<div style='margin-left: 16px'><table style='width: 100%; float:left; border-collapse: collapse'><tr>${row}</tr></table></div>", width: 8
            paragraph sampleDiv, width: 1
            i++
        }

                        
    }*/



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
       state.tileList.each() {
           String name = "<div style='width: 100px; float:left; padding: 4px; margin: 14px 24px 0 24px;font-size:95%'>${tileName(it)}</div>"
           String dashboard = "<div style='margin: -10px 0 12px 0'>${dashboard(tileName(it), 0.6, it)}</div>"
           input "btnEdit${it}", "button", title: "edit", submitOnChange: true, width: 1
           paragraph name + dashboard, width: 11
       }
    }
}
    
void devicesMenu(Integer tileId) {
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
    //Boolean result = state."btn${name}" ?: false
    //if (state."btn${name}") {
    //    state."btn${name}" = false
    //}
    //if (state.button == "btn${name}") {
    //    state.button = ""
    //    result = true
    //}
    if (state.buttonHit == "btn${name}") {
        log.debug "btn processed: ${name}"
        state.buttonHit = null
        result = true
    }
    return result
}

void appButtonHandler(btn) {
    log.debug "Button hit: ${btn}"
    //state."${btn}" = true
    //state.button = btn
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

String sampleTile(Integer tileId, Float scale, String name, Integer spacing = 14) {
    String tileStyle = "background-color:hsla(0,0%,50.2%,.5);border-radius: 3px;"
    String pName = ""
    //String tileType = getTileData(tileId, "tileType")
    String tileText
    Float fontSize = settings["tile${tileId}FontSize"] ? settings["tile${tileId}FontSize"]*scale : 16*scale
    Float padding = settings["tile${tileId}Padding"] != null ? settings["tile${tileId}Padding"]*scale : 6*scale
    Long maxSize = maxTileChars ?: 1024
    //log.debug "${tileId} fontsize: ${fontSize}"
    tileText = getTileContents(tileId, fontSize.toInteger(), padding.toInteger(), maxSize)
    //if (tileType == "Status") {
    //    tileText = getStatusString(tileId,fontSize.toInteger(), padding.toInteger(), maxSize)
    //} else {
     //   tileText = getHistoryString(tileId,fontSize.toInteger(), padding.toInteger(), maxSize)
    //}
    if (name) {
       pName = "<p style='margin:0;padding-left:6px;font-size:70%'>${name}</p>"
    } 
    //String container = "<div style='white-space:nowrap;line-height:1.2;font-weight:normal;padding: 12px 8px 8px 8px;${tileStyle}'>" +
    String container = "<div style='white-space:nowrap;font-weight:normal;padding: 12px 8px 8px 8px;${tileStyle}'>" +
        "${tileText}${pName}</div>"
    String tile = "<div style='float:left;margin:${spacing}px 0 0 ${spacing}px;'>"+container+"</div>"
    return tile
}


String dashboard(String title, Float scale, Integer tileId = null) {
    Integer dashbPadding = 0
    String dashbTextStyle = "color:#fff;text-shadow: 1px 1px 4px #000;font-size: 14px;text-align: center;line-height:1.0;"
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
            state.tileList.each() {
               tile = tile + sampleTile(it, scale, tileName(it))
            } 
            /*  Limit 4 tiles per row
            String tileRow = ""
            Integer maxTilesPerRow = 4
            for (Integer i in 1..numTiles) { 
                tileRow = tileRow + sampleTile(i, scale, tileName(i))
                if (i % maxTilesPerRow == 0 || i == numTiles) {
                    tile = tile + "<div style='clear:both'>" + tileRow + "</div>" 
                    tileRow = ""
                }
            }*/
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
    state.tileList?.each {
        subscribeEvents (it)
    }
}

//void createTileDevice(String id, String label, String namespace, String typeName) {
//String historyDevice() {
//    "SummarizerHistory_${app.id}"
//}

def getTileDevice (Integer tileId) {
    String dni = getTileDeviceDni (tileId)
    return getChildDevice (dni)
}

String getTileDeviceDni (Integer tileId) {
    return "StatusTile${app.id}_${tileId}"
}

void createTileDevice(Integer tileId) {

    if (!getTileDevice(tileId)) {
       String dni= getTileDeviceDni(tileId)
       String namespace = "darwinsden"
       String label= tileName(tileId)
       String name = "Status Tile ${tileId}"
       String typeName = "Tile Device"
   
       //if (attType.contains(type)) {
            log.debug "Adding new tile device for tile: ${tileId} - type: ${type} typeName: ${typeName} dni: ${dni}"
            try {
                //device = addChildDevice(namespace, typeName, dni)
                device = addChildDevice(namespace, typeName, dni, [label: label, name: label])
                //device = addChildDevice(namespace, typeName, dni, null, [label: label, name: label])
            } catch (e) {
                log.debug "Unable to add device for tile id: ${tileId} ${type} ${typeName}: ${e}"
            }
     } else {
            log.debug "Tile Device ${tileId} exists"
        }
    //} else if (device) {
    //    deleteChildDevice(id)
    //}
}
                         
def initialize() {
    //def averageDev = getChildDevice("AverageTemp_${app.id}")
    //unsubscribe()
    subscribeDevices()
    state.tileList?.each {
        createTileDevice (it)
    }
    //String label = appName ? appName + " History" : "Summarizer History"
    //createOrDeleteTileDevice("Device History Tile", historyDevice(), label, "darwinsden", "Dashboard Tile Device")
    //;abel = appName ? appName + " Status" : "Summarizer Status"
    //createOrDeleteTileDevice("Current Status Tile", statusDevice(), label, "darwinsden", "Dashboard Tile Device")
    //label = appName ? appName + " Status and History" : "Summarizer Status and History"
    //createOrDeleteTileDevice("Combo Status and History Tile", comboDevice(), label, "darwinsden", "Dashboard Tile Device")
    refresh()
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

String getDivFormat(Integer tileId, Integer fontSize) {
    String title
    String titleStr = ""
    String imgStr = ""
    String divStyle = "line-height:1.1;font-size:${fontSize}px;"
    if (settings["tile${tileId}Title"] == null) {
        title = determineTileTitle(tileId) 
    } else if (settings["tile${tileId}Title"] != " ") {
        title = settings["tile${tileId}Title"]
    }
    if (title || settings["tile${tileId}IconUrl"]) {
        if (title) {
            titleStr = "<div style='font-size:110%;margin:3px 8px;float:left'><b>${title}</b></div>"
        }
        if (settings["tile${tileId}IconUrl"]) {
            imgStr = "<img src='${settings["tile${tileId}IconUrl"]}' style='height:${(fontSize*1.1).toInteger()+7}px' align='left'>"
        } 
        titleStr = "<div style='display:inline-block'>${imgStr}${titleStr}</div>"
    }
    if (settings["tile${tileId}BorderC"]) {
        divStyle=divStyle + "padding:3px;border-radius:10px;border:2px solid ${settings["tile${tileId}BorderC"]};"
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
    return nameStr
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

String getStatusString(Integer tileId, Integer fontSize, Integer padding, Long maxSize) {
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
         cssStyle = "<style>.${tableClass} td:nth-child(${attrColumn}){text-align:${justify}}td{text-align:left;padding:0 ${padding}px}</style>" 
         
    }
     Integer whiteSpaceSize = (1.5+(padding*padding/fontSize/2.0)).toInteger() //whitespace pseudo-padding
    String tileString = cssStyle + getDivFormat(tileId, fontSize)
    String endTag = "</table></div>"
    Integer maxWidth = 0
    Integer clipped = 0
    String contentStr = ""
   
    //  endTag = "</table>" + endTag
    tileString = tileString + "<table class='${tableClass}'align='center'>" //center ensures table itself is centered left-right
    
    if (status) {
        status.each {
            String row
            String content = ""
            String devName = customLabel(tileId, it.name, it.attribute) ?: it.name
            String attrName = ""
            
            if (showAttr) {
                attrName = it.attribute
            } 
            Long numAgeDays = 0

            if (settings["tile${tileId}AgedMode"] == null || settings["tile${tileId}AgedMode"] =="anyAttribute") {
                numAgeDays =  msToDays(now()-lastDeviceUpdateTime(tileId, it.dni))
            } else if (settings["tile${tileId}AgedMode"] == "attribute") {
                 numAgeDays = msToDays(now().toLong()-it.time)
             }

            Boolean isAged = numAgeDays > 2
            //log.debug "aaa: ${tileId} ${it.attribute} ${it.value} ${isAged}"
            String val = formatValue(tileId, it.attribute, it.value, isAged)
            String trStr
            if (isAged)
            {
                trStr = "<tr style='color:#BEBEBE'>"
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
                row=addToRow (row, attrName, colLayout, whiteSpaceSize)
                content = content + attrName
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

            if (tileString.size() + row.size() + endTag.size() <= maxSize) {
                tileString = tileString + row
                contentStr = contentStr + content
            } else {
                clipped = clipped + tileString.size() + row.size() + endTag.size() - maxSize
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
    return tileString
}

String getTileContents(Integer tileId, def fontSize, def padding, Long maxSize) {
    String tileType = getTileData(tileId, "tileType")
    if (tileType == "Status") {
        tileText = getStatusString(tileId,fontSize.toInteger(), padding.toInteger(), maxSize)
    } else {
        tileText = getHistoryString(tileId,fontSize.toInteger(), padding.toInteger(), maxSize)
    }
}

String getHistoryString(Integer tileId, def fontSize, def padding, Long maxSize) {
    String cssStyle = "<style>td:nth-child(2){text-align:right}td{text-align:left;padding:0 ${padding}px;font-size:${fontSize}px};</style>"
    String startTag = cssStyle + getDivFormat(tileId, fontSize) + "<table align='center'>"
    String histString = "" 
    String endTag = "</table></div>"
    String contentStr = ""
    Integer histTableSize = 0
    Integer maxLines = settings["tile${tileId}MaxLines"] ?: 20
    List histTable = []
    if (state."tile${tileId}History") {
        state."tile${tileId}History".each {
             ///log.debug "ccc: ${tileId} ${it.attr} ${it.value}"
            String value = formatValue(tileId, it.attr, it.value)
            String timeDate = formatDate(tileId, it.unixTime)
            String devName = customLabel(tileId, it.name, it.attr) ?: it.name
            if (settings["tile${tileId}MaxDevChars"] != null && devName.length() > settings["tile${tileId}MaxDevChars"]) {
               devName = devName.substring(0, settings["tile${tileId}MaxDevChars"].toInteger())
            }
            String rowAdd = "<tr><td>${devName}</td><td>${value}</td><td>${timeDate}</td></tr>"
            if (startTag.size() + histString.size() + rowAdd.size() + endTag.size() <= maxSize && histTable.size() < maxLines) {
                histTable << rowAdd
                histString = histString + rowAdd
                contentStr = contentStr+"${devName}${value}${timeDate}"
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
    }
    String tileString = startTag + histString + endTag 
    setTileData (tileId, "tileChars", tileString.size())
    setTileData (tileId, "tileFormatChars", tileString.size() - contentStr.size())
    setTileData (tileId, "histSize", histTable.size())
    return tileString
}

List updateStatus(Integer tileId) {
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
                //log.debug "string val sourt for ${it} ${entryData.value instanceof Collection}"
                if (entryData.value instanceof Collection || entryData.value instanceof String || entryData.value instanceof GString ) {
                    stringValSort = true
                }
            }
        }
    }
    //Sort
  
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
    if (settings["tile${tileId}MaxLines"]) {
        status = status.take(settings["tile${tileId}MaxLines"].toInteger())
    }
    return status
}

/*void updateHistoryTile(Integer tileId) {
    return
    Integer fontSize = settings.tileFontSize?.toInteger() ?: 20
    Integer padding = settings.tilePadding != null ? settings.tilePadding.toInteger() : 6
    Long maxSize = maxTileChars ?: 1024
        String tileString = getHistoryString(tileId, fontSize, padding,  maxSize)
        device = getChildDevice(historyDevice())
       device.sendEvent(name: "tile", value: tileString, displayed: true, isStateChange: true)
}
*/

void updateTile(Integer tileId) {
    //String dni= getTileDeviceDni(tileId)
   // Integer fontSize = settings.tileFontSize?.toInteger() ?: 20
  //  Integer padding = settings.tilePadding != null ? settings.tilePadding.toInteger() : 6
    Long maxSize = maxTileChars ?: 1024
    Float fontSize = settings["tile${tileId}FontSize"] ? settings["tile${tileId}FontSize"] : 16
    Float padding = settings["tile${tileId}Padding"] != null ? settings["tile${tileId}Padding"] : 6
    String tileText = getTileContents(tileId, fontSize.toInteger(), padding.toInteger(), maxSize)
    
    
     //   String tileString = getStatusString(fontSize, padding, maxSize)
     def device = getTileDevice(tileId)
     if (device) {
        device?.sendEvent(name: "tile", value: tileText, displayed: true, isStateChange: true)
     }
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
            log.warn "Error: '${e}' applying style for tile: ${tileId}. Attribute: ${attribute}. Value: ${value}. Style: ${it}"
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
            log.warn "Error: ${e} applying sprint style for tile: ${tileId}, value: ${value}, attribute: ${it.key} ${attribute}, style: ${it.value}"
            //Extra debug - delete...
            List status = updateStatus(tileId)
            log.debug "${status}"
        }
    }
    return result

}
    
String formatValue(Integer tileId, String attribute, def value, Boolean aged=null) {
    // log.debug "bbb: ${tileId} ${attribute} ${value} ${aged}"
    String result = formatValNum (tileId, attribute, value)
    String style = formatValStyle(tileId, attribute, value) 
    if (style && !aged) {
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
    logDebug "Setting last update time: dni: ${dni} attr: ${attribute} time: ${unixTime}"
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

void addHistoryEvent (Integer tileId, evt) {
    Integer listSize = 20
    def newEvent = [unixTime: evt.unixTime, value: evt.value, name: evt.displayName, attr: evt.name]
    logDebug "Adding history event to tile ${tileId}: ${newEvent}"
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
    logDebug "Sensor event: name: ${evt.name} val: ${evt.value} time: ${evt.date} name: ${evt.displayName}"
    // log.debug "Sensor event: name: ${evt.name} val: ${evt.value} time: ${evt.date} name: ${evt.displayName}"
    state.tileList.each() {
        if (tileSubscribesToEvent (it, evt.device.getDeviceNetworkId(), evt.name)) {
            if (getTileData(it, "tileType") == "History") {
                addHistoryEvent(it, evt)
            }
            setLastUpdateTime (evt.device.getDeviceNetworkId(), evt.name, evt.unixTime)
            //updateStatus(it)
            updateTile(it)
        }
    }
}

def logDebug(msg) {
    if (debugOutput) {
        log.debug msg
    }
}

def refresh(child) {
    //
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
