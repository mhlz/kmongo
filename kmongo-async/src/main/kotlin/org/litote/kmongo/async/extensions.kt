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
package org.litote.kmongo.async

import com.mongodb.ReadPreference
import com.mongodb.async.client.AggregateIterable
import com.mongodb.async.client.DistinctIterable
import com.mongodb.async.client.FindIterable
import com.mongodb.async.client.ListIndexesIterable
import com.mongodb.async.client.MapReduceIterable
import com.mongodb.async.client.MongoCollection
import com.mongodb.async.client.MongoDatabase
import com.mongodb.async.client.MongoIterable
import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.model.BulkWriteOptions
import com.mongodb.client.model.CountOptions
import com.mongodb.client.model.FindOneAndDeleteOptions
import com.mongodb.client.model.FindOneAndReplaceOptions
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.IndexModel
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.InsertOneOptions
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import org.bson.BsonDocument
import org.bson.conversions.Bson
import org.litote.kmongo.util.KMongoUtil
import org.litote.kmongo.util.KMongoUtil.EMPTY_JSON
import org.litote.kmongo.util.KMongoUtil.defaultCollectionName
import org.litote.kmongo.util.KMongoUtil.extractId
import org.litote.kmongo.util.KMongoUtil.idFilterQuery
import org.litote.kmongo.util.KMongoUtil.setModifier
import org.litote.kmongo.util.KMongoUtil.toBson
import org.litote.kmongo.util.KMongoUtil.toBsonList
import org.litote.kmongo.util.KMongoUtil.toWriteModel


//*******
//MongoDatabase extension methods
//*******

/**
 * Gets a collection.
 *
 * @param collectionName the name of the collection to return
 * @param <T>            the default target type of the collection to return
 * @return the collection
 */
inline fun <reified T : Any> MongoDatabase.getCollection(collectionName: String): MongoCollection<T>
        = getCollection(collectionName, T::class.java)

/**
 * Gets a collection.
 *
 * @param <T>            the default target type of the collection to return
 *                       - the name of the collection is determined by [defaultCollectionName]
 * @return the collection
 * @see defaultCollectionName
 */
inline fun <reified T : Any> MongoDatabase.getCollection(): MongoCollection<T>
        = getCollection(defaultCollectionName(T::class), T::class.java)

/**
 * Executes the given command in the context of the current database with the given read preference.
 *
 * @param command        the command to be run
 * @param readPreference the {@link com.mongodb.ReadPreference} to be used when executing the command
 * @param <TResult>      the type of the class to use instead of {@code Document}.
 * @param callback       the callback that is passed the command result
 */
inline fun <reified TResult : Any> MongoDatabase.runCommand(command: String, readPreference: ReadPreference, noinline callback: (TResult?, Throwable?) -> Unit)
        = runCommand(toBson(command), readPreference, TResult::class.java, callback)

/**
 * Executes the given command in the context of the current database with the given read preference.
 *
 * @param command        the command to be run
 * @param <TResult>      the type of the class to use instead of {@code Document}.
 * @param callback       the callback that is passed the command result
 */
inline fun <reified TResult : Any> MongoDatabase.runCommand(command: String, noinline callback: (TResult?, Throwable?) -> Unit)
        = runCommand(command, readPreference, callback)

//*******
//MongoCollection extension methods
//*******

/**
 * Create a new MongoCollection instance with a different default class to cast any documents returned from the database into..
 *
 * @param <NewTDocument> the default class to cast any documents returned from the database into.
 * @return a new MongoCollection instance with the different default class
 */
inline fun <reified NewTDocument : Any> MongoCollection<*>.withDocumentClass(): MongoCollection<NewTDocument>
        = withDocumentClass(NewTDocument::class.java)

/**
 * Counts the number of documents in the collection according to the given options.
 *
 * @param filter   the query filter
 * @param callback the callback passed the number of documents in the collection
 */
