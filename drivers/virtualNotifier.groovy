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
*
*/
def version() {"v1.1"}

metadata {
  	definition (name: "Virtual Notifier", namespace: "darwinsden", author: "Darwin") {
    	capability "Notification"
  	}
	
	attribute "lastMessage", "string"
 }

preferences {
	input("isDebugEnabled", "bool", title: "Enable debug logging?", defaultValue: false, required: false)
}

def installed() {
    initialize()
}

def updated() {
 	initialize()
}

def initialize() {
    state.version = version()
}

def deviceNotification(message) {
    logDebug "Message Received: ${message}"
    sendEvent(name: "lastMessage", value: "${message}",isStateChange: true)
}

private logDebug(msg) {
	if (isDebugEnabled) {
		log.debug "$msg"
	}
}
