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
metadata {
	definition (name: "Nanoleaf Aurora Smarter", namespace: "SteveTheGeekAH", author: "Steve The Geek") {
		capability "Light"
		capability "Switch Level"
		capability "Switch"
		capability "Color Control"
        
        command "previousScene"
        command "nextScene"
            
        attribute "scene", "String"

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

		standardTile("previousScene", "scene", width: 1, height: 2, decoration: "flat") {
			state "default", label: "<", backgroundColor: "#ffffff", action: "previousScene"
		} 

		valueTile("currentScene", "scene", width: 4, height: 2, decoration: "flat") {
			state "val", label: '${currentValue}', backgroundColor: "#ffffff"
		} 

		standardTile("nextScene", "scene", width: 1, height: 2, decoration: "flat") {
			state "default", label: ">", backgroundColor: "#ffffff", action: "nextScene"
		} 
        
		main "switch"
		details(["switch","previousScene","currentScene", "nextScene"])
	}

    preferences {
        input name: "makersKey", type: "text", title: "Makers API Key", description: "Enter The Key Associated With Your Makers Account", required: true
    }

}

def parse(String description) {
	
}

def off() {
    sendMakerEvent("aurora_off", null)
	sendEvent(name: "switch", value: "off", isStateChange: true)
} 

def on() {
    sendMakerEvent("aurora_on", null)
	sendEvent(name: "switch", value: "on", isStateChange: true)
}

def previousScene() {
  def sceneList = ["antiValentine", "candyGrams", "colorBurst", "fireplace", "forest", "hotRomance", "innerPeace", "mellowSunrise", "nemo", "northernLights", "romantic", "rosesAreRed", "snowfall", "sunset", "vibrantSunrise", "violetsAreBlue"]
  def currentSelectedScene = device.currentValue("scene");
  def index = sceneList.indexOf(currentSelectedScene)
  
  index--
  
  if(index == -1) {
     index = sceneList.size -1
  }
  changeScene(sceneList[index])
}

def nextScene() {
  def sceneList = ["antiValentine", "candyGrams", "colorBurst", "fireplace", "forest", "hotRomance", "innerPeace", "mellowSunrise", "nemo", "northernLights", "romantic", "rosesAreRed", "snowfall", "sunset", "vibrantSunrise", "violetsAreBlue"]
  def currentSelectedScene = device.currentValue("scene");
  def index = sceneList.indexOf(currentSelectedScene)
  
  index++
  
  if(index == sceneList.size) {
     index = 0
  }
  changeScene(sceneList[index])
}

def changeScene(String scene) {
   sendMakerEvent("aurora_scene_${scene}", null)
   sendEvent(name: "scene", value: scene, isStateChange: true)
}

def setLevel(Integer value) {
   sendMakerEvent("aurora_brightness", "${value}")
   sendEvent(name: "level", value: value, isStateChange: true)
}

void setColor(value) {
   def hex = value.hex.substring(1)
   sendMakerEvent("aurora_rgb", "${hex}")
   sendEvent(name: "brightness", value: 100, isStateChange: true)
   sendEvent(name: "color", value: value.hex, isStateChange: true)
}


def sendMakerEvent(String event, String value1) {

    log.debug("event: ${event}");
    log.debug("value1: ${value1}");  

    def path ="/trigger/${event}/with/key/${makersKey}"
    log.debug(path);
	def params

    if(value1) {
        params = [
            uri: "https://maker.ifttt.com",
            path: path,
            query: [value1:value1]
        ]
    }
    else {
        params = [
            uri: "https://maker.ifttt.com",
            path: path
        ]
    }

	try {
        httpGet(params) { resp ->
            def data = resp.data
            //log.debug "response data: ${resp.data.count}"
        }
    } catch (e) {
        log.error "something went wrong: $e"
    }
}