fun <T> MongoCollection<T>.count(filter: String, callback: (Long?, Throwable?) -> Unit)
        = count(filter, CountOptions(), callback)

/**
 * Counts the number of documents in the collection according to the given options.
 *
 * @param filter   the query filter
 * @param callback the callback passed the number of documents in the collection
 */
fun <T> MongoCollection<T>.count(filter: String, options: CountOptions, callback: (Long?, Throwable?) -> Unit)
        = count(toBson(filter), options, callback)


/**
 * Gets the distinct values of the specified field name.
 *
 * @param fieldName   the field name
 * @param <TResult>   the target type of the iterable
 * @return an iterable of distinct values
 */
inline fun <reified TResult : Any> MongoCollection<*>.distinct(fieldName: String): DistinctIterable<TResult>
        = distinct(fieldName, EMPTY_JSON)

/**
 * Gets the distinct values of the specified field name.
 *
 * @param fieldName   the field name
 * @param filter      the query filter
 * @param <TResult>   the target type of the iterable
 * @return an iterable of distinct values
 */
inline fun <reified TResult : Any> MongoCollection<*>.distinct(fieldName: String, filter: String): DistinctIterable<TResult>
        = distinct(fieldName, toBson(filter), TResult::class.java)

/**
 * Finds all documents that match the filter in the collection.
 *
 * @param  filter the query filter
 * @return the find iterable interface
 */
fun <T> MongoCollection<T>.find(filter: String): FindIterable<T>
        = find(toBson(filter))

/**
 * Finds the first document that match the filter in the collection.
 *
 * @param filter the query filter
 * @param callback a callback that is passed the first item or null
 */
fun <T> MongoCollection<T>.findOne(filter: String = EMPTY_JSON, callback: (T?, Throwable?) -> Unit)
        = find(filter).first(callback)

/**
 * Finds the first document that match the filter in the collection.
 *
 * @param filter the query filter
 * @param callback a callback that is passed the first item or null
 */
fun <T> MongoCollection<T>.findOne(filter: Bson, callback: (T?, Throwable?) -> Unit)
        = find(filter).first(callback)

/**
 * Finds the document that match the [ObjectId] parameter.
 *
 * @param id       the object id
 * @param callback a callback that is passed the first item or null
 */
fun <T> MongoCollection<T>.findOneById(id: Any, callback: (T?, Throwable?) -> Unit)
        = findOne(idFilterQuery(id), callback)

/**
 * Aggregates documents according to the specified aggregation pipeline.  If the pipeline ends with a $out stage, the returned
 * iterable will be a query of the collection that the aggregation was written to.  Note that in this case the pipeline will be
 * executed even if the iterable is never iterated.
 *
 * @param pipeline    the aggregate pipeline
 * @param <TResult>   the target document type of the iterable
 * @return an iterable containing the result of the aggregation operation
 */
inline fun <reified TResult : Any> MongoCollection<*>.aggregate(vararg pipeline: String): AggregateIterable<TResult>
        = aggregate(toBsonList(pipeline, codecRegistry), TResult::class.java)

/**
 * Aggregates documents according to the specified aggregation pipeline.  If the pipeline ends with a $out stage, the returned
 * iterable will be a query of the collection that the aggregation was written to.  Note that in this case the pipeline will be
 * executed even if the iterable is never iterated.
 *
 * @param pipeline    the aggregate pipeline
 * @param <TResult>   the target document type of the iterable
 * @return an iterable containing the result of the aggregation operation
 */
inline fun <reified TResult : Any> MongoCollection<*>.aggregate(vararg pipeline: Bson): AggregateIterable<TResult>
        = aggregate(pipeline.toList(), TResult::class.java)

/**
 * Aggregates documents according to the specified map-reduce function.
 *
 * @param mapFunction    a JavaScript function that associates or "maps" a value with a key and emits the key and value pair.
 * @param reduceFunction a JavaScript function that "reduces" to a single object all the values associated with a particular key.
 * @param <TResult>      the target document type of the iterable.
 * *
 * @return an iterable containing the result of the map-reduce operation
 */
