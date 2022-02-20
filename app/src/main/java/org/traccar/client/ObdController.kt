/*
 * Copyright 2015 - 2022 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.client

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.util.Log

import com.github.pires.obd.commands.ObdCommand
import com.github.pires.obd.commands.control.ModuleVoltageCommand
import com.github.pires.obd.commands.engine.RPMCommand
import com.github.pires.obd.commands.engine.ThrottlePositionCommand
import com.github.pires.obd.commands.protocol.ObdRawCommand
import com.github.pires.obd.commands.SpeedCommand

import java.io.IOException

class ObdController(private val context: Context) {

   val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
   val connection : ObdConnection = ObdConnection()

   var enabled = false

   fun init() {
       if (bluetoothAdapter != null) {
           if (bluetoothAdapter?.isEnabled == false) {
               val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//               context.startActivityForResult(enableBtIntent, RC_ENABLE_BLUETOOTH)
               Log.i("traccar", "Bluetooth disabled")
           }
           val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
           var obdDevice : BluetoothDevice? = null
           pairedDevices?.forEach { device ->
               val deviceName = device.name.lowercase()

               val dAlias = device.alias?.lowercase()
               val deviceAlias = if (dAlias != null) dAlias else deviceName

               val deviceHardwareAddress = device.address // MAC address
               message("obd device |" + deviceName + "|" + deviceAlias + "|" + deviceHardwareAddress, true);
               if (deviceAlias.contains("traccar")) {
                    enabled = true
                    connection.setDevice(device)
               }
           }
           if (!enabled) {
               message("No obd device found", true)
           }
       } else {
           Log.i("traccar", "BluetoothAdapter not available")
       }
   }

   // 8=P, 7=R, 6=N, 3=D, 1=L
   val boltGearMap = mapOf("8" to "P", "7" to "R", "6" to "N", "3" to "D","1" to "L")
   val emptyMap = mapOf("" to "")

   fun getValue(position : Position, name : String, cmd : ObdCommand, map : Map<String,String>, replace : Boolean) : Boolean {

      if (position.extras.has(name) && !replace) {
          return true
      }

      try {
          if (connection.isConnected()) {
              if (connection.run(cmd)) {
                  val result = cmd.calculatedResult
                  if (result.length > 0) {
                      position.extras.put(name, map.getOrDefault(result, result))
                      return true
                  }
              }
          }
      } catch (e: IOException) {
          Log.e("traccar", "Error on command " + name + " " + cmd, e)
      }
      return false
   }



   fun getExtras(position : Position) {
        if (enabled && connection.canConnect()) {
            connection.connect();
            if (connection.isConnected()) {

                position.extras.put("obdDevice", connection.getName())

                if (connection.vin.length > 0) {
                    position.extras.put("vin", connection.vin);
                }
                getValue(position, "obdBattery", ModuleVoltageCommand(), emptyMap, true)
                getValue(position, "obdSpeed", SpeedCommand(), emptyMap, true)
                getValue(position, "throttle", ThrottlePositionCommand(), emptyMap, true)
                getValue(position, "rpm", RPMCommand(), emptyMap, true)

                // Odometer gives values in hundred meters, and traccar
                // needs meters
                getValue(position, "odometer", ObdCalculatedCommand("", "01A6", "INT32(A:B:C:D)", 100.0), emptyMap, true)

                // Bolt Battery information (percent)
                getValue(position, "evBattery", ObdCalculatedCommand("7E4", "228334", "PCT(A)", 1.0), emptyMap, true)
                // Generic EV Battery information
                getValue(position, "evBattery", ObdCalculatedCommand("", "019A", "INT32(A:B:C:D)", 1.0), emptyMap, false)

                getValue(position, "gear", ObdCalculatedCommand("7E1", "222889", "A", 1.0), boltGearMap, true)
                  
            }
        }
   }

   companion object {
       fun message(st : String, toStatus : Boolean) {
           Log.i("traccar", "obd : " + st)
           if (toStatus) {
               StatusActivity.addMessage("obd : " + st)
           }
       } 

       fun errorMessage(st : String, e : Exception) {
           Log.e("traccar", "obd : " + st, e)
           StatusActivity.addMessage("obd error : " + st + " " + e.message)
       } 
   }
}