package distribuidos.documentconverters;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;

public class Inserts {
    
    public static int registrarDocumento(Connection connection, String nombreDocumento, String filePath, long userId) throws SQLException {
        String sql = "INSERT INTO Documents (userId, file_name, file_path) VALUES (?, ?, ?)"; 
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, userId); // Asumiendo que tienes el userId
            stmt.setString(2, nombreDocumento);
            stmt.setString(3, filePath);
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1); // Retorna el ID del documento insertado
                } else {
                    throw new SQLException("Error al obtener el ID del documento");
                }
            }
        }
    }

    public static int registrarConversion(Connection connection, long userId, int nodeId, int documentId) throws SQLException {
        String sql = "INSERT INTO Conversion (userId, nodeId, documentId) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, userId);
            stmt.setInt(2, nodeId);
            stmt.setInt(3, documentId);
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1); // Retorna el ID de la conversión
                } else {
                    throw new SQLException("Error al obtener el ID de la conversión");
                }
            }
        }
    }
    
    public boolean existeNodo(Connection conn, int nodeId) throws SQLException {
    String sql = "SELECT COUNT(*) FROM node WHERE id = ?";
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setInt(1, nodeId);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return rs.getInt(1) > 0;
        }
    }
    return false;
}

    
    public void insertarNodoSiNoExiste(Connection conn, int nodeId, String nombreNodo, String estado) throws SQLException {
    if (!existeNodo(conn, nodeId)) {
        String sql = "INSERT INTO node (id, nombre, estado) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, nodeId);
            stmt.setString(2, nombreNodo);
            stmt.setString(3, estado);
            stmt.executeUpdate();
        }
    }
    }
    
    public static int registrarLote(Connection connection, int nodeId, long userId, int documentId) throws SQLException {
    String sql = "INSERT INTO Conversion (nodeId, userId, documentId) VALUES (?, ?, ?)";
    try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        stmt.setInt(1, nodeId);
        stmt.setLong(2, userId);
        stmt.setInt(3, documentId); // Se añade documentId, ya que es requerido en la base de datos.
        stmt.executeUpdate();

        try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1); // Retorna el ID de la conversión
            } else {
                throw new SQLException("Error al obtener el ID de la conversión");
            }
        }
    }
}


    public static void actualizarLote(Connection connection, int conversionId, long elapsedTime) throws SQLException {
        String sql = "INSERT INTO Log (spent_time, ConversionId) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, elapsedTime);
            stmt.setInt(2, conversionId);
            stmt.executeUpdate();
        }
    }

    public static String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().substring(0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to generate hash", e);
        }
    }
}