inline fun <reified TResult : Any> MongoCollection<*>.mapReduce(mapFunction: String, reduceFunction: String): MapReduceIterable<TResult>
        = mapReduce(mapFunction, reduceFunction, TResult::class.java)


/**
 * Inserts the provided document. If the document is missing an identifier, the driver should generate one.
 *
 * @param document the document to insert
 * @param callback the callback that is completed once the insert has completed
 *
 * @throws com.mongodb.MongoWriteException        returned via the callback
 * @throws com.mongodb.MongoWriteConcernException returned via the callback
 * @throws com.mongodb.MongoCommandException      returned via the callback
 * @throws com.mongodb.MongoException             returned via the callback
 */
inline fun <reified T : Any> MongoCollection<T>.insertOne(document: String, noinline callback: (Void?, Throwable?) -> Unit)
        = insertOne(document, InsertOneOptions(), callback)

/**
 * Inserts the provided document. If the document is missing an identifier, the driver should generate one.
 *
 * @param document the document to insert
 * @param options  the options to apply to the operation
 * @param callback the callback that is completed once the insert has completed
 *
 * @throws com.mongodb.MongoWriteException        returned via the callback
 * @throws com.mongodb.MongoWriteConcernException returned via the callback
 * @throws com.mongodb.MongoCommandException      returned via the callback
 * @throws com.mongodb.MongoException             returned via the callback
 */
inline fun <reified T : Any> MongoCollection<T>.insertOne(document: String, options: InsertOneOptions, noinline callback: (Void?, Throwable?) -> Unit)
        = withDocumentClass<BsonDocument>().insertOne(toBson(document, T::class), options, callback)


/**
 * Removes at most one document from the collection that matches the given filter.  If no documents match, the collection is not
 * modified.
 *
 * @param filter   the query filter to apply the the delete operation
 * @param callback the callback passed the result of the remove one operation
 *
 * @throws com.mongodb.MongoWriteException        returned via the callback
 * @throws com.mongodb.MongoWriteConcernException returned via the callback
 * @throws com.mongodb.MongoException             returned via the callback
 */
fun <T> MongoCollection<T>.deleteOne(filter: String, callback: (DeleteResult?, Throwable?) -> Unit)
        = deleteOne(toBson(filter), callback)

/**
 * Removes at most one document from the [ObjectId] parameter.  If no documents match, the collection is not
 * modified.
 *
 * @param id   the object id
 * @param callback the callback passed the result of the remove one operation
 *
 * @throws com.mongodb.MongoWriteException        returned via the callback
 * @throws com.mongodb.MongoWriteConcernException returned via the callback
 * @throws com.mongodb.MongoException             returned via the callback
 */
fun <T> MongoCollection<T>.deleteOneById(id: Any, callback: (DeleteResult?, Throwable?) -> Unit)
        = deleteOne(idFilterQuery(id), callback)

/**
 * Removes all documents from the collection that match the given query filter.  If no documents match, the collection is not modified.
 *
 * @param filter   the query filter to apply the the delete operation
 * @param callback the callback passed the result of the remove many operation
 *
 * @throws com.mongodb.MongoWriteException        returned via the callback
 * @throws com.mongodb.MongoWriteConcernException returned via the callback
 * @throws com.mongodb.MongoException             returned via the callback
 */
fun <T> MongoCollection<T>.deleteMany(filter: String, callback: (DeleteResult?, Throwable?) -> Unit)
        = deleteMany(toBson(filter), callback)

/**
 * Save the document.
 * If the document has no id field, or if the document has a null id value, insert the document.
 * Otherwise, call [replaceOneById] with upsert true.
 *
 * @param document the document to save
 * @param callback the callback passed the result of the save operation
 *
 * @throws com.mongodb.MongoWriteException        if the write failed due some other failure specific to the update command
 * @throws com.mongodb.MongoWriteConcernException if the write failed due being unable to fulfil the write concern
 * @throws com.mongodb.MongoException             if the write failed due some other failure
 */
