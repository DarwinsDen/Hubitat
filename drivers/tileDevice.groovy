/**
 *  Tile Device
 *
 *  Copyright 2021 DarwinsDen.com
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
 */

metadata {
    definition(name: "Tile Device", namespace: "darwinsden", author: "eric@darwinsden.com") {
        capability "Refresh"
        attribute "tile", "text"
    }
}

def installed() {
    log.debug "Installed"
    initialize()
}

def updated() {
}

def initialize() {
}


def refresh() {
    def status = parent.refresh(this)
}
