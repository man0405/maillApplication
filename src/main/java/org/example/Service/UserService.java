package org.example.Service;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.example.Database.MongoDBConnection;
import org.mindrot.jbcrypt.BCrypt;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.or;

public class UserService {
    private MongoCollection<Document> usersCollection;

    public UserService() {
        MongoDatabase database = MongoDBConnection.getDatabase();
        usersCollection = database.getCollection("users");
    }

    // Phương thức đăng ký người dùng
    public boolean registerUser(String email, String password) {
        // Mã hóa mật khẩu trước khi lưu
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        Document user = new Document("email", email)
                .append("password", hashedPassword)
                .append("created_at", new java.util.Date());

        try {
            usersCollection.insertOne(user);
            return true;
        } catch (Exception e) {
            // Có thể email hoặc email đã tồn tại
            e.printStackTrace();
            return false;
        }
    }

    // Phương thức xác thực người dùng
    public boolean authenticateUser(String email, String password) {
        Document user = usersCollection.find(eq("email", email)).first();
        if (user != null) {
            String storedHashedPassword = user.getString("password");
            return BCrypt.checkpw(password, storedHashedPassword);
        }
        return false;
    }

    // Lấy email của người dùng
    public String getUserEmail(String email) {
        Document user = usersCollection.find(eq("email", email)).first();
        if (user != null) {
            return user.getString("email");
        }
        return null;
    }

    // Kiểm tra xem user hoặc email đã tồn tại chưa
    public boolean userExists(String email) {
        Document user = usersCollection.find(or(eq("email", email), eq("email", email))).first();
        return user != null;
    }
}