fun <T : Any> MongoCollection<T>.save(document: T, callback: (Void?, Throwable?) -> Unit) {
    val id = KMongoUtil.getIdValue(document)
    if (id != null) {
        replaceOneById(id, document, UpdateOptions().upsert(true), { r, t -> callback.invoke(null, t) })
    } else {
        insertOne(document, callback)
    }
}

/**
 * Replace a document in the collection according to the specified arguments.
 *
 * @param id          the object id
 * @param replacement the replacement document
 * @param callback    the callback passed the result of the replace one operation
 *
 * @throws com.mongodb.MongoWriteException        returned via the callback
 * @throws com.mongodb.MongoWriteConcernException returned via the callback
 * @throws com.mongodb.MongoException             returned via the callback
 */
fun <T : Any> MongoCollection<T>.replaceOneById(id: Any, replacement: T, callback: (UpdateResult?, Throwable?) -> Unit)
        = replaceOneById(id, replacement, UpdateOptions(), callback)

/**
 * Replace a document in the collection according to the specified arguments.
 *
 * @param id          the object id
 * @param replacement the replacement document
 * @param options     the options to apply to the replace operation
 * @param callback    the callback passed the result of the replace one operation
 *
 * @throws com.mongodb.MongoWriteException        returned via the callback
 * @throws com.mongodb.MongoWriteConcernException returned via the callback
 * @throws com.mongodb.MongoException             returned via the callback
 */
fun <T : Any> MongoCollection<T>.replaceOneById(id: Any, replacement: T, options: UpdateOptions, callback: (UpdateResult?, Throwable?) -> Unit)
        = replaceOne(idFilterQuery(id), replacement, options, callback)

/**
 * Replace a document in the collection according to the specified arguments.
 *
 * @param replacement the document to replace - must have an non null id
 * @param callback    the callback passed the result of the replace one operation
 *
 * @throws com.mongodb.MongoWriteException        returned via the callback
 * @throws com.mongodb.MongoWriteConcernException returned via the callback
 * @throws com.mongodb.MongoException             returned via the callback
 */
inline fun <reified T : Any> MongoCollection<T>.replaceOne(replacement: T, noinline callback: (UpdateResult?, Throwable?) -> Unit)
        = replaceOneById(extractId(replacement, T::class), replacement, callback)

/**
 * Replace a document in the collection according to the specified arguments.
 *
 * @param filter      the query filter to apply to the replace operation
 * @param replacement the replacement document
 * @param options     the options to apply to the replace operation
 * @param callback    the callback passed the result of the replace one operation
 *
 * @throws com.mongodb.MongoWriteException        returned via the callback
 * @throws com.mongodb.MongoWriteConcernException returned via the callback
 * @throws com.mongodb.MongoException             returned via the callback
 */
fun <T : Any> MongoCollection<T>.replaceOne(filter: String, replacement: T, options: UpdateOptions, callback: (UpdateResult?, Throwable?) -> Unit)
        = withDocumentClass<BsonDocument>().replaceOne(toBson(filter), KMongoUtil.filterIdToBson(replacement), options, callback)

/**
 * Replace a document in the collection according to the specified arguments.
 *
 * @param filter      the query filter to apply to the replace operation
 * @param replacement the replacement document
 * @param callback    the callback passed the result of the replace one operation
 *
 * @throws com.mongodb.MongoWriteException        returned via the callback
 * @throws com.mongodb.MongoWriteConcernException returned via the callback
 * @throws com.mongodb.MongoException             returned via the callback
 */
fun <T : Any> MongoCollection<T>.replaceOne(filter: String, replacement: T, callback: (UpdateResult?, Throwable?) -> Unit)
        = replaceOne(filter, replacement, UpdateOptions(), callback)

