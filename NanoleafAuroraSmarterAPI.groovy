/**
 *  Copyright 2015 SmartThings
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
 
import groovy.json.JsonSlurper

metadata {
	definition (name: "Nanoleaf Aurora Smarter API", namespace: "SteveTheGeekAH", author: "Steve The Geek") {
		capability "Light"
		capability "Switch Level"
		capability "Switch"
		capability "Color Control"
        capability "Polling"
        capability "Refresh"
        
        command "previousScene"
        command "nextScene"
            
        attribute "scene", "String"
        attribute "scenesList", "String"
	}

	simulator {

	}
    
	tiles {

		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, decoration: "flat", canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label: 'on', action: "off", icon: "http://stevethegeek.net/smartthings/aurora/aurora-on.png", backgroundColor: "#79b821"
				attributeState "off", label: 'off', action: "on", icon: "http://stevethegeek.net/smartthings/aurora/aurora-off.png", backgroundColor: "#ffffff"
			}
            tileAttribute ("level", key: "SLIDER_CONTROL", range:"(1..100)") {
                attributeState "level", action:"setLevel"
            }
            tileAttribute ("color", key: "COLOR_CONTROL") {
                attributeState "color", action:"setColor"
            }
    }

		standardTile("previousScene", "scene", width: 1, height: 1, decoration: "flat") {
			state "default", label: "", backgroundColor: "#ffffff", action: "previousScene", icon: "http://stevethegeek.net/smartthings/aurora/aurora-left.png"
		} 

		valueTile("currentScene", "scene", width: 4, height: 1, decoration: "flat") {
			state "val", label: '${currentValue}', backgroundColor: "#ffffff"
		} 

		standardTile("nextScene", "scene", width: 1, height: 1, decoration: "flat") {
			state "default", label: "", backgroundColor: "#ffffff", action: "nextScene", icon: "http://stevethegeek.net/smartthings/aurora/aurora-right.png"
		} 
        
        standardTile("refresh", "device.switch", decoration: "flat", width: 1, height: 1) {
            state "default", action:"refresh", icon:"st.secondary.refresh"
        } 
        
        
		main "switch"
		details(["switch","previousScene","currentScene", "nextScene", "refresh"])
	}

    preferences {
        input name: "apiKey", type: "text", title: "Aurora API Key", description: "Enter The Key Returned By The Api Authentication Method", required: true
    }

}

def parse(String description) {
    def message = parseLanMessage(description)
    if(message.json) {
      def auroraOn = message.json.state.on.value
      
      if(auroraOn && device.currentValue("switch") == "off") {
         log.debug("Aurora has been switched on outside of Smartthings")
      	 sendEvent(name: "switch", value: "on", isStateChange: true)
      }
      if(!auroraOn && device.currentValue("switch") == "on") {
         log.debug("Aurora has been switched off outside of Smartthings")
      	 sendEvent(name: "switch", value: "off", isStateChange: true)
      }
      
      def currentScene = message.json.effects.select
      if(currentScene != device.currentValue("scene")) {
         log.debug("Scene was changed outside of Smartthings")
         sendEvent(name: "scene", value: currentScene, isStateChange: true)
      }

      def currentBrightness = message.json.state.brightness.value
      def deviceBrightness = "${device.currentValue("level")}"
      if(currentBrightness != device.currentValue("level")) {
         log.debug("Brightness was changed outside of Smartthings")
         sendEvent(name: "level", value: currentBrightness, isStateChange: true)
      }
      
      def effectsList = message.json.effects.list
            
      if(effectsList.toString() != device.currentValue("scenesList").toString()) {
         log.debug("List of effects was changed in the Aurora App")
         sendEvent(name: "scenesList", value: effectsList, isStateChange: true)
      }

    } else {
      log.debug("Response from PUT, do nothing")
    }
}

def poll() {
    log.debug("polled")
    refresh()
}

def refresh() {
	return createGetRequest("");
}

def off() {
	sendEvent(name: "switch", value: "off", isStateChange: true)
	return createPutRequest("state", "{\"on\" : false}")
} 

def on() {
	sendEvent(name: "switch", value: "on", isStateChange: true)
	return createPutRequest("state", "{\"on\" : true}")
}

def previousScene() {
  def sceneListString = device.currentValue("scenesList").replaceAll(", ", ",")
  def sceneList = sceneListString.substring(1, sceneListString.length()-1).tokenize(',')
  def currentSelectedScene = device.currentValue("scene");
  def index = sceneList.indexOf(currentSelectedScene)
  
  log.debug(index)
  
  if(index == -1) {
    index = 1;
  }

  index--
  
  if(index == -1) {
     index = sceneList.size -1
  }
  changeScene(sceneList[index])
}

def nextScene() {
  def sceneListString = device.currentValue("scenesList").replaceAll(", ", ",")
  def sceneList = sceneListString.substring(1, sceneListString.length()-1).tokenize(',')
  def currentSelectedScene = device.currentValue("scene");
  def index = sceneList.indexOf(currentSelectedScene)
  
  index++
  
  if(index == sceneList.size) {
     index = 0
  }
  changeScene(sceneList[index])
}

def changeScene(String scene) {
    sendEvent(name: "scene", value: scene, isStateChange: true)
    return createPutRequest("effects", "{\"select\" : \"${scene}\"}")
}

def setLevel(Integer value) {
    sendEvent(name: "level", value: value, isStateChange: true)
	return createPutRequest("state", "{\"brightness\" : ${value}}")
}

def setColor(value) {
    sendEvent(name: "scene", value: "--", isStateChange: true)
    sendEvent(name: "color", value: value.hex, isStateChange: true)
    return createPutRequest("state", "{\"hue\" : ${(value.hue*360/100).toInteger()}, \"sat\" : ${value.saturation.toInteger()}}")
}

// gets the address of the hub
private getCallBackAddress() {
    return device.hub.getDataValue("localIP") + ":" + device.hub.getDataValue("localSrvPortTCP")
}

private createPutRequest(String url, String body) {

	log.debug("/api/beta/${apiKey}/${url}")
    log.debug("body : ${body}")
    
    def result = new physicalgraph.device.HubAction(
            method: "PUT",
            path: "/api/beta/${apiKey}/${url}",
            body: body,
            headers: [
                HOST: getHostAddress()
            ]
        )

        return result;
}

private createGetRequest(String url) {

	//log.debug("/api/beta/${apiKey}/${url}")
    
    def result = new physicalgraph.device.HubAction(
            method: "GET",
            path: "/api/beta/${apiKey}/${url}",
            headers: [
                HOST: getHostAddress()
            ]
        )

        return result;
}

// gets the address of the device
private getHostAddress() {
    def ip = getDataValue("ip")
    def port = getDataValue("port")

    if (!ip || !port) {
        def parts = device.deviceNetworkId.split(":")
        if (parts.length == 2) {
            ip = parts[0]
            port = parts[1]
        } else {
            log.warn "Can't figure out ip and port for device: ${device.id}"
        }
    }

    //log.debug "Using IP: $ip and port: $port for device: ${device.id}"
    return convertHexToIP(ip) + ":" + convertHexToInt(port)
}

private Integer convertHexToInt(hex) {
    return Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
    return [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

