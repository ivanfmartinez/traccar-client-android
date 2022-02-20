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

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket

import android.util.Log

import com.github.pires.obd.commands.ObdCommand
import com.github.pires.obd.commands.control.VinCommand
import com.github.pires.obd.commands.protocol.AdaptiveTimingCommand
import com.github.pires.obd.commands.protocol.EchoOffCommand
import com.github.pires.obd.commands.protocol.LineFeedOffCommand
import com.github.pires.obd.commands.protocol.TimeoutCommand
import com.github.pires.obd.commands.protocol.ObdProtocolCommand
import com.github.pires.obd.commands.protocol.ObdRawCommand
import com.github.pires.obd.commands.protocol.ObdResetCommand
import com.github.pires.obd.commands.protocol.SelectProtocolCommand
import com.github.pires.obd.enums.ObdProtocols
import com.github.pires.obd.exceptions.NoDataException
import com.github.pires.obd.exceptions.NonNumericResponseException
import com.github.pires.obd.exceptions.ResponseException
import com.github.pires.obd.exceptions.StoppedException
import com.github.pires.obd.exceptions.UnableToConnectException


import java.io.IOException
import java.net.SocketException
import java.util.UUID



class ObdConnection {

   val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
   val defaultHeader = "7DF"
 
   var socket: BluetoothSocket? = null
   var nextConnect: Long = 0
   var obdDevice: BluetoothDevice? = null
   var connected = false
   var vin : String = ""
   var lastHeader: String = ""

   fun setDevice(device : BluetoothDevice) {
       disconnect()
       obdDevice = device;
       nextConnect = 0;
   }

   fun connect() {
       if (!connected) {
           var lastError : Exception? = null
           // try more than one time to connect
           for (retry in 1..3) {
               try {
                   socket = obdDevice!!.createInsecureRfcommSocketToServiceRecord(uuid)
                   socket!!.connect()

                   ObdController.message("obd connected " + retry + " " + getName(), true)

                   connected = true
                   lastHeader = "" + System.currentTimeMillis()
                   vin = ""

                   //TODO better init logic sometimes it does not work
		   for (cfgRetry in 1..3) {
                       run(ObdResetCommand())
                       run(EchoOffCommand())
                       run(LineFeedOffCommand())
                   }
                   run( SelectProtocolCommand(ObdProtocols.AUTO) )
                   run(TimeoutCommand(125))
                   run(AdaptiveTimingCommand(1))

                   var vinCmd = VinCommand()
                   if (run(vinCmd)) {
                       vin = vinCmd.getResult()
                   }

		   break
               } catch (e: IOException) {
                   lastError = e
               }
           }
           if (lastError != null) {
               error(lastError);
           }
       }
      
   }

   fun getName() : String {
       return obdDevice!!.name.toString() + " " + obdDevice!!.address
   }

   private fun error(e : Exception) {
       nextConnect = System.currentTimeMillis() + (600 * 1000)
       ObdController.errorMessage("obd error", e)
       disconnect()
   }

   fun disconnect() {
       if (connected) {
           socket!!.close()
       }
       connected = false
   }

   fun canConnect() : Boolean {
       return (System.currentTimeMillis() > nextConnect);
   }

   fun isConnected() : Boolean {
       return connected;
   }


   private fun setHeader(h : String) {
       if (!lastHeader.equals(h)) {
           if (h.length > 0) {
               lastHeader = h
               run(ObdRawCommand("AT SH " + h))
           } else {
               lastHeader = ""
               run(ObdRawCommand("AT SH " + defaultHeader))
           }           
       }
   }

   private fun formatCommand(cmd : ObdCommand) : String {
       return cmd.toString() + "|" + cmd.getCalculatedResult() + "|" + cmd.getFormattedResult() + "|" + cmd.getResult() + "|"
   }

   fun run(cmd : ObdCommand) : Boolean {
       try {
           // TODO cache vin and return cached value
           if (connected) {
               val input = socket!!.getInputStream()

               // remove any pending data on the connection
               while (input.available() > 0) {
                   input.read();
               }

               if (cmd is ObdHeaderCommand) {
                   setHeader(cmd.header)
               } else if (cmd !is ObdProtocolCommand) {
                   setHeader("")
               }
               cmd.run(input, socket!!.getOutputStream())
               ObdController.message(formatCommand(cmd), false)
               return true
           }
       } catch (e: NoDataException) {
           ObdController.message("Nodata for command : " + formatCommand(cmd), false);
       } catch (e: NonNumericResponseException) {
           ObdController.errorMessage("NonNumericResponse for command : " + formatCommand(cmd), e);
       } catch (e: NumberFormatException) {
           ObdController.errorMessage("NumberFormatException  for command : " + formatCommand(cmd), e);
       } catch (e: ResponseException) {
           ObdController.errorMessage("ResponseException " + e + " for command : " + formatCommand(cmd), e);
       } catch (e: StoppedException) {
           ObdController.errorMessage("StoppedException for command : " + formatCommand(cmd), e)
       } catch (e: UnableToConnectException) {
           ObdController.errorMessage("UnableToConnectException for command : " + formatCommand(cmd), e)
       } catch (e: SocketException) {
           error(e)
       }
       return false

   }

}