/**
 * Update a single document in the collection according to the specified arguments.
 *
 * @param filter   a document describing the query filter
 * @param update   a document describing the update. The update to apply must include only update operators.
 * @param callback the callback passed the result of the update one operation
 *
 * @throws com.mongodb.MongoWriteException        returned via the callback
 * @throws com.mongodb.MongoWriteConcernException returned via the callback
 * @throws com.mongodb.MongoException             returned via the callback
 */
fun <T> MongoCollection<T>.updateOne(filter: String, update: String, callback: (UpdateResult?, Throwable?) -> Unit)
        = updateOne(filter, update, UpdateOptions(), callback)

/**
 * Update a single document in the collection according to the specified arguments.
 *
 * @param filter   a document describing the query filter
 * @param update   a document describing the update. The update to apply must include only update operators.
 * @param options  the options to apply to the update operation
 * @param callback the callback passed the result of the update one operation
 *
 * @throws com.mongodb.MongoWriteException        returned via the callback
 * @throws com.mongodb.MongoWriteConcernException returned via the callback
 * @throws com.mongodb.MongoException             returned via the callback
 */
fun <T> MongoCollection<T>.updateOne(filter: String, update: String, options: UpdateOptions, callback: (UpdateResult?, Throwable?) -> Unit)
        = updateOne(toBson(filter), toBson(update), options, callback)

/**
 * Update a single document in the collection according to the specified arguments.
 *
 * @param filter   a document describing the query filter
 * @param update   the update object
 * @param callback the callback passed the result of the update one operation
 *
 * @throws com.mongodb.MongoWriteException        returned via the callback
 * @throws com.mongodb.MongoWriteConcernException returned via the callback
 * @throws com.mongodb.MongoException             returned via the callback
 */
fun <T> MongoCollection<T>.updateOne(filter: String, update: Any, callback: (UpdateResult?, Throwable?) -> Unit)
        = updateOne(filter, setModifier(update), UpdateOptions(), callback)

/**
 * Update a single document in the collection according to the specified arguments.
 *
 * @param target  the update object - must have an non null id
 * @param callback  the callback passed the result of the update one operation
 *
 * @throws com.mongodb.MongoWriteException        returned via the callback
 * @throws com.mongodb.MongoWriteConcernException returned via the callback
 * @throws com.mongodb.MongoException             returned via the callback
 */
inline fun <reified T : Any> MongoCollection<T>.updateOne(target: T, noinline callback: (UpdateResult?, Throwable?) -> Unit) {
    return updateOneById(extractId(target, T::class), target, callback)
}

/**
 * Update a single document in the collection according to the specified arguments.
 *
 * @param id        the object id
 * @param update    a document describing the update. The update to apply must include only update operators.
 * @param callback  the callback passed the result of the update one operation
 *
 * @throws com.mongodb.MongoWriteException        returned via the callback
 * @throws com.mongodb.MongoWriteConcernException returned via the callback
 * @throws com.mongodb.MongoException             returned via the callback
 */
fun <T> MongoCollection<T>.updateOneById(id: Any, update: String, callback: (UpdateResult?, Throwable?) -> Unit)
        = updateOneById(id, update, UpdateOptions(), callback)

/**
 * Update a single document in the collection according to the specified arguments.
 *
 * @param id        the object id
 * @param update    a document describing the update. The update to apply must include only update operators.
 * @param options  the options to apply to the update operation
 * @param callback  the callback passed the result of the update one operation
 *
 * @throws com.mongodb.MongoWriteException        returned via the callback
 * @throws com.mongodb.MongoWriteConcernException returned via the callback
 * @throws com.mongodb.MongoException             returned via the callback
 */
fun <T> MongoCollection<T>.updateOneById(id: Any, update: String, options: UpdateOptions, callback: (UpdateResult?, Throwable?) -> Unit)
        = updateOne(idFilterQuery(id), update, options, callback)

