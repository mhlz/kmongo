/*
 * Copyright (C) 2016 Litote
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.litote.kmongo

import de.flapdoodle.embed.mongo.MongodProcess
import de.flapdoodle.embed.mongo.MongodStarter
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder
import de.flapdoodle.embed.mongo.config.Net
import de.flapdoodle.embed.mongo.distribution.Version.Main.PRODUCTION
import de.flapdoodle.embed.process.runtime.Network

/**
 *
 */
object EmbeddedMongo {

    val mongodProcess: MongodProcess by lazy {
        createInstance()
    }

    private fun createInstance(): MongodProcess {
        val port = Network.getFreeServerPort()

        val mongodConfig = MongodConfigBuilder()
                .version(PRODUCTION)
                .net(Net(port, Network.localhostIsIPv6()))
                .build()

        return MongodStarter.getDefaultInstance().prepare(mongodConfig).start()
    }
}