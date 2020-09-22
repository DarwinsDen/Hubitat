/**
 *  Battery Status
 *
 *  Copyright 2020 DarwinsDen.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *	02-Jul-2020 >>> v0.1.0e.20200802 - Initial
 *
 */

metadata {
    definition(name: "Battery Status Monitor", namespace: "darwinsden", author: "darwin@darwinsden.com") {
        capability "Battery"
        capability "Polling"
        capability "Sensor"
        capability "Health Check"
        capability "Refresh"

        attribute "batteryTile", "string"
    }

    preferences {}

    simulator {}

    tiles(scale: 2) {
        valueTile("battery", "device.batteryPercent", width: 2, height: 2) {
            state("battery", label: ': Battery :\n ${currentValue}% \n', unit: "%",)
        }
 
        main(["battery"])
        details(["battery"
        ])
    }
}

def installed() {
    log.debug "Installed"
    initialize()
}

def updated() {
    log.debug "Updated"
    initialize()
}

def refresh() {
    //log.debug "Executing refresh"
    def status = parent.refresh(this)
}

def poll() {
  logDebug "poll()"
  def status = parent.refresh(this)
}


def initialize() {
    log.debug "initializing battery monitor device"
}

def ping() {
	log.debug "pinged"	
}

def parse(String description) {
    log.debug "${description}"
}