/**
 * Update a single document in the collection according to the specified arguments.
 *
 * @param id        the object id
 * @param update    the update object
 * @param callback  the callback passed the result of the update one operation
 *
 * @throws com.mongodb.MongoWriteException        returned via the callback
 * @throws com.mongodb.MongoWriteConcernException returned via the callback
 * @throws com.mongodb.MongoException             returned via the callback
 */
fun <T> MongoCollection<T>.updateOneById(id: Any, update: Any, callback: (UpdateResult?, Throwable?) -> Unit)
        = updateOneById(id, update, UpdateOptions(), callback)

/**
 * Update a single document in the collection according to the specified arguments.
 *
 * @param id        the object id
 * @param update    the update object
 * @param options  the options to apply to the update operation
 * @param callback  the callback passed the result of the update one operation
 *
 * @throws com.mongodb.MongoWriteException        returned via the callback
 * @throws com.mongodb.MongoWriteConcernException returned via the callback
 * @throws com.mongodb.MongoException             returned via the callback
 */
fun <T> MongoCollection<T>.updateOneById(id: Any, update: Any, options: UpdateOptions, callback: (UpdateResult?, Throwable?) -> Unit)
        =
        //strange but update can be a String
        if (update is String) {
            updateOneById(idFilterQuery(id), update, options, callback)
        } else {
            updateOne(idFilterQuery(id), setModifier(update), options, callback)
        }

/**
 * Update all documents in the collection according to the specified arguments.
 *
 * @param filter   a document describing the query filter
 * @param update   a document describing the update. The update to apply must include only update operators.
 * @param options  the options to apply to the update operation
 * @param callback the callback passed the result of the update one operation
 *
 * @throws com.mongodb.MongoWriteException        returned via the callback
 * @throws com.mongodb.MongoWriteConcernException returned via the callback
 * @throws com.mongodb.MongoException             returned via the callback
 */
fun <T> MongoCollection<T>.updateMany(filter: String, update: String, options: UpdateOptions, callback: (UpdateResult?, Throwable?) -> Unit)
        = updateMany(toBson(filter), toBson(update), options, callback)

/**
 * Update all documents in the collection according to the specified arguments.
 *
 * @param filter   a document describing the query filter
 * @param update   a document describing the update. The update to apply must include only update operators.
 * @param callback the callback passed the result of the update one operation
 *
 * @throws com.mongodb.MongoWriteException        returned via the callback
 * @throws com.mongodb.MongoWriteConcernException returned via the callback
 * @throws com.mongodb.MongoException             returned via the callback
 */
fun <T> MongoCollection<T>.updateMany(filter: String, update: String, callback: (UpdateResult?, Throwable?) -> Unit)
        = updateMany(filter, update, UpdateOptions(), callback)

/**
 * Atomically find a document and remove it.
 *
 * @param filter   the query filter to find the document with
 * @param options  the options to apply to the operation
 * @param callback the callback passed the document that was removed.  If no documents matched the query filter, then null will be returned
 */
fun <T> MongoCollection<T>.findOneAndDelete(filter: String, options: FindOneAndDeleteOptions, callback: (T?, Throwable?) -> Unit)
        = findOneAndDelete(toBson(filter), options, callback)

/**
 * Atomically find a document and remove it.
 *
 * @param filter   the query filter to find the document with
 * @param callback the callback passed the document that was removed.  If no documents matched the query filter, then null will be returned
 */
fun <T> MongoCollection<T>.findOneAndDelete(filter: String, callback: (T?, Throwable?) -> Unit)
        = findOneAndDelete(filter, FindOneAndDeleteOptions(), callback)

/**
 * Atomically find a document and replace it.
 *
 * @param filter      the query filter to apply the the replace operation
 * @param replacement the replacement document
 * @param options     the options to apply to the operation
 *
 * @param callback    the callback passed the document that was replaced.  Depending on the value of the `returnDocument`
 *                    property, this will either be the document as it was before the update or as it is after the update.  If no
 *                    documents matched the query filter, then null will be returned
 */
