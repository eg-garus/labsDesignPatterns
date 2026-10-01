package com.lab1.lab1;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class DatabaseInitializer {
    private Connection connection;
    private DataSource dataSource;

    public DatabaseInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void connect() throws SQLException {
        connection = dataSource.getConnection();
        createTables();
        findShortestTrack();
    }

    public void testQuery() throws SQLException {
        try(Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT 1")
        ) {
            if (resultSet.next()) {
                int value = resultSet.getInt(1);

                System.out.println(value);
            }
        }
    }

    public void createExecutorTable() throws SQLException {
        try(Statement statement = connection.createStatement()) {
            String createTableQuery = "CREATE TABLE IF NOT EXISTS executor(id SERIAL PRIMARY KEY, name VARCHAR(50) NOT NULL)";
            statement.executeUpdate(createTableQuery);
        }
    }

    public void createAlbumTable() throws SQLException {
        try(Statement statement = connection.createStatement()) {
            String createTableQuery = "CREATE TABLE IF NOT EXISTS album(id SERIAL PRIMARY KEY, name VARCHAR(50) NOT NULL, genre VARCHAR(50) NOT NULL, executor_id INTEGER NOT NULL, FOREIGN KEY (executor_id) REFERENCES executor(id))";
            statement.executeUpdate(createTableQuery);
        }
    }

    public void createTrackTable() throws SQLException {
        try(Statement statement = connection.createStatement()) {
            String createTableQuery = "CREATE TABLE IF NOT EXISTS track(id SERIAL PRIMARY KEY, name VARCHAR(50) NOT NULL, duration INTEGER NOT NULL, album_id INTEGER NOT NULL, FOREIGN KEY (album_id) REFERENCES album(id))";
            statement.executeUpdate(createTableQuery);
        }
    }

    public void createTables() throws SQLException {
        createExecutorTable();
        createAlbumTable();
        createTrackTable();
    }

    public void insertExecutor(String name) throws SQLException {
        try(PreparedStatement statement = connection.prepareStatement("INSERT INTO executor(name) VALUES(?)")) {
            statement.setString(1, name);
            statement.executeUpdate();
        }
    }

    public void insertExecutorInfo() throws SQLException {
        String[] executors = {
        "Лолита",
        "Queen",
        "Metallica",
        "Nirvana",
        "ABBA"
    };
        for (String executor : executors) {
            insertExecutor(executor);
        }
    }

    public void insertAlbum(String name, String genre, int executorId) throws SQLException {
        try(PreparedStatement statement = connection.prepareStatement("INSERT INTO album(name, genre, executor_id) VALUES(?, ?, ?)")) {
            statement.setString(1, name);
            statement.setString(2, genre);
            statement.setInt(3, executorId);
            statement.executeUpdate();
        }
    }

    public int getExecutorIdByName(String name) throws SQLException {
        try(PreparedStatement statement = connection.prepareStatement("SELECT id FROM executor WHERE name =?")) {
            statement.setString(1, name);
            try(ResultSet resultSet = statement.executeQuery()) {
            if(resultSet.next()) {
                return resultSet.getInt(1);
            }
            return -1;
            }
        }

    }

    public void insertAlbumInfo() throws SQLException {
        String[][] albums = {
            {"A Night at the Opera", "Rock", "Queen"},
            {"Master of Puppets", "Metal", "Metallica"},
            {"Nevermind", "Grunge", "Nirvana"},
            {"Voyage", "Pop", "ABBA"},
            {"Русское поле", "Pop", "Лолита"}
        };
        for (String[] album : albums) {
            insertAlbum(album[0], album[1], getExecutorIdByName(album[2]));
        }
    }

    public void insertTrack(String name, int duration, int albumId) throws SQLException {
        try(PreparedStatement statement = connection.prepareStatement("INSERT INTO track(name, duration, album_id) VALUES(?, ?, ?)")) {
            statement.setString(1, name);
            statement.setInt(2, duration);
            statement.setInt(3, albumId);
            statement.executeUpdate();
        }
    }

    public int getAlbumIdByName(String name) throws SQLException {
        try(PreparedStatement statement = connection.prepareStatement("SELECT id FROM album WHERE name =?")) {
            statement.setString(1, name);
            try(ResultSet resultSet = statement.executeQuery()) {
            if(resultSet.next()) {
                return resultSet.getInt(1);
            }
            return -1;
            }
        }

    }

    public void insertTrackInfo() throws SQLException {
        String[][] tracks = {
            {"Bohemian Rhapsody", "354", "A Night at the Opera"},
            {"Love of My Life", "217", "A Night at the Opera"},
            {"Master of Puppets", "515", "Master of Puppets"},
            {"Battery", "312", "Master of Puppets"},
            {"Smells Like Teen Spirit", "301", "Nevermind"},
            {"Come As You Are", "219", "Nevermind"},
            {"I Still Have Faith in You", "222", "Voyage"},
            {"Don't Shut Me Down", "224", "Voyage"},
            {"Конь", "210", "Русское поле"}
        };
        for (String[] track : tracks) {
            int albumId = getAlbumIdByName(track[2]);
            if(albumId != -1) {
                insertTrack(track[0], Integer.parseInt(track[1]), albumId);
            }
        }
    }

    public void insertData() throws SQLException {
        insertExecutorInfo();
        insertAlbumInfo();
        insertTrackInfo();
    }

    public void findShortestTrack() throws SQLException {
        try (
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(
                "SELECT a.name AS Альбом, t.name AS Композиция " +
                "FROM album AS a, track AS t " +
                "WHERE a.id = t.album_id " +
                "AND t.duration >= 5 " +
                "AND t.duration IN ( " +
                    "SELECT MIN(duration) " +
                    "FROM track " +
                    "WHERE a.id = album_id " +
                    "GROUP BY album_id " +
                    "HAVING COUNT(*) >= 1 " +
                ") " +
                "GROUP BY a.name, t.name"
            )
        ) {
            while (resultSet.next()) {
                String album = resultSet.getString("Альбом");
                String track = resultSet.getString("Композиция");

                System.out.println(
                    "Album: " + album + ", shortest track: " + track
                );
            }
        }
    }

    public void deleteAlbum(int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM album WHERE id = ?")) {

            statement.setInt(1, id);

            statement.executeUpdate();
        }
    }

    public void updateAlbum(int id, String name, String genre, String executorName) throws SQLException {
        int executorId = getExecutorIdByName(executorName);

        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE album " +
                "SET name = ?, genre = ?, executor_id = ? " +
                "WHERE id = ?")) {

            statement.setString(1, name);
            statement.setString(2, genre);
            statement.setInt(3, executorId);
            statement.setInt(4, id);

            statement.executeUpdate();
        }
    }

    public void testAlbumOperations() throws SQLException {
        insertAlbum("Test Album", "Rock", 1);

        //updateAlbum(6, "Updated Album", "Pop", "ABBA");

        //deleteAlbum(6);
    }
}