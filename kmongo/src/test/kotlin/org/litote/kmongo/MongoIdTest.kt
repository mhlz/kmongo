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

import org.bson.Document
import org.bson.types.ObjectId
import org.junit.Test
import org.litote.kmongo.model.Friend
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 *
 */
class MongoIdTest : KMongoBaseTest<Friend>() {

    class StringId(val _id: String? = null)

    class WithMongoId(@MongoId val key: ObjectId? = null)

    class WithMongoStringId(@MongoId val key: String? = null)

    class CompositeId(val _id: Key?)

    class CompositeKey(@MongoId val key: Key?)

    data class Key(val category: String, val index: Int)

    @Test
    fun testObjectIdSetByKMongo() {
        val friend = Friend("Joe")
        assertNull(friend._id)
        col.insertOne(friend)
        assertNotNull(friend._id)
        assertEquals("Joe", col.findOneById(friend._id!!)!!.name)
    }

    @Test
    fun testStringIdSetByKMongo() {
        val stringId = StringId()
        assertNull(stringId._id)
        val stringIdCol = col.withDocumentClass<StringId>()
        stringIdCol.insertOne(stringId)
        assertNotNull(stringId._id)
        assertEquals(stringId._id, stringIdCol.findOneById(stringId._id!!)!!._id)
    }

    @Test
    fun testObjectIdWithMongoIdSetByKMongo() {
        val withMongoId = WithMongoId()
        assertNull(withMongoId.key)
        val withMongoIdCol = col.withDocumentClass<WithMongoId>()
        withMongoIdCol.insertOne(withMongoId)
        assertNotNull(withMongoId.key)
        assertEquals(withMongoId.key, withMongoIdCol.findOneById(withMongoId.key!!)!!.key)
        assertFalse(col.withDocumentClass<Document>().findOneById(withMongoId.key)!!.containsKey("key"))
    }

    @Test
    fun testJsonKeyObjectIdSetByKMongo() {
        val withMongoId = WithMongoId(ObjectId())
        val withMongoIdCol = col.withDocumentClass<WithMongoId>()
        withMongoIdCol.insertOne(withMongoId.json)
        assertEquals(withMongoId.key, withMongoIdCol.findOneById(withMongoId.key!!)!!.key)
        assertFalse(col.withDocumentClass<Document>().findOneById(withMongoId.key)!!.containsKey("key"))
    }

    @Test
    fun testJsonKeyObjectIdSetByHand() {
        val withMongoId = WithMongoId(ObjectId())
        val withMongoIdCol = col.withDocumentClass<WithMongoId>()
        withMongoIdCol.insertOne("{key:${withMongoId.key!!.json}}")
        assertEquals(withMongoId.key, withMongoIdCol.findOneById(withMongoId.key)!!.key)
        assertFalse(col.withDocumentClass<Document>().findOneById(withMongoId.key)!!.containsKey("key"))
    }

    @Test
    fun testJsonKeyStringSetByKMongo() {
        val withMongoId = WithMongoStringId("keyValue")
        val withMongoIdCol = col.withDocumentClass<WithMongoStringId>()
        withMongoIdCol.insertOne(withMongoId.json)
        assertEquals(withMongoId.key, withMongoIdCol.findOneById(withMongoId.key!!)!!.key)
        assertFalse(col.withDocumentClass<Document>().findOneById(withMongoId.key)!!.containsKey("key"))
        assertTrue(col.withDocumentClass<Document>().findOneById(withMongoId.key)!!.get("_id") is String)
    }

    @Test
    fun testJsonKeyStringSetByHand() {
        val withMongoId = WithMongoStringId("keyValue")
        val withMongoIdCol = col.withDocumentClass<WithMongoStringId>()
        withMongoIdCol.insertOne("{key:${withMongoId.key!!.json}}")
        assertEquals(withMongoId.key, withMongoIdCol.findOneById(withMongoId.key)!!.key)
        assertFalse(col.withDocumentClass<Document>().findOneById(withMongoId.key)!!.containsKey("key"))
        assertTrue(col.withDocumentClass<Document>().findOneById(withMongoId.key)!!.get("_id") is String)
    }

    @Test
    fun testJsonKeyGeneration() {
        val withMongoIdCol = col.withDocumentClass<WithMongoStringId>()
        withMongoIdCol.insertOne("{}")
        assertTrue(withMongoIdCol.findOne()!!.key!! is String)
        assertTrue(col.withDocumentClass<Document>().findOne()!!.get("_id") is String)
        assertFalse(col.withDocumentClass<Document>().findOne()!!.containsKey("key"))
    }

    @Test
    fun testCompositeIdInsertion() {
        val compositeId = CompositeId(Key("alpha", 2))
        val compositeIdCol = col.withDocumentClass<CompositeId>()
        compositeIdCol.insertOne(compositeId)
        assertEquals(compositeId._id, compositeIdCol.findOneById(compositeId._id!!)!!._id)
    }

    @Test
    fun testCompositeKeyInsertion() {
        val compositeKey = CompositeKey(Key("alpha", 2))
        val compositeKeyCol = col.withDocumentClass<CompositeKey>()
        compositeKeyCol.insertOne(compositeKey)
        assertEquals(compositeKey.key, compositeKeyCol.findOneById(compositeKey.key!!)!!.key)
    }

    @Test(expected = IllegalArgumentException::class)
    fun insertionWithNullCompositeIdShouldFail() {
        val compositeId = CompositeId(null)
        val compositeIdCol = col.withDocumentClass<CompositeId>()
        compositeIdCol.insertOne(compositeId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun insertionWithNullCompositeKeyShouldFail() {
        val compositeKey = CompositeKey(null)
        val compositeKeyCol = col.withDocumentClass<CompositeKey>()
        compositeKeyCol.insertOne(compositeKey)
    }
}