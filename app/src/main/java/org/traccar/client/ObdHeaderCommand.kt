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

open class ObdHeaderCommand : ObdCommand {

    val header: String;

    constructor(header: String, command: String) : super(command) {
        this.header = header
    }

    override fun performCalculations() {
    }

    override fun getFormattedResult() : String {
        return getResult()
    }


    override fun getCalculatedResult() : String {
        return getResult()
    }


    override fun getName() : String {
        //TODO check for kotlin native way
        return (this as java.lang.Object).getClass().getName() + " " + header + " " + cmd
    }

}
