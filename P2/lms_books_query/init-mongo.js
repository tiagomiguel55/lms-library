// Initialize MongoDB database and user for the query service
db = db.getSiblingDB('query_books_dev');

// Create user with read/write permissions
db.createUser({
    user: 'lmsuser',
    pwd: 'lmspass',
    roles: [
        {
            role: 'readWrite',
            db: 'query_books_dev'
        }
    ]
});

// Create collections with indexes
db.createCollection('books');
db.createCollection('authors');
db.createCollection('genres');
db.createCollection('pending_book_events');
db.createCollection('photos');
db.createCollection('forbidden_names');

// Create indexes for better query performance
db.books.createIndex({ "isbn.isbn": 1 }, { unique: true });
db.books.createIndex({ "title.title": 1 });
db.books.createIndex({ "genre.genre": 1 });
db.books.createIndex({ "authors.authorNumber": 1 });
db.books.createIndex({ "authors.name.name": 1 });

db.authors.createIndex({ "authorNumber": 1 }, { unique: true });
db.authors.createIndex({ "name.name": 1 });

db.genres.createIndex({ "genre": 1 }, { unique: true });

db.pending_book_events.createIndex({ "bookId": 1 }, { unique: true });
db.pending_book_events.createIndex({ "genreName": 1 });

db.forbidden_names.createIndex({ "forbiddenName": 1 });

print('MongoDB initialization complete for query_books_dev');
services:
    mongodb_query_dev:
        container_name: mongodb_query_dev
        image: mongo:7.0
        restart: unless-stopped
        ports:
            - "27017:27017"
        environment:
            MONGO_INITDB_ROOT_USERNAME: admin
            MONGO_INITDB_ROOT_PASSWORD: admin
            MONGO_INITDB_DATABASE: query_books_dev
        volumes:
            - mongodb_query_volume:/data/db
            - ./init-mongo.js:/docker-entrypoint-initdb.d/init-mongo.js:ro
        networks:
            - lms_network
        healthcheck:
            test: ["CMD", "mongosh", "--eval", "db.adminCommand('ping')"]
            interval: 10s
            retries: 5
            timeout: 5s

    rabbitmq_dev:
        container_name: rabbitmq_dev
        image: rabbitmq:3-management
        restart: unless-stopped
        ports:
            - "5672:5672"
            - "15672:15672"
        environment:
            RABBITMQ_DEFAULT_USER: guest
            RABBITMQ_DEFAULT_PASS: guest
        networks:
            - lms_network
        healthcheck:
            test: ["CMD", "rabbitmqctl", "status"]
            interval: 10s
            retries: 5
            timeout: 5s

networks:
    lms_network:
        name: lms_network
        driver: bridge

volumes:
    mongodb_query_volume:

