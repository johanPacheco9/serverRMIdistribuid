package distribuidos.documentconverters;

import distribuidos.documentconverter.interfaces.Document;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DocumentConverterS extends UnicastRemoteObject implements IdocumentService {

    private List<IdocumentService> nodes;
    private static final Logger logger = Logger.getLogger(DocumentConverterS.class.getName());
    private final ExecutorService executor;

    // Constructor que exporta el objeto remoto
    public DocumentConverterS() throws RemoteException {
        super();
        nodes = new ArrayList<>();
        executor = Executors.newFixedThreadPool(5);
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
    public List<byte[]> distributeConversion(List<Document> documents) throws RemoteException {
        System.out.println("📥 Servidor RMI: Recibiendo " + documents.size() + " documentos.");
        logger.info("Iniciando distribución de documentos en paralelo.");

        List<IdocumentService> availableNodes = getAvailableNodes();

        if (availableNodes.isEmpty()) {
            String errorMsg = "No hay nodos disponibles para procesar los documentos.";
            logger.severe(errorMsg);
            throw new RemoteException(errorMsg);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(availableNodes.size());
        List<byte[]> resultList = Collections.synchronizedList(new ArrayList<>());

        int numDocumentos = documents.size();
        int numNodos = availableNodes.size();
        int documentosPorNodo = (int) Math.ceil((double) numDocumentos / numNodos);
        long userId = 1; // ID del usuario

        for (int i = 0; i < numNodos; i++) {
            int start = i * documentosPorNodo;
            int end = Math.min(start + documentosPorNodo, numDocumentos);

            if (start >= numDocumentos) break; // Evita asignar nodos sin documentos

            List<Document> subList = documents.subList(start, end);
            IdocumentService node = availableNodes.get(i);

            executorService.execute(() -> {
                try (Connection conn = new DBConnection().getConnection()) { // Abre la conexión dentro del hilo
                    for (Document doc : subList) {
                        try {
                            Inserts.registrarDocumento(conn, doc.getName(), doc.getPath(), userId);
                            logger.info("📤 Enviando documento " + doc.getName() + " al nodo " + node);

                            List<byte[]> convertedFiles = node.convertToPDF(Collections.singletonList(doc));


                            synchronized (resultList) {
                                resultList.addAll(convertedFiles);
                            }

                        } catch (Exception ex) {
                            logger.log(Level.SEVERE, "❌ Error procesando documento: " + doc.getName(), ex);
                        }
                    }
                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, "❌ Error en la base de datos: ", ex);
                }
            });
        }

        executorService.shutdown();
        try {
            executorService.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "⚠️ Error esperando la terminación del pool de hilos.", e);
        }
        System.out.println("📤 Enviando respuesta con " + resultList.size() + " archivos PDF.");
        
        return resultList;
    }

    public void registerNodes() throws RemoteException {
        try {
            String[] nodeNames = {
                "rmi://192.168.1.8:8087/documentService", 
                "rmi://192.168.1.6:8087/node1/documentService"
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
    public boolean isNodeAvailable() throws RemoteException {
        logger.info("Comprobando disponibilidad del nodo.");
        return true; // Simulando que el nodo siempre está disponible, puedes agregar la lógica real
    }

    @Override
    public List<byte[]> convertToPDF(List<Document> documents) throws RemoteException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
