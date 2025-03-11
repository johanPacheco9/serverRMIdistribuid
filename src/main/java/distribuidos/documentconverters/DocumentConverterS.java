package distribuidos.documentconverters;

import distribuidos.documentconverter.interfaces.IdocumentService;
import java.io.Serializable;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DocumentConverterS extends UnicastRemoteObject implements IdocumentService {

    private List<IdocumentService> nodes;
    private static final Logger logger = Logger.getLogger(DocumentConverterS.class.getName());

    // Constructor que exporta el objeto remoto
    public DocumentConverterS() throws RemoteException {
        super();
        nodes = new ArrayList<>();
        logger.info("Servidor de conversión de documentos iniciado.");
    }

    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.createRegistry(8086);
            DocumentConverterS server = new DocumentConverterS();

            Naming.rebind("rmi://192.168.1.6:8086/documentServer", server);
            logger.info("Servidor RMI iniciado en rmi://192.168.1.6:8086/documentServer");

            server.registerNodes();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error en el servidor: ", e);
        }
    }

 @Override
public List<byte[]> distributeConversion(List<byte[]> documents) throws RemoteException {
    System.out.println("Servidor RMI: Recibiendo " + documents.size() + " documentos.");
    logger.info("Iniciando distribución de documentos para conversión.");

    // Obtener nodos disponibles
    List<IdocumentService> availableNodes = getAvailableNodes();

    if (availableNodes.isEmpty()) {
        String errorMsg = "No hay nodos disponibles para procesar los documentos.";
        logger.severe(errorMsg);
        throw new RemoteException(errorMsg);
    }

    List<byte[]> allConvertedDocs = new ArrayList<>();

    try (Connection connection = new DBConnection().getConnection()) {
        // Verifica o registra el nodo, similar a cómo lo hacías antes con `insertarNodoSiNoExiste`
        int nodeId = 1;  // Establece el ID del nodo según tu caso
        long userId = 1;  // ID del usuario


        for (int i = 0; i < documents.size(); i++) {
            byte[] document = documents.get(i);

            // Registrar el documento en la base de datos
            int documentId = Inserts.registrarDocumento(connection, "document_" + i, "/path/to/document_" + i, userId); // Ajusta el path y userId según tu caso

            // Seleccionar un nodo disponible
            IdocumentService node = availableNodes.get(i % availableNodes.size());
            
            // Registrar qué nodo se está utilizando
            logger.info("Distribuyendo documento " + i + " al nodo: " + node);
            System.out.println("Distribuyendo documento " + i + " al nodo: " + node);

            // Registrar la conversión antes de llamar al nodo
            int conversionId = Inserts.registrarLote(connection, nodeId, userId, documentId); // Asumiendo nodeId = 1 por ahora

            // Llamar al nodo para realizar la conversión
            List<byte[]> convertedDocs = node.convertToPDF(Collections.singletonList(document));
            allConvertedDocs.addAll(convertedDocs);

            // Actualizar la base de datos con el tiempo de conversión
            long elapsedTime = 2000; // Establece el tiempo real de conversión, o calcularlo
            Inserts.actualizarLote(connection, conversionId, elapsedTime);

            // Registrar que el documento fue procesado por el nodo
            logger.info("Documento " + i + " convertido por el nodo: " + node);
            System.out.println("Documento " + i + " convertido por el nodo: " + node);
        }
    } catch (SQLException ex) {
        logger.log(Level.SEVERE, "Error en la base de datos: ", ex);
    }

    logger.info("Distribución de documentos completada.");
    return allConvertedDocs;
}




    public void registerNodes() throws RemoteException {
        try {
            String[] nodeNames = {
                "rmi://192.168.1.8:8087/documentService", 
                "rmi://192.168.1.15:8086/documentService"
            };

            for (String nodeName : nodeNames) {
                try {
                    IdocumentService node = (IdocumentService) Naming.lookup(nodeName);
                    nodes.add(node);
                    logger.info("Nodo registrado: " + nodeName);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error al registrar el nodo: " + nodeName, e);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error general al registrar nodos: ", e);
        }
    }

    // Obtener los nodos disponibles
    public List<IdocumentService> getAvailableNodes() throws RemoteException {
    logger.info("Verificando disponibilidad de nodos.");
    List<IdocumentService> availableNodes = new ArrayList<>();
    for (IdocumentService node : nodes) {
        try {
            if (node.isNodeAvailable()) {
                availableNodes.add(node);
                logger.info("Nodo disponible: " + node);
            }
        } catch (RemoteException e) {
            logger.warning("Nodo no disponible: " + node);
        }
    }
    return availableNodes;
}

    @Override
    public List<byte[]> convertToPDF(List<byte[]> documents) throws RemoteException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isNodeAvailable() throws RemoteException {
        logger.info("Comprobando disponibilidad del nodo.");
        return true; // Simulando que el nodo siempre está disponible, puedes agregar la lógica real
    }
}