fun <T> MongoCollection<T>.findOneAndReplace(filter: String, replacement: T, options: FindOneAndReplaceOptions, callback: (T?, Throwable?) -> Unit)
        = findOneAndReplace(toBson(filter), replacement, options, callback)

/**
 * Atomically find a document and replace it.
 *
 * @param filter      the query filter to apply the the replace operation
 * @param replacement the replacement document
 *
 * @param callback    the callback passed the document that was replaced.  Depending on the value of the `returnDocument`
 *                    property, this will either be the document as it was before the update or as it is after the update.  If no
 *                    documents matched the query filter, then null will be returned
 */
fun <T> MongoCollection<T>.findOneAndReplace(filter: String, replacement: T, callback: (T?, Throwable?) -> Unit)
        = findOneAndReplace(filter, replacement, FindOneAndReplaceOptions(), callback)

/**
 * Atomically find a document and update it.
 *
 * @param filter   a document describing the query filter
 * @param update   a document describing the update. The update to apply must include only update operators.
 * @param options  the options to apply to the operation
 *
 * @param callback the callback passed the document that was updated.  Depending on the value of the `returnOriginal` property,
 *                 this will either be the document as it was before the update or as it is after the update.  If no documents matched
 *                  the query filter, then null will be returned
 */
fun <T> MongoCollection<T>.findOneAndUpdate(filter: String, update: String, options: FindOneAndUpdateOptions, callback: (T?, Throwable?) -> Unit)
        = findOneAndUpdate(toBson(filter), toBson(update), options, callback)

/**
 * Atomically find a document and update it.
 *
 * @param filter   a document describing the query filter
 * @param update   a document describing the update. The update to apply must include only update operators.
 *
 * @param callback the callback passed the document that was updated.  Depending on the value of the `returnOriginal` property,
 *                 this will either be the document as it was before the update or as it is after the update.  If no documents matched
 *                  the query filter, then null will be returned
 */
fun <T> MongoCollection<T>.findOneAndUpdate(filter: String, update: String, callback: (T?, Throwable?) -> Unit)
        = findOneAndUpdate(filter, update, FindOneAndUpdateOptions(), callback)

/**
 * Creates an index.  If successful, the callback will be executed with the name of the created index as the result.

 * @param key      an object describing the index key(s)
 * @param callback the callback that is completed once the index has been created
 */
fun <T> MongoCollection<T>.createIndex(key: String, callback: (String?, Throwable?) -> Unit)
        = createIndex(key, IndexOptions(), callback)

/**
 * Creates an index.  If successful, the callback will be executed with the name of the created index as the result.

 * @param key      an object describing the index key(s)
 * @param options  the options for the index
 * @param callback the callback that is completed once the index has been created
 */
fun <T> MongoCollection<T>.createIndex(key: String, options: IndexOptions, callback: (String?, Throwable?) -> Unit)
        = createIndex(toBson(key), options, callback)


/**
 * Get all the indexes in this collection.

 * @param <TResult>   the target document type of the iterable.
 * @return the list indexes iterable interface
 */
inline fun <reified TResult : Any> MongoCollection<*>.listIndexes(): ListIndexesIterable<TResult>
        = listIndexes(TResult::class.java)

/**
 * Drops the index given the keys used to create it.

 * @param keys the keys of the index to remove
 * @param callback  the callback that is completed once the index has been dropped
 */
fun <T> MongoCollection<T>.dropIndex(keys: String, callback: (Void?, Throwable?) -> Unit)
        = dropIndex(toBson(keys), callback)


/**
 * Executes a mix of inserts, updates, replaces, and deletes.

 * @param requests the writes to execute
 * @param callback the callback passed the result of the bulk write
 */
inline fun <reified T : Any> MongoCollection<T>.bulkWrite(vararg requests: String, noinline callback: (BulkWriteResult?, Throwable?) -> Unit)
        = withDocumentClass<BsonDocument>().bulkWrite(toWriteModel(requests, codecRegistry, T::class), BulkWriteOptions(), callback)

