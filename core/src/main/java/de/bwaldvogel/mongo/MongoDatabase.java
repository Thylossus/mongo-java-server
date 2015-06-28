package de.bwaldvogel.mongo;

import io.netty.channel.Channel;

import org.bson.BSONObject;

import de.bwaldvogel.mongo.exception.MongoServerException;
import de.bwaldvogel.mongo.wire.message.MongoDelete;
import de.bwaldvogel.mongo.wire.message.MongoInsert;
import de.bwaldvogel.mongo.wire.message.MongoQuery;
import de.bwaldvogel.mongo.wire.message.MongoUpdate;

public interface MongoDatabase {

    String getDatabaseName();

    void handleClose(Channel channel);

    BSONObject handleCommand(Channel channel, String command, BSONObject query) throws MongoServerException;

    Iterable<BSONObject> handleQuery(MongoQuery query) throws MongoServerException;

    void handleInsert(MongoInsert insert) throws MongoServerException;

    void handleDelete(MongoDelete delete) throws MongoServerException;

    void handleUpdate(MongoUpdate update) throws MongoServerException;

    boolean isEmpty();

    MongoCollection<?> resolveCollection(String collectionName, boolean throwIfNotFound) throws MongoServerException;

    void drop() throws MongoServerException;

    void dropCollection(String collectionName) throws MongoServerException;

    void moveCollection(MongoDatabase oldDatabase, MongoCollection<?> collection, String newCollectionName)
            throws MongoServerException;

    MongoCollection<?> deregisterCollection(String collectionName) throws MongoServerException;

}
