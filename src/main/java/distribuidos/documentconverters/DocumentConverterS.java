package distribuidos.documentconverters;

import distribuidos.documentconverter.interfaces.IdocumentService;
import java.io.Serializable;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class DocumentConverterS extends UnicastRemoteObject implements IdocumentService {

    private List<IdocumentService> nodes;

    // Constructor que exporta el objeto remoto
    public DocumentConverterS() throws RemoteException {
        super();
        nodes = new ArrayList<>();
    }

    public static void main(String[] args) {
        try {
          
            Registry registry = LocateRegistry.createRegistry(8086);
            DocumentConverterS server = new DocumentConverterS();

            Naming.rebind("rmi://192.168.1.6:8086/documentServer", server);
            System.out.println("Servidor RMI iniciado...");

            server.registerNodes();
        } catch (Exception e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }

    @Override
    public List<byte[]> distributeConversion(List<byte[]> documents) throws RemoteException {
        List<IdocumentService> availableNodes = getAvailableNodes();

        if (availableNodes.isEmpty()) {
            throw new RemoteException("No hay nodos disponibles para procesar los documentos.");
        }

        List<byte[]> allConvertedDocs = new ArrayList<>();
        
        for (int i = 0; i < documents.size(); i++) {
            IdocumentService node = availableNodes.get(i % availableNodes.size());
            List<byte[]> convertedDocs = node.convertToPDF(List.of(documents.get(i)));
            allConvertedDocs.addAll(convertedDocs);
        }

        return allConvertedDocs;
    }

    public void registerNodes() throws RemoteException {
        try {
            
            String[] nodeNames = {"rmi://192.168.1.8:8087/documentService", "rmi://192.168.1.10:8086/documentService"};
            for (String nodeName : nodeNames) {
                IdocumentService node = (IdocumentService) Naming.lookup(nodeName);
                nodes.add(node);
                System.out.println("Nodo registrado: " + nodeName);
            }
        } catch (Exception e) {
            System.err.println("Error al registrar nodos: " + e.getMessage());
        }
    }

    // Obtener los nodos disponibles
    public List<IdocumentService> getAvailableNodes() throws RemoteException {
        List<IdocumentService> availableNodes = new ArrayList<>();
        for (IdocumentService node : nodes) {
            if (node.isNodeAvailable()) {
                availableNodes.add(node);
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
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