/**
 * Executes a mix of inserts, updates, replaces, and deletes.

 * @param requests the writes to execute
 * @param options  the options to apply to the bulk write operation
 * @param callback the callback passed the result of the bulk write
 */
inline fun <reified T : Any> MongoCollection<T>.bulkWrite(vararg requests: String, options: BulkWriteOptions, noinline callback: (BulkWriteResult?, Throwable?) -> Unit)
        = withDocumentClass<BsonDocument>().bulkWrite(toWriteModel(requests, codecRegistry, T::class), options, callback)


//*******
//IndexModel extension methods
//*******

/**
 * Construct an instance with the given keys and options.
 *
 * @param keys the index keys
 * @param options the index options
 */
fun IndexModel.IndexModel(keys: String, options: IndexOptions = IndexOptions()): IndexModel
        = IndexModel(toBson(keys), options)

//*******
//DistinctIterable extension methods
//*******

/**
 * Sets the query filter to apply to the query.
 *
 * @param filter the filter, which may be null
 * @return this
 */
fun <T> DistinctIterable<T>.filter(filter: String): DistinctIterable<T>
        = filter(toBson(filter))

//*******
//FindIterable extension methods
//*******

/**
 * Sets the query filter to apply to the query.
 *
 * @param filter the filter, which may be null
 * @return this
 */
fun <T> FindIterable<T>.filter(filter: String): FindIterable<T>
        = filter(toBson(filter))

/**
 * Sets the query modifiers to apply to this operation.
 *
 * @param modifiers the query modifiers to apply
 * @return this
 */
fun <T> FindIterable<T>.modifiers(modifiers: String): FindIterable<T>
        = modifiers(toBson(modifiers))

/**
 * Sets a document describing the fields to return for all matching documents.
 *
 * @param projection the project document
 * @return this
 */
fun <T> FindIterable<T>.projection(projection: String): FindIterable<T>
        = projection(toBson(projection))

/**
 * Sets the sort criteria to apply to the query.
 *
 * @param sort the sort criteria
 * @return this
 */
fun <T> FindIterable<T>.sort(sort: String): FindIterable<T>
        = sort(toBson(sort))

//*******
//MapReduceIterable extension methods
//*******

/**
 * Sets the global variables that are accessible in the map, reduce and finalize functions.
 *
 * @param scope the global variables that are accessible in the map, reduce and finalize functions.
 * @return this
 */
fun <T> MapReduceIterable<T>.scope(scope: String): MapReduceIterable<T>
        = scope(toBson(scope))

/**
 * Sets the sort criteria to apply to the query.
 *
 * @param sort the sort criteria, which may be null
 * @return this
 */
fun <T> MapReduceIterable<T>.sort(sort: String): MapReduceIterable<T>
        = sort(toBson(sort))

/**
 * Sets the query filter to apply to the query.
 *
 * @param filter the filter to apply to the query
 * @return this
 */
fun <T> MapReduceIterable<T>.filter(filter: String): MapReduceIterable<T>
        = filter(toBson(filter))

//*******
//MongoIterable extension methods
//*******

/**
 * Iterates over all the documents, adding each to the given target.
 *
 * @param target   the collection to insert into
 * @param callback a callback that will be passed the target containing all documents
 */
fun <TResult> MongoIterable<TResult>.toList(callback: (List<TResult>?, Throwable?) -> Unit)
        = into(mutableListOf<TResult>(), callback)

//*******
//json extension property
//*******

/**
 * Get the extended json representation of this object
 *
 * See [Mongo extended json format](https://docs.mongodb.com/manual/reference/mongodb-extended-json) for details
 */
val Any.json: String
    get() = KMongoUtil.toExtendedJson(this)


/**
 * Format this string to remove space(s) between $ and next char
 */
fun String.formatJson(): String
        = KMongoUtil.formatJson(this)

