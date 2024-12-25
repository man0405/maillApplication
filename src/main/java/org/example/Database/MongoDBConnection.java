package org.example.Database;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoDBConnection {

    private  static final  String CONNECTION_STRING = "mongodb+srv://admin:c2fs8vzN5QNNYK9T@express-test.oqp8nev.mongodb.net/?retryWrites=true&w=majority&appName=express-test";
    private static MongoClient mongoClient = null;
    private static MongoDatabase database = null;

    public static MongoDatabase getDatabase() {
        if (database == null) {
            mongoClient = MongoClients.create(CONNECTION_STRING);
            database = mongoClient.getDatabase("networkfinal"); // Tên database
        }
        return database;
    }

    public static void closeConnection() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}
