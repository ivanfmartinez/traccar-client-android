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

import com.github.pires.obd.commands.ObdCommand

class ObdCalculatedCommand : ObdHeaderCommand {

    val formula: String
    val scale : Double

    var skipBytes: Int = 0
    var value : Double = 0.0


    constructor(header: String, command: String, formula: String, scale : Double) : super(header,command) {
        this.formula = formula
        this.scale = scale
        skipBytes = if (command.startsWith("22")) 2 else 1
    }


    override fun performCalculations() {
//        ObdController.message(formula + " skip=" + skipBytes + " cmd=" + cmd + " " + getResult(), true)
          value = when (formula) {
              //TODO better support, dynamic calculation
              "A" -> scale * buffer.get(skipBytes + 1) 
              "INT32(A:B:C:D)" -> scale *(buffer.get(skipBytes + 1) shl 24) + (buffer.get(skipBytes + 2) shl 16) + 
                                  (buffer.get(skipBytes + 3) shl 8) + buffer.get(skipBytes + 4)
              "PCT(A)" -> scale * ((buffer.get(skipBytes + 1) * 100.0) / 255)
              else -> 0.0
          }
    }


    override fun getFormattedResult() : String {
        return ""+value
    }


    override fun getCalculatedResult() : String {
        return ""+value
    }

}
