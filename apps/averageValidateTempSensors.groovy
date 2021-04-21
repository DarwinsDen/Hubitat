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
    name: "Average Valid Temperature Sensors",
    namespace: "darwinsden",
    author: "Darwin",
    description: "Average multiple temperature sensors. Disregard sensors if readings aren't recent.",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "")

preferences {
	page(name: "mainPage")
}

def mainPage() {
	dynamicPage(name: "mainPage", title: " ", install: true, uninstall: true) {
		section {
			input "appName", "text", title: "Name this average temperature app/device", submitOnChange: true
			if(appName) app.updateLabel("$appName")
			input "sensors", "capability.temperatureMeasurement", title: "Select Temperature Sensors", submitOnChange: true, required: true, multiple: true
			sensors.each {
				input "weight$it.id", "decimal", title: "Enter weight factor for ${it} (eg 3 affects the average 3x more than a 1)", defaultValue: 1.0, submitOnChange: true, width: 3
			}
			input "numSamples", "number", title: "Number of sample sensor readings for running average", defaultValue: 1, submitOnChange: true
			initStates()
            input "staleMinutes", "number", title: "Number of minutes since last report for sensor data to be valid. Otherwise reading will be dropped from current average:", defaultValue: 60, submitOnChange: true
		    input "logAged", "bool", title: "Log when a sensor has not reported in the last 24 hours", defaultValue: true
            input "debugOutput", "bool", title: "Enable debug logging", defaultValue: false
        }
	}
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	def averageDev = getChildDevice("AverageTemp_${app.id}")
	if(!averageDev) averageDev = addChildDevice("hubitat", "Virtual Temperature Sensor", "AverageTemp_${app.id}", null, [label: appName, name: appName])
	averageDev.setTemperature(averageTemp(staleMinutes))
	subscribe(sensors, "temperature", sensorCallback)
}

def initStates() {
	def temp = averageTemp(0)
    if (numSamples > 1) {
	   if(!state.tempHistory) {
		  state.tempHistory = []
		  for(int i = 0; i < numSamples; i++) state.tempHistory += temp
	   }
    }
  	if(!state.lastUpdateTime) {
        state.lastUpdateTime = [:]
    }
    sensors.each {
        if (!state.lastUpdateTime [it.id.toString()]) {
            log.debug "adding ${it.id.toString()}"
            state.lastUpdateTime [it.id.toString()]=now()
        }
    }
}

def uninstalled() {
    removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

def getAverageTemp(staleTime) {
	def sum = 0
	def count = 0
    def validSensorFound = false
    def minutesSinceLastReport = 0
    //log.debug "states: ${state.lastUpdateTime}"
	sensors.each {
      if (state?.lastUpdateTime[it.id.toString()]) {
        minutesSinceLastReport = (now() - state.lastUpdateTime[it.id.toString()])/60000
      }
      if (staleTime == 0 || minutesSinceLastReport < staleTime) {
          def weightFactor = settings["weight$it.id"] != null ? settings["weight$it.id"] : 1
          logDebug "averaging ${it} ${it.currentTemperature} with weight ${weightFactor} and age ${minutesSinceLastReport.toDouble().round(1)} minutes."
		  sum = sum + it.currentTemperature * weightFactor
		  count = count + weightFactor
          validSensorFound = true
       } else {
          logDebug "Ignoring ${it} ${it.currentTemperature} with age of ${minutesSinceLastReport.toDouble().round(1)} minutes exceeds ${staleTime} minute threshold."
          if (minutesSinceLastReport > 1440) {
             if (logAged == null || logAged) {
		         log.info "${it} has not received temperature updates in ${minutesSinceLastReport.toDouble().round(1)} minutes."
             }
          }
      }
	}
    def result = null
    if (validSensorFound) {
        result = (sum / count).toDouble().round(1)
        //log.debug "count: ${count} sum ${sum}"
	}
    logDebug "average temperature is: ${result}"
	return result
}

def averageTemp(numStaleMins = 0) {
    def staleMins = numStaleMins != null ? numStaleMins : 60
    def result = getAverageTemp (staleMins)
    if (!result) {
        log.debug "no valid current temperature sensors found, doubling stale time"
        result = getAverageTemp (staleMins * 2)
        if (!result) {
           log.debug "no valid current temperature sensors found, quadrupling stale time"
           result = getAverageTemp (staleMins * 4)
           if (!result) {
               log.debug "no valid current temperature sensors found, averaging all"
               result = getAverageTemp (0)
           }
        }
    }
    
    if (!result) {
      log.info "something went horribly, horribly wrong"
    }
    return result
}
        
def runningAvgTemp(samples = 1) {
    if (samples > 1) {
       state.tempHistory = state.tempHistory.drop(1) + avg
	   def sum = 0
       def count = 0
       state.tempHistory.each {
           if (it) {
             count = count + 1
             sum = sum + it
           }
       }
       if (count > 0) {
	      result = sum / count
       } else {
           result = averageTemp(staleMinutes)
       }
    } else {
       result = averageTemp(staleMinutes)
    }
    //log.debug "running average is ${result}"
    return result
}

def sensorCallback(evt) {
	def averageDev = getChildDevice("AverageTemp_${app.id}")
    if (state.lastUpdateTime == null) {
        state.lastUpdateTime = [:]
    }
    state.lastUpdateTime[evt.deviceId.toString()]=now()
	def avg = averageTemp(staleMinutes)
	if(numSamples > 1) {
      result = runningAvgTemp(numSamples)
    } else {
        result = avg
    }
	averageDev.setTemperature(result)
}

def logDebug(msg) {
	if (debugOutput) {
		log.debug msg
	}
